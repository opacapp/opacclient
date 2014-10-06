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
 * Provides internationalized strings for messages in the API implementations
 * 
 * @author Johan v. Forstner
 *
 */
public interface StringProvider {
	public static String LIMITED_NUM_OF_CRITERIA = "limited_num_of_criteria";
	public static String NO_CRITERIA_INPUT = "no_criteria_input";
	public static String COMBINATION_NOT_SUPPORTED = "combination_not_supported";
	public static String UNKNOWN_ERROR = "unknown_error";
	public static String UNKNOWN_ERROR_WITH_DESCRIPTION = "unknown_error_with_description";
	public static String UNKNOWN_ERROR_ACCOUNT = "unknown_error_account";
	public static String UNKNOWN_ERROR_ACCOUNT_WITH_DESCRIPTION = "unknown_error_account_with_description";
	public static String INTERNAL_ERROR = "internal_error";
	public static String INTERNAL_ERROR_WITH_DESCRIPTION = "internal_error_with_description";
	public static String LOGIN_FAILED = "login_failed";
	
	public abstract String getString(String identifier);
	
	public abstract String getFormattedString(String identifier, Object... args);
}
