package de.geeksfactory.opacclient.apis;

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class AdisSearchTest extends BaseHtmlTest {
    private String file;

    public AdisSearchTest(String file) {
        this.file = file;
    }

    private static final String[] FILES =
            new String[]{"Stuttgart_StB.html", "Nuernberg.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Test
    public void testParseSearchPage() throws OpacApi.OpacErrorException, JSONException {
        String html = readResource("/adis/search/" + file);
        try {
            List<SearchField> searchFields = Adis.parseSearchFields(Jsoup.parse(html));
            for (SearchField searchField : searchFields) {
                assertNotNull(searchField.getDisplayName());
                if (searchField instanceof DropdownSearchField) {
                    DropdownSearchField field = (DropdownSearchField) searchField;
                    assertTrue(field.getDropdownValues().size() > 0);
                }
                if ("AUSGAB_1".equals(searchField.getId())) {
                    if (!(searchField instanceof DropdownSearchField)) {
                        fail("AUSGAB_1 not instanceof DropdownSearchField");
                    }
                }
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
