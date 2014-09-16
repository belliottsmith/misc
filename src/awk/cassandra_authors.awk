{ 
	body = body $0 "\n";
	$0 = tolower($0);
	if (substr($0, 1, 5) == "date:") {
		match($0, " [0-9][0-9][0-9][0-9] ");
		thisyear = substr($0, RSTART + 1, 4);
		skip = thisyear < year;
	}
	if (substr($0,1,8) == "author: ") {  committer = remap(parse($0, "author: ", ".*", "<")); } 
	if (match($0,"patch by.*[;,]") > 0) { 
		gsub(",", ";"); 
		author = remap(parse($0, "patch by", ".*", ";")); 
		if (match($0,"reviewed by.*for") > 0) { 
			reviewer = remap(parse($0, "reviewed by", ".*", "for")); 
		}
	}
	else if (match($0,"patch by.* and ") > 0) { author = remap(parse($0, "patch by", ".*", " and ")); author2 = remap(parse($0, "and", ".*", "")); }  
	if (match($0, "ninja") > 0 && author == "") { author = committer; }
	if (match($0,"[0-9]+ files? changed, [0-9]+ insertions\\(\\+\\), [0-9]+ deletions\\(\\-\\)") > 0) { 
		if (!skip)
		{
			additions = parseInt($0, "insertions");
			deletions = parseInt($0, "deletions");
			committer_c[committer] += 1;
			committer_a[committer] += additions;
			committer_d[committer] += deletions;
			if (author == "")
			{
				unknown_author_c[committer] += 1;
				unknown_author_a[committer] += additions;
				unknown_author_d[committer] += deletions;
			}
			else
			{
				author_c[author] += 1;
				author_a[author] += additions;
				author_d[author] += deletions;
				if (author2 != "")
				{
					author_c[author2] += 1;
					author_a[author2] += additions;
					author_d[author2] += deletions;
				}
			}
			reviewer_c[reviewer] += 1;
			reviewer_a[reviewer] += additions;
			reviewer_d[reviewer] += deletions;
		}
		author = ""; reviewer = ""; committer = ""; body = ""; author2 = "";
	}
}
END {
	statsout("committer", committer_c, committer_a, committer_d);
	statsout("author", author_c, author_a, author_d);
	statsout("unknown author", unknown_author_c, unknown_author_a, unknown_author_d);
	statsout("reviewer", reviewer_c, reviewer_a, reviewer_d);
}
function remap(name) {
	if (match(name, " ") > 0) 
	{
		name = substr(name, 1, RSTART - 1); 
		return remap(name);
	}
	if (name == "belliottsmith") return "benedict";
	if (name == "belliotsmith") return "benedict";
	if (name == "brandonwilliams") return "brandon";
	if (name == "bes") return "benedict";
	if (name == "pcmanus") return "slebresne";
	if (name == "aleksey") return "iamaleksey";
	if (name == "ayeschenko") return "iamaleksey";
	if (name == "pyaskevich") return "xedin";
	if (name == "t") return "jake";
	if (name == "vparthasarathy") return "vijay";
	if (name == "vijay2win") return "vijay";
	if (name == "yuki") return "yukim";
	if (name == "joshua") return "josh";
	return name;
}
function parseInt(s, suffix) {
	return parse(s, "", "[0-9]+", suffix);
}
function parse(s, prefix, find, suffix) {
	match(s, prefix " *" find " *" suffix);
	part = substr(s, RSTART + length(prefix), RLENGTH - (length(prefix) + length(suffix)));
	match(part, find)
	part = substr(part, RSTART, RLENGTH);
	gsub("^ +", "", part);
	gsub(" +$", "", part);
	return part;
}
function statsout(type, counts, additions, deletions) {
	print(type ":");
	n = asorti(additions, sorted, "cmp_val_desc");
	for (i = 1; i <= n ; i++)
	{
		name = sorted[i];
		if (additions[name] > 100 && counts[name] > 2) {
			print(name ": " counts[name] " changes, " additions[name] "+, " deletions[name] "-");
		}
	}
	print("");
}
function cmp_val_desc(i1, v1, i2, v2)
{
     return (v2 - v1)
}
