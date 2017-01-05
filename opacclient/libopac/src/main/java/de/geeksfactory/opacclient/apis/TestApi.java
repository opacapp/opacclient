package de.geeksfactory.opacclient.apis;

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.Detail;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.LentItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.ReservedItem;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public class TestApi extends ApacheBaseApi {
    private Library library;
    private List<SearchResult> list = new ArrayList<>();
    private List<DetailedItem> detailList = new ArrayList<>();

    @Override
    public void start() throws IOException {

    }

    @Override
    public void init(Library library, HttpClientFactory httpClientFactory) {
        this.library = library;

        makeSearchResult("Kurz", null, false);
        makeSearchResult("Weit hinten, hinter den Wortbergen, fern der Länder", null, false);
        makeSearchResult("Kurz", null, true);
        makeSearchResult("Weit hinten, hinter den Wortbergen, fern der Länder", null, true);

        String image =
                "http://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Old_book_bindings" +
                        ".jpg/800px-Old_book_bindings.jpg";
        makeSearchResult("Kurz", image, false);
        makeSearchResult("Weit hinten, hinter den Wortbergen, fern der Länder", image, false);
        makeSearchResult(
                "Weit hinten, hinter den Wortbergen, fern der Länder Vokalien und Konsonantien " +
                        "leben die Blindtexte.",
                image, false);
        makeSearchResult("Kurz", image, true);
        makeSearchResult("Weit hinten, hinter den Wortbergen, fern der Länder", image, true);

        super.init(library, httpClientFactory);
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
        DetailedItem item = new DetailedItem();
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
    public DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException {
        return null;
    }

    @Override
    public DetailedItem getResult(int position) throws IOException, OpacErrorException {
        return detailList.get(position);
    }

    @Override
    public ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException {
        return null;
    }

    @Override
    public ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ProlongResult(MultiStepResult.Status.OK);
    }

    @Override
    public ProlongAllResult prolongAll(Account account, int useraction, String selection)
            throws IOException {
        return null;
    }

    @Override
    public CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new CancelResult(MultiStepResult.Status.OK);
    }

    @Override
    public AccountData account(Account account)
            throws IOException, JSONException, OpacErrorException {
        AccountData data = new AccountData(account.getId());
        List<LentItem> lent = new ArrayList<>();
        List<ReservedItem> reservations = new ArrayList<>();

        try {
            JSONObject d = new JSONObject(httpGet(library.getData().getString("url"), "UTF-8"));

            for (int i = 0; i < d.getJSONArray("lent").length(); i++) {
                JSONObject l = d.getJSONArray("lent").getJSONObject(i);
                LentItem lentItem = new LentItem();
                for (Iterator iter = l.keys(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    lentItem.set(key, l.getString(key));
                }
                lent.add(lentItem);
            }
            for (int i = 0; i < d.getJSONArray("reservations").length(); i++) {
                JSONObject l = d.getJSONArray("reservations").getJSONObject(i);
                ReservedItem resItem = new ReservedItem();
                for (Iterator iter = l.keys(); iter.hasNext(); ) {
                    String key = (String) iter.next();
                    resItem.set(key, l.getString(key));
                }
                reservations.add(resItem);
            }
        } catch (NotReachableException e) {
            for (int i = 0; i < 6; i++) {
                LentItem lentItem = new LentItem();
                lentItem.setAuthor("Max Mustermann");
                lentItem.setTitle("Lorem Ipsum");
                lentItem.setStatus("hier ist der Status");
                lentItem.setDeadline(new LocalDate(1442564454547L));
                lentItem.setRenewable(true);
                lentItem.setProlongData("foo");
                lentItem.setHomeBranch("Meine Zweigstelle");
                lentItem.setLendingBranch("Ausleihzweigstelle");
                lentItem.setBarcode("Barcode");
                lent.add(lentItem);

                ReservedItem reservedItem = new ReservedItem();
                reservedItem.setAuthor("Max Mustermann");
                reservedItem.setTitle("Lorem Ipsum");
                reservedItem.setReadyDate(LocalDate.now());
                reservations.add(reservedItem);
            }
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
    public List<SearchField> parseSearchFields()
            throws IOException, OpacErrorException, JSONException {
        List<SearchField> fields = new ArrayList<>();
        fields.add(new TextSearchField("free", "Freie Suche", false, false, "Freie Suche", true,
                false));
        return fields;
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
