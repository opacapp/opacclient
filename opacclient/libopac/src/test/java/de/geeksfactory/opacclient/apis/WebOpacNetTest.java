package de.geeksfactory.opacclient.apis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebOpacNetTest {
    @Test
    public void testSetTitleAndSubtitleSimple() {
        String[] titleAndSubtitle = WebOpacNet.getTitleAndSubtitle("A Title");
        assertEquals("A Title", titleAndSubtitle[0]);
        assertEquals(1, titleAndSubtitle.length);
    }

    @Test
    public void testSetTitleAndSubtitleTitle() {
        String[] titleAndSubtitle = WebOpacNet
                .getTitleAndSubtitle("<¬1>K&#246;rper und Seele [DVD-Videoaufzeichnung]</¬1>");
        assertEquals("Körper und Seele [DVD-Videoaufzeichnung]", titleAndSubtitle[0]);
        assertEquals(1, titleAndSubtitle.length);
    }

    @Test
    public void testSetTitleAndSubtitleBoth() {
        String[] titleAndSubtitle = WebOpacNet.getTitleAndSubtitle(
                "<¬1>Letzte Dinge regeln</¬1><¬2>f&#252;rs Lebensende vorsorgen - mit " +
                        "Todesf&#228;llen umgehen</¬2>");
        assertEquals("Letzte Dinge regeln", titleAndSubtitle[0]);
        assertEquals("fürs Lebensende vorsorgen - mit Todesfällen umgehen", titleAndSubtitle[1]);
    }
}
