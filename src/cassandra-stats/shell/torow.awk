#! /usr/bin/gawk -f
{
  x[$2]=$1
}
END {
  if (div == "") { div = 1 }
  out = ticket;
  for (v in x) { out = out "," v ":" (x[v] / div) }
  print(out);
}
