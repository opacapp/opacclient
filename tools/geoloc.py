#!/usr/bin/python3
# Searches the assets/bibs/ directory for libraries with missing geolocation
# and requests the location data from Google Maps.
# The user is prompted to confirm the data.
import urllib.request
import urllib.parse
import sys
import json
import os

DIR = 'assets/bibs/'
    
for filename in os.listdir(DIR):
	f = DIR + filename
	data = json.load(open(f))
	if 'geo' in data:
		continue
		
	print("Suche Position für: %s, %s\n" % (data['city'], data['group']))
	
	uri = 'https://maps.googleapis.com/maps/api/geocode/json?'+urllib.parse.urlencode({'address':data['city'],'sensor':'false'})
	jsoncontent = urllib.request.urlopen(uri).read().decode()
	geocode = json.loads(jsoncontent)
	
	if geocode['status'] != 'OK':
		print("ERROR! %s" %filename)
		continue
		
	key = 1
	for res in geocode['results']:
		print("["+str(key)+"]", ", ".join([a["long_name"] for a in res['address_components']]))
		key += 1
		
	print("\nWelches? [Enter für ablehnen]")
		
	inp = input()
	
	os.system('clear')
	
	if inp.strip() == '':
		continue
		
	if int(inp.strip()) > 0:
		res = geocode['results'][int(inp.strip())-1]
		data['geo'] = [float(res['geometry']['location']['lat']), float(res['geometry']['location']['lng'])]
	

	json.dump(data, open(f, 'w'), indent=4)
