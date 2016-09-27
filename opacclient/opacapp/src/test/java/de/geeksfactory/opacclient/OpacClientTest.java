package de.geeksfactory.opacclient;

import android.content.res.AssetManager;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import de.geeksfactory.opacclient.storage.PreferenceDataSource;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpacClientTest {
    public static final String IDENT = "Test_Library";
    public static final String ENCODING = "UTF-8";
    private OpacClient app;
    private File filesDir;
    private AssetManager assets;
    private PreferenceDataSource preferences;

    @Before
    public void setUp() throws IOException {
        app = spy(OpacClient.class);

        assets = mock(AssetManager.class);
        when(assets.open(OpacClient.ASSETS_BIBSDIR + "/" + IDENT + ".json"))
                .thenReturn(IOUtils.toInputStream("", ENCODING));
        when(app.getAssets()).thenReturn(assets);

        filesDir = mock(File.class);
        when(app.getLibrariesDir()).thenReturn(filesDir);

        doReturn(IOUtils.toInputStream("", ENCODING)).when(app).openFile(filesDir, IDENT + ".json");

        preferences = mock(PreferenceDataSource.class);
        when(app.getPreferenceDataSource()).thenReturn(preferences);
        when(preferences.getLastLibraryConfigUpdateVersion()).thenReturn(BuildConfig.VERSION_CODE);
    }

    @Test
    public void shouldUseBundledVersionWhenUpdatedNotAvailable() throws IOException {
        doReturn(false).when(app).fileExists(filesDir, IDENT + ".json");
        try {
            app.getLibrary(IDENT);
        } catch (JSONException | NullPointerException e) {
        }
        verify(assets).open(OpacClient.ASSETS_BIBSDIR + "/" + IDENT + ".json");
    }

    @Test
    public void shouldUseBundledVersionWhenUpdatedFromOldVersion() throws IOException {
        when(preferences.getLastLibraryConfigUpdateVersion())
                .thenReturn(BuildConfig.VERSION_CODE - 1);
        doReturn(false).when(app).fileExists(filesDir, IDENT + ".json");
        try {
            app.getLibrary(IDENT);
        } catch (JSONException | NullPointerException e) {
        }
        verify(assets).open(OpacClient.ASSETS_BIBSDIR + "/" + IDENT + ".json");
    }

    @Test
    public void shouldUseUpdatedVersionWhenAvailable() throws IOException {
        doReturn(true).when(app).fileExists(filesDir, IDENT + ".json");
        try {
            app.getLibrary(IDENT);
        } catch (JSONException | NullPointerException e) {
        }
        verify(app).openFile(filesDir, IDENT + ".json");
    }
}
