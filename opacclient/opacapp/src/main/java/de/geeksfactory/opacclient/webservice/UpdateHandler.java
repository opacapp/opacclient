package de.geeksfactory.opacclient.webservice;


import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import de.geeksfactory.opacclient.BuildConfig;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.PreferenceDataSource;
import de.geeksfactory.opacclient.storage.SearchFieldDataSource;
import retrofit2.Response;

public class UpdateHandler {

    protected Response<List<Library>> getServerResponse(WebService service, DateTime last_update)
            throws IOException {
        return service.getLibraryConfigs(last_update, BuildConfig.VERSION_CODE, 0, null)
                      .execute();
    }

    public int updateConfig(WebService service, PreferenceDataSource prefs,
            LibraryConfigUpdateService.FileOutput output,
            SearchFieldDataSource searchFields)
            throws IOException, JSONException {
        DateTime last_update = prefs.getLastLibraryConfigUpdate();
        if (prefs.getLastLibraryConfigUpdateVersion() != BuildConfig.VERSION_CODE) {
            last_update = prefs.getBundledConfigUpdateTime();
        }

        Response<List<Library>> response = getServerResponse(service, last_update);
        if (!response.isSuccessful()) {
            throw new IOException(String.valueOf(response.code()));
        }

        if (prefs.getLastLibraryConfigUpdateVersion() != BuildConfig.VERSION_CODE) {
            output.clearFiles();
        }

        List<Library> updatedLibraries = response.body();

        for (Library lib : updatedLibraries) {
            if (lib == null) {
                continue;
            }
            String filename = lib.getIdent() + ".json";
            JSONObject json = lib.toJSON();
            output.writeFile(filename, json.toString());

            if (searchFields.hasSearchFields(lib.getIdent())) {
                // clear cached search fields when configuration was updated
                searchFields.clearSearchFields(lib.getIdent());
            }
        }

        DateTime lastUpdate = new DateTime(response.headers().get("X-Page-Generated"));
        prefs.setLastLibraryConfigUpdate(lastUpdate);
        prefs.setLastLibraryConfigUpdateVersion(BuildConfig.VERSION_CODE);

        return updatedLibraries.size();
    }
}
