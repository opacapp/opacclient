package de.geeksfactory.opacclient.apis;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public class TestApi implements OpacApi {
    private List<SearchResult> list = new ArrayList<>();
    private List<DetailledItem> detailList = new ArrayList<>();

    @Override
    public void start() throws IOException {

    }

    @Override
    public void init(Library library) {
        makeSearchResult("Kurz", null, false);
        makeSearchResult("Weit hinten, hinter den Wortbergen, fern der Länder", null, false);
        makeSearchResult("Kurz", null, true);
        makeSearchResult("Weit hinten, hinter den Wortbergen, fern der Länder", null, true);

        String image =
                "http://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Old_book_bindings" +
                        ".jpg/800px-Old_book_bindings.jpg";
        makeSearchResult("Kurz", image, false);
        makeSearchResult("Weit hinten, hinter den Wortbergen, fern der Länder", image, false);
        makeSearchResult("Kurz", image, true);
        makeSearchResult("Weit hinten, hinter den Wortbergen, fern der Länder", image, true);
    }

    @Override
    public SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException, JSONException {
        return new SearchRequestResult(list, list.size(), 1, 0);
    }

    private void makeSearchResult(String name, String url, boolean reservable) {
        SearchResult res = new SearchResult();
        res.setNr(list.size());
        res.setInnerhtml("<b>" + name + "</b><br/>Lorem ipsum <i>dolor</i> sit amet.");
        res.setCover(url);
        res.setType(SearchResult.MediaType.BOOK);
        list.add(res);
        DetailledItem item = new DetailledItem();
        item.setTitle(name);
        item.setReservable(reservable);
        item.setCover(url);
        item.addDetail(new Detail("Autor", "Max Mustermann"));
        item.addDetail(new Detail("Beschreibung", "Weit hinten, hinter den Wortbergen, " +
                "fern der Länder Vokalien und Konsonantien leben die Blindtexte. Abgeschieden " +
                "wohnen sie in Buchstabhausen an der Küste des Semantik, " +
                "eines großen Sprachozeans. Ein kleines Bächlein namens Duden fließt durch ihren " +
                "Ort und versorgt sie mit den nötigen Regelialien. Es ist ein paradiesmatisches " +
                "Land, in dem einem gebratene Satzteile in den Mund fliegen.\n" +
                "\n" +
                "Nicht einmal von der allmächtigen Interpunktion werden die Blindtexte beherrscht" +
                " – ein geradezu unorthographisches Leben. Eines Tages aber beschloß eine kleine " +
                "Zeile Blindtext, ihr Name war Lorem Ipsum, hinaus zu gehen in die weite " +
                "Grammatik. Der große Oxmox riet ihr davon ab, da es dort wimmele von bösen " +
                "Kommata, wilden Fragezeichen und hinterhältigen Semikoli, " +
                "doch das Blindtextchen ließ sich nicht beirren.\n" +
                "Es packte seine sieben Versalien, schob sich sein Initial in den Gürtel und " +
                "machte sich auf den Weg. Als es die ersten Hügel des Kursivgebirges erklommen " +
                "hatte, warf es einen letzten Blick zurück auf die Skyline seiner Heimatstadt " +
                "Buchstabhausen, die Headline von Alphabetdorf und die Subline seiner eigenen " +
                "Straße, der Zeilengasse. Wehmütig lief ihm eine rhetorische Frage über die " +
                "Wange, dann setzte es seinen Weg fort.\n" +
                "\n" +
                "Unterwegs traf es eine Copy. Die Copy warnte das Blindtextchen, da, " +
                "wo sie herkäme wäre sie zigmal umgeschrieben worden und alles, " +
                "was von ihrem Ursprung noch übrig wäre, sei das Wort \"und\" und das " +
                "Blindtextchen solle umkehren und wieder in sein eigenes, " +
                "sicheres Land zurückkehren.\n" +
                "\n" +
                "Doch alles Gutzureden konnte es nicht überzeugen und so dauerte es nicht lange, " +
                "bis ihm ein paar heimtückische Werbetexter auflauerten, " +
                "es mit Longe und Parole betrunken machten und es dann in ihre Agentur " +
                "schleppten, wo sie es für ihre Projekte wieder und wieder mißbrauchten. Und wenn" +
                " es nicht umgeschrieben wurde, dann benutzen Sie es immernoch." +
                ""));
        detailList.add(item);
    }

    @Override
    public SearchRequestResult volumeSearch(Map<String, String> query)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public SearchRequestResult searchGetPage(int page)
            throws IOException, OpacErrorException, JSONException {
        return null;
    }

    @Override
    public DetailledItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public DetailledItem getResult(int position) throws IOException, OpacErrorException {
        return detailList.get(position);
    }

    @Override
    public ReservationResult reservation(DetailledItem item, Account account,
            int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public AccountData account(Account account)
            throws IOException, JSONException, OpacErrorException {
        AccountData data = new AccountData(account.getId());
        List<Map<String, String>> lent = new ArrayList<>();
        List<Map<String, String>> reservations = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Map<String, String> lentItem = new HashMap<>();
            lentItem.put(AccountData.KEY_LENT_AUTHOR, "Max Mustermann");
            lentItem.put(AccountData.KEY_LENT_TITLE, "Lorem Ipsum");
            lentItem.put(AccountData.KEY_LENT_STATUS, "hier ist der Status");
            lent.add(lentItem);

            Map<String, String> reservationItem = new HashMap<>();
            reservationItem.put(AccountData.KEY_RESERVATION_AUTHOR, "Max Mustermann");
            reservationItem.put(AccountData.KEY_RESERVATION_TITLE, "Lorem Ipsum");
            reservationItem.put(AccountData.KEY_RESERVATION_READY, "heute");
            reservations.add(reservationItem);
        }
        data.setLent(lent);
        data.setReservations(reservations);
        return data;
    }

    @Override
    public void checkAccountData(Account account)
            throws IOException, JSONException, OpacErrorException {

    }

    @Override
    public List<SearchField> getSearchFields()
            throws IOException, OpacErrorException, JSONException {
        List<SearchField> fields = new ArrayList<>();
        fields.add(new TextSearchField("free", "Freie Suche", false, false, "Freie Suche", true,
                false));
        return fields;
    }

    @Override
    public boolean isAccountSupported(Library library) {
        return true;
    }

    @Override
    public boolean isAccountExtendable() {
        return false;
    }

    @Override
    public String getAccountExtendableInfo(Account account) throws IOException {
        return null;
    }

    @Override
    public String getShareUrl(String id, String title) {
        return null;
    }

    @Override
    public int getSupportFlags() {
        return 0;
    }

    @Override
    public boolean shouldUseMeaningDetector() {
        return false;
    }

    @Override
    public void setStringProvider(StringProvider stringProvider) {

    }

    @Override
    public Set<String> getSupportedLanguages() throws IOException {
        return null;
    }

    @Override
    public void setLanguage(String language) {

    }
}
