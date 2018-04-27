package de.geeksfactory.opacclient.apis;

import org.junit.Test;

import de.geeksfactory.opacclient.i18n.DummyStringProvider;
import de.geeksfactory.opacclient.objects.DetailedItem;

import static org.junit.Assert.assertEquals;

public class WebOpacNetTest {
    @Test
    public void testSetTitleAndSubtitleSimple() {
        DetailedItem item = new DetailedItem();
        WebOpacNet.setTitleAndSubtitle(item, "A Title", new DummyStringProvider());
        assertEquals("A Title", item.getTitle());
        assertEquals(0, item.getDetails().size());
    }

    @Test
    public void testSetTitleAndSubtitleTitle() {
        DetailedItem item = new DetailedItem();
        WebOpacNet
                .setTitleAndSubtitle(item, "<¬1>K&#246;rper und Seele [DVD-Videoaufzeichnung]</¬1>",
                        new DummyStringProvider());
        assertEquals("Körper und Seele [DVD-Videoaufzeichnung]", item.getTitle());
        assertEquals(0, item.getDetails().size());
    }

    @Test
    public void testSetTitleAndSubtitleBoth() {
        DetailedItem item = new DetailedItem();
        WebOpacNet.setTitleAndSubtitle(item,
                "<¬1>Letzte Dinge regeln</¬1><¬2>f&#252;rs Lebensende vorsorgen - mit " +
                        "Todesf&#228;llen umgehen</¬2>",
                new DummyStringProvider());
        assertEquals("Letzte Dinge regeln", item.getTitle());
        assertEquals(1, item.getDetails().size());
        assertEquals("subtitle", item.getDetails().get(0).getDesc());
        assertEquals("fürs Lebensende vorsorgen - mit Todesfällen umgehen",
                item.getDetails().get(0).getContent());
    }
}
