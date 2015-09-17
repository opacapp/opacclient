package de.geeksfactory.opacclient.apis;

import org.junit.Test;

import java.util.Objects;

public class WebOpacAtTest {

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

}
