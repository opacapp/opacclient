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
    print(filename)
    f = DIR + filename
    data = json.load(open(f))
    if 'country' not in data:
        if data['group'] in ('Österreich', 'Schweiz'):
            data['country'] = data['group']
            print("Bundesland zu " + data['city'], end=": ")
            data['state'] = input().strip()
        else:
            data['country'] = 'Deutschland'
            data['state'] = data['group']
    if 'group' in data:
        del data['group']

    json.dump(data, open(f, 'w'), indent=4, sort_keys=True)
