package bes.misc.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AggregateThreadDump
{

    static final Pattern TRACE = Pattern.compile("\\s*" +
                                                 "(java\\.lang\\.Thread\\.State: " +
                                                 "|- parking to wait for  <[0-9a-fx]+>" +
                                                 "|- locked <[0-9a-fx]+>" +
                                                 "|at " +
                                                 ").*");

    static final Pattern REPLACE = Pattern.compile("<[0-9a-fx]+>");

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

        Map<String, Collection<String>> traceToThread = new LinkedHashMap<>();
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String thread = null;
            StringBuffer trace = new StringBuffer();
            for (String line = reader.readLine() ; line != null ; line = reader.readLine())
            {
                Matcher m = TRACE.matcher(line);
                if (!m.matches())
                {
                    if (trace.length() == 0)
                    {
                        thread = line;
                    }
                    else
                    {
                        String fullTrace = trace.toString();
                        trace.setLength(0);
                        Collection<String> threads = traceToThread.get(fullTrace);
                        if (threads == null)
                            traceToThread.put(fullTrace, threads = new TreeSet<>());
                        threads.add(thread);
                        thread = null;
                    }
                }
                else
                {
                    m = REPLACE.matcher(line);
                    trace.append(m.replaceAll("?"));
                    trace.append("\n");
                }
            }
        }
        for (Map.Entry<String, Collection<String>> e : traceToThread.entrySet())
        {
            System.out.println("\n======================\nTrace:");
            System.out.println(e.getKey());
            System.out.printf("Threads:\n");
            for (String thread : e.getValue())
                System.out.printf("\t%s\n", thread);
        }
    }

    private static void fail()
    {
        System.out.println("usage: aggrdump <dump>");
        System.exit(1);
    }

}
