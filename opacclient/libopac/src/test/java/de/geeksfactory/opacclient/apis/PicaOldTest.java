package de.geeksfactory.opacclient.apis;

import org.jsoup.Jsoup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;

@RunWith(Parameterized.class)
public class PicaOldTest extends BaseTest {
    private String file;

    public PicaOldTest(String file) {
        this.file = file;
    }

    private static final String[] FILES = new String[]{"marburg.html", "frankfurt_stgeorgen.html"};

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
        String html = readResource("/pica_old/medialist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<Map<String, String>> media = new ArrayList<>();
        PicaOld.parseMediaList(media, Jsoup.parse(html), new DummyStringProvider(),
                new ArrayList<String>());
    }

    @Test
    public void testParseResList() throws OpacApi.OpacErrorException {
        String html = readResource("/pica_old/reslist/" + file);
        if (html == null) return; // we may not have all files for all libraries
        List<Map<String, String>> media = new ArrayList<>();
        PicaOld.parseResList(media, Jsoup.parse(html), new DummyStringProvider());
    }
}
