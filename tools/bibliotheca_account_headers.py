#!/usr/bin/python3
# Searches for Bibliotheca libraries in the assets/bibs/ directory and tries if they have a w3oini.txt configuration to
# find out what the headers in their account view are called.
import json
import os
import configparser
import urllib.request
from urllib.error import HTTPError, URLError
from multiprocessing import Pool

from socket import timeout

DIR = 'opacclient/opacapp/src/main/assets/bibs/'
HEADERS_LENT = 'opacclient/libopac/src/main/resources/bibliotheca/headers_lent.json'
HEADERS_RESERVATIONS = 'opacclient/libopac/src/main/resources/bibliotheca/headers_reservations.json'

def loadconfig(filename):
    f = os.path.join(DIR, filename)
    data = json.load(open(f))
    if data['api'] == 'bibliotheca':
        url = data['data']['baseurl']
        try:
            return urllib.request.urlopen(url + '/w3oini.txt', timeout=10).read().decode('iso-8859-1')
        except (HTTPError, URLError, configparser.ParsingError):
            print('could not find config for {}'.format(filename))
            return None
        except timeout:
            print('timeout for {}'.format(filename))
            return None


def handleconfig(filename, config_str):
    config = configparser.RawConfigParser(allow_no_value=True, strict=False)
    try:
        config.read_string(config_str)
        for i in range(1, 21):
            conf = config.get("ANZEIGEKONTOFELDER", "konto" + str(i))
            key = conf.split("#")[0].lower()
            titles = conf.split("#")[1:]

            type_lent = None
            type_reservations = None

            if key in ('exemplarnr', 'buchungsnr'):
                type_lent = 'barcode'
            elif key == 'verf':
                type_lent = type_reservations = 'author'
            elif key == 'titel':
                type_lent = type_reservations = 'title'
            elif key == 'frist':
                type_lent = 'returndate'
            elif key == 'bereit':
                type_reservations = 'availability'
            elif key == 'ausleihstatus':
                type_lent = 'status'
            elif key == 'zwst':
                type_lent = 'homebranch'
                type_reservations = 'branch'
            elif key == 'ausleihstelle':
                type_lent = 'lendingbranch'
            elif key == 'mediengrp':
                type_lent = type_reservations = 'format'
            elif key == 'bereit bis':
                type_reservations = 'expirationdate'
            elif key == 'reserviert' or key == 'saeumnisgebuehr':
                pass

            if type_lent is not None:
                for title in titles:
                    if title not in headers_lent:
                        print('adding {} to headers_lent.json with meaning {}'.format(title, type_lent))
                        headers_lent[title] = type_lent
                    elif headers_lent[title] != type_lent:
                        print('CONFLICT: {} should be {}, but is {} in headers_lent.json!'
                              .format(title, type_lent, headers_lent[title]))

            if type_reservations is not None:
                for title in titles:
                    if title not in headers_reservations:
                        print('adding {} to headers_reservations.json with meaning {}'.format(title,
                                                                                              type_reservations))
                        headers_reservations[title] = type_reservations
                    elif headers_reservations[title] != type_reservations:
                        print('CONFLICT: {} should be {}, but is {} in headers_reservations.json!'
                              .format(title, type_reservations, headers_reservations[title]))
    except configparser.ParsingError:
        print('could not parse config for {}'.format(filename))


def save(filename, data):
    with open(filename, 'w') as fp:
        json.dump(data, fp, sort_keys=True, indent=4)
        fp.write("\n")


if __name__ == '__main__':
    print('loading configs')
    p = Pool(50)
    filenames = os.listdir(DIR)
    configs = p.map(loadconfig, filenames)
    print('received {} configs'.format(sum(x is not None for x in configs)))
    print('parsing configs')

    headers_lent = json.load(open(HEADERS_LENT))
    headers_reservations = json.load(open(HEADERS_RESERVATIONS))
    for (filename, config) in zip(filenames, configs):
        if config is not None:
            handleconfig(filename, config)

    save(HEADERS_LENT, headers_lent)
    save(HEADERS_RESERVATIONS, headers_reservations)