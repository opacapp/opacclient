package de.geeksfactory.opacclient.meanings;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.MeaningDetectorImpl;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.utils.JsonKeyIterator;

public class Main {

    public static final String BIBS_DIR = "../opacapp/src/main/assets/bibs/";
    public static final String MEANINGS_DIR = "../libopac/src/main/resources/meanings/";

    public static void main(String[] args) throws IOException, JSONException {
        Security.addProvider(new BouncyCastleProvider());
        Collection<String[]> libraries = libraries();
        Set<String> ignored = new MeaningDetectorImpl(null).getIgnoredFields();
        Scanner in = new Scanner(System.in);
        final ExecutorService service = Executors.newFixedThreadPool(25);
        List<TaskInfo> tasks = new ArrayList<>();
        for (String[] libraryNameArray : libraries) {
            String libraryName = libraryNameArray[0];
            Library library;
            try {
                library = Library.fromJSON(
                        libraryName,
                        new JSONObject(readFile(BIBS_DIR + libraryName + ".json")));
                Future<Map<String, List<SearchField>>> future = service
                        .submit(new GetSearchFieldsCallable(library));
                tasks.add(new TaskInfo(library, future));
            } catch (JSONException | IOException e) {
                // e.printStackTrace();
            }
        }

        for (TaskInfo entry : tasks) {
            Library library = entry.lib;
            try {
                Map<String, List<SearchField>> fields = entry.future.get();
                if (fields == null)
                    continue;
                for (String lang : fields.keySet()) {
                    System.out.println("Bibliothek: " + library.getIdent()
                            + ", Sprache: " + lang);
                    MeaningDetectorImpl detector = new MeaningDetectorImpl(library);
                    for (int i = 0; i < fields.get(lang).size(); i++) {
                        fields.get(lang)
                              .set(i,
                                      detector.detectMeaning(fields.get(lang)
                                                                   .get(i)));
                    }
                    for (SearchField field : fields.get(lang)) {
                        if (field.getMeaning() != null
                                || ignored.contains(field.getDisplayName())
                                || field.getData() != null
                                && field.getData().has("meaning")
                                && ignored.contains(field.getData().getString(
                                "meaning")))
                            continue;
                        String name;
                        if (field.getData() != null
                                && field.getData().has("meaning")) {
                            name = field.getData().getString("meaning");
                            System.out.print("Unbekanntes Feld: '" + name
                                    + "' (Anzeigename: "
                                    + field.getDisplayName() + ") ");
                        } else {
                            name = field.getDisplayName();
                            System.out.print("Unbekanntes Feld: '" + name
                                    + "' ");
                        }
                        Meaning meaning = null;
                        boolean ignoredField = false;
                        while (meaning == null && !ignoredField) {
                            String str = in.nextLine();
                            if (str.equals("")
                                    || str.toLowerCase().equals("ignore")) {
                                ignoredField = true;
                                addIgnoredField(name);
                                ignored.add(name);
                            } else {
                                try {
                                    meaning = Meaning
                                            .valueOf(str.toUpperCase());
                                } catch (IllegalArgumentException e) {
                                    meaning = null;
                                }
                            }
                        }
                        if (meaning != null) {
                            detector.addMeaning(name, meaning);
                            saveMeaning(name, meaning);
                        }
                    }
                }
            } catch (JSONException | IOException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }
        in.close();
        service.shutdown();
    }

    private static Collection<String[]> libraries() {
        List<String[]> libraries = new ArrayList<>();
        for (String file : new File(BIBS_DIR).list()) {
            libraries.add(new String[]{file.replace(".json", "")});
        }
        return libraries;
    }

    private static String readFile(String path)
            throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return Charset.forName("UTF-8").decode(ByteBuffer.wrap(encoded)).toString();
    }

    private static void writeFile(String path, String data) throws IOException {
        Files.write(new File(path).toPath(), data.getBytes("UTF-8"));
    }

    private static class TaskInfo {
        public Future<Map<String, List<SearchField>>> future;
        public Library lib;

        public TaskInfo(Library lib,
                Future<Map<String, List<SearchField>>> future) {
            this.future = future;
            this.lib = lib;
        }
    }


    private static void addIgnoredField(String name) throws IOException,
            JSONException {
        String filename = MEANINGS_DIR + "ignore.json";
        JSONArray json = new JSONArray(readFile(filename));
        json.put(name);
        writeFile(filename, json.toString(4));
    }

    private static void saveMeaning(String name, Meaning meaning)
            throws IOException, JSONException {
        String filename = MEANINGS_DIR + "general.json";
        JSONObject json = new JSONObject(readFile(filename));

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
            json.getJSONArray(meaning.toString()).put(name);
        } else {
            json.put(name, meaning.toString());
        }
        writeFile(filename, json.toString(4));
    }
}
