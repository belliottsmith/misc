#!/usr/bin/gawk -f

{
  ticketdiscussion = 0;
  total = 0;
  part = NF - 3;
  i = 3;
  while (i < NF)
  {
    val = $i
    pos = match(val, ":");
    num = substr(val, pos + 1);
    ticketdiscussion += num;
    total += num;
    i = i + 1;
  }
  i = 3;
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
	    participants[name] += part;
	    participantsq[name] += part * part;
            discuss = ticketdiscussion - num
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
    commentsd = stdev(count, sums[name], sumsq[name]) / count;
    part = participants[name] / count
    partsd = stdev(counts[name], participants[name], participantsq[name]) / count;
    discuss = discussion[name] / count;
    maxdiscuss = maxdiscussion[name];
    discusssd = stdev(counts[name], discussion[name], discussionsq[name]) / count;
    if (mentions != 0) {
      printf("%30s: %4s tickets with %2.0f (avg %3.1f+/-%.1f) mentions and %4.0f (avg %3.1f+/-%.1f) mentions of others\n", name, count, sums[name], comment, commentsd, participants[name], part, partsd);
    } else if (assignee == 0) {
      printf("%30s: %4s tickets making %4.0f (avg %3.1f+/-%.1f) comments; with %4.0f (avg %3.1f+/-%.1f) participants and %4.0f (avg %3.1f+/-%.1f) discussion\n", name, count, sums[name], comment, commentsd, participants[name], part, partsd, discussion[name], discuss, discusssd);
    } else {
      printf("%30s: %4s tickets receiving %4.0f (avg %3.1f+/-%.1f) comments; with %4.0f (avg %3.1f+/-%.1f) participants\n", name, count, sums[name], comment, commentsd, participants[name], part, partsd);
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

