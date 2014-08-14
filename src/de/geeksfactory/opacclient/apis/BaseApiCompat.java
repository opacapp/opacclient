package de.geeksfactory.opacclient.apis;

import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.searchfields.BarcodeSearchField;
import de.geeksfactory.opacclient.searchfields.CheckboxSearchField;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.TextSearchField;

public abstract class BaseApiCompat extends BaseApi implements OpacApi {

	public abstract String[] getSearchFieldsCompat();
	
	@Override
	public List<SearchField> getSearchFields() {
		List<SearchField> searchFields = new ArrayList<SearchField>();
		for (String field:getSearchFieldsCompat()) {
			if (field.equals(KEY_SEARCH_QUERY_FREE)) {
				searchFields.add(new TextSearchField(field, "", false, "Freie Suche", true, false));
			} else if (field.equals(KEY_SEARCH_QUERY_TITLE)) {
				searchFields.add(new TextSearchField(field, "Titel", false, "Stichwort", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_AUTHOR)) {
				searchFields.add(new TextSearchField(field, "Verfasser", false, "Nachname, Vorname", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_KEYWORDA)) {
				searchFields.add(new TextSearchField(field, "Schlagwort", false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_KEYWORDB)) {
				searchFields.add(new TextSearchField(field, "Schlagwort", true, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_BRANCH)) {
				//TODO:
			} else if (field.equals(KEY_SEARCH_QUERY_HOME_BRANCH)) {
				//TODO:
			} else if (field.equals(KEY_SEARCH_QUERY_ISBN)) {
				searchFields.add(new BarcodeSearchField(field, "Strichcode", false, "ISBN"));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR)) {
				searchFields.add(new TextSearchField(field, "Jahr", false, "", false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR_RANGE_START)) {
				searchFields.add(new TextSearchField(field, "Jahr", false, "von", false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR_RANGE_END)) {
				searchFields.add(new TextSearchField(field, "Jahr", true, "bis", false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_SYSTEM)) {
				searchFields.add(new TextSearchField(field, "Systematik", false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_AUDIENCE)) {
				searchFields.add(new TextSearchField(field, "Interessenkreis", false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_PUBLISHER)) {
				searchFields.add(new TextSearchField(field, "Verlag", false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_CATEGORY)) {
				//TODO:
			} else if (field.equals(KEY_SEARCH_QUERY_BARCODE)) {
				searchFields.add(new BarcodeSearchField(field, "Strichcode", false, "Buchungsnr."));
			} else if (field.equals(KEY_SEARCH_QUERY_LOCATION)) {
				searchFields.add(new TextSearchField(field, "Ort", false, "", false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_DIGITAL)) {
				searchFields.add(new CheckboxSearchField(field, "nur digitale Medien"));
			} else if (field.equals(KEY_SEARCH_QUERY_AVAILABLE)) {
				searchFields.add(new CheckboxSearchField(field, "nur verfügbare Medien"));
			}
		}
		return searchFields;
	}

}
