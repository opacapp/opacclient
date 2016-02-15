package de.geeksfactory.opacclient.apis;

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
public class ZonesAccountTest extends BaseAccountTest {
    private String file;

    public ZonesAccountTest(String file) {
        this.file = file;
    }

    private static final String[] FILES = new String[]{"koeln.html"};

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
        String html = readResource("/zones/medialist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<Map<String, String>> media = Zones.parseMediaList(Jsoup.parse(html));
        for (Map<String, String> item : media) {
            assertContainsData(item, AccountData.KEY_LENT_TITLE);
            assertContainsData(item, AccountData.KEY_LENT_DEADLINE);
            assertContainsData(item, AccountData.KEY_LENT_DEADLINE_TIMESTAMP);
        }
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException {
        String html = readResource("/zones/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<Map<String, String>> media = Zones.parseResList(Jsoup.parse(html));
    }
}
