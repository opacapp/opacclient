#!/usr/bin/python3
# Searches the assets/bibs/ directory and reformat all json files
import json
import os

DIR = 'opacclient/opacapp/src/main/assets/bibs/'

for filename in os.listdir(DIR):
    f = os.path.join(DIR, filename)
    data = json.load(open(f))
    json.dump(data, open(f, 'w'), indent=4, sort_keys=True)
