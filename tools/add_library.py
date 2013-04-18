#!/usr/bin/python3
import sys
import json
import os
import configparser
import urllib.request, urllib.parse, urllib.error

LIBDIR = 'assets/bibs/'
TYPES = ['NONE', 'BOOK', 'CD', 'CD_SOFTWARE', 'CD_MUSIC', 'DVD', 'MOVIE', 'AUDIOBOOK', 'PACKAGE',
		'GAME_CONSOLE', 'EBOOK', 'SCORE_MUSIC', 'PACKAGE_BOOKS', 'UNKNOWN', 'NEWSPAPER',
		'BOARDGAME', 'SCHOOL_VERSION', 'MAP', 'BLURAY', 'AUDIO_CASSETTE', 'ART', 'MAGAZINE',
		'GAME_CONSOLE_WII', 'GAME_CONSOLE_NINTENDO', 'GAME_CONSOLE_PLAYSTATION',
		'GAME_CONSOLE_XBOX', 'LP_RECORD', 'MP3', 'URL']

def getInput(required = False, default = None):
	if default is not None:
		print("[Standard: %s]" % default, end=" ")
	print("> ", end="")
	inp = input().strip()
	if default is not None and (inp is None or inp == ''):
		return default
	if required and (inp is None or inp == ''):
		print("Feld muss gefüllt werden.")
		return getInput(required=required, default=default)
	if inp is None or inp == '':
		return None
	return inp

def loadGeoPossibilities(city):
	uri = 'https://maps.googleapis.com/maps/api/geocode/json?'+urllib.parse.urlencode({'address':city,'sensor':'false'})
	jsoncontent = urllib.request.urlopen(uri).read().decode()
	geocode = json.loads(jsoncontent)
	
	if geocode['status'] != 'OK':
		print("ERROR! %s" %filename)
		return False
		
	possibilities = []
		
	for res in geocode['results']:
		possibilities.append(
				(
					", ".join([a["long_name"] for a in res['address_components']]),
					[float(res['geometry']['location']['lat']), float(res['geometry']['location']['lng'])]
				)
			)
	return possibilities

class Api:
	
	def getDefaultSupportString(self):
		return 'Katalogsuche und Konto'
		
	def prompt(self, data):
		return data

class Bibliotheca(Api):
	
	def getDefaultSupportString(self):
		return 'Katalogsuche und Konto'
		
	def prompt(self, data):
		datadata = data['data']
		global TYPES
		
		print("Muss eine bestimmte Datenbank geladen werden?")
		print("Enter drücken, wenn nicht benötigt.")
		inp = getInput(required=False)
		suffix = ''
		if inp is not None:
			datadata['db'] = inp
			
			print("Welchen Suffix hat diese Datenbank im System? (Bsp. _DB1)")
			inp = getInput(required=False, default='')
			if inp is not None:
				suffix = inp
				
		fetched = None
		
		try:
			fetched = self._fetchData(datadata['baseurl'], suffix)
			datadata['accounttable'] = fetched['accounttable']
			datadata['reservationtable'] = fetched['reservationtable']
			datadata['copiestable'] = fetched['copiestable']
		except:
			print("WARNUNG! Konfiguration konnte nicht ausgelesen werden. HANDARBEIT NÖTIG!")
			print("Mehr Informationen:")
			print("https://github.com/raphaelm/opacclient/wiki/Supported-library-types#bibliotheca")
			
		if fetched is not None:
			if len(fetched['mediatypes']) > 0:
				print("Bitte weise die Medientypen ihren Entsprechungen in der App zu.")
				print("Verfügbar sind:")
				print(" ".join(TYPES))
				print("")
				datadata['mediatypes'] = {}
				for k, v in fetched['mediatypes'].items():
					inp = ''
					while inp not in TYPES:
						print("'%s' ('%s')?" % (v, k))
						inp = getInput(required=False, default="UNKNOWN")
						datadata['mediatypes'][k] = inp
			
		data['data'] = datadata
		return data
		
	def _fetchData(self, url, suff = ''):
		config = configparser.RawConfigParser(allow_no_value=True)
		config.read_string(urllib.request.urlopen(url+'/w3oini.txt').read().decode('iso-8859-1'))
		data = {
			'accounttable': [-1]*8,
			'reservationtable': [-1]*6,
			'copiestable': [-1]*7
		}
		i_acc = 0
		i_res = 0
		for i in range(1,21):
			conf = config.get("ANZEIGEKONTOFELDER", "konto"+str(i))
			if conf == '': continue
			key = conf.split("#")[0]
			
			if key == 'buchungsnr':
				data['accounttable'][0] = i_acc
				i_acc += 1
			elif key == 'verf':
				data['accounttable'][1] = i_acc
				data['reservationtable'][0] = i_res
				i_acc += 1
				i_res += 1
			elif key == 'titel':
				data['accounttable'][2] = i_acc
				data['reservationtable'][1] = i_res
				i_acc += 1
				i_res += 1
			elif key == 'frist':
				data['accounttable'][3] = i_acc
				i_acc += 1
			elif key == 'bereit':
				data['reservationtable'][2] = i_res
				i_res += 1
			elif key == 'ausleihstatus':
				data['accounttable'][4] = i_acc
				i_acc += 1
			elif key == 'zwst':
				data['accounttable'][5] = i_acc
				data['reservationtable'][3] = i_res
				i_acc += 1
				i_res += 1
			elif key == 'ausleihstelle':
				data['accounttable'][6] = i_acc
				i_acc += 1
			elif key == 'mediengrp':
				i_acc += 1
			elif key == 'bereit bis':
				data['reservationtable'][5] = i_res
				i_res += 1
			else:
				print("WARNING! NOT COUNTING ", key, url)
				sys.exit(0)
		data['accounttable'][-1] = i_acc
		data['reservationtable'][-2] = i_res
		if data['accounttable'][6] == -1 and data['accounttable'][5] > 0:
			data['accounttable'][6] = data['accounttable'][5]

		i_copy = 0
		for i in range(1,11):
			conf = config.get("ANZEIGE_EXEMPLAR"+suff, "AE"+str(i))
			if conf == '': continue
			key = conf.split("#")[1]
			if key == 'buchungsnr':
				data['copiestable'][0] = i_copy
			elif key == 'zweigstelle':
				data['copiestable'][1] = i_copy
			elif key == 'standort2':
				data['copiestable'][2] = i_copy
			elif key == 'standort':
				data['copiestable'][3] = i_copy
			elif key == 'exemplarstatus':
				data['copiestable'][4] = i_copy
			elif key == 'rueckgabedatum':
				data['copiestable'][5] = i_copy
			elif key == 'Auslanzvorbestakt':
				data['copiestable'][6] = i_copy
			i_copy += 1
			
		data['mediatypes'] = {}
		for i in range(1,100):
			conf = config.get("ANZEIGE_MEDIGRPPIC", "MEDIGRPPIC"+str(i))
			if conf == '': continue
			split = conf.split("#")
			data['mediatypes'][split[1]] = split[2]
		return data

