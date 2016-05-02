package de.geeksfactory.opacclient.apis;

import org.joda.time.LocalDate;
import org.junit.Test;

import java.util.Objects;

public class LitteraTest {

    @Test
    public void testParseTotalResults() {
        assert Littera.parseTotalResults("Treffer pro Seite  •  1 - 10 von 46 Treffern") == 46;
        assert Littera.parseTotalResults("Hits per page  •  1 - 10 of 46") == 46;
        assert Littera.parseTotalResults("Sayfa başına sonuç  •  46 sonuçtan 1 - 10") == 46;
    }

    @Test
    public void testParseCopyReturn() throws Exception {
        assert Objects.equals(Littera.parseCopyReturn("1 (voraussichtl. bis 31.07.2015)"),
                new LocalDate(2015, 7, 31));
    }

}
