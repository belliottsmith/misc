package bes.misc.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseCassandraFlushLog
{

    static final Pattern FLUSHING = Pattern.compile(".* Enqueuing flush of (.*): ([0-9]+) \\(([0-9]+)%\\) on-heap, ([0-9]+) \\(([0-9]+)%\\) off-heap.*");
    static final Pattern FLUSHED = Pattern.compile(".* Completed flushing .*/(.*)-(.*)-[a-z]{2}-[0-9]+-Data.db \\(([0-9]+) bytes\\).*");

    static class Record
    {
        final String cf;
        final float onheap;
        final float offheap;

        Record(String cf, String onheap, String offheap)
        {
            this.cf = cf;
            this.onheap = Float.parseFloat(onheap);
            this.offheap = Float.parseFloat(offheap);
        }
    }
    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            fail();
        }
        for (String arg : args)
        {
            File file = new File(arg);
            if (!file.exists())
            {
                System.out.print("File does not exist: " + file);
                fail();
            }

            float onheap = 0f, offheap = 0f;
            Map<String, Queue<Record>> inProgress = new HashMap<>();
            {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                for (String line = reader.readLine() ; line != null ; line = reader.readLine())
                {
                    Matcher m;
                    if ((m = FLUSHING.matcher(line)).matches())
                    {
                        String cf = m.group(1);
                        Record record = new Record(cf, m.group(2), m.group(4));
                        inProgress.computeIfAbsent(cf, (x) -> new ArrayDeque<>()).add(record);
                        onheap += record.onheap;
                        offheap += record.offheap;
                        System.out.printf("%.0f, %.0f\n", onheap, offheap);
                    }
                    else if ((m = FLUSHED.matcher(line)).matches())
                    {
                        String cf = m.group(2);
                        Record record = inProgress.computeIfAbsent(cf, (x) -> new ArrayDeque<>()).poll();
                        if (record == null)
                            continue;
                        onheap -= record.onheap;
                        offheap -= record.offheap;
                        System.out.printf("%.0f, %.0f\n", onheap, offheap);
                    }
                }
            }
        }
    }

    private static void fail()
    {
        System.out.println("usage: flush <file1> <file2>");
        System.exit(1);
    }

}
