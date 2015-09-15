package de.geeksfactory.opacclient.apis;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.Objects;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.SearchQuery;

public class WebOpacAtTest {

    WebOpacAt getApi() {
        WebOpacAt api = new WebOpacAt();
        Library lib = new Library();
        lib.setData(new JSONObject(Collections.singletonMap("baseurl", "http://www.stbinnsbruck.web-opac.at")));
        api.init(lib);
        return api;
    }

    @Test
    public void testParseTotalResults() {
        assert WebOpacAt.parseTotalResults("Treffer pro Seite  •  1 - 10 von 46 Treffern") == 46;
        assert WebOpacAt.parseTotalResults("Hits per page  •  1 - 10 of 46") == 46;
        assert WebOpacAt.parseTotalResults("Sayfa başına sonuç  •  46 sonuçtan 1 - 10") == 46;
    }

    @Test
    public void testParseCopyReturn() throws Exception {
        assert Objects.equals(WebOpacAt.parseCopyReturn("1 (voraussichtl. bis 31.07.2015)"), "31.07.2015");

    }

    @Test
    public void testSearchFields() throws Exception {
        getApi().getSearchFields();
    }

    @Test
    public void testEmptyQuery() throws Exception {
        getApi().search(Collections.<SearchQuery>emptyList());
    }

}
