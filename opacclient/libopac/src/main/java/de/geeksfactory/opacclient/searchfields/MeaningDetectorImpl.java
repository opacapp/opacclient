package de.geeksfactory.opacclient.searchfields;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.utils.JsonKeyIterator;

public class MeaningDetectorImpl implements MeaningDetector {

    private static final String DIR = "meanings";
    private Map<String, String> meanings;
    private ClassLoader classLoader;

    public MeaningDetectorImpl(Library lib) {
        meanings = new HashMap<>();
        classLoader = getClass().getClassLoader();

        if (lib != null) {
            InputStream file;
            if ((file = getFile("general.json")) != null) // General
            {
                loadFile(file);
            }
            if ((file = getFile(lib.getApi() + ".json")) != null) // Api specific
            {
                loadFile(file);
            }
            if ((file = getFile(lib.getIdent() + ".json")) != null) // Library specific
            {
                loadFile(file);
            }
        }
    }

    private InputStream getFile(String s) {
        return classLoader.getResourceAsStream(DIR + "/" + s);
    }

    private static String readFile(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        is.close();
        return builder.toString();
    }

    private void loadFile(InputStream is) {
        try {
            String jsonStr = readFile(is);
            JSONObject json = new JSONObject(jsonStr);

            // Detect layout of the JSON entries. Can be "field name":
            // "meaning" or "meaning": [ "field name", "field name", ... ]
            Iterator<String> iter = new JsonKeyIterator(json);
            if (!iter.hasNext()) {
                return; // No entries
            }

            String firstKey = iter.next();
            Object firstValue = json.get(firstKey);
            boolean arrayLayout = firstValue instanceof JSONArray;
            if (arrayLayout) {
                for (int i = 0; i < ((JSONArray) firstValue).length(); i++) {
                    meanings.put(((JSONArray) firstValue).getString(i),
                            firstKey);
                }
                while (iter.hasNext()) {
                    String key = iter.next();
                    JSONArray val = json.getJSONArray(key);
                    for (int i = 0; i < val.length(); i++) {
                        meanings.put(val.getString(i), key);
                    }
                }
            } else {
                meanings.put(firstKey, (String) firstValue);
                while (iter.hasNext()) {
                    String key = iter.next();
                    String val = json.getString(key);
                    meanings.put(key, val);
                }
            }
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SearchField detectMeaning(SearchField field) {
        if (field.getData() != null && field.getData().has("meaning")) {
            try {
                String meaningData = field.getData().getString("meaning");
                String meaningName = meanings.get(meaningData);
                if (meaningName != null) {
                    return processMeaning(field, meaningName);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            String meaningName = meanings.get(field.getDisplayName());
            if (meaningName != null) {
                return processMeaning(field, meaningName);
            }
        }
        field.setAdvanced(true);
        return field;
    }

    /**
     * Adds a meaning to this MeaningDetector. This will not be persisted.
     *
     * @param name    the name of the field
     * @param meaning the meaning to assign
     */
    public void addMeaning(String name, Meaning meaning) {
        meanings.put(name, meaning.toString());
    }

    private SearchField processMeaning(SearchField field, String meaningName) {
        Meaning meaning = Meaning.valueOf(meaningName);
        SearchField oldfield = field;
        if (field instanceof TextSearchField && meaning == Meaning.FREE) {
            ((TextSearchField) field).setFreeSearch(true);
            ((TextSearchField) field).setHint(field.getDisplayName());
        } else if (field instanceof TextSearchField
                && (meaning == Meaning.BARCODE || meaning == Meaning.ISBN)) {
            field = new BarcodeSearchField(field.getId(),
                    field.getDisplayName(), field.isAdvanced(),
                    ((TextSearchField) field).isHalfWidth(),
                    ((TextSearchField) field).getHint());
        } else if (meaning == Meaning.AUDIENCE || meaning == Meaning.SYSTEM
                || meaning == Meaning.KEYWORD || meaning == Meaning.PUBLISHER) {
            field.setAdvanced(true);
        }
        field.setData(oldfield.getData());
        field.setMeaning(meaning);
        return field;
    }

    public Set<String> getIgnoredFields() throws IOException,
            JSONException {
        JSONArray json = new JSONArray(readFile(getFile("ignore.json")));
        Set<String> ignored = new HashSet<>();
        for (int i = 0; i < json.length(); i++) {
            ignored.add(json.getString(i));
        }
        return ignored;
    }
}
