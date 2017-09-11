/**
 * Copyright (C) 2013 by Raphael Michel under the MIT license:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation 
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software 
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER 
 * DEALINGS IN THE SOFTWARE.
 */
package de.geeksfactory.opacclient.apis;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.i18n.StringProvider;
import de.geeksfactory.opacclient.networking.HttpClientFactory;
import de.geeksfactory.opacclient.networking.NotReachableException;
import de.geeksfactory.opacclient.objects.Account;
import de.geeksfactory.opacclient.objects.AccountData;
import de.geeksfactory.opacclient.objects.DetailedItem;
import de.geeksfactory.opacclient.objects.Filter;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.reporting.ReportHandler;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchQuery;

/**
 * Generic interface for accessing online library catalogues.
 *
 * @author Raphael Michel
 */
public interface OpacApi {

    /**
     * Availability of the "prolong all lent items" feature
     *
     * Flag to be present in the result of {@link #getSupportFlags()}.
     */
    int SUPPORT_FLAG_ACCOUNT_PROLONG_ALL = 0x0000002;

    /**
     * Availability of the "quicklinks" feature
     *
     * Flag to be present in the result of {@link #getSupportFlags()}.
     */
    @SuppressWarnings("UnusedDeclaration") // Plus Edition compatibility
            int SUPPORT_FLAG_QUICKLINKS = 0x0000004;

    /**
     * When the results are shown as an endless scrolling list, will reload the page the selected
     * result is located on if this flag is not present.
     *
     * Flag to be present in the result of {@link #getSupportFlags()}.
     */
    int SUPPORT_FLAG_ENDLESS_SCROLLING = 0x0000008;

    /**
     * Allow account change on reservation click.
     *
     * Flag to be present in the result of {@link #getSupportFlags()}.
     */
    int SUPPORT_FLAG_CHANGE_ACCOUNT = 0x0000010;

    /**
     * Asks the user responsibly about reservation fees
     *
     * Flag to be present in the result of {@link #getSupportFlags()}.
     */
    int SUPPORT_FLAG_WARN_RESERVATION_FEES = 0x0000020;

    /**
     * Asks the user responsibly about prolong fees
     */
    int SUPPORT_FLAG_WARN_PROLONG_FEES = 0x0000040;

    /**
     * May be called on application startup and you are free to call it in our {@link #search}
     * implementation or similar positions. It is commonly used to initialize a session. You MUST
     * NOT rely on it being called and should check by yourself, whether it was already called (if
     * your following calls require it to be called before). You SHOULD use this function to
     * populate the MetaDataSource e.g. with information on your library's branches.
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @throws IOException           if network connection failed
     * @throws NotReachableException may throw this if the library couldn't be reached
     */
    void start() throws IOException;

    /**
     * Is called whenever a new API object is created. The difference to start is that you can rely
     * on it but must not use blocking network functions in it. I use it to initialize my
     * DefaultHTTPClient and to store the metadata and library objects.
     *
     * @param library           The library the Api is initialized for
     * @param httpClientFactory A HttpClientFactory instance that will be used for instantiating
     *                          HTTP clients. This factory is pluggable because we want to use
     *                          platform-specific code on Android.
     */
    void init(Library library, HttpClientFactory httpClientFactory);

    /**
     * Performs a catalogue search. The given <code>List&lt;SearchQuery&gt;</code> contains the
     * search criteria. See documentation on <code>SearchResult</code> for details.
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it. See documentation on DetailedItem for details.
     *
     * @param query see above
     * @return List of results and additional information, or result object with the error flag set
     * to true.
     * @throws JSONException
     * @see de.geeksfactory.opacclient.objects.SearchResult
     */
    SearchRequestResult search(List<SearchQuery> query)
            throws IOException, OpacErrorException,
            JSONException;

    /**
     * Performs a catalogue search for volumes of an item. The query is given to it from {@link
     * DetailedItem#getVolumesearch()}.
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it. See documentation on DetailedItem for details.
     *
     * @param query see above
     * @return List of results and additional information, or result object with the error flag set
     * to true.
     * @see de.geeksfactory.opacclient.objects.SearchResult
     */
    SearchRequestResult volumeSearch(Map<String, String> query)
            throws IOException, OpacErrorException;

