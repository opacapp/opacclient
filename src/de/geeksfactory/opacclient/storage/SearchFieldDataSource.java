package de.geeksfactory.opacclient.storage;

import java.util.List;

import de.geeksfactory.opacclient.searchfields.SearchField;

/**
 * Interface for providing access to cached SearchField data
 * 
 * @author Johan von Forstner
 */
public interface SearchFieldDataSource {
	/**
	 * Save search fields for a specific library.
	 * 
	 * @param libraryId
	 *            The ID of the library (Library.getIdent())
	 * @param fields
	 *            the list of search fields to save
	 */
	public void saveSearchFields(String libraryId, List<SearchField> fields);

	/**
	 * Get the saved search fields of a library.
	 * 
	 * @param libraryId
	 *            The ID of the library (Library.getIdent())
	 * @return List of search fields or null if the library has no cached data
	 */
	public List<SearchField> getSearchFields(String libraryId);

	/**
	 * @param libraryId
	 *            The ID of the library (Library.getIdent())
	 * @return whether the library has cached data
	 */
	public boolean hasSearchFields(String libraryId);

	/**
	 * Clear the cached data for a specific library
	 * 
	 * @param libraryId
	 *            The ID of the library (Library.getIdent())
	 */
	public void clearSearchFields(String libraryId);

	/**
	 * Clear the cached data for all libraries
	 */
	public void clearAll();

	/**
	 * @param libraryId
	 *            The ID of the library (Library.getIdent())
	 * @return Timecode (like System.currentTimeMillis()) for when the data of
	 *         this library was last updated
	 */
	public long getLastSearchFieldUpdateTime(String libraryId);
	
	/**
	 * @param libraryId
	 *            The ID of the library (Library.getIdent())
	 * @return Version code of the last app version that updated the search field for this library
	 */
	public int getLastSearchFieldUpdateVersion(String libraryId);
}
