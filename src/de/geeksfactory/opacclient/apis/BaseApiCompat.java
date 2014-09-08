package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.geeksfactory.opacclient.NotReachableException;
import de.geeksfactory.opacclient.objects.SearchRequestResult;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.searchfields.SearchQuery;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.storage.MetaDataSource;

public abstract class BaseApiCompat extends BaseApi implements OpacApi {

	public abstract String[] getSearchFieldsCompat();
	public abstract SearchRequestResult search(Map<String, String> query) throws IOException, NotReachableException, OpacErrorException;
	
	public SearchRequestResult search(List<SearchQuery> queryList)
			throws NotReachableException, IOException, OpacErrorException {
		Map<String, String> queryMap = new HashMap<String, String>();
		for (SearchQuery query:queryList) {
			queryMap.put(query.getKey(), query.getValue());
		}
		return search(queryMap);
	}

	@Override
	public List<SearchField> getSearchFields() throws OpacErrorException {

		List<SearchField> searchFields = new ArrayList<SearchField>();
		Set<String> fieldsCompat = new HashSet<String>(
				Arrays.asList(getSearchFieldsCompat()));

		try {
			metadata.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_BRANCH)
				|| fieldsCompat.contains(KEY_SEARCH_QUERY_HOME_BRANCH)
				|| fieldsCompat.contains(KEY_SEARCH_QUERY_CATEGORY)) {
			// only look for metadata if we need it
			if (!metadata.hasMeta(library.getIdent())) {
				metadata.close();
				try {
					start();
				} catch (IOException e) {
				}
				try {
					metadata.open();
				} catch (Exception e) {
				}
			}

			if (!metadata.hasMeta(library.getIdent()))
				throw new OpacErrorException(
						"Es ist ein Fehler beim Laden der Suchfelder aufgetreten. "
								+ "Bitte prüfen Sie Ihre Internetverbindung.");
		}

		Map<String, String> all = new HashMap<String, String>();
		all.put("key", "");
		all.put("value", "Alle");

		if (fieldsCompat.contains(KEY_SEARCH_QUERY_FREE)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_FREE, "",
					false, false, "Freie Suche", true, false);
			field.setMeaning(Meaning.FREE);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_TITLE)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_TITLE,
					"Titel", false, false, "Stichwort", false, false);
			field.setMeaning(Meaning.TITLE);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_AUTHOR)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_AUTHOR,
					"Verfasser", false, false, "Nachname, Vorname", false,
					false);
			field.setMeaning(Meaning.AUTHOR);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_DIGITAL)) {
			SearchField field = new CheckboxSearchField(KEY_SEARCH_QUERY_DIGITAL,
					"nur digitale Medien", false);
			field.setMeaning(Meaning.DIGITAL);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_AVAILABLE)) {
			SearchField field = new CheckboxSearchField(KEY_SEARCH_QUERY_AVAILABLE,
							"nur verfügbare Medien", false);
			field.setMeaning(Meaning.AVAILABLE);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_ISBN)) {
			SearchField field = new BarcodeSearchField(KEY_SEARCH_QUERY_ISBN,
					"Strichcode", false, false, "ISBN");
			field.setMeaning(Meaning.ISBN);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_BARCODE)) {
			SearchField field = new BarcodeSearchField(KEY_SEARCH_QUERY_BARCODE,
					"Strichcode", false, true, "Buchungsnr.");
			field.setMeaning(Meaning.BARCODE);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_YEAR, "Jahr",
					false, false, "", false, true);
			field.setMeaning(Meaning.YEAR);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR_RANGE_START)) {
			SearchField field = new TextSearchField(
					KEY_SEARCH_QUERY_YEAR_RANGE_START, "Jahr", false, false,
					"von", false, true);
			field.setMeaning(Meaning.YEAR);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR_RANGE_END)) {
			SearchField field = new TextSearchField(
					KEY_SEARCH_QUERY_YEAR_RANGE_END, "Jahr", false, true,
					"bis", false, true);
			field.setMeaning(Meaning.YEAR);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_BRANCH)) {
			List<Map<String, String>> data = metadata.getMeta(
					library.getIdent(), MetaDataSource.META_TYPE_BRANCH);
			data.add(0, all);
			SearchField field = new DropdownSearchField(KEY_SEARCH_QUERY_BRANCH,
					"Zweigstelle", false, data);
			field.setMeaning(Meaning.BRANCH);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_HOME_BRANCH)) {
			List<Map<String, String>> data = metadata.getMeta(
					library.getIdent(), MetaDataSource.META_TYPE_HOME_BRANCH);
			data.add(0, all);
			SearchField field =
					new DropdownSearchField(KEY_SEARCH_QUERY_HOME_BRANCH,
							"Aktuelle Zweigstelle („eigene Zweigstelle“)",
							false, data);
			field.setMeaning(Meaning.HOME_BRANCH);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_CATEGORY)) {
			List<Map<String, String>> data = metadata.getMeta(
					library.getIdent(), MetaDataSource.META_TYPE_CATEGORY);
			data.add(0, all);
			SearchField field = new DropdownSearchField(KEY_SEARCH_QUERY_CATEGORY,
					"Mediengruppe", false, data);
			field.setMeaning(Meaning.CATEGORY);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_PUBLISHER)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_PUBLISHER,
					"Verlag", false, false, "", false, false);
			field.setMeaning(Meaning.PUBLISHER);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_KEYWORDA)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_KEYWORDA,
					"Schlagwort", true, false, "", false, false);
			field.setMeaning(Meaning.KEYWORD);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_KEYWORDB)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_KEYWORDB,
					"Schlagwort", true, true, "", false, false);
			field.setMeaning(Meaning.KEYWORD);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_SYSTEM)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_SYSTEM,
					"Systematik", true, false, "", false, false);
			field.setMeaning(Meaning.SYSTEM);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_AUDIENCE)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_AUDIENCE,
					"Interessenkreis", true, false, "", false, false);
			field.setMeaning(Meaning.AUDIENCE);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_LOCATION)) {
			SearchField field = new TextSearchField(KEY_SEARCH_QUERY_LOCATION,
					"Ort", false, false, "", false, false);
			field.setMeaning(Meaning.LOCATION);
			searchFields.add(field);
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_ORDER)) {
			// TODO: Implement this (was this even usable before?)
		}
		return searchFields;
	}
	
	@Override
	public boolean shouldUseMeaningDetector() {
		return false;
	}

}