    /**
     * If your {@link #search(List)} implementation puts something different from <code>null</code>
     * into {@link SearchRequestResult#setFilters(List)}, this will be called to apply a filter to
     * the last search request.
     *
     * If your {@link #search(List)} implementation does not set {@link
     * SearchRequestResult#setFilters(List)}, this wil never be called. Just return
     * <code>null</code>.
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it. See documentation on DetailedItem for details.
     *
     * @param filter The filter to be applied.
     * @param option The filters option to be applied. If the <code>option.isApplied()</code>
     *               returns <code>true</code>, the filter is to be removed!
     * @return List of results and additional information, or result object with the error flag set
     * to true.
     * @see de.geeksfactory.opacclient.objects.SearchResult
     * @see de.geeksfactory.opacclient.objects.Filter
     * @since 2.0.6
     */
    @SuppressWarnings({"SameReturnValue", "RedundantThrows", "UnusedDeclaration"})
    // Plus Edition compatibility
    SearchRequestResult filterResults(Filter filter, Filter.Option option)
            throws IOException, OpacErrorException;

    /**
     * Get result page <code>page</code> of the search performed last with {@link #search}.
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it. See documentation on DetailedItem for details.
     *
     * @param page page number to fetch
     * @return List of results and additional information, or result object with the error flag set
     * to true.
     * @see #search(List)
     * @see de.geeksfactory.opacclient.objects.SearchResult
     */
    SearchRequestResult searchGetPage(int page) throws IOException,
            OpacErrorException, JSONException;

    /**
     * Get details for the item with unique ID id.
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @param id         id of object to fetch
     * @param homebranch The users "home branch". "Home" library branch. Some library systems
     *                   require this information at search request time to determine where book
     *                   reservations should be placed. If in doubt, set to <code>null</code>.
     * @return Media details
     * @see DetailedItem
     */
    DetailedItem getResultById(String id, String homebranch)
            throws IOException, OpacErrorException;

    /**
     * Get details for the item at <code>position</code> from last {@link #search} or {@link
     * #searchGetPage} call.
     *
     * We generally prefer {@link #getResultById(String, String)}, so if you implement
     * <code>getResultById</code> <strong>AND</strong> <em>every</em> search result of your driver
     * has an id set, you can omit this method (respectively, return null).
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @param position position of object in last search
     * @return Media details
     * @see DetailedItem
     */
    DetailedItem getResult(int position) throws IOException,
            OpacErrorException;

    /**
     * Perform a reservation on the item last fetched with <code>getResultById</code> or
     * <code>getResult</code> for Account <code>acc</code>. (if applicable)
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @param item       The item to place a reservation for.
     * @param account    Account to be used
     * @param useraction Identifier for the selection made by the user in <code>selection</code>, if
     *                   a selection was made (see {@link ReservationResult#getActionIdentifier()})
     *                   or 0, if no selection was required. If your last method call returned
     *                   <code>CONFIRMATION_NEEDED</code>, this is set to
     *                   <code>ACTION_CONFIRMATION</code>
     *                   if the user positively confirmed the action.
     * @param selection  When the method is called for the first time or if useraction is
     *                   <code>ACTION_CONFIRMATION</code>, this parameter is null. If you return
     *                   <code>SELECTION</code> in your {@link ReservationResult#getStatus()}, this
     *                   method will be called again with the user's selection present in
     *                   selection.
     * @return A <code>ReservationResult</code> object which has to have the status set.
     */
    ReservationResult reservation(DetailedItem item, Account account,
            int useraction, String selection) throws IOException;

    /**
     * Extend the lending period of the item identified by the given String (see
     * <code>AccountData</code>)
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @param media      Media identification
     * @param account    Account to be used
     * @param useraction Identifier for the selection made by the user in <code>selection</code>, if
     *                   a selection was made (see {@link ProlongResult#getActionIdentifier()}) or
     *                   0, if no selection was required. If your last method call returned
     *                   <code>CONFIRMATION_NEEDED</code>, this is set to
     *                   <code>ACTION_CONFIRMATION</code>
     *                   if the user positively confirmed the action.
     * @param selection  When the method is called for the first time or if useraction is
     *                   <code>ACTION_CONFIRMATION</code>, this parameter is null. If you return
     *                   <code>SELECTION</code> in your {@link ProlongResult#getStatus()}, this
     *                   method will be called again with the user's selection present in
     *                   selection.
     * @return A <code>ProlongResult</code> object which has to have the status set.
     */
    ProlongResult prolong(String media, Account account, int useraction,
            String selection) throws IOException;

