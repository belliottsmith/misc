#!/usr/bin/gawk -f

{
  if ($1 == "")
      next;
  ticketdiscussion = 0;
  total = 0;
  part = NF - 4;
  i = 4;
  watching = $2;
  while (i < NF)
  {
    val = $i
    pos = match(val, ":");
    num = substr(val, pos + 1);
    ticketdiscussion += num;
    total += num;
    i = i + 1;
  }
  i = 4;
  while (i < NF)
  {
    val = $i
    pos = match(val, ":");
    name = substr(val, 1, pos - 1);
    num = substr(val, pos + 1);
    if (assignee != 0 && name == $1)
    {
            total -= num;
	    counts[name]++;
	    sums[name] += total;
	    sumsq[name] += total * total;
    }
    else if (assignee == 0)
    {
	    counts[name]++;
	    sums[name] += num;
	    sumsq[name] += num * num;
    }
    if (assignee == 0 || name == $1)
    {
            watchers[name] += watching;
            watchersq[name] += watching * watching;
	    participants[name] += part;
	    participantsq[name] += part * part;
            discuss = ticketdiscussion - num;
	    discussion[name] += discuss;
	    discussionsq[name] += discuss * discuss;
    }
    i = i + 1;
  }
}
END {
  n = asorti(counts, sorted, "cmp_val_desc");  
  for (i = 1 ; i <= n ; i++)
  {
    name = sorted[i];
    count = counts[name];
    comment = sums[name] / count;
    commentsd = stdev(count, sums[name], sumsq[name]);
    part = participants[name] / count
    partsd = stdev(count, participants[name], participantsq[name]);
    discuss = discussion[name] / count;
    maxdiscuss = maxdiscussion[name];
    discusssd = stdev(count, discussion[name], discussionsq[name]);
    watch = watchers[name] / count;
    watchd = stdev(counts[name], watchers[name], watchersq[name]);
    if (mentions != 0) {
      printf("%30s: %4s tickets with %2.0f (avg %3.1f+/-%.1f) mentions and %4.0f (avg %3.1f+/-%.1f) mentions of others\n", name, count, sums[name], comment, commentsd, participants[name], part, partsd);
    } else if (assignee == 0) {
      printf("%30s: %4s tickets making %4.0f (avg %3.1f+/-%.1f) comments; with %4.0f (avg %3.1f+/-%.1f) people, %4.0f (avg %3.1f+/-%.1f) discussion and %4.0f (avg %3.1f+/-%.1f) watchers\n", name, count, sums[name], comment, commentsd, participants[name], part, partsd, discussion[name], discuss, discusssd, watchers[name], watch, watchd);
    } else {
      printf("%30s: %4s tickets receiving %4.0f (avg %3.1f+/-%.1f) comments from %4.0f (avg %3.1f+/-%.1f) people, with %4.0f (avg %3.1f+/-%.1f) watchers\n", name, count, sums[name], comment, commentsd, participants[name], part, partsd, watchers[name], watch, watchd);
    }
  }
}
function stdev(count, sum, sumsq)
{
  mean = sum / count;
  return sqrt((sumsq/count) - (mean * mean));
}
function cmp_val_desc(i1, v1, i2, v2)
{
     return (v2 - v1)
}

