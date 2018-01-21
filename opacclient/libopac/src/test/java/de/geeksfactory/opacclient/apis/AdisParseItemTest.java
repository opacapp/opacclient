package de.geeksfactory.opacclient.apis;

import org.junit.Test;

import de.geeksfactory.opacclient.objects.AccountItem;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.SearchResult;

import static org.junit.Assert.assertEquals;

public class AdisParseItemTest {
    @Test
    public void testParseDvd() {
        testParseItem("[DVD-Video]  ¬La¬ boum 2 - die Fete geht weiter  [DVD] "
                , "La boum 2 - die Fete geht weiter"
                , null
                , SearchResult.MediaType.DVD);
    }

    @Test
    public void testParseSpaces() {
        // 2 Spaces
        testParseItem("[CD]  Some kind of trouble  [CD] / James Blunt#TR Pop/Rock B#15332436"
                , "Some kind of trouble"
                , "James Blunt"
                , SearchResult.MediaType.CD);

        // No Spaces
        testParseItem("[CD]Some kind of trouble[CD] / James  Blunt#TR Pop/Rock B#15332436"
                , "Some kind of trouble"
                , "James  Blunt"
                , SearchResult.MediaType.CD);
    }

    @Test
    public void testParseCd() {
        testParseItem("[CD] Some kind  of trouble [CD] / James Blunt#TR Pop/Rock B#15332436"
        ,"Some kind of trouble"
                , "James Blunt"
                , SearchResult.MediaType.CD);
    }

    @Test
    public void testParseBook() {
        testParseItem(" Blitzlichtgewitter : Roman / Christian Linker#5.2 Link#15167448"
                , "Blitzlichtgewitter"
                , "Christian Linker"
                , SearchResult.MediaType.BOOK);
    }

    @Test
    public void testParseUnknown() {
        // Unbekannter MediaType
        testParseItem("[HUGO] Some kind  of  trouble [HUGO] / James Blunt#TR Pop/Rock B#15332436"
                , "[HUGO] Some kind of trouble [HUGO]"
                , "James Blunt", SearchResult.MediaType.UNKNOWN);
    }

    private void testParseItem(String text, String title, String author,
            SearchResult.MediaType mediaType) {
        String[] split = text.split("[/#\n]");
        AccountItem item = new LentItem();
        Adis.parseItemText(split, true, item);
        assertEquals(title, item.getTitle());
        assertEquals(author, item.getAuthor());
        assertEquals(mediaType, item.getMediaType());
    }
}
