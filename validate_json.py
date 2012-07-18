#!/usr/bin/env python
# -*- coding: utf-8 -*-
import json
bibs = json.load(open("assets/bibs.json"))["bibs"]

e = False

remove_these = ' .()-/'
for k,v in bibs.items():
	if not v[0].startswith("http"):
		print "Error at", k, ":", v
		print "Index 0 has to be an URL"
		print ""
		e = True
	if len(v[1]) != 7:
		print "Error at", k, ":", v
		print "Index 1 has be an array with a length of 7"
		print ""
		e = True

if not e:
	print "Valid. %s libraries." % len(bibs)
