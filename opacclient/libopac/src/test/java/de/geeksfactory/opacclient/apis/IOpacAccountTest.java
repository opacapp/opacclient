package de.geeksfactory.opacclient.apis;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.objects.AccountData;

@RunWith(Parameterized.class)
public class IOpacAccountTest extends BaseAccountTest {
    private String file;

    public IOpacAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES = new String[]{"heide.html"};

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> files() {
        List<String[]> files = new ArrayList<>();
        for (String file : FILES) {
            files.add(new String[]{file});
        }
        return files;
    }

    @Test
    public void testParseMediaList() throws OpacApi.OpacErrorException {
        String html = readResource("/iopac/" + file);
        List<Map<String, String>> media = new ArrayList<>();
        IOpac.parseMediaList(media, Jsoup.parse(html), new JSONObject());
        for (Map<String, String> item : media) {
            assertContainsData(item, AccountData.KEY_LENT_TITLE);
            assertContainsData(item, AccountData.KEY_LENT_DEADLINE);
            assertContainsData(item, AccountData.KEY_LENT_DEADLINE_TIMESTAMP);
        }
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException {
        String html = readResource("/iopac/" + file);
        List<Map<String, String>> media = new ArrayList<>();
        IOpac.parseResList(media, Jsoup.parse(html), new JSONObject());
    }
}
