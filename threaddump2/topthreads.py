#!/usr/bin/env python
# topthreads.py - takes the top and thread dump output from multidump.sh and produces a
# list of the top threads by average CPU consumption including Java thread names
# usage: topthreads.py [top file] [thread dump file]
import sys
import collections
import re

topfile = open(sys.argv[1])
dumpfile = open(sys.argv[2])

regex = re.compile(r'"([^"]*)".*nid=0x([0-9a-f]*)')
thread_names = {}
for line in dumpfile:
    match = regex.match(line)
    if match:
        name, pid = match.groups()
        thread_names[int(pid, 16)] = name

cpu_use = collections.defaultdict(list)
for line in topfile:
    if line[1:5].isdigit():
        fields=line.split()
        cpu_use[int(fields[0])].append(float(fields[8]))
#avg_cpu_use = {pid: sum(list)/len(list) for pid, list in cpu_use.iteritems()}
avg_cpu_use = {}
for pid, list in cpu_use.iteritems():
    avg_cpu_use[pid] = sum(list)/len(list)
sorted_cpu_use = sorted(avg_cpu_use.iteritems(), key=lambda x: x[1], reverse=True)

print 'PID   %CPU  Process'
print '===== ===== ======='
for pid, avg in sorted_cpu_use:
    if avg >= 5:
       print '{0:5d} {1:.2f} {2:s}'.format(pid, avg, thread_names.get(pid))
#EOF       
