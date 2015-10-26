package de.geeksfactory.opacclient.apis;

import org.junit.Test;

import java.util.Arrays;

public class BiBer1992Test {

    @Test
    public void testParseTitles() {
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Signature / Author: Bar"),
                new String[]{"Bar", "Author"});
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Signature / Author:"),
                new String[]{"", "Author"});
        assert Arrays
                .equals(BiBer1992.findTitleAndAuthor("Signature / Bar"), new String[]{"Bar", null});
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Signature / "), new String[]{"", null});
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Bar"), new String[]{"Bar", null});

        // Found in Freising
        assert Arrays
                .equals(BiBer1992.findTitleAndAuthor("Author: Bar"), new String[]{"Bar", "Author"});
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Android Welt 3/15 Mai-Juni"),
                new String[]{"Android Welt 3/15 Mai-Juni", null});

        // Found in Waiblingen
        // They assured us only test items have those useless slashes, so we do not add extra
        // code for this, but we take it as a stress-test for the parser
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Test / /English /for /runaways"),
                new String[]{"/English /for /runaways", null});
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Test / /Andersen, /Lale: /Mein /Leben"),
                new String[]{"/Mein /Leben", "/Andersen, /Lale"});

        // Found in Hagen
        // "Springe" seems to be one of their branches
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Buch/Jonasson, Jonas: Die / Springe"),
                new String[]{"Die / Springe", "Jonasson, Jonas"});
        assert Arrays.equals(BiBer1992.findTitleAndAuthor("Buch/Deutschland - Osten / Springe"),
                new String[]{"Deutschland - Osten / Springe", null});
    }

}
