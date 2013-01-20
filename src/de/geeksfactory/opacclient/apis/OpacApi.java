package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

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

/**
 * Generic interface for accessing online library catalogues.
 * 
 * @author Raphael Michel
 */
public interface OpacApi {

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Keywords to search in item's title
	 */
	public static String KEY_SEARCH_QUERY_TITLE = "titel";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Author
	 */
	public static String KEY_SEARCH_QUERY_AUTHOR = "verfasser";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Keyword A
	 */
	public static String KEY_SEARCH_QUERY_KEYWORDA = "schlag_a";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Keyword B
	 */
	public static String KEY_SEARCH_QUERY_KEYWORDB = "schlag_b";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Library branch
	 */
	public static String KEY_SEARCH_QUERY_BRANCH = "zweigstelle";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * ISBN
	 */
	public static String KEY_SEARCH_QUERY_ISBN = "isbn";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Year of publication
	 */
	public static String KEY_SEARCH_QUERY_YEAR = "jahr";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Year range start
	 */
	public static String KEY_SEARCH_QUERY_YEAR_RANGE_START = "jahr_von";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Year range end
	 */
	public static String KEY_SEARCH_QUERY_YEAR_RANGE_END = "jahr_bis";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Year range end
	 */
	public static String KEY_SEARCH_QUERY_SYSTEM = "systematik";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Systematic identification
	 */
	public static String KEY_SEARCH_QUERY_AUDIENCE = "interessenkreis";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Item's publisher
	 */
	public static String KEY_SEARCH_QUERY_PUBLISHER = "verlag";

	/**
	 * Bundle key for {@link #search(Bundle)}:
	 * 
	 * Item's category
	 */
	public static String KEY_SEARCH_QUERY_CATEGORY = "mediengruppe";

	/**
	 * Is called on application startup and you are free to call it in <our
	 * {@link #search} implementation or similar positions. It is commonly used
	 * to initialize a session. You must not rely on it being called and should
	 * check by yourself, whether it was already called (if your following calls
	 * require it). I also populate MetaDataSource with branch information in
	 * this method (documentation on this will follow).
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @throws IOException
	 *             if network connection failed
	 * @throws NotReachableException
	 *             may throw this if the library couldn't be reached
	 */
	public void start() throws IOException, NotReachableException;

	/**
	 * Is called whenever a new API object is created. The difference to start
	 * is that you can rely on it but must not use blocking network functions in
	 * it. I use it to initialize my DefaultHTTPClient and to store the context
	 * and library objects.
	 * 
	 * @param context
	 *            A valid context inside the application
	 * @param library
	 *            The library the Api is initialized for
	 */
	public void init(Context context, Library library);

	/**
	 * Performs a catalogue search. The given <code>Bundle</code> contains the
	 * search criteria. See documentation on <code>SearchResult</code> for
	 * details.
	 * 
	 * The <code>Bundle</code> can contain any of the <code>KEY_SEARCH_*</code>
	 * constants as keys.
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it. See documentation on DetailledItem for
	 * details.
	 * 
	 * @param query
	 *            see above
	 * @return List of results or <code>null</code> on failure. In case of
	 *         failure, <code>getLast_error</code> will be called for more
	 *         information.
	 * @see de.geeksfactory.opacclient.objects.SearchResult
	 */
	public List<SearchResult> search(Bundle query) throws IOException,
			NotReachableException;

	/**
	 * Get result page <code>page</code> of the search performed last with
	 * {@link #search}.
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it. See documentation on DetailledItem for
	 * details.
	 * 
	 * @param page
	 *            page number to fetch
	 * @return List of results or <code>null</code> on failure. In case of
	 *         failure, <code>getLast_error</code> will be called for more
	 *         information.
	 * @see #search(Bundle)
	 * @see de.geeksfactory.opacclient.objects.SearchResult
	 */
	public List<SearchResult> searchGetPage(int page) throws IOException,
			NotReachableException;

	/**
	 * Get details for the item with unique ID id.
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @param id
	 *            id of object to fetch
	 * @return Media details
	 * @see de.geeksfactory.opacclient.objects.DetailledItem
	 */
	public DetailledItem getResultById(String id) throws IOException,
			NotReachableException;

	/**
	 * Get details for the item at <code>position</code> from last
	 * {@link #search} or {@link #searchGetPage} call.
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @param position
	 *            position of object in last search
	 * @return Media details
	 * @see de.geeksfactory.opacclient.objects.DetailledItem
	 */
	public DetailledItem getResult(int position) throws IOException;