    /**
     * Extend the lending period of all lent items. Will only be called if your {@link
     * #getSupportFlags()} implementation's return value contains the {@link
     * #SUPPORT_FLAG_ACCOUNT_PROLONG_ALL} flag. If you don't support the feature, just implement a
     * stub method, like <code>return false;</code>
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @return A <code>ProlongAllResult</code> object which has to have the status set.
     * @see OpacApi#prolong(String, Account, int, String)
     * @see de.geeksfactory.opacclient.objects.AccountData
     */
    ProlongAllResult prolongAll(Account account, int useraction,
            String selection) throws IOException;

    /**
     * Cancel a media reservation/order identified by the given String (see AccountData
     * documentation) (see <code>AccountData</code>)
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @param media      Media identification
     * @param account    Account to be used
     * @param useraction Identifier for the selection made by the user in <code>selection</code>, if
     *                   a selection was made (see {@link CancelResult#getActionIdentifier()}) or 0,
     *                   if no selection was required. If your last method call returned
     *                   <code>CONFIRMATION_NEEDED</code>, this is set to
     *                   <code>ACTION_CONFIRMATION</code>
     *                   if the user positively confirmed the action.
     * @param selection  When the method is called for the first time or if useraction is
     *                   <code>ACTION_CONFIRMATION</code>, this parameter is null. If you return
     *                   <code>SELECTION</code> in your {@link CancelResult#getStatus()}, this
     *                   method will be called again with the user's selection present in
     *                   selection.
     * @return A <code>CancelResult</code> object which has to have the status set.
     */
    CancelResult cancel(String media, Account account, int useraction,
            String selection) throws IOException, OpacErrorException;

    /**
     * Load account view (borrowed and reserved items, see <code>AccountData</code>)
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @param account The account to display
     * @return Account details
     * @see de.geeksfactory.opacclient.objects.AccountData
     */
    AccountData account(Account account) throws IOException,
            JSONException, OpacErrorException;

    /**
     * Check the validity of given account data. This is separate from the {@link #account(Account)}
     * function because just checking the login can be much faster than retrieving all the account
     * data.
     *
     * This function is always called from a background thread, you can use blocking network
     * operations in it.
     *
     * @param account The account to check
     * @throws OpacErrorException when the login data is invalid or there's another error message
     *                            from the OPAC system
     */
    void checkAccountData(Account account) throws IOException,
            JSONException, OpacErrorException;

    /**
     * Returns a list of search criteria which are supported by this OPAC and should be visible in
     * the search activity. Values should be instances of subclasses of the abstract SearchField
     * class. This is called asynchronously, so you can load webpages to get the search fields, but
     * you should save them to the metadata afterwards to make it faster.
     *
     * @return List of allowed fields
     * @throws OpacErrorException
     * @throws JSONException
     * @see #search
     */
    @SuppressWarnings("RedundantThrows")
    List<SearchField> getSearchFields() throws IOException,
            OpacErrorException, JSONException;

    /**
     * Some library systems allow us to share search results. If your library system allows this
     * natively (to link directly on search results), you can return the corresponding URL with this
     * function. If your library does not support this at all, return <code>null</code>. If you
     * library only accepts direkt links when a session is open, get in touch with me
     * (mail@raphaelmichel.de) to get it integrated in the opacapp.de proxy.
     *
     * @param id    Media id of the item to be shared
     * @param title Title of the item to be shared
     * @return An URL or <strong>null</strong>.
     */
    String getShareUrl(String id, String title);

    /**
     * Return which optional features your API implementation supports.
     *
     * @return combination (bitwise OR) of <code>SUPPORT_FLAG_*</code> constants
     */
    int getSupportFlags();

    /**
     * Sets the StringProvider to use for error messages etc.
     *
     * @param stringProvider the StringProvider to use
     */
    void setStringProvider(StringProvider stringProvider);

