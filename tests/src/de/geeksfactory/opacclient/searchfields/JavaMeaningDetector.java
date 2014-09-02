package de.geeksfactory.opacclient.searchfields;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.MeaningDetector;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;


public class JavaMeaningDetector implements MeaningDetector {
	
	private Map<String, String> meanings;
	private static final String ASSETS_FIELDSDIR = "../assets/meanings";
	
	public JavaMeaningDetector(Library lib) {
		meanings = new HashMap<String, String>();
		File[] files = new File(ASSETS_FIELDSDIR).listFiles();
		
		for (File file:files) {
			if (file.getName().equals("general.json")) // General
				readFile(file);
			else if (file.getName().equals(lib.getApi() + ".json")) // Api specific
				readFile(file);
			else if (file.getName().equals(lib.getIdent() + ".json")) // Library specific
				readFile(file);
		}
	}

	private void readFile(File file) {
		StringBuilder builder = new StringBuilder();
		try {
			InputStream fis = new FileInputStream(file);
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(fis,
					"utf-8"));
			String line;	
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
	
			fis.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			JSONObject json = new JSONObject(builder.toString());
	
			// Detect layout of the JSON entries. Can be "field name":
			// "meaning" or "meaning": [ "field name", "field name", ... ]
			Iterator<String> iter = json.keys();
			if (!iter.hasNext())
				return; // No entries
	
			String firstKey = iter.next();
			Object firstValue = json.get(firstKey);
			boolean arrayLayout = firstValue instanceof JSONArray;
			if (arrayLayout) {
				for (int i = 0; i < ((JSONArray) firstValue).length(); i++)
					meanings.put(((JSONArray) firstValue).getString(i), firstKey);
				while (iter.hasNext()) {
					String key = iter.next();
					JSONArray val = json.getJSONArray(key);
					for (int i = 0; i < val.length(); i++)
						meanings.put(val.getString(i), key);
				}
			} else {
				meanings.put(firstKey, (String) firstValue);
				while (iter.hasNext()) {
					String key = iter.next();
					String val = json.getString(key);
					meanings.put(key, val);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public SearchField detectMeaning(SearchField field) {
		if (field.getData() != null && field.getData().has("meaning")) {
			try {
				String meaningData = field.getData().getString("meaning");	
				String meaningName = meanings.get(meaningData);
				if (meaningName != null)
					return processMeaning(field, meaningName);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else {
			String meaningName = meanings.get(field.getDisplayName());
			if (meaningName != null)
				return processMeaning(field, meaningName);
		}
		field.setAdvanced(true);
		return field;
	}
	
	private SearchField processMeaning(SearchField field, String meaningName) {
		Meaning meaning = Meaning.valueOf(meaningName);
		if (field instanceof TextSearchField && meaning == Meaning.FREE) {
			((TextSearchField) field).setFreeSearch(true);
			((TextSearchField) field).setHint(field.getDisplayName());
		} else if (field instanceof TextSearchField
				&& (meaning == Meaning.BARCODE || meaning == Meaning.ISBN)) {
			field = new BarcodeSearchField(field.getId(),
					field.getDisplayName(), field.isAdvanced(),
					((TextSearchField) field).isHalfWidth(),
					((TextSearchField) field).getHint());
		} else if (meaning == Meaning.AUDIENCE
				|| meaning == Meaning.SYSTEM
				|| meaning == Meaning.KEYWORD
				|| meaning == Meaning.PUBLISHER) {
			field.setAdvanced(true);
		}
		field.setMeaning(meaning);
		return field;
	}

}
