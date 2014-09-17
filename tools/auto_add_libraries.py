#!/usr/bin/python3
from add_library import *
import webbrowser
import sys

APIS = {
    'bibliotheca' : Bibliotheca,
    'sisis'       : Sisis,
    'biber1992'   : Biber1992,
    'zones22'     : Zones22,
    'iopac'       : IOpac,
    'pica'        : Pica,
    'adis'        : Adis,
    'webopac.net' : WebOpacNet,
    'winbiap'     : WinBiap,
}

if __name__ == '__main__':
    print("Hallo! Dieses Skript hilft dir, eine neue Bibliothek hinzuzufügen")

    if not os.path.isdir(LIBDIR):
        print("Bitte wechsle in das Verzeichnis, in dem die App liegt.")
        sys.exit(0)

    print("Welche Datei enthält die Liste der Bibliotheken?")
    filename = getInput(required=True, default=sys.argv[1] if len(sys.argv) > 1 else "");
    json_list = open(filename).read();
    list = json.loads(json_list);


    api = sys.argv[2] if len(sys.argv) > 2 else ""
    while api not in APIS.keys():
        print("Welche API-Implementierung wird genutzt?")
        print("Verfügbar sind: " + " ".join(APIS.keys()))
        api = getInput(required=True)

    for library in list:
        data = {};

        try:
            webbrowser.get().open('https://www.google.de/search?hl=de&q=%s' % library['title'])
        except:
            pass

        library['title'] = library['title'].replace(library['city'], "");
        library['title'] = library['title'].strip();

        data['city'] = library['city'];
        data['title'] = library['title'];

        print("---------------------------------");
        print("Stadt: " + data['city']);
        print("Bibliothek: " + data['title']);

        print("Lade Geodaten...")

        geo = loadGeoPossibilities(data['city'])
        for k, g in enumerate(geo):
            print("[%d] %s" % (k + 1, g[0]))

        print("Welche dieser Positionen trifft am besten zu? 0 für überspringen.")
        print("Nummer", end=" ")
        geokey = int(getInput(default="0"))
        if geokey > 0:
            data['geo'] = geo[geokey - 1][1]
        if geokey == 0:
            continue

        print("In welchem Land liegt die Bibliothek?")
        data['country'] = getInput(required=True, default="Deutschland")

        data['state'] = library['state'];
        print("Bundesland: " + data['state']);

        data['api'] = api;
        data['data'] = {};
        data['data']['baseurl'] = library['baseurl'];
        print("OPAC-URL: " + data['data']['baseurl']);

        print("URL zu einer Informationsseite")
        print("Sollte Öffnungszeiten u.ä. enthalten")

        data['data']['information'] = getInput(required=True)

        print("Grad der Unterstützung")
        data['support'] = APIS[api]().getDefaultSupportString();

        data = APIS[api]().prompt(data)

        ok = False;
        while not ok:
            print("Dateiname")
            print("Sowas wie 'Mannheim' oder 'Heidelberg_Uni'. Möglichst keine Leerzeichen und Umlaute.")

            if data['title'] in ("Stadtbibliothek", "Stadtbücherei", "Gemeindebücherei"):
                name = data['city'];
            else:
                name = data['city'] + " " + data['title'];
            name = name.replace(" ", "_").replace("ä", "ae").replace("ü", "ue").replace("ö", "oe").replace("ß", "ss");

            ident = getInput(required=True, default = name);

            if os.path.isfile(LIBDIR + ident + '.json'):
                print("ACHTUNG: Datei existiert bereits. Überschreiben? (j/n)");
                value = getInput(required=True, default="n")
                if value == "j":
                    ok = True;
            else:
                ok = True;


        print(json.dumps(data, indent=4, sort_keys=True), end="\n\n")
        json.dump(data, open(LIBDIR + ident + '.json', 'w'), sort_keys=True, indent=4)
        print("In Datei %s geschrieben." % (LIBDIR + ident + '.json'))
