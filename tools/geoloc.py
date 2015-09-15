#!/usr/bin/python3
# Searches the assets/bibs/ directory for libraries with missing geolocation
# and requests the location data from Google Maps.
# The user is prompted to confirm the data.
import urllib.request
import urllib.parse
import sys
import json
import os
from add_library import loadGeoPossibilities, getInput

DIR = 'opacclient/opacapp/src/main/assets/bibs/'

for filename in os.listdir(DIR):
    f = DIR + filename
    data = json.load(open(f))
    if 'geo' in data:
        if len(data['geo']) != 2 or data['geo'][0] == 0 or data['geo'][1] == 0:
            print("Invalid: %s" %filename)    
        continue

    print("Suche Position für: %s, %s, %s\n" % (data['title'], data['city'], data['state']))

    geo = loadGeoPossibilities(data)
    if len([g for g in geo if g[1] != data['geo']]):
        for k, g in enumerate(geo):
            print("[%d]    %s" % (k + 1, g[0]))

        print("Welche dieser Positionen trifft am besten zu? 0 für keine.")
        print("Nummer", end=" ")
        geokey = int(getInput(default="0"))
        if geokey > 0:
            data['geo'] = geo[geokey - 1][1]

    os.system('clear')

    with open(f, 'w') as fp:
        json.dump(data, fp, indent=4, sort_keys=True)
        fp.write("\n")
