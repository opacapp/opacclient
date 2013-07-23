package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.util.List;

import org.json.JSONException;

import android.content.ContentValues;
import android.os.Bundle;
import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailledItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.storage.MetaDataSource;

/**
 * Generic interface for accessing online library catalogues.
 * 
 * @author Raphael Michel
 */
public interface OpacApi {

	/**
	 * Keywords to do a free search. Some APIs do support this, some don't. If
	 * supported, it must at least search in title and author field, but should
	 * also search abstract and other things.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_FREE = "free";

	/**
	 * Item title to search for. Doesn't have to be the full title, can also be
	 * a substring to be searched.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_TITLE = "titel";

	/**
	 * Author name to search for.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_AUTHOR = "verfasser";

	/**
	 * "Keyword A". Most libraries require very special input in this field. May
	 * be only shown if "advanced fields" is set in user preferences.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_KEYWORDA = "schlag_a";

	/**
	 * "Keyword B". Most libraries require very special input in this field. May
	 * be only shown if "advanced fields" is set in user preferences. Can only
	 * be set, if <code>KEY_SEARCH_QUERY_KEYWORDA</code> is set as well.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_KEYWORDB = "schlag_b";

	/**
	 * Library branch to search in. The user is able to select from multiple
	 * options, generated from the MetaData you store in the MetaDataSource you
	 * get in {@link #init(MetaDataSource, Library)}.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_BRANCH = "zweigstelle";

	/**
	 * "Home" library branch. Some library systems require this information at
	 * search request time to determine where book reservations should be
	 * placed. If in doubt, don't use. Behaves similar to
	 * <code>KEY_SEARCH_QUERY_BRANCH</code> .
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_HOME_BRANCH = "homebranch";

	/**
	 * An ISBN / EAN code to search for. We cannot promise whether it comes with
	 * spaces or hyphens in between but it most likely won't. If it makes a
	 * difference to you, eliminate everythin except numbers and X. We also
	 * cannot say whether a ISBN10 or a ISBN13 is supplied - if relevant, check
	 * in your {@link #search(Bundle)} implementation.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_ISBN = "isbn";

	/**
	 * Year of publication. Your API can either support this or both the
	 * <code>KEY_SEARCH_QUERY_YEAR_RANGE_*</code> fields (or none of them).
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_YEAR = "jahr";

	/**
	 * End of range, if year of publication can be specified as a range. Can not
	 * be combined with <code>KEY_SEARCH_QUERY_YEAR</code> but has to be
	 * combined with <code>KEY_SEARCH_QUERY_YEAR_RANGE_END</code>.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_YEAR_RANGE_START = "jahr_von";

	/**
	 * Start of range, if year of publication can be specified as a range. Can
	 * not be combined with <code>KEY_SEARCH_QUERY_YEAR</code> but has to be
	 * combined with <code>KEY_SEARCH_QUERY_YEAR_RANGE_START</code>.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_YEAR_RANGE_END = "jahr_bis";

	/**
	 * Systematic identification, used in some libraries. Rarely in use. May be
	 * only shown if "advanced fields" is set in user preferences.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_SYSTEM = "systematik";

	/**
	 * Some libraries support a special "audience" field with specified values.
	 * Rarely in use. May be only shown if "advanced fields" is set in user
	 * preferences.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_AUDIENCE = "interessenkreis";

	/**
	 * The "publisher" search field
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_PUBLISHER = "verlag";

	/**
	 * Item category (like "book" or "CD"). The user is able to select from
	 * multiple options, generated from the MetaData you store in the
	 * MetaDataSource you get in {@link #init(MetaDataSource, Library)}.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_CATEGORY = "mediengruppe";

	/**
	 * Unique item identifier. In most libraries, every single book has a unique
	 * number, most of the time printed on the in form of a barcode, sometimes
	 * encoded in a NFC chip.
	 * 
	 * Bundle key for {@link #search(Bundle)} and possible value for
	 * {@link #getSearchFields()}.
	 */
	public static final String KEY_SEARCH_QUERY_BARCODE = "barcode";

	/**
	 * Returns whether – if account view is not supported in the given library –
	 * there is an automatic mechanism to help implementing account support in
	 * this city. Only makes sense when {@link #isAccountSupported(Library)} can
	 * return true and {@link #getAccountExtendableInfo(Account)} returns
	 * something useful.
	 * 
	 * Flag to be present in the result of {@link #getSupportFlags()}.
	 */
	public static final int SUPPORT_FLAG_ACCOUNT_EXTENDABLE = 0x0000001;

	/**
	 * Availability of the "prolong all lent items" feature
	 * 
	 * Flag to be present in the result of {@link #getSupportFlags()}.
	 */
	public static final int SUPPORT_FLAG_ACCOUNT_PROLONG_ALL = 0x0000002;

	/**
	 * Availability of the "quicklinks" feature
	 * 
	 * Flag to be present in the result of {@link #getSupportFlags()}.
	 */
	public static final int SUPPORT_FLAG_QUICKLINKS = 0x0000004;

