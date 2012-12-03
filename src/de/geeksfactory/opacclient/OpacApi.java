package de.geeksfactory.opacclient;

import java.io.IOException;
import java.net.SocketException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.SearchResult;

public interface OpacApi {

	public String[] getSearchFields();

	public String getLast_error();

	public String getResults();

	public void start() throws ClientProtocolException, IOException,
			NotReachableException, SocketException;

	public void init(Context context, JSONObject data);

	public List<SearchResult> search(String stichwort, String verfasser,
			String schlag_a, String schlag_b, String zweigstelle,
			String mediengruppe, String isbn, String jahr_von, String jahr_bis,
			String notation, String interessenkreis, String verlag, String order)
			throws IOException, NotReachableException;

	public List<SearchResult> searchGetPage(int page) throws IOException,
			NotReachableException;

	public DetailledItem getResultById(String id) throws IOException,
			NotReachableException;

	public DetailledItem getResult(int position) throws IOException;

	public boolean reservation(String zst, String ausw, String pwd)
			throws IOException;

	public boolean prolong(String a) throws IOException, NotReachableException;

	public boolean cancel(String a) throws IOException, NotReachableException;

	public List<List<String[]>> account(Account acc) throws IOException,
			NotReachableException, JSONException, AccountUnsupportedException,
			SocketException;

}