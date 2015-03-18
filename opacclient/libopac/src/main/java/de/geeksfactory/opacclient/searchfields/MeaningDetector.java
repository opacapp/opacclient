package de.geeksfactory.opacclient.searchfields;

/**
 * A MeaningDetector is an object providing a method which is given a
 * {@link SearchField} and tries to detect the meaning of this search field.
 */
public interface MeaningDetector {
    public SearchField detectMeaning(SearchField field);
}
