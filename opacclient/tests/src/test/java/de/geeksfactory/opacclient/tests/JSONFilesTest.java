package de.geeksfactory.opacclient.tests;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.tests.apitests.LibraryApiTestCases;

public class JSONFilesTest {

    private static final String FOLDER = "../opacapp/src/main";

    @Test
    public void testValidJson() throws IOException, JSONException {
        List<JSONException> exceptions = new ArrayList<>();
        StringBuilder message = new StringBuilder();
        for (String file : new File(FOLDER + "/assets/bibs/").list()) {
            try {
                Library.fromJSON(file.replace(".json", ""),
                        new JSONObject(LibraryApiTestCases.readFile(FOLDER + "/assets/bibs/" + file,
                                Charset.defaultCharset())));
            } catch (JSONException e) {
                message.append("\n" + file + " " + e.getMessage());
                exceptions.add(e);
            }
        }
        if (exceptions.size() > 0) {
            throw new RuntimeException(message.toString());
        }
    }
}
