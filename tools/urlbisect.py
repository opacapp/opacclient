"""
Takes an URL that works and an URL that doesn't work and finds out the difference.
The URLs must have the same host/path, only the queries can differ.
An URL is seen as working, if SEARCHSTR is contained in the response.
"""
URL_BAD = 'http://primo.ub.uni-due.de/primo_library/libweb/action/search.do?mode=Advanced&tab=localude&indx=1&dum=true&srt=rank&vid=UDE&frbg=&tb=t&title2=2&=Selected_Databases&=dummyLocations&=EXLSelectOption+EXLSearchInputScopesOptionSelected_Databases&=Ausgew%C3%A4hlte+Datenbanken&vl%28D2327293UI3%29=all_items&scp.scps=scope%3A%28UDEALEPH%29%2Cscope%3A%28UDESEMAPP%29%2Cscope%3A%28UDEDUEPUBLICO%29&vl%28100316065UI0%29=any&vl%28freeText0%29=harry+potter&vl%281UI0%29=contains&=AND&vl%282482768UI5%29=all_items&vl%282482478UI4%29=all_items'
URL_GOOD = 'http://primo.ub.uni-due.de/primo_library/libweb/action/search.do?dscnt=0&vl(1UI0)=contains&tab=localude&vl(1UI2)=contains&dstmp=1429601477151&srt=rank&mode=Advanced&indx=1&tb=t&vl(freeText0)=harry+potter&fn=search&vid=UDE&vl(2482481UI1)=title&vl(2482768UI5)=all_items&vl(freeText2)=&vl(2482478UI4)=all_items&title2=2&vl(1UI1)=contains&frbg=&vl(2482769UI2)=creator&ct=search&dum=true&vl(100316065UI0)=any&vl(D2327293UI3)=all_items&Submit=Suche&vl(freeText1)='
SEARCHSTR = 'auf den Spuren eines zauberhaften Bestsellers'

import requests
import sys
import re
import copy
from urllib.parse import urlparse, parse_qs

good_parsed = urlparse(URL_GOOD)
bad_parsed = urlparse(URL_BAD)
good_query = parse_qs(good_parsed.query)
bad_query = parse_qs(bad_parsed.query)

def test(qsd):
    r = requests.get('%s://%s%s' % (good_parsed.scheme, good_parsed.netloc, good_parsed.path),
                     params=qsd)
    if 'id="connect"' in r.text:
        pattern = re.compile("\"/goto/([^\"]+)\"")
        m = pattern.search(r.text)
        if m:
            r = requests.get(m.group(1))
    return (SEARCHSTR in r.text)

if not test(good_query):
    print("Good query is not good.")
    sys.exit(0)

if test(bad_query):
    print("Bad query is not bad.")
    sys.exit(0)

bad_query_clone = copy.copy(bad_query)
for k, v in bad_query.items():
    if k not in good_query:
        del bad_query_clone[k]
        print("Remote %s from bad query" % k)
        if test(bad_query_clone):
            print ("MATCH")
            sys.exit(0)

for k, v in good_query.items():
    if k not in bad_query:
        bad_query_clone[k] = v
        print("Add %s to bad query" % k)
        if test(bad_query_clone):
            print ("MATCH")
            sys.exit(0)
