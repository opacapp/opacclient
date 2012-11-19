package de.geeksfactory.opacclient;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.SearchResult;

public interface OpacApi {
	String[] SEARCH_FIELDS = new String[] { "stichwort", "verfasser",
			"schlag_a", "schlag_b", "zweigstelle", "mediengruppe", "isbn",
			"jahr_von", "jahr_bis", "notation", "interessenkreis", "verlag",
			"order" };

	String getLast_error();

	String getResults();

	void init(Context context, JSONObject data) throws ClientProtocolException,
			IOException, NotReachableException, SocketException;

	List<SearchResult> search(String stichwort, String verfasser,
			String schlag_a, String schlag_b, String zweigstelle,
			String mediengruppe, String isbn, String jahr_von, String jahr_bis,
			String notation, String interessenkreis, String verlag, String order)
			throws IOException, NotReachableException;

	List<SearchResult> search_page(int page) throws IOException,
			NotReachableException;

	DetailledItem getResultById(String id) throws IOException,
			NotReachableException;

	DetailledItem getResult(int position) throws IOException;

	boolean reservation(String zst, String ausw, String pwd) throws IOException;

	boolean prolong(String a) throws IOException, NotReachableException;

	boolean cancel(String a) throws IOException, NotReachableException;

	List<List<String[]>> account(String ausw, String pwd) throws IOException,
			NotReachableException, JSONException, AccountUnsupportedException,
			SocketException;

}