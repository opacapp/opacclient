package de.geeksfactory.opacclient.apis;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.utils.JsonKeyIterator;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertThat;

/**
 * Tests validity of JSON files shipped with libopac
 */
public class JSONFilesTest extends BaseHtmlTest {

    List<String> keys_both = Arrays.asList("title", "author", "format", "id", "status");
    List<String> keys_lent =
            Arrays.asList("barcode", "returndate", "homebranch", "lendingbranch", "prolongurl",
                    "renewable", "download");
    List<String> keys_reservations =
            Arrays.asList("availability", "expirationdate", "branch", "cancelurl", "bookingurl");

    List<String> keys_meaning;

    JSONObject bibliothecaHeadersLent;
    JSONObject bibliothecaHeadersReservations;
    JSONObject biber1992HeadersLent;
    JSONObject biber1992HeadersReservations;
    List<JSONObject> meaningsData;
    JSONArray meaningsIgnore;

    @Before
    public void setUp() throws JSONException, IOException {
        bibliothecaHeadersLent = new JSONObject(readResource("/bibliotheca/headers_lent.json"));
        bibliothecaHeadersReservations =
                new JSONObject(readResource("/bibliotheca/headers_reservations.json"));
        biber1992HeadersLent = new JSONObject(readResource("/biber1992/headers_lent.json"));
        biber1992HeadersReservations =
                new JSONObject(readResource("/biber1992/headers_reservations.json"));

        List<String> meaningsFiles = getResourceFiles("/meanings");
        meaningsData = new ArrayList<>();
        for (String filename : meaningsFiles) {
            String content = readResource("/meanings/" + filename);
            if (filename.equals("ignore.json")) {
                meaningsIgnore = new JSONArray(content);
            } else {
                meaningsData.add(new JSONObject(content));
            }
        }

        keys_meaning = new ArrayList<>();
        for (SearchField.Meaning meaning : SearchField.Meaning.values()) {
            keys_meaning.add(meaning.toString());
        }
    }

    @Test
    public void testBibliothecaHeadersLent() throws JSONException {
        Iterator iterator = bibliothecaHeadersLent.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            if (!bibliothecaHeadersLent.isNull(key)) {
                String value = bibliothecaHeadersLent.getString(key);
                assertThat(value, anyOf(isIn(keys_both), isIn(keys_lent)));
            }
        }
    }

    @Test
    public void testBibliothecaHeadersReservations() throws JSONException {
        Iterator iterator = bibliothecaHeadersReservations.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            if (!bibliothecaHeadersReservations.isNull(key)) {
                String value = bibliothecaHeadersReservations.getString(key);
                assertThat(value, anyOf(isIn(keys_both), isIn(keys_reservations)));
            }
        }
    }

    @Test
    public void testBiBer1992HeadersLent() throws JSONException {
        Iterator iterator = biber1992HeadersLent.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            if (!biber1992HeadersLent.isNull(key)) {
                String value = biber1992HeadersLent.getString(key);
                assertThat(value, anyOf(isIn(keys_both), isIn(keys_lent),
                        equalTo("author+title"), equalTo("renewals_number")));
            }
        }
    }

    @Test
    public void testBiBer1992HeadersReservations() throws JSONException {
        Iterator iterator = biber1992HeadersReservations.keys();
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            if (!biber1992HeadersReservations.isNull(key)) {
                String value = biber1992HeadersReservations.getString(key);
                assertThat(value, anyOf(isIn(keys_both), isIn(keys_reservations),
                        equalTo("author+title"), equalTo("renewals_number")));
            }
        }
    }

    @Test
    public void testMeanings() throws JSONException {
        for (JSONObject json : meaningsData) {
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
                assertThat(firstKey, isIn(keys_meaning));
                while (iter.hasNext()) {
                    String key = iter.next();
                    assertThat(key, isIn(keys_meaning));
                }
            } else {
                assertThat((String) firstValue, isIn(keys_meaning));
                while (iter.hasNext()) {
                    String key = iter.next();
                    String val = json.getString(key);
                    assertThat(val, isIn(keys_meaning));
                }
            }
        }
    }

    @Test
    public void testMeaningsIgnore() throws JSONException {
        for (int i = 0; i < meaningsIgnore.length(); i++) {
            assertThat(meaningsIgnore.get(i), instanceOf(String.class));
        }
    }
}
