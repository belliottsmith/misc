package bes.misc.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParseCassandraCompactionLog
{

    static final Pattern COMPACTING = Pattern.compile("INFO *\\[CompactionExecutor:([0-9]+).* Compacting \\[(.*)\\].*");
    static final Pattern COMPACTED = Pattern.compile("INFO *\\[CompactionExecutor:([0-9]+).* Compacted ([0-9]+) sstables.*");
    static final Pattern SSTABLEREADER = Pattern.compile("SSTableReader\\(path='[^)]*/([^/]+-ka-[0-9]+)-Data.db'\\)");

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

        Set<String> compacted = new HashSet<>();
        Map<String, String> inProgress = new HashMap<>();
        Map<String, Set<String>> inProgressPerCompactor = new HashMap<>();
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            for (String line = reader.readLine() ; line != null ; line = reader.readLine())
            {
                Matcher m;
                if ((m = COMPACTING.matcher(line)).matches())
                {
                    String compactor = m.group(1);
                    Set<String> sstables = new HashSet<>();
                    m = SSTABLEREADER.matcher(m.group(2));
                    while (m.find())
                        sstables.add(m.group(1).replaceAll("-tmp(link)?", ""));

                    boolean printed = false;
                    for (String sstable : sstables)
                    {
                        if (inProgress.containsKey(sstable) || compacted.contains(sstable))
                        {
                            if (!printed)
                            {
                                System.out.println(line);
                                printed = true;
                            }
                            System.out.printf("%s %s\n", sstable, compacted.contains(sstable)
                                                                  ? "has already been compacted"
                                                                  : "is already being compacted by " + inProgress.get(sstable));
                        }
                        else
                        {
                            inProgress.put(sstable, compactor);
                        }
                    }
                    inProgressPerCompactor.put(compactor, sstables);
                }
                else if ((m = COMPACTED.matcher(line)).matches())
                {
                    String compactor = m.group(1);
                    int count = Integer.parseInt(m.group(2));
                    Set<String> sstables = inProgressPerCompactor.remove(compactor);
                    if (sstables != null)
                    {
                        if (sstables.size() != count)
                        {
                            System.out.println(line);
                            System.out.printf("Incorrect compacted count; %d expected, %d reported\n", sstables.size(), count);
                        }
                        compacted.addAll(sstables);
                    }
                }
            }
        }
    }

    private static void fail()
    {
        System.out.println("usage: compaction <dump>");
        System.exit(1);
    }

}