    /**
     * Get all languages supported by this library. This will be a Set of language codes defined in
     * ISO-639-1 (two-letter codes in lower case, see
     * <a href="http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes">this list</a>).
     * We don't need to use this function in the app because the API will automatically
     * fall back if the language set is not supported, but it is used in the MeaningDetector tool to
     * get search fields for all supported languages. This function may use blocking network
     * operations and may return null if the API doesn't support different languages.
     *
     * @throws IOException
     */
    Set<String> getSupportedLanguages() throws IOException;

    /**
     * Set the language to use. This should be one of the language codes defined in ISO-639-1
     * (two-letter codes in lower case, see
     * <a href="http://en.wikipedia.org/wiki/List_of_ISO_639-1_codes">this list</a>).
     * The API should use the default language of the library if this is not called and
     * should fall back first to English and then to the library's default language if the requested
     * language is not available.
     *
     * @param language the language to use
     */
    void setLanguage(String language);

    /**
     * Sets the report handler to use.
     *
     * If an error occurs or other unexpected things happen (such as needing to use a fallback
     * behaviour that should normally not need to be used) the API might generate a {@link
     * de.geeksfactory.opacclient.reporting.Report} containing further debugging information. You
     * can optionally handle these reports (e.g. to send them to the developer) using this methods.
     *
     * @param reportHandler the report handler to use
     */
    void setReportHandler(ReportHandler reportHandler);

    /**
     * A general exception containing a human-readable error message
     */
    class OpacErrorException extends Exception {

        private static final long serialVersionUID = 5834803212488872907L;

        public OpacErrorException(String message) {
            super(message);
        }

    }

    /**
     * The result of a multi-step-supporting method call.
     *
     * This is a way of implementing an operating which may need an unregular number of steps with
     * user interaction. When the user starts the operation, the method is called. It may return
     * success or error, after which the operation does not continue, but it also may return that it
     * requires user interaction - either a selection or a confirmation. After the user interacted,
     * the same method is being called again, but with other parameters.
     *
     * @since 2.0.18
     */
    abstract class MultiStepResult {

        /**
         * Action type identifier for process confirmation
         */
        public static final int ACTION_CONFIRMATION = 2;

        /**
         * Action number to use for custom selection type identifiers.
         */
        public static final int ACTION_USER = 100;
        protected Status status;
        protected List<Map<String, String>> selection;
        protected List<String[]> details;
        protected int actionidentifier;
        protected String message;

        /**
         * Create a new Result object holding the return status of the operation.
         *
         * @param status The return status
         * @see #getStatus()
         */
        public MultiStepResult(Status status) {
            this.status = status;
        }

        /**
         * Create a new Result object holding the return status of the operation and a message
         *
         * @param status  The return status
         * @param message A message
         * @see #getStatus()
         */
        public MultiStepResult(Status status, String message) {
            this.status = status;
            this.message = message;
        }

        /**
         * Get the return status of the operation. Can be <code>OK</code> if the operation was
         * successful, <code>ERROR</code> if the operation failed, <code>SELECTION_NEEDED</code> if
         * the user should select one of the options presented in {@link #getSelection()} or
         * <code>CONFIRMATION_NEEDED</code> if the user should confirm the details returned by
         * <code>getDetails</code>. .
         */
        public Status getStatus() {
            return status;
        }

        /**
         * Identifier for the type of user selection if {@link #getStatus()} is
         * <code>SELECTION_NEEDED</code>.
         *
         * @return One of the <code>ACTION_</code> constants or a number above
         * <code>ACTION_USER</code>.
         */
        public int getActionIdentifier() {
            return actionidentifier;
        }

        /**
         * Set identifier for the type of user selection if {@link #getStatus()} is
         * <code>SELECTION_NEEDED</code>.
         *
         * @param actionidentifier One of the <code>ACTION_</code> constants or a number above
         *                         <code>ACTION_USER</code>.
         */
        public void setActionIdentifier(int actionidentifier) {
            this.actionidentifier = actionidentifier;
        }

        /**
         * Get values the user should select one of if {@link #getStatus()} is
         * <code>SELECTION_NEEDED</code>.
         *
         * @return A list of maps with the keys 'key' and 'value', where 'key' is what is to be
         * returned back to reservation() and the value is what is to be displayed to the user.
         */
        public List<Map<String, String>> getSelection() {
            return selection;
        }

