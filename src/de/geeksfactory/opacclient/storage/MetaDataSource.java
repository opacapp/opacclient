package de.geeksfactory.opacclient.storage;

import java.util.List;

import android.content.ContentValues;
import android.database.SQLException;

/**
 * Data source for library meta data (like e.g. the list of branches) stored in
 * a key-value format
 * 
 * @author Raphael Michel
 * @since 2.0.0
 */
public interface MetaDataSource {

	public static String META_TYPE_BRANCH = "zst";
	public static String META_TYPE_CATEGORY = "mg";

	/**
	 * Open up the connection to the data source. Needs to be called before any
	 * read or write operation.
	 * 
	 * @throws SQLException
	 *             on failure
	 */
	public void open() throws SQLException;

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
	public List<ContentValues> getMeta(String library, String type);

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