	/**
	 * May be called on application startup and you are free to call it in <our
	 * {@link #search} implementation or similar positions. It is commonly used
	 * to initialize a session. You MUST NOT rely on it being called and should
	 * check by yourself, whether it was already called (if your following calls
	 * require it to be called before). You SHOULD use this function to populate
	 * the MetaDataSource e.g. with information on your library's branches.
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
	 * it. I use it to initialize my DefaultHTTPClient and to store the metadata
	 * and library objects.
	 * 
	 * @param metadata
	 *            A MetaDataSource to store metadata in
	 * @param library
	 *            The library the Api is initialized for
	 */
	public void init(MetaDataSource metadata, Library library);

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
	 * @return List of results and additional information, or <code>null</code>
	 *         on failure. In case of failure, <code>getLast_error</code> will
	 *         be called for more information.
	 * @see de.geeksfactory.opacclient.objects.SearchResult
	 */
	public SearchRequestResult search(Bundle query) throws IOException,
			NotReachableException;

	/**
	 * If your {@link #search(Bundle)} implementation puts something different
	 * from <code>null</code> into {@link SearchRequestResult#setFilters(List)},
	 * this will be called to apply a filter to the last search request.
	 * 
	 * If your {@link #search(Bundle)} implementation does not set
	 * {@link SearchRequestResult#setFilters(List)}, this wil never be called.
	 * Just return <code>null</code>.
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it. See documentation on DetailledItem for
	 * details.
	 * 
	 * @param filter
	 *            The filter to be applied.
	 * @param option
	 *            The filters option to be applied. If the
	 *            <code>option.isApplied()</code> returns <code>true</code>, the
	 *            filter is to be removed!
	 * @return List of results or <code>null</code> on failure. In case of
	 *         failure, <code>getLast_error</code> will be called for more
	 *         information.
	 * @see de.geeksfactory.opacclient.objects.SearchResult
	 * @see de.geeksfactory.opacclient.objects.Filter
	 * @since 2.0.6
	 */
	public SearchRequestResult filterResults(Filter filter, Filter.Option option)
			throws IOException, NotReachableException;

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
	public SearchRequestResult searchGetPage(int page) throws IOException,
			NotReachableException;

