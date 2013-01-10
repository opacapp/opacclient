package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.content.Context;
import android.os.Bundle;
import de.geeksfactory.opacclient.AccountUnsupportedException;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchResult;

public interface OpacApi {

	public String[] getSearchFields();

	public String getLast_error();

	public String getResults();

	public boolean isAccountSupported(Library library);

	public boolean isAccountExtendable();

	public SimpleDateFormat getDateFormat();

	public String getAccountExtendableInfo(Account acc)
			throws ClientProtocolException, SocketException, IOException,
			NotReachableException;

	public void start() throws ClientProtocolException, IOException,
			NotReachableException, SocketException;

	public void init(Context context, Library lib);

	public List<SearchResult> search(Bundle query) throws IOException,
			NotReachableException;

	public List<SearchResult> searchGetPage(int page) throws IOException,
			NotReachableException;

	public DetailledItem getResultById(String id) throws IOException,
			NotReachableException;

	public DetailledItem getResult(int position) throws IOException;

	public boolean reservation(String zst, Account acc) throws IOException;

	public boolean prolong(String a) throws IOException, NotReachableException;

	public boolean cancel(String a) throws IOException, NotReachableException;

	public AccountData account(Account acc) throws IOException,
			NotReachableException, JSONException, AccountUnsupportedException,
			SocketException;

}