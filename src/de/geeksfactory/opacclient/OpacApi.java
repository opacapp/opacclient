package de.geeksfactory.opacclient;

import java.util.List;

import org.json.JSONArray;

import de.geeksfactory.opacclient.storage.SearchResult;

import android.content.Context;

public interface OpacApi {
	String[] SEARCH_FIELDS = new String[] { "stichwort", "verfasser",
			"schlag_a", "schlag_b", "zweigstelle", "mediengruppe", "isbn",
			"jahr_von", "jahr_bis", "notation", "interessenkreis", "verlag",
			"order" };

	String getLast_error();

	String getResults();

	void init(String opac_url, Context context, JSONArray bib);

	List<SearchResult> search(String stichwort, String verfasser,
			String schlag_a, String schlag_b, String zweigstelle,
			String mediengruppe, String isbn, String jahr_von, String jahr_bis,
			String notation, String interessenkreis, String verlag, String order);

	List<SearchResult> search_page(int page);

	SearchResult getResultById(String id);

	SearchResult getResult(int position);

	boolean reservation(String zst, String ausw, String pwd);

	boolean prolong(String a);

	boolean cancel(String a);

	List<List<String[]>> account(String ausw, String pwd);

}