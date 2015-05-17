/**
 * Copyright (C) 2013 by Johan von Forstner under the MIT license:
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
package de.geeksfactory.opacclient.i18n;

/**
 * The StringProvider interface exposes an abstract method of translating strings from the OpacApi
 * classes. Since version 3.2.1, it is highly discouraged to hardcode any string into the OpacApi
 * implementations which might be displayed to the user, as this makes internationalization very
 * hard.
 * <p/>
 * While Android has a powerful i18n library using the 'string resources' concept, our apis.*
 * subpackage should not depend on Android, to make it seperable from the app, e.g. for running our
 * test suite on a PC. Therefore, this abstract interface provides access to Android's i18n API on
 * Android devices. It currently performs a no-op on other devices.
 *
 * @author Johan v. Forstner
 * @since 3.2.1
 */
public interface StringProvider {
    public static String LIMITED_NUM_OF_CRITERIA = "limited_num_of_criteria";
    public static String NO_CRITERIA_INPUT = "no_criteria_input";
    public static String COMBINATION_NOT_SUPPORTED = "combination_not_supported";
    public static String UNKNOWN_ERROR = "unknown_error";
    public static String UNKNOWN_ERROR_WITH_DESCRIPTION = "unknown_error_with_description";
    public static String UNKNOWN_ERROR_ACCOUNT = "unknown_error_account";
    public static String UNKNOWN_ERROR_ACCOUNT_WITH_DESCRIPTION =
            "unknown_error_account_with_description";
    public static String INTERNAL_ERROR = "internal_error";
    public static String INTERNAL_ERROR_WITH_DESCRIPTION = "internal_error_with_description";
    public static String LOGIN_FAILED = "login_failed";
    public static String COULD_NOT_LOAD_ACCOUNT = "could_not_load_account";
    public static String CONNECTION_ERROR = "api_connection_error";
    public static String LENT_UNTIL = "lent_until";
    public static String SUBTITLE = "subtitle";
    public static String PICA_WHICH_COPY = "pica_which_copy";
    public static String NO_COPY_RESERVABLE = "no_copy_reservable";
    public static String COULD_NOT_LOAD_DETAIL = "could_not_load_detail";
    public static String ERROR = "error";
    public static String DOWNLOAD = "download";
    public static String REMINDERS = "reminders";
    public static String PROLONGED_ABBR = "prolonged_abbr";
    public static String FREE_SEARCH = "free_search";
    public static String WRONG_LOGIN_DATA = "wrong_account_data";
    public static String PROLONGING_IMPOSSIBLE = "prolonging_impossible";
    public static String PROLONGING_EXPIRED = "prolonging_expired";
    public static String PROLONGING_WAITING = "prolonging_expired";
    public static String ORDER = "order";
    public static String ORDER_DEFAULT = "order_default";
    public static String ORDER_YEAR_ASC = "order_year_asc";
    public static String ORDER_CATEGORY_ASC = "order_category_asc";
    public static String ORDER_YEAR_DESC = "order_year_desc";
    public static String ORDER_CATEGORY_DESC = "order_category_desc";
    public static String NO_RESULTS = "no_results";
    public static String INTERLIB_BRANCH = "interlib_branch";
    public static String STACKS_BRANCH = "stacks_branch";
    public static String PROVISION_BRANCH = "provision_branch";

    /**
     * Returns the translated string identified by identifier
     *
     * @param identifier The ID of the string
     * @return the translated string
     */
    public abstract String getString(String identifier);

    /**
     * Returns a translated formatted string
     *
     * @param identifier The ID of the string
     * @param args       Formatting arguments
     * @return the translated and formatted string
     */
    public abstract String getFormattedString(String identifier, Object... args);

    /**
     * Returns a translated quantity string
     *
     * @param identifier The ID of the string
     * @param count      Number for determining the plural to use
     * @param args       Formatting arguments
     * @return the translated and formatted string
     */
    public abstract String getQuantityString(String identifier, int count, Object... args);
}
