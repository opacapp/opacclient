#!/usr/bin/env python
# -*- coding: utf-8 -*-
import json
bibs = json.load(open("assets/bibs.json"))["bibs"]

e = False

remove_these = ' .()-/'
for k,v in bibs.items():
	if len(v) != 5:
		print "Error at", k, ":", v
		print "\tHas to be 5 elements long"
		print ""
		e = True
	if not v[0].startswith("http"):
		print "Error at", k, ":", v
		print "\tIndex 0 has to be an URL"
		print ""
		e = True
	if len(v[1]) != 7:
		print "Error at", k, ":", v
		print "\tIndex 1 has be an array with a length of 7"
		print ""
		e = True
	if v[2] is not None:
		if len(v[2]) != 8:
			print "Error at", k, ":", v
			print "\tIndex 2 has be an array with a length of 8"
			print ""
			e = True
	if v[3] is not None:
		if len(v[3]) != 5:
			print "Error at", k, ":", v
			print "\tIndex 3 has be an array with a length of 5"
			print ""
			e = True
	if v[4] is not None:
		if not v[4].startswith("/"):
			print "Error at", k, ":", v
			print "\tIndex 4 has to start with /"
			print ""
			e = True

if not e:
	print "Valid. %s libraries." % len(bibs)
