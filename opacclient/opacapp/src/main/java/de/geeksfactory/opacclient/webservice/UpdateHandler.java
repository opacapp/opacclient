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

    public int updateConfig(WebService service, PreferenceDataSource prefs,
            LibraryConfigUpdateService.FileOutput output,
            SearchFieldDataSource searchFields)
            throws IOException, JSONException {
        if (prefs.getLastLibraryConfigUpdateVersion() != BuildConfig.VERSION_CODE) {
            output.clearFiles();
            prefs.clearLastLibraryConfigUpdate();
        }

        Response<List<Library>>
                response = service.getLibraryConfigs(prefs.getLastLibraryConfigUpdate(),
                BuildConfig.VERSION_CODE, 0, null).execute();
        if (!response.isSuccessful()) {
            throw new IOException(String.valueOf(response.code()));
        }

        List<Library> updatedLibraries = response.body();

        for (Library lib : updatedLibraries) {
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
