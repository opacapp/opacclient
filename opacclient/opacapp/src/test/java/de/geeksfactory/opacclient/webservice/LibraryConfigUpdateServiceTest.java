package de.geeksfactory.opacclient.webservice;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.BuildConfig;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.PreferenceDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;
import retrofit2.mock.Calls;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class LibraryConfigUpdateServiceTest {

    private static final DateTime LAST_UPDATE = new DateTime(2000, 1, 1, 0, 0);
    private static final String IDENT = "Test_Library";
    private WebService service;
    private PreferenceDataSource prefs;
    private LibraryConfigUpdateService.FileOutput output;
    private Library library;
    private SearchFieldDataSource searchFields;

    @Before
    public void setUp() {
        service = mock(WebService.class);
        prefs = mock(PreferenceDataSource.class);
        output = mock(LibraryConfigUpdateService.FileOutput.class);
        searchFields = mock(SearchFieldDataSource.class);
        when(prefs.getLastLibraryConfigUpdate()).thenReturn(LAST_UPDATE);
        when(prefs.getLastLibraryConfigUpdateVersion()).thenReturn(BuildConfig.VERSION_CODE);
        library = new Library();
        library.setIdent(IDENT);
    }

    @Test
    public void shouldSetLastUpdate() throws IOException, JSONException {
        List<Library> libraries = new ArrayList<>();
        when(service.getLibraryConfigs(LAST_UPDATE, BuildConfig.VERSION_CODE, 0, null))
                .thenReturn(Calls.response(libraries));

        new UpdateHandler().updateConfig(service, prefs, output, searchFields);
        verifyNoMoreInteractions(output);
        verify(prefs).setLastLibraryConfigUpdate(any(DateTime.class));
        verify(prefs).setLastLibraryConfigUpdateVersion(BuildConfig.VERSION_CODE);
    }

    @Test
    public void shouldUpdateLibrary() throws IOException, JSONException {
        List<Library> libraries = new ArrayList<>();
        libraries.add(library);
        when(service.getLibraryConfigs(LAST_UPDATE, BuildConfig.VERSION_CODE, 0, null))
                .thenReturn(Calls.response(libraries));

        new UpdateHandler().updateConfig(service, prefs, output, searchFields);
        verify(output).writeFile(IDENT + ".json", library.toJSON().toString());
        verifyNoMoreInteractions(output);
    }

    @Test
    public void shouldClearWhenLastUpdateFromOldVersion() throws IOException, JSONException {
        when(prefs.getLastLibraryConfigUpdateVersion()).thenReturn(BuildConfig.VERSION_CODE - 1);

        List<Library> libraries = new ArrayList<>();
        when(service.getLibraryConfigs(null, BuildConfig.VERSION_CODE, 0, null))
                .thenReturn(Calls.response(libraries));

        new UpdateHandler().updateConfig(service, prefs, output, searchFields);
        verify(output).clearFiles();
    }

    @Test
    public void shouldClearSearchFields() throws IOException, JSONException {
        List<Library> libraries = new ArrayList<>();
        libraries.add(library);
        when(service.getLibraryConfigs(LAST_UPDATE, BuildConfig.VERSION_CODE, 0, null))
                .thenReturn(Calls.response(libraries));
        when(searchFields.hasSearchFields(IDENT)).thenReturn(true);

        new UpdateHandler().updateConfig(service, prefs, output, searchFields);
        verify(searchFields).clearSearchFields(IDENT);
    }
}
