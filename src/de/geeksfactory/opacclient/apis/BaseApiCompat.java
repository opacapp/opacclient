package de.geeksfactory.opacclient.apis;

import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.objects.SearchField;

public abstract class BaseApiCompat extends BaseApi implements OpacApi {

	public abstract String[] getSearchFieldsCompat();
	
	@Override
	public List<SearchField> getSearchFields() {
		List<SearchField> searchFields = new ArrayList<SearchField>();
		for (String field:getSearchFieldsCompat()) {
			if (field.equals(KEY_SEARCH_QUERY_FREE)) {
				searchFields.add(SearchField.getTextInstance(field, "", "Freie Suche", false, true, false));
			} else if (field.equals(KEY_SEARCH_QUERY_TITLE)) {
				searchFields.add(SearchField.getTextInstance(field, "Titel", "Stichwort", false, false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_AUTHOR)) {
				searchFields.add(SearchField.getTextInstance(field, "Verfasser", "Nachname, Vorname", false, false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_KEYWORDA)) {
				searchFields.add(SearchField.getTextInstance(field, "Schlagwort", "", false, false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_KEYWORDB)) {
				searchFields.add(SearchField.getTextInstance(field, "Schlagwort", "", true, false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_BRANCH)) {
				//TODO:
			} else if (field.equals(KEY_SEARCH_QUERY_HOME_BRANCH)) {
				//TODO:
			} else if (field.equals(KEY_SEARCH_QUERY_ISBN)) {
				searchFields.add(SearchField.getBarcodeInstance(field, "Strichcode", "ISBN", false));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR)) {
				searchFields.add(SearchField.getTextInstance(field, "Jahr", "", false, false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR_RANGE_START)) {
				searchFields.add(SearchField.getTextInstance(field, "Jahr", "von", false, false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_YEAR_RANGE_END)) {
				searchFields.add(SearchField.getTextInstance(field, "Jahr", "bis", true, false, true));
			} else if (field.equals(KEY_SEARCH_QUERY_SYSTEM)) {
				searchFields.add(SearchField.getTextInstance(field, "Systematik", "", false, false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_AUDIENCE)) {
				searchFields.add(SearchField.getTextInstance(field, "Interessenkreis", "", false, false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_PUBLISHER)) {
				searchFields.add(SearchField.getTextInstance(field, "Verlag", "", false, false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_CATEGORY)) {
				//TODO:
			} else if (field.equals(KEY_SEARCH_QUERY_BARCODE)) {
				searchFields.add(SearchField.getBarcodeInstance(field, "Strichcode", "Buchungsnr.", false));
			} else if (field.equals(KEY_SEARCH_QUERY_LOCATION)) {
				searchFields.add(SearchField.getTextInstance(field, "Ort", "", false, false, false));
			} else if (field.equals(KEY_SEARCH_QUERY_DIGITAL)) {
				searchFields.add(SearchField.getCheckboxInstance(field, "nur digitale Medien"));
			} else if (field.equals(KEY_SEARCH_QUERY_AVAILABLE)) {
				searchFields.add(SearchField.getCheckboxInstance(field, "nur verfügbare Medien"));
			}
		}
		return searchFields;
	}

}
