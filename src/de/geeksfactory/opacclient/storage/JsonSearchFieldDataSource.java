package de.geeksfactory.opacclient.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import de.geeksfactory.opacclient.searchfields.SearchField;

public class JsonSearchFieldDataSource implements SearchFieldDataSource {

	private File dir;
	private Context context;

	private static final String KEY_FIELDS = "fields";
	private static final String KEY_TIME = "time";
	private static final String KEY_VERSION = "version";

	public JsonSearchFieldDataSource(Context context) {
		this.dir = new File(context.getFilesDir(), "fields");
		dir.mkdirs();
		this.context = context;
	}

	@Override
	public void saveSearchFields(String libraryId, List<SearchField> fields) {
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		try {
			for (SearchField field : fields) {
				array.put(field.toJSON());
			}
			object.put(KEY_FIELDS, array);
			object.put(KEY_TIME, System.currentTimeMillis());
			try {
				object.put(
						KEY_VERSION,
						context.getPackageManager().getPackageInfo(
								context.getPackageName(), 0).versionCode);
			} catch (NameNotFoundException e) {
				// should never happen
				e.printStackTrace();
			}
			writeToJsonFile(libraryId, object);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<SearchField> getSearchFields(String libraryId) {
		try {
			JSONObject json = readJsonFile(libraryId);
			List<SearchField> list = new ArrayList<SearchField>();
			JSONArray fields = json.getJSONArray(KEY_FIELDS);
			for (int i = 0; i < fields.length(); i++) {
				list.add(SearchField.fromJSON(fields.getJSONObject(i)));
			}
			return list;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean hasSearchFields(String libraryId) {
		return jsonFileExists(libraryId);
	}

	@Override
	public void clearSearchFields(String libraryId) {
		removeJsonFile(libraryId);
	}

	@Override
	public void clearAll() {
		File[] files = dir.listFiles();
		for (File file : files) {
			file.delete();
		}
	}

	@Override
	public long getLastSearchFieldUpdateTime(String libraryId) {
		try {
			JSONObject json = readJsonFile(libraryId);
			return json.getLong(KEY_TIME);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int getLastSearchFieldUpdateVersion(String libraryId) {
		try {
			JSONObject json = readJsonFile(libraryId);
			return json.getInt(KEY_VERSION);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private void writeToJsonFile(String filename, JSONObject data) {
		File file = new File(dir, filename + ".json");
		try {
			PrintWriter writer = new PrintWriter(file, "UTF-8");
			writer.print(data.toString());
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private JSONObject readJsonFile(String filename) throws IOException,
			JSONException {
		File file = new File(dir, filename + ".json");
		BufferedReader br = new BufferedReader(new FileReader(file));
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			String data = sb.toString();
			return new JSONObject(data);
		} finally {
			br.close();
		}
	}

	private boolean jsonFileExists(String filename) {
		File file = new File(dir, filename + ".json");
		return file.exists();
	}

	private void removeJsonFile(String filename) {
		File file = new File(dir, filename + ".json");
		file.delete();
	}
}
