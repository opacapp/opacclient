package de.geeksfactory.opacclient.apis;

import org.jsoup.Jsoup;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;

public class PicaOldTest extends BaseTest {
    @Test
    public void testParseMediaList() throws OpacApi.OpacErrorException {
        String html = readResource("/pica_old/medialist/marburg.html");
        List<Map<String, String>> media = new ArrayList<>();
        PicaOld.parseMediaList(media, Jsoup.parse(html), new DummyStringProvider(),
                new ArrayList<String>());
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException {
        String html = readResource("/pica_old/reslist/marburg.html");
        List<Map<String, String>> media = new ArrayList<>();
        PicaOld.parseResList(media, Jsoup.parse(html), new DummyStringProvider());
    }
}
