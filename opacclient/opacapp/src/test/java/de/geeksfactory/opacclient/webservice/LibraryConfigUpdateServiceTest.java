package de.geeksfactory.opacclient.webservice;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.PreferenceDataSource;
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

    @Before
    public void setUp() {
        service = mock(WebService.class);
        prefs = mock(PreferenceDataSource.class);
        output = mock(LibraryConfigUpdateService.FileOutput.class);
        when(prefs.getLastLibraryConfigUpdate()).thenReturn(LAST_UPDATE);
        library = new Library();
        library.setIdent(IDENT);
    }

    @Test
    public void shouldSetLastUpdate() throws IOException, JSONException {
        List<Library> libraries = new ArrayList<>();
        when(service.getLibraryConfigs(LAST_UPDATE)).thenReturn(Calls.response(libraries));

        LibraryConfigUpdateService.updateConfig(service, prefs, output);
        verifyNoMoreInteractions(output);
        verify(prefs).setLastLibraryConfigUpdate(any(DateTime.class));
    }

    @Test
    public void shouldUpdateLibrary() throws IOException, JSONException {
        List<Library> libraries = new ArrayList<>();
        libraries.add(library);
        when(service.getLibraryConfigs(LAST_UPDATE)).thenReturn(Calls.response(libraries));

        LibraryConfigUpdateService.updateConfig(service, prefs, output);
        verify(output).writeFile(IDENT + ".json", library.toJSON().toString());
        verifyNoMoreInteractions(output);
    }
}
