/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package bes.misc.trial.timehorizon;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Test
{

    static long now = 32L << 30;
    static void parkLoop(long sleep)
    {
        now += ThreadLocalRandom.current().nextLong((long) (sleep * 0.95), (long)(sleep * 1.05));
    }

    /*
     * Start coalescing by sleeping if the moving average is < the requested window.
     * The actual time spent waiting to coalesce will be the min( window, moving average * 2)
     * The actual amount of time spent waiting can be greater then the window. For instance
     * observed time spent coalescing was 400 microseconds with the window set to 200 in one benchmark.
     */
    static class TimeHorizonMovingAverageCoalescingStrategy
    {
        // for now we'll just use 64ms per bucket; this can be made configurable, but results in ~1s for 16 samples
        private static final int INDEX_SHIFT = 26;
        private static final long BUCKET_INTERVAL = 1L << 26;
        private static final int BUCKET_COUNT = 16;
        private static final long INTERVAL = BUCKET_INTERVAL * BUCKET_COUNT;
        private static final long MEASURED_INTERVAL = BUCKET_INTERVAL * (BUCKET_COUNT - 1);

        // the minimum timestamp we will now accept updates for; only moves forwards, never backwards
        private long epoch = 0;
        // the buckets, each following on from epoch; the measurements run from ix(epoch) to ix(epoch - 1)
        // ix(epoch-1) is a partial result, that is never actually part of the calculation, and most updates
        // are expected to hit this bucket
        private final int samples[] = new int[BUCKET_COUNT];
        private long sum = 0;
        private final long maxCoalesceWindow;

        public TimeHorizonMovingAverageCoalescingStrategy(int maxCoalesceWindow)
        {
            this.maxCoalesceWindow = TimeUnit.MICROSECONDS.toNanos(maxCoalesceWindow);
            sum = 0;
        }

        public void logSample(long nanos)
        {
            long epoch = this.epoch;
            long delta = nanos - epoch;
            if (delta < 0)
                // have to simply ignore, but would be a bit crazy to get such reordering
                return;

            if (delta > INTERVAL)
                epoch = rollepoch(delta, epoch, nanos);

            int ix = ix(nanos);
            samples[ix]++;

            // if we've updated an old bucket, we need to update the sum to match
            if (ix != ix(epoch - 1))
                sum++;
        }

        private long averageGap()
        {
            if (sum == 0)
                return MEASURED_INTERVAL;
            return MEASURED_INTERVAL / sum;
        }

        public boolean maybeSleep(int messages)
        {
            // only sleep if we can expect to double the number of messages we're sending in the time interval
            long sleep = messages * averageGap();
            if (sleep > maxCoalesceWindow)
                return false;
            // assume we receive as many messages as we expect; apply the same logic to the future batch:
            // expect twice as many messages to consider sleeping for "another" interval; this basically translates
            // to doubling our sleep period until we exceed our max sleep window
            while (sleep * 2 < maxCoalesceWindow)
                sleep *= 2;
            parkLoop(sleep);
            return true;
        }

        // this sample extends past the end of the range we cover, so rollover
        private long rollepoch(long delta, long epoch, long nanos)
        {
            if (delta > 2 * INTERVAL)
            {
                // this sample is more than twice our interval ahead, so just clear our counters completely
                epoch = epoch(nanos);
                sum = 0;
                Arrays.fill(samples, 0);
            }
            else
            {
                // ix(epoch - 1) => last index; this is our partial result bucket, so we add this to the sum
                sum += samples[ix(epoch - 1)];
                // then we roll forwards, clearing buckets, until our interval covers the new sample time
                while (epoch + INTERVAL < nanos)
                {
                    int index = ix(epoch);
                    sum -= samples[index];
                    samples[index] = 0;
                    epoch += BUCKET_INTERVAL;
                }
            }
            // store the new epoch
            this.epoch = epoch;
            return epoch;
        }

        private long epoch(long latestNanos)
        {
            return (latestNanos - MEASURED_INTERVAL) & ~(BUCKET_INTERVAL - 1);
        }

        private int ix(long nanos)
        {
            return (int) ((nanos >>> INDEX_SHIFT) & (BUCKET_COUNT - 1));
        }
    }

    public static void main(String[] args)
    {
        TimeHorizonMovingAverageCoalescingStrategy strat = new TimeHorizonMovingAverageCoalescingStrategy(200);
        long avg = TimeUnit.MICROSECONDS.toNanos(250);
        long end = now + TimeUnit.MINUTES.toNanos(10);
        long ts = now;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        long nextlog = now + TimeUnit.MINUTES.toNanos(1);
        long[] buffer = new long[10000];
        while (now < end)
        {
            int ni = 0;
            while (ts <= now)
            {
                buffer[ni++] = ts;
                ts = rnd.nextLong(ts, ts + 2 * avg);
            }

            if (ni == 0)
            {
                now = ts;
                continue;
            }

            int i = 0;
            while (i < ni)
                strat.logSample(buffer[i++]);

            strat.maybeSleep(ni);

            while (ts <= now)
            {
                buffer[ni++] = ts;
                ts = rnd.nextLong(ts, ts + 2 * avg);
            }

            while (i < ni)
                strat.logSample(buffer[i++]);

            if (now >= nextlog)
            {
                System.out.printf("%d %d\n", TimeUnit.NANOSECONDS.toMicros(strat.averageGap()), ni);
                nextlog += TimeUnit.SECONDS.toNanos(1);;
            }
        }
    }
}
