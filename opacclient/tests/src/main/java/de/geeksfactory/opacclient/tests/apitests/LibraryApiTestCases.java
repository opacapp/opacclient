package de.geeksfactory.opacclient.tests.apitests;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.apis.Adis;
import de.geeksfactory.opacclient.apis.BiBer1992;
import de.geeksfactory.opacclient.apis.Bibliotheca;
import de.geeksfactory.opacclient.apis.IOpac;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.apis.Pica;
import de.geeksfactory.opacclient.apis.SISIS;
import de.geeksfactory.opacclient.apis.SRU;
import de.geeksfactory.opacclient.apis.TouchPoint;
import de.geeksfactory.opacclient.apis.WebOpacNet;
import de.geeksfactory.opacclient.apis.WinBiap;
import de.geeksfactory.opacclient.apis.Zones22;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.JavaMeaningDetector;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

@RunWith(Parallelized.class)
public class LibraryApiTestCases {

	private Library library;
	private OpacApi api;
	private List<SearchField> fields;
    private static final String FOLDER = "opacapp/src/main";

	public LibraryApiTestCases(String library) throws JSONException,
			IOException {
		this.library = Library.fromJSON(
				library,
				new JSONObject(readFile(FOLDER + "/assets/bibs/" + library + ".json",
						Charset.defaultCharset())));
	}

	@Parameters(name = "{0}")
	public static Collection<String[]> libraries() {
		List<String[]> libraries = new ArrayList<>();
		for (String file : new File(FOLDER + "/assets/bibs/").list()) {
			libraries.add(new String[] { file.replace(".json", "") });
		}
		return libraries;
	}

	@Before
	public void setUp() throws IOException,
			OpacErrorException, JSONException {
		Security.addProvider(new BouncyCastleProvider());
		api = getApi(library);
		fields = api.getSearchFields();
		JavaMeaningDetector detector = new JavaMeaningDetector(library);
		for (int i = 0; i < fields.size(); i++) {
			fields.set(i, detector.detectMeaning(fields.get(i)));
		}
	}

	@Test
	public void testEmptySearch() throws IOException,
			OpacErrorException, JSONException {
		List<SearchQuery> query = new ArrayList<>();
		SearchField field = findFreeSearchOrTitle(fields);
		if (field == null)
			throw new OpacErrorException( // TODO: prevent this
					"There is no free or title search field");
		query.add(new SearchQuery(field,
				"fasgeadstrehdaxydsfstrgdfjxnvgfhdtnbfgn"));
		try {
			SearchRequestResult res = api.search(query);
			assertTrue(res.getTotal_result_count() == 0);
		} catch (OpacErrorException e) {
			// Expected, should be an empty result.
		}
	}

	@Test
	public void testSearchScrolling() throws
            IOException, OpacErrorException, JSONException {
		List<SearchQuery> query = new ArrayList<>();
		SearchField field = findFreeSearchOrTitle(fields);
		if (field == null)
			throw new OpacErrorException( // TODO: prevent this
					"There is no free or title search field");
		query.add(new SearchQuery(field, "harry"));
		SearchRequestResult res = api.search(query);
		assertTrue(res.getResults().size() <= res.getTotal_result_count());
		assertTrue(res.getResults().size() > 0);

		SearchResult third;
		if (res.getResults().size() >= 3)
			third = res.getResults().get(2);
		else
			third = res.getResults().get(res.getResults().size() - 1);
		DetailledItem detail = null;
		if (third.getId() != null)
			detail = api.getResultById(third.getId(), "");
		else
			detail = api.getResult(third.getNr());
		assertNotNull(detail);
		confirmDetail(third, detail);

		if (res.getResults().size() < res.getTotal_result_count()) {
			api.searchGetPage(2);
			SearchResult second = res.getResults().get(1);
			DetailledItem detail2 = null;
			if (second.getId() != null)
				detail2 = api.getResultById(second.getId(), "");
			else
				detail2 = api.getResult(second.getNr());
			confirmDetail(second, detail2);
		}
	}

	/**
	 * Create an account with credentials that probably nobody has and try to
	 * login. This should normally give an OpacErrorException.
	 */
	@Test
	public void testWrongLogin() throws IOException, JSONException {
		if (!api.isAccountSupported(library))
			return;
		Account account = new Account();
		account.setId(0);
		account.setLabel("Test account");
		account.setLibrary(library.getIdent());
		account.setName("upvlgFLMNN2AyVsIzowcwzdypRXM2x");
		account.setPassword("OTqbXhMJMKtjconhxX0LXMqWZsY2Ez");
		
		OpacErrorException exception = null;
		try {
			api.checkAccountData(account);
		} catch (OpacErrorException e) {
			exception = e;
		}
		assertTrue(exception != null);
	}

	private void confirmDetail(SearchResult result, DetailledItem detail) {
		assertTrue(detail != null);
		assertTrue(detail.getDetails().size() > 0);
		if (detail.isReservable())
			assertTrue(detail.getReservation_info() != null);
		if (result.getId() != null && detail.getId() != null
				&& !detail.getId().equals("")) {
			assertTrue(result.getId().equals(detail.getId()));
		}
		if (detail.getTitle() != null) {
			// At least 30% of the words in the title should already have been
			// in the search results, if we got the correct item.
			float cnt = 0;
			float fnd = 0;
			String innerstring = Jsoup.parse(result.getInnerhtml()).text();
			for (String word : detail.getTitle().split(" ")) {
				if (innerstring.contains(word))
					fnd++;
				cnt++;
			}
			assertTrue(fnd > 0);
		}
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	/**
	 * @param fields
	 *            List of SearchFields
	 * @return The first free search field from the list. If there is none, the
	 *         title search fields and if that doesn't exist either, null
	 */
	private SearchField findFreeSearchOrTitle(List<SearchField> fields) {
		for (SearchField field : fields) {
			if (field instanceof TextSearchField
					&& ((TextSearchField) field).isFreeSearch())
				return field;
		}
		for (SearchField field : fields) {
			if (field instanceof TextSearchField
					&& field.getMeaning() == Meaning.TITLE)
				return field;
		}
		return null;
	}

	public static OpacApi getApi(Library library) {
		OpacApi api = null;
		if (library.getApi().equals("bond26")
				|| library.getApi().equals("bibliotheca"))
			// Backwardscompatibility
			api = new Bibliotheca();
		else if (library.getApi().equals("oclc2011")
				|| library.getApi().equals("sisis"))
			// Backwards compatibility
			api = new SISIS();
		else if (library.getApi().equals("zones22"))
			api = new Zones22();
		else if (library.getApi().equals("biber1992"))
			api = new BiBer1992();
		else if (library.getApi().equals("pica"))
			api = new Pica();
		else if (library.getApi().equals("iopac"))
			api = new IOpac();
		else if (library.getApi().equals("adis"))
			api = new Adis();
		else if (library.getApi().equals("sru"))
			api = new SRU();
		else if (library.getApi().equals("winbiap"))
			api = new WinBiap();
		else if (library.getApi().equals("webopac.net"))
			api = new WebOpacNet();
		else if (library.getApi().equals("touchpoint"))
			api = new TouchPoint();
		else
			api = null;
		api.init(library);
		return api;
	}
}
