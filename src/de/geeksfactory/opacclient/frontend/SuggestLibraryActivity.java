package de.geeksfactory.opacclient.frontend;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.EditText;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import de.geeksfactory.opacclient.R;

public class SuggestLibraryActivity extends Activity {
	
	private EditText etCountry;
	private EditText etState;
	private EditText etCity;
	private EditText etName;
	private EditText etComment;
	private Button btnSend;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_suggest_library);
		
		etCountry = (EditText) findViewById(R.id.etCountry);
		etState = (EditText) findViewById(R.id.etState);
		etCity = (EditText) findViewById(R.id.etCity);
		etName = (EditText) findViewById(R.id.etName);
		etComment = (EditText) findViewById(R.id.etComment);
		btnSend = (Button) findViewById(R.id.btnSend);
		
		if(savedInstanceState != null) {
			etCountry.setText(savedInstanceState.getCharSequence("country"));
			etState.setText(savedInstanceState.getCharSequence("state"));
			etCity.setText(savedInstanceState.getCharSequence("city"));
			etName.setText(savedInstanceState.getCharSequence("name"));
			etComment.setText(savedInstanceState.getCharSequence("comment"));
		}
		
		btnSend.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent send = new Intent(Intent.ACTION_SENDTO);
				String uriText = "mailto:" + Uri.encode("info@opacapp.de") + 
				          "?subject=" + Uri.encode("Bibliotheksvorschlag") + 
				          "&body=" + Uri.encode(createMessage());
				Uri uri = Uri.parse(uriText);

				send.setData(uri);
				startActivity(Intent.createChooser(send, "Send mail..."));
				finish();
			}
			
		});
	}
	
	private String createMessage() {
		try {
			return createJSON().toString(4) + "\n\n" +
					etComment.getText().toString();
		} catch (JSONException e) {
			return "";
		}
	}
	
	protected JSONObject createJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("country", etCountry.getText().toString());
			json.put("state", etState.getText().toString());
			json.put("city", etCity.getText().toString());
			json.put("title", etName.getText().toString());
			return json;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putCharSequence("country", etCountry.getText());
		outState.putCharSequence("state", etState.getText());
		outState.putCharSequence("city", etCity.getText());
		outState.putCharSequence("name", etName.getText());
		outState.putCharSequence("comment", etComment.getText());
	}
}
