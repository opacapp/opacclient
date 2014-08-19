package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.util.Log;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.DropdownSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.TextSearchField;
import de.geeksfactory.opacclient.storage.MetaDataSource;

public abstract class BaseApiCompat extends BaseApi implements OpacApi {

	public abstract String[] getSearchFieldsCompat();
	
	@Override
	public List<SearchField> getSearchFields(MetaDataSource metadata, Library library) throws OpacErrorException {
		try {
			metadata.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!metadata.hasMeta(library.getIdent())) {
			try {
				start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (!metadata.hasMeta(library.getIdent()))
			throw new OpacErrorException("Fehler beim Laden der Suchfelder");
				
		Map<String, String> all = new HashMap<String, String>();
		all.put("key", "");
		all.put("value", "Alle");
		
		List<SearchField> searchFields = new ArrayList<SearchField>();
		Set<String> fieldsCompat = new HashSet<String>(Arrays.asList(getSearchFieldsCompat()));
		
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_FREE)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_FREE, "", false, false, "Freie Suche", true, false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_TITLE)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_TITLE, "Titel", false, false, "Stichwort", false, false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_AUTHOR)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_AUTHOR, "Verfasser", false, false, "Nachname, Vorname", false, false));
		} 
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_DIGITAL)) {
			searchFields.add(new CheckboxSearchField(KEY_SEARCH_QUERY_DIGITAL, "nur digitale Medien", false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_AVAILABLE)) {
			searchFields.add(new CheckboxSearchField(KEY_SEARCH_QUERY_AVAILABLE, "nur verfügbare Medien", false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_ISBN)) {
			searchFields.add(new BarcodeSearchField(KEY_SEARCH_QUERY_ISBN, "Strichcode", false, false, "ISBN"));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_BARCODE)) {
			searchFields.add(new BarcodeSearchField(KEY_SEARCH_QUERY_BARCODE, "Strichcode", false, true, "Buchungsnr."));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_YEAR, "Jahr", false, false, "", false, true));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR_RANGE_START)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_YEAR_RANGE_START, "Jahr", false, false, "von", false, true));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_YEAR_RANGE_END)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_YEAR_RANGE_END, "Jahr", false, true, "bis", false, true));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_BRANCH)) {
			List<Map<String, String>> data = metadata.getMeta(
					library.getIdent(), MetaDataSource.META_TYPE_BRANCH);
			data.add(0, all);
			searchFields.add(new DropdownSearchField(KEY_SEARCH_QUERY_BRANCH, "Zweigstelle", false, data));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_HOME_BRANCH)) {
			List<Map<String, String>> data = metadata.getMeta(
					library.getIdent(), MetaDataSource.META_TYPE_HOME_BRANCH);
			data.add(0, all);
			searchFields.add(new DropdownSearchField(KEY_SEARCH_QUERY_HOME_BRANCH, "Aktuelle Zweigstelle („eigene Zweigstelle“)", false, data));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_CATEGORY)) {
			List<Map<String, String>> data = metadata.getMeta(
					library.getIdent(), MetaDataSource.META_TYPE_CATEGORY);
			data.add(0, all);
			searchFields.add(new DropdownSearchField(KEY_SEARCH_QUERY_CATEGORY, "Mediengruppe", false, data));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_PUBLISHER)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_PUBLISHER, "Verlag", false, false, "", false, false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_KEYWORDA)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_KEYWORDA, "Schlagwort", true, false, "", false, false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_KEYWORDB)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_KEYWORDB, "Schlagwort", true, true, "", false, false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_SYSTEM)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_SYSTEM, "Systematik", true, false, "", false, false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_AUDIENCE)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_AUDIENCE, "Interessenkreis", true, false, "", false, false));		
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_LOCATION)) {
			searchFields.add(new TextSearchField(KEY_SEARCH_QUERY_LOCATION, "Ort", false, false, "", false, false));
		}
		if (fieldsCompat.contains(KEY_SEARCH_QUERY_ORDER)) {
			//TODO: Implement this (was this even usable before?)
		}
		return searchFields;
	}

}
