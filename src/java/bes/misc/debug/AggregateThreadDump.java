package bes.misc.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AggregateThreadDump
{

    static final Pattern TRACE = Pattern.compile("\\s*" +
                                                 "(java\\.lang\\.Thread\\.State: " +
                                                 "|- parking to wait for  <[0-9a-fx]+>" +
                                                 "|- waiting on <[0-9a-fx]+>" +
                                                 "|- locked <[0-9a-fx]+>" +
                                                 "|at " +
                                                 "| - " +
                                                 ").*");

    static final Pattern STATE = Pattern.compile("\\sjava\\.lang\\.Thread\\.State: ([A-Z_]+).*");

    static final Pattern ECLIPSE_THREAD = Pattern.compile("Thread 0x[a-z0-9]+");

    static final Pattern JDK8_THREAD = Pattern.compile("\\s*Thread ([0-9xa-z]+): \\(state = ([A-Z_]+)\\)");

    static final Pattern REPLACE = Pattern.compile("<[0-9a-fx]+>");

    // TODO: merge stack traces together
    public static void main(String[] args) throws IOException
    {
        if (args.length == 0)
        {
            fail();
        }
        File file = new File(args[0]);
        if (!file.exists())
        {
            System.out.print("File does not exist: " + file);
            fail();
        }

        Map<String, Map<String, Collection<String>>> stateToTraceToThread = new LinkedHashMap<>();
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String thread = null;
            String state = null;
            StringBuilder trace = new StringBuilder();
            for (String line = reader.readLine() ; line != null ; line = reader.readLine())
            {
                Matcher m = TRACE.matcher(line);
                if (!m.matches())
                {
                    if (trace.length() == 0 || ECLIPSE_THREAD.matcher(line).matches())
                    {
                        thread = line;
                    }
                    else if ((m = JDK8_THREAD.matcher(line)).matches())
                    {
                        thread = m.group(1);
                        state = m.group(2);
                    }
                    else
                    {
                        if (state == null)
                            state = "";
                        String fullTrace = trace.toString();
                        trace.setLength(0);
                        Map<String, Collection<String>> traceToThread = stateToTraceToThread.get(state);
                        if (traceToThread == null)
                            stateToTraceToThread.put(state, traceToThread = new LinkedHashMap<>());
                        Collection<String> threads = traceToThread.get(fullTrace);
                        if (threads == null)
                            traceToThread.put(fullTrace, threads = new TreeSet<>());
                        threads.add(thread);
                        thread = null;
                        state = null;
                    }
                }
                else
                {
                    m = REPLACE.matcher(line);
                    line = m.replaceAll("?");
                    m = STATE.matcher(line);
                    if (m.matches())
                        state = m.group(1);
                    trace.append(line);
                    trace.append("\n");
                }
            }
        }

        LinkedHashSet<String> states = new LinkedHashSet<>();
        states.addAll(Arrays.asList("RUNNABLE", "TIMED_WAITING", "WAITING"));
        states.addAll(stateToTraceToThread.keySet());
        for (String state : states)
        {
            if (!stateToTraceToThread.containsKey(state))
                continue;
            List<Map.Entry<String, Collection<String>>> traces = new ArrayList<>(stateToTraceToThread.get(state).entrySet());
            Collections.sort(traces, new Comparator<Map.Entry<String, Collection<String>>>()
            {
                public int compare(Map.Entry<String, Collection<String>> o1, Map.Entry<String, Collection<String>> o2)
                {
                    return o2.getValue().size() - o1.getValue().size();
                }
            });
            int runningTotalThreads = 0;
            for (Map.Entry<String, Collection<String>> e : traces)
            {
                System.out.println("\n======================\nTrace:");
                System.out.println(e.getKey());
                int threads = e.getValue().size();
                runningTotalThreads += threads;
                System.out.printf("Threads (%d/%d):\n", threads, runningTotalThreads);
                for (String thread : e.getValue())
                    System.out.printf("\t%s\n", thread);
            }
        }
    }

    private static void fail()
    {
        System.out.println("usage: aggrdump <dump>");
        System.exit(1);
    }

}
