package de.geeksfactory.opacclient.tests;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.tests.apitests.LibraryApiTestCases;

@RunWith(Parameterized.class)
public class JSONFilesTest {
    private static final String FOLDER = "../opacapp/src/main";
    private String json;
    private String library;

    public JSONFilesTest(String library) throws IOException {
        this.library = library;
        this.json = LibraryApiTestCases.readFile(FOLDER + "/assets/bibs/" + library + ".json",
                Charset.defaultCharset());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> libraries() {
        return LibraryApiTestCases.getLibraries(FOLDER);
    }


    @Test
    public void testValidJson() throws JSONException {
        Library.fromJSON(library, new JSONObject(json));
    }
}