	/**
	 * Perform a reservation on the item last fetched with
	 * <code>getResultById</code> or <code>@getResult</code> for Account
	 * <code>acc</code> and the branch <code>zst</code> (if applicable)
	 * 
	 * WARNING: This API is about to change. If you are serious using it for an
	 * implementation, please contact me!
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @param account
	 *            Account to be used
	 * @param branch
	 *            Branch identifier
	 * @return ReservationResult.OK on success, ReservationResult.ERROR on error
	 */
	public ReservationResult reservation(String branch, Account account)
			throws IOException;

	/**
	 * The result of a {@link OpacApi#reservation(String, Account)} call
	 */
	public enum ReservationResult {
		/**
		 * Everything went well
		 */
		OK,
		/**
		 * An error occured
		 */
		ERROR,
		/**
		 * (Reserved, currently not in use.)
		 */
		SELECTBRANCH
	}

	/**
	 * Extend the lending period of the item identified by the given String (see
	 * <code>AccountData</code>)
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @param media
	 *            Media identification
	 * @return <code>true</code> on success, <code>false</code> on failure. In
	 *         case of failure, <code>getLast_error</code> will be called for
	 *         more information.
	 * @see de.geeksfactory.opacclient.objects.AccountData
	 */
	public boolean prolong(String media) throws IOException;

	/**
	 * Cancel a media reservation/order identified by the given String (see
	 * AccountData documentation) (see <code>AccountData</code>)
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @param media
	 *            Media identification
	 * @return <code>true</code> on success, <code>false</code> on failure. In
	 *         case of failure, <code>getLast_error</code> will be called for
	 *         more information.
	 * @see de.geeksfactory.opacclient.objects.AccountData
	 */
	public boolean cancel(String media) throws IOException;

	/**
	 * Load account view (borrowed and reserved items, see
	 * <code>AccountData</code>)
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @param account
	 *            The account to display
	 * @return Account details or <code>null</code> on failure. In case of
	 *         failure, <code>getLast_error</code> will be called for more
	 *         information.
	 * @see de.geeksfactory.opacclient.objects.AccountData
	 */
	public AccountData account(Account account) throws IOException,
			JSONException, AccountUnsupportedException;

	/**
	 * Returns an array of search criterias which are supported by this OPAC and
	 * should be visible in the search activity. Valid values in returned field
	 * are the same as the valid keys in <code>search</code>.
	 * 
	 * @return List of allowed fields
	 * @see #search
	 */
	public String[] getSearchFields();

	/**
	 * Sometimes if one of your methods fails and you return null, it makes
	 * sense to provide additional information. If the error occured in search,
	 * it is displayed to the user. There are also some special hooks (like
	 * <code>is_a_redirect</code> for <code>OCLC2011</code>) which activate
	 * certain methods in calling activities.
	 * 
	 * @return Error details
	 */
	public String getLast_error();

	/**
	 * Get result information for last search.
	 * 
	 * @return A string like "312 items found."
	 */
	public String getResults();

	/**
	 * Returns whether – if account view is not supported in the given library –
	 * there is an automatic mechanism to help implementing account support in
	 * this city.
	 * 
	 * @param library
	 *            Library to check compatibility
	 * @return <code>true</code> if account view is supported or
	 *         <code>false</code> if it isn't.
	 */
	public boolean isAccountSupported(Library library);

	/**
	 * Returns whether – if account view is not supported in the given library –
	 * there is an automatic mechanism to help implementing account support in
	 * this city.
	 * 
	 * @return <code>true</code> if account support can easily be implemented
	 *         with some extra information or <code>false</code> if it can't.
	 */
	public boolean isAccountExtendable();

	/**
	 * Is called if <code>isAccountSupported</code> returns false but
	 * <code>isAccountExtendable</code> returns <code>true</code>. The return
	 * value is sent in a crash report which can be submitted by the user. It is
	 * currently implemented in BOND26 and just returns the HTML of the OPAC's
	 * account page (with the user logged in).
	 * 
	 * @param account
	 *            Account data the user entered
	 * @return Some information to be sent in a crash report
	 */
	public String getAccountExtendableInfo(Account account) throws IOException,
			NotReachableException;

	/**
	 * Returns the date format the OPAC uses for return dates.
	 * 
	 * @return Date format
	 */
	public SimpleDateFormat getDateFormat();

}