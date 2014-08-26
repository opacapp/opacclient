package de.geeksfactory.opacclient.tests.apitests;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

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
import de.geeksfactory.opacclient.apis.WebOpacNet;
import de.geeksfactory.opacclient.apis.WinBiap;
import de.geeksfactory.opacclient.apis.Zones22;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.objects.SearchResult;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.storage.DummyMetaDataSource;

@RunWith(Parallelized.class)
public class LibraryApiTestCases extends TestCase {

	private Library library;
	private OpacApi api;

	public LibraryApiTestCases(String library) throws JSONException,
			IOException {
		this.library = Library.fromJSON(
				library,
				new JSONObject(readFile("../assets/bibs/" + library + ".json",
						Charset.defaultCharset())));
	}

	@Parameters(name = "{0}")
	public static Collection<String[]> libraries() {
		List<String[]> libraries = new ArrayList<String[]>();
		for (String file : new File("../assets/bibs/").list()) {
			libraries.add(new String[] { file.replace(".json", "") });
		}
		return libraries;
	}

	@Before
	public void setUp() {
		api = null;
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
		else
			api = null;
		api.init(new DummyMetaDataSource(), library);
	}

	@Test
	public void testEmptySearch() throws NotReachableException, IOException,
			OpacErrorException {
		Map<String, String> query = new HashMap<String, String>();
		SearchField field = findFreeSearchOrTitle(api.getSearchFields());
		if (field == null)
			throw new OpacErrorException( //TODO: prevent this
					"There is no free or title search field");
		query.put(field.getId(), "fasgeadstrehdaxydsfstrgdfjxnvgfhdtnbfgn");
		try {
			SearchRequestResult res = api.search(query);
			assertTrue(res.getTotal_result_count() == 0);
		} catch (OpacErrorException e) {
			// Expected, should be an empty result.
		}
	}

	@Test
	public void testSearchScrolling() throws NotReachableException,
			IOException, OpacErrorException {
		Map<String, String> query = new HashMap<String, String>();
		SearchField field = findFreeSearchOrTitle(api.getSearchFields());
		if (field == null)
			throw new OpacErrorException( //TODO: prevent this
					"There is no free or title search field");
		query.put(field.getId(), "harry");
		SearchRequestResult res = api.search(query);
		assertTrue(res.getResults().size() <= res.getTotal_result_count());
		assertTrue(res.getResults().size() > 0);

		SearchResult third = res.getResults().get(2);
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
	 *         first search field with ID "titel" and if that doesn't exist
	 *         either, null
	 */
	private SearchField findFreeSearchOrTitle(List<SearchField> fields) {
		for (SearchField field : fields) {
			if (field instanceof TextSearchField
					&& ((TextSearchField) field).isFreeSearch())
				return field;
		}
		for (SearchField field : fields) {
			if (field instanceof TextSearchField
					&& field.getId().equals(OpacApi.KEY_SEARCH_QUERY_TITLE))
				return field;
		}
		return null;
	}
}
