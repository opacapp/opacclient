package de.geeksfactory.opacclient.searchfields;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;

public class JavaMeaningDetector implements MeaningDetector {

    private final String assets_fieldsdir;
    private Map<String, String> meanings;

    public JavaMeaningDetector(Library lib) {
        this(lib, "opacapp/src/main/assets/meanings");
    }

    public JavaMeaningDetector(Library lib, String assets_fieldsdir) {
        meanings = new HashMap<>();
        this.assets_fieldsdir = assets_fieldsdir;
        if (lib != null) {
            File file;
            if ((file = new File(assets_fieldsdir, "general.json")).exists()) // General
                loadFile(file);
            if ((file = new File(assets_fieldsdir, lib.getApi() + ".json")).exists()) // Api
                // specific
                loadFile(file);
            if ((file = new File(assets_fieldsdir, lib.getIdent() + ".json")).exists()) // Library
                // specific
                loadFile(file);
        }
    }

    private static String readFile(File file) throws IOException {
        byte[] encoded = Files.readAllBytes(file.toPath());
        return Charset.forName("UTF-8").decode(ByteBuffer.wrap(encoded))
                .toString();
    }

    private static void writeFile(File file, String data) throws IOException {
        Files.write(file.toPath(), data.getBytes("UTF-8"));
    }

    private void loadFile(File file) {
        try {
            String jsonStr = readFile(file);
            JSONObject json = new JSONObject(jsonStr);

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
                    meanings.put(((JSONArray) firstValue).getString(i),
                            firstKey);
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
        } else if (meaning == Meaning.AUDIENCE || meaning == Meaning.SYSTEM
                || meaning == Meaning.KEYWORD || meaning == Meaning.PUBLISHER) {
            field.setAdvanced(true);
        }
        field.setMeaning(meaning);
        return field;
    }

    public Set<String> getIgnoredFields() throws IOException,
            JSONException {
        JSONArray json = new JSONArray(readFile(new File(assets_fieldsdir,
                "ignore.json")));
        Set<String> ignored = new HashSet<>();
        for (int i = 0; i < json.length(); i++) {
            ignored.add(json.getString(i));
        }
        return ignored;
    }

    public void addMeaning(String name, Meaning meaning)
            throws IOException, JSONException {
        File file = new File(assets_fieldsdir, "general.json");
        String jsonStr = readFile(file);
        JSONObject json = new JSONObject(jsonStr);

        // Detect layout of the JSON entries. Can be "field name":
        // "meaning" or "meaning": [ "field name", "field name", ... ]
        Iterator<String> iter = json.keys();
        if (!iter.hasNext())
            return; // No entries

        String firstKey = iter.next();
        Object firstValue = json.get(firstKey);
        boolean arrayLayout = firstValue instanceof JSONArray;
        if (arrayLayout) {
            json.getJSONArray(meaning.toString()).put(name);
        } else {
            json.put(name, meaning.toString());
        }
        writeFile(file, json.toString(4));

        meanings.put(name, meaning.toString());
    }

    public void addIgnoredField(String name) throws IOException,
            JSONException {
        File file = new File(assets_fieldsdir, "ignore.json");
        JSONArray json = new JSONArray(readFile(file));
        json.put(name);
        writeFile(file, json.toString(4));
    }
}
