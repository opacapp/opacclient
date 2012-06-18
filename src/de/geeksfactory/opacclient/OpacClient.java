package de.geeksfactory.opacclient;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class OpacClient extends Application {

	protected OpacWebApi ohc;
    
	public static String[] BIBNAMES = {
		"Stadtbibliothek Mannheim", 		//  0
		"Stadtbibliothek Heidelberg", 		//  1
		"Stadtbibliothek Ludwigshafen", 	//  2
		"Stadtbibliothek Tübingen", 		//  3
		"Stadtbibliothek Leverkusen",		//  4 
		"Stadtbücherei Frankfurt",			//  5
		"Bibliothek Sigmaringen",			//  6
		"Stadtbibliothek Bad Homburg",		//  7
		"Stadtbibliothek Pforzheim",		//  8
		"Bücherei Speyer",					//  9
		"Büchereien Wien",					// 10
		"Bibliothek Rheine",				// 11
		"Stadtbücherei Steinfurt",			// 12
		"Stadtbücherei Bamberg",			// 13
		"Stadtbibliothek Neu-Ulm",			// 14
		"Stadtbibliothek Dachau",			// 15
		"HAIT Dresden"						// 16
		};
	public static String[] BIBURLS = {
		"http://katalog.mannheim.de/wopac", 					//  0
		"http://ww3.heidelberg.de/wwwopac", 					//  1
		"http://bibliothek-katalog.ludwigshafen.de/webopac", 	//  2
		"http://wwwopac.rz-as.de/tuebingen", 					//  3
		"http://www.bibliothek.leverkusen.de/wwwopac", 			//  4
		"http://www.stadtbuecherei-frankfurt.de/opac",			//  5
		"http://www.bib.sigmaringen.de",						//  6
		"http://wwwopac.bad-homburg.de/webopac",				//  7
		"https://opac1.stadt-pforzheim.de",						//  8
		"http://buecherei.speyer.de",							//  9
		"http://katalog.buechereien.wien.at",					// 10
		"http://www.rheine-bibliothek.de/webopac",				// 11
		"http://topac30.citeq.de/opac80",						// 12
		"http://opac.stadtbuecherei-bamberg.de",				// 13
		"http://wwwopac.stadt.neu-ulm.de/webopac",				// 14
		"http://opac.dachau.de/webopac",						// 15
		"http://www.hait.tu-dresden.de/webopac"					// 16
		};
	
	public static int NOTIF_ID = 1;
	public static int BROADCAST_REMINDER = 2;
	
	@Override
	public void onCreate() {
		super.onCreate();
  	  	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		
		ohc = new OpacWebApi(sp.getString("opac_url", getResources().getString(R.string.opac_mannheim)), this);
	}
	
	public void web_error(Exception e){
		web_error(e, ohc.getLast_error());
	}
	
	public void web_error(Exception e, String t){
        Intent intent = new Intent(this, ErrorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("e", e);
        intent.putExtra("t", t);
        startActivity(intent);
	}

}
