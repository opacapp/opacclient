package de.geeksfactory.opacclient.apis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		for (String field:getSearchFieldsCompat()) {
			if (field.equals(KEY_SEARCH_QUERY_FREE)) {
				searchFields.add(new TextSearchField(field, "", false, false, "Freie Suche", true, false));
			} else if (field.equals(KEY_SEARCH_QUERY_TITLE)) {
				searchFields.add(new TextSearchField(field, "Titel", false, false, "Stichwort", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_AUTHOR)) {
				searchFields.add(new TextSearchField(field, "Verfasser", false, false, "Nachname, Vorname", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_DIGITAL)) {
				searchFields.add(new CheckboxSearchField(field, "nur digitale Medien", false));
			} else if (field.equals(KEY_SEARCH_QUERY_AVAILABLE)) {
				searchFields.add(new CheckboxSearchField(field, "nur verfügbare Medien", false));
			} else if (field.equals(KEY_SEARCH_QUERY_ISBN)) {
				searchFields.add(new BarcodeSearchField(field, "Strichcode", false, false, "ISBN"));
			} else if (field.equals(KEY_SEARCH_QUERY_BARCODE)) {
				searchFields.add(new BarcodeSearchField(field, "Strichcode", false, false, "Buchungsnr."));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR)) {
				searchFields.add(new TextSearchField(field, "Jahr", false, false, "", false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR_RANGE_START)) {
				searchFields.add(new TextSearchField(field, "Jahr", false, false, "von", false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR_RANGE_END)) {
				searchFields.add(new TextSearchField(field, "Jahr", false, true, "bis", false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_BRANCH)) {
				List<Map<String, String>> data = metadata.getMeta(
						library.getIdent(), MetaDataSource.META_TYPE_BRANCH);
				data.add(0, all);
				searchFields.add(new DropdownSearchField(field, "Zweigstelle", false, data));
			} else if (field.equals(KEY_SEARCH_QUERY_HOME_BRANCH)) {
				List<Map<String, String>> data = metadata.getMeta(
						library.getIdent(), MetaDataSource.META_TYPE_HOME_BRANCH);
				data.add(0, all);
				searchFields.add(new DropdownSearchField(field, "Aktuelle Zweigstelle („eigene Zweigstelle“)", false, data));
			} else if (field.equals(KEY_SEARCH_QUERY_CATEGORY)) {
				List<Map<String, String>> data = metadata.getMeta(
						library.getIdent(), MetaDataSource.META_TYPE_CATEGORY);
				data.add(0, all);
				searchFields.add(new DropdownSearchField(field, "Mediengruppe", false, data));
			} else if (field.equals(KEY_SEARCH_QUERY_PUBLISHER)) {
				searchFields.add(new TextSearchField(field, "Verlag", false, false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_KEYWORDA)) {
				searchFields.add(new TextSearchField(field, "Schlagwort", true, false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_KEYWORDB)) {
				searchFields.add(new TextSearchField(field, "Schlagwort", true, true, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_SYSTEM)) {
				searchFields.add(new TextSearchField(field, "Systematik", true, false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_AUDIENCE)) {
				searchFields.add(new TextSearchField(field, "Interessenkreis", true, false, "", false, false));		
			} else if (field.equals(KEY_SEARCH_QUERY_LOCATION)) {
				searchFields.add(new TextSearchField(field, "Ort", false, false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_ORDER)) {
				//TODO: Implement this (was this even usable before?)
			}
		}
		return searchFields;
	}

}
