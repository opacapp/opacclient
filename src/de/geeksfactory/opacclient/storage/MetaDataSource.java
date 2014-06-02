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
package de.geeksfactory.opacclient.storage;

import java.util.List;
import java.util.Map;

import android.database.SQLException;
import de.geeksfactory.opacclient.apis.OpacApi;

/**
 * Data source for library meta data (like e.g. the list of branches) stored in
 * a key-value format
 * 
 * @author Raphael Michel
 * @since 2.0.0
 */
public interface MetaDataSource {

	/**
	 * Meta type: library branches
	 */
	public static String META_TYPE_BRANCH = "zst";

	/**
	 * Meta type: library branches qualified to be used as a "home branch". You
	 * normally do not need this.
	 * 
	 * @see OpacApi#KEY_SEARCH_QUERY_HOME_BRANCH
	 */
	public static String META_TYPE_HOME_BRANCH = "home_branch";

	/**
	 * Meta type: categoroes, like "books" or "video games"
	 */
	public static String META_TYPE_CATEGORY = "mg";

	/**
	 * Open up the connection to the data source. Needs to be called before any
	 * read or write operation.
	 * 
	 * @throws SQLException
	 *             on failure
	 */
	public void open() throws Exception;

	/**
	 * Close the connection to the data source. Implementations might require
	 * that each connection is closed before another is opened.
	 */
	public void close();

	/**
	 * Add an entry to the database.
	 * 
	 * @param type
	 *            The type of information. Can be one of the
	 *            <code>META_TYPE_</code> constants but also a string specific
	 *            to your OpacApi implementation.
	 * @param library
	 *            The library identification string this entry should be
	 *            associated with, see
	 *            {@link de.geeksfactory.opacclient.objects.Library#getIdent()}
	 * @param key
	 *            The key the information is stored with, for example the ID of
	 *            a branch.
	 * @param value
	 *            The value to be stored, for example the name of a branch
	 * @return dataset ID
	 */
	public long addMeta(String type, String library, String key, String value);

	/**
	 * Get all datasets of a specific type associated with a specific library.
	 * 
	 * @param library
	 *            The library identification, see
	 *            {@link de.geeksfactory.opacclient.objects.Library#getIdent()}
	 * @param type
	 *            The type of information which should be fetched. Can be one of
	 *            the <code>META_TYPE_</code> constants but also a string
	 *            specific to your OpacApi implementation.
	 * @return A list of datasets, stored in <code>ContentValues</code> objects.
	 */
	public List<Map<String, String>> getMeta(String library, String type);

	/**
	 * Checks whether there is meta data present for a specific library.
	 * 
	 * @param library
	 *            The library identification, see
	 *            {@link de.geeksfactory.opacclient.objects.Library#getIdent()}
	 * @return <code>true</code> if datasets for this library exist,
	 *         <code>false</code> otherwise
	 */
	public boolean hasMeta(String library);

	/**
	 * Checks whether there is meta data of the given type present for a
	 * specific library.
	 * 
	 * @param library
	 *            The library identification, see
	 *            {@link de.geeksfactory.opacclient.objects.Library#getIdent()}
	 * @return <code>true</code> if datasets for this library exist,
	 *         <code>false</code> otherwise
	 */
	public boolean hasMeta(String library, String type);

	/**
	 * Clear all meta data for a specific library.
	 * 
	 * @param library
	 *            The library identification, see
	 *            {@link de.geeksfactory.opacclient.objects.Library#getIdent()}
	 */
	public void clearMeta(String library);

	/**
	 * Clear all meta data for all libraries. Be careful!
	 */
	public void clearMeta();

}
