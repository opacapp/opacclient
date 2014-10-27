#!/usr/bin/python3
# Converts the old "support" string in the JSON files and the "accountSupported"
# data attribute from some libraries to a new boolean value indicating whether 
# accounts are supported
import urllib.request
import urllib.parse
import sys
import json
import os

DIR = 'assets/bibs/'

for filename in os.listdir(DIR):
    f = DIR + filename
    data = json.load(open(f))
    support = False
    if 'support' in data:
        support = 'Konto' in data['support']
        del data['support']
    if 'account' in data['data']:
        support = data['data']['account']
        del data['data']['account']
    if 'accountSupported' in data['data']:
        support = data['data']['accountSupported']
        del data['data']['accountSupported']
    data['account_supported'] = support
    print(filename + ' ' + str(support))
    json.dump(data, open(f, 'w'), indent=4, sort_keys=True)
