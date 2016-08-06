package de.geeksfactory.opacclient.apis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BaseHtmlTest {
    /**
     * Reads content from an InputStream into a string
     *
     * @param is InputStream to read from
     * @return String content of the InputStream
     */
    private static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e1) {
            reader = new BufferedReader(new InputStreamReader(is));
        }
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    protected String readResource(String filename) {
        InputStream is = getClass().getResourceAsStream(filename);
        if (is == null) return null;
        try {
            return convertStreamToString(is);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        InputStream in = getClass().getResourceAsStream(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String resource;
        while ((resource = br.readLine()) != null) {
            filenames.add(resource);
        }
        return filenames;
    }

    protected static void assertContainsData(Map<String, String> item, String key) {
        assertTrue(item.containsKey(key));
        assertNotNull(item.get(key));
        assertNotEquals(item.get(key).trim(), "");
    }

    protected static void assertContainsData(String text) {
        assertNotNull(text);
        assertNotEquals(text, "");
    }

    protected static void assertNullOrNotEmpty(String text) {
        if (text != null) {
            assertNotEquals(text, "");
        }
    }
}
