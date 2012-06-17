package de.geeksfactory.opacclient;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);
        
        TextView tvAbout = (TextView) findViewById(R.id.tvAbout);
        
        try {
			tvAbout.setText(Html.fromHtml("OpacClient für Android "+(getPackageManager().getPackageInfo(getPackageName(), 0).versionName)+
					"<br /><br />Eine App von <b>Raphael Michel</b><br /><br />" +
					"<a href='http://www.raphaelmichel.de'>www.raphaelmichel.de</a><br />"+
					"<a href='mailto:raphael@geeksfactory.de'>raphael@geeksfactory.de</a><br /><br />" +
					"Veröffentlicht von der geek's factory<br />" +
					"<a href='http://www.geeksfactory.de'>www.geeksfactory.de</a><br /><br />"+
					"Diese App ist freie Software, den Source Code gibt es hier:<br />"+
					"<a href='https://github.com/raphaelm/opacclient'>github.com/raphaelm/opacclient</a><br /><br />"+
					"Die Icons stammen aus dem Human O2 Set von Oliver Scholtz<br /><br />"+
					"Gedacht zur Bedienung von BOND Web-Opacs V2.6<br /><br />"+
					"Danke an Céline A.<br />"+
					"Die App verwendet <a href='http://jsoup.org'>jsoup</a> und mag es.<br />"
					));
			tvAbout.setMovementMethod(LinkMovementMethod.getInstance());
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