	/**
	 * Get details for the item with unique ID id.
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @param id
	 *            id of object to fetch
	 * @param homebranch
	 *            The users "home branch". Only filled if your library system
	 *            supports <code>KEY_SEARCH_QUERY_HOME_BRANCH</code>. Assume
	 *            that it can be <code>null</code>. If in doubt, ignore.
	 * @return Media details
	 * @see de.geeksfactory.opacclient.objects.DetailledItem
	 * @see #KEY_SEARCH_QUERY_HOME_BRANCH
	 */
	public DetailledItem getResultById(String id, String homebranch)
			throws IOException, NotReachableException;

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
	 * <code>getResultById</code> or <code>getResult</code> for Account
	 * <code>acc</code>. (if applicable)
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @param reservation_info
	 *            Data your <code>getDetail</code> implementation returned as
	 *            reservation_info for this item.
	 * @param account
	 *            Account to be used
	 * @param useraction
	 *            Identifier for the selection made by the user in
	 *            <code>selection</code>, if a selection was made (see
	 *            {@link ReservationResult#getActionIdentifier()}) or 0, if no
	 *            selection was required. If your last method call returned
	 *            <code>CONFIRMATION_NEEDED</code>, this is set to
	 *            <code>ACTION_CONFIRMATION</code> if the user positively
	 *            confirmed the action.
	 * @param selection
	 *            When the method is called for the first time or if useraction
	 *            is <code>ACTION_CONFIRMATION</code>, this parameter is null.
	 *            If you return <code>SELECTION</code> in your
	 *            {@link ReservationResult#getStatus()}, this method will be
	 *            called again with the user's selection present in selection.
	 * @return A <code>ReservationResult</code> object which has to have the
	 *         status set.
	 */
	public ReservationResult reservation(String reservation_info,
			Account account, int useraction, String selection)
			throws IOException;

	/**
	 * The result of a {@link OpacApi#reservation(String, Account, int, String)}
	 * call
	 */
	public class ReservationResult {
		public enum Status {
			/**
			 * Everything went well
			 */
			OK,
			/**
			 * This is not supported in this API implementation
			 */
			UNSUPPORTED,
			/**
			 * An error occured
			 */
			ERROR,
			/**
			 * The user has to make a selection
			 */
			SELECTION_NEEDED,
			/**
			 * The user has to confirm the reservation
			 */
			CONFIRMATION_NEEDED
		};

		private Status status;
		private ContentValues selection;
		private List<String[]> details;
		private int actionidentifier;

		/**
		 * Action type identifier for library branch selection
		 */
		public static final int ACTION_BRANCH = 1;

		/**
		 * Action type identifier for process confirmation
		 */
		public static final int ACTION_CONFIRMATION = 2;

		/**
		 * Action number to use for custom selection type identifiers.
		 */
		public static final int ACTION_USER = 100;

		/**
		 * Create a new ReservationResult object holding the return status of
		 * the reservation() operation.
		 * 
		 * @param status
		 *            The return status
		 * @see #getStatus()
		 */
		public ReservationResult(Status status) {
			this.status = status;
		}

		/**
		 * Get the return status of the reservation() operation. Can be
		 * <code>OK</code> if the operation was successful, <code>ERROR</code>
		 * if the operation failed, <code>SELECTION_NEEDED</code> if the user
		 * should select one of the options presented in {@link #getSelection()}
		 * or <code>CONFIRMATION_NEEDED</code> if the user should confirm the
		 * details returned by <code>getDetails</code>. .
		 */
		public Status getStatus() {
			return status;
		}

		/**
		 * Identifier for the type of user selection if {@link #getStatus()} is
		 * <code>SELECTION_NEEDED</code>.
		 * 
		 * @return One of the <code>ACTION_</code> constants or a number above
		 *         <code>ACTION_USER</code>.
		 */
		public int getActionIdentifier() {
			return actionidentifier;
		}

		/**
		 * Set identifier for the type of user selection if {@link #getStatus()}
		 * is <code>SELECTION_NEEDED</code>.
		 * 
		 * @param actionidentifier
		 *            One of the <code>ACTION_</code> constants or a number
		 *            above <code>ACTION_USER</code>.
		 */
		public void setActionIdentifier(int actionidentifier) {
			this.actionidentifier = actionidentifier;
		}

		/**
		 * Get values the user should select one of if {@link #getStatus()} is
		 * <code>SELECTION_NEEDED</code>.
		 * 
		 * @return ContentValue tuples with key to give back and value to show
		 *         to the users.
		 */
		public ContentValues getSelection() {
			return selection;
		}

		/**
		 * Set values the user should select one of if {@link #getStatus()} is
		 * set to <code>SELECTION_NEEDED</code>.
		 * 
		 * @param selection
		 *            Store with key-value-tuples where the key is what is to be
		 *            returned back to reservation() and the value is what is to
		 *            be displayed to the user.
		 */
		public void setSelection(ContentValues selection) {
			this.selection = selection;
		}

		/**
		 * Set details the user should confirm if {@link #getStatus()} is
		 * <code>CONFIRMATION_NEEDED</code>.
		 * 
		 * @return ContentValue tuples with key to give back and value to show
		 *         to the users.
		 */
		public List<String[]> getDetails() {
			return details;
		}

		/**
		 * Set values the user should select one of if {@link #getStatus()} is
		 * set to <code>CONFIRMATION_NEEDED</code> .
		 * 
		 * @param details
		 *            List containing reservation details. A detail is stored as
		 *            an array of two strings, the detail's description (e.g.
		 *            "Fee") and the detail itself (e.g. "2 EUR")
		 */
		public void setDetails(List<String[]> details) {
			this.details = details;
		}
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
	 *         case of failure, {@link #getLast_error()} will be called for more
	 *         information.
	 * @see de.geeksfactory.opacclient.objects.AccountData
	 */
	public boolean prolong(Account account, String media) throws IOException;

	/**
	 * Extend the lending period of all lent items. Will only be called if your
	 * {@link #getSupportFlags()} implementation's return value contains the
	 * {@link #SUPPORT_FLAG_ACCOUNT_PROLONG_ALL} flag. If you don't support the
	 * feature, just implement a stub method, like <code>return false;</code>
	 * 
	 * This function is always called from a background thread, you can use
	 * blocking network operations in it.
	 * 
	 * @return <code>true</code> on success, <code>false</code> on failure. In
	 *         case of failure, {@link #getLast_error()} will be called for more
	 *         information.
	 * @see de.geeksfactory.opacclient.objects.AccountData
	 */
	public boolean prolongAll(Account account) throws IOException;

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
	public boolean cancel(Account account, String media) throws IOException;

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
			JSONException;

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
	 * <code>is_a_redirect</code> for <code>SISIS</code>) which activate
	 * certain methods in calling activities.
	 * 
	 * @return Error details
	 */
	public String getLast_error();

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
	 * @deprecated Functionality is provided by {@link #getSupportFlags()},
	 *             please return <code>SUPPORT_FLAG_ACCOUNT_EXTENDABLE</code>
	 *             from <code>getSupportFlags</code>.
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
	 * Some library systems allow us to share search results. If your library
	 * system allows this natively (to link directly on search results), you can
	 * return the corresponding URL with this function. If your library does not
	 * support this at all, return <code>null</code>. If you library only
	 * accepts direkt links when a session is open, get in touch with me
	 * (mail@raphaelmichel.de) to get it integrated in the opacapp.de proxy.
	 * 
	 * @param id
	 *            Media id of the item to be shared
	 * @param title
	 *            Title of the item to be shared
	 * @return An URL or <strong>null</strong>.
	 */
	public String getShareUrl(String id, String title);

	/**
	 * Return which optional features your API implementation supports.
	 * 
	 * @return combination (bitwise OR) of <code>SUPPORT_FLAG_*</code> constants
	 */
	public int getSupportFlags();

}