class Sisis(Api):
	
	def getDefaultSupportString(self):
		return 'Katalogsuche und Konto'
		
	def prompt(self, data):
		print("Sind zusätzliche Parameter nötig?")
		print("Ein häufiges Beispiel wäre sowas wie 'Login=opsb'")
		inp = getInput(required=False)
		if inp is not None:
			data['data']['startparams'] = inp
		return data
	
class Biber1992(Api):
	
	def getDefaultSupportString(self):
		return 'Katalogsuche'
		
	def prompt(self, data):
		print("WARNUNG! Konfiguration kann nicht ausgelesen werden. HANDARBEIT NÖTIG!")
		return data
	
class Zones22(Api):
	
	def getDefaultSupportString(self):
		return 'Katalogsuche'

APIS = {
		'bibliotheca' : Bibliotheca,
		'sisis'       : Sisis,
		'biber1992'   : Biber1992,
		'zones22'     : Zones22
	}

data = {}

if __name__ == '__main__':

	print("Hallo! Dieses Skript hilft dir, eine neue Bibliothek hinzuzufügen")

	if not os.path.isdir(LIBDIR):
		print("Bitte wechsle in das Verzeichnis, in dem die App liegt.")
		sys.exit(0)

	print("In welcher Stadt befindet sich die Bibliothek?")
	print("Suffixe wie 'Frankfurt (Main)' werden in Klammern gesetzt.")

	data['city'] = getInput(required=True)

	print("Lade Geodaten...")

	geo = loadGeoPossibilities(data['city'])
	for k, g in enumerate(geo):
		print("[%d]	%s" % (k+1, g[0]))
		
	print("Welche dieser Positionen trifft am besten zu? 0 für keine.")
	print("Nummer", end=" ")
	geokey = int(getInput(default="0"))
	if geokey > 0:
		data['geo'] = geo[geokey-1][1]

	print("Zu welcher Gruppe soll die Bibliothek gehören?")
	print("Aktuell benutzen wir für die Gruppennamen die deutschen Bundesländer sowie 'Österreich' und 'Schweiz'")

	data['group'] = getInput(required=True)

	print("Wie heißt die Bibliothek?")
	print("Dies sollte etwas in dieser Stadt eindeutiges sein wie 'Stadtbibliothek', 'Unibibliothek' oder 'Ruprecht-Karls-Universität'. Der Name der Stadt soll nicht erneut vorkommen!")

	data['title'] = getInput(default="Stadtbibliothek")

	print("Welche API-Implementierung wird genutzt?")
	print("Verfügbar sind: " + " ".join(APIS.keys()))

	data['api'] = ''
	while data['api'] not in APIS.keys():
		data['api'] = getInput(required=True)

	print("URL zum OPAC")
	print("Ohne abschließenden /")

	data['data'] = {}
	data['data']['baseurl'] = getInput(required=True)

	print("URL zu einer Informationsseite")
	print("Sollte Öffnungszeiten u.ä. enthalten")

	data['data']['information'] = getInput(required=True)

	api = APIS[data['api']]()
	data = api.prompt(data)

	print("Grad der Unterstützung")

	data['support'] = getInput(required=False, default=api.getDefaultSupportString())

	print("Dateiname")
	print("Sowas wie 'Mannheim' oder 'Heidelberg_Uni'. Möglichst keine Leerzeichen und Umlaute.")

	ident = getInput(required=True)

	print(json.dumps(data, indent=4, sort_keys=True), end="\n\n")
	json.dump(data, open(LIBDIR+ident+'.json', 'w'), sort_keys=True, indent=4)
	print("In Datei %s geschrieben." % (LIBDIR+ident+'.json'))