        /**
         * Set values the user should select one of if {@link #getStatus()} is set to
         * <code>SELECTION_NEEDED</code>.
         *
         * @param selection Store with key-value-tuples where the key is what is to be returned back
         *                  to reservation() and the value is what is to be displayed to the user.
         */
        public void setSelection(List<Map<String, String>> selection) {
            this.selection = selection;
        }

        /**
         * If {@link #getStatus()} is <code>CONFIRMATION_NEEDED</code>, this gives you more
         * information to display to the user. This is a list of of unknown length. Every list entry
         * is an array of strings that of size one or two (which can vary between the elements of
         * the list). If the size of such an array A is two, then A[0] contains a description of
         * A[1], e.g. <code>A = {"Fee", "2 EUR"}</code> or <code>A = {"Pickup location", "Central library"}</code>.
         * If the size is only one, it is a general message to be shown, e.g.
         * <code>{"This action will cost 2 EUR."}</code>.
         *
         * @return A list of String[] entries, as described above.
         */
        public List<String[]> getDetails() {
            return details;
        }

        /**
         * Set details the user should confirm if {@link #getStatus()} is
         * <code>CONFIRMATION_NEEDED</code>.
         *
         * @param details List containing reservation details. See {@link #getDetails()} for what this means.
         */
        public void setDetails(List<String[]> details) {
            this.details = details;
        }

        /**
         * @return A optional message, e.g. to explain an error status code
         */
        public String getMessage() {
            return message;
        }

        /**
         * Set an optional message, e.g. to explain an error status code
         */
        public void setMessage(String message) {
            this.message = message;
        }

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
             * The user's web browser should be opened
             */
            EXTERNAL,
            /**
             * An error occured
             */
            ERROR,
            /**
             * The user has to make a selection
             */
            SELECTION_NEEDED,
            /**
             * The user has to confirm the prolonging
             */
            CONFIRMATION_NEEDED,
            /**
             * We need the user's emaila ddress
             */
            EMAIL_NEEDED
        }

        @Override
        public String toString() {
            return "MultiStepResult{" +
                    "status=" + status +
                    ", selection=" + selection +
                    ", details=" + details +
                    ", actionidentifier=" + actionidentifier +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

    /**
     * The result of a {@link OpacApi#reservation(DetailedItem, Account, int, String)} call
     */
    class ReservationResult extends MultiStepResult {

        /**
         * Action type identifier for library branch selection
         */
        public static final int ACTION_BRANCH = 1;

        public ReservationResult(Status status) {
            super(status);
        }

        public ReservationResult(Status status, String message) {
            super(status, message);
        }
    }

    /**
     * The result of a {@link OpacApi#prolong(String, Account, int, String)} call
     */
    class ProlongResult extends MultiStepResult {

        public ProlongResult(Status status) {
            super(status);
        }

        public ProlongResult(Status status, String message) {
            super(status, message);
        }
    }

    /**
     * The result of a {@link OpacApi#prolongAll(Account, int, String)} call
     */
    class ProlongAllResult extends MultiStepResult {

        public static final String KEY_LINE_TITLE = "title";
        public static final String KEY_LINE_AUTHOR = "author";
        public static final String KEY_LINE_NR = "nr";
        public static final String KEY_LINE_OLD_RETURNDATE = "olddate";
        public static final String KEY_LINE_NEW_RETURNDATE = "newdate";
        public static final String KEY_LINE_MESSAGE = "message";
        protected List<Map<String, String>> results;

        /**
         * @param results A list of ContentValues containing the success values for all the single
         *                items we (tried to) renew.
         */
        public ProlongAllResult(Status status, List<Map<String, String>> results) {
            super(status);
            this.results = results;
        }

        public ProlongAllResult(Status status, String message) {
            super(status, message);
        }

        public ProlongAllResult(Status status) {
            super(status);
        }

        public List<Map<String, String>> getResults() {
            return results;
        }

    }

    /**
     * The result of a {@link OpacApi#cancel(String, Account, int, String)} call
     */
    class CancelResult extends MultiStepResult {

        public CancelResult(Status status) {
            super(status);
        }

        public CancelResult(Status status, String message) {
            super(status, message);
        }
    }
}
