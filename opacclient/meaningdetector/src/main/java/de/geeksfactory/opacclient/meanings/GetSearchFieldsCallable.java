package de.geeksfactory.opacclient.meanings;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import de.geeksfactory.opacclient.OpacApiFactory;
import de.geeksfactory.opacclient.apis.ApacheBaseApi;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.SearchField;

public class GetSearchFieldsCallable implements Callable<Map<String, List<SearchField>>> {
    private Library lib;

    public GetSearchFieldsCallable(Library lib) {
        this.lib = lib;
    }

    @Override
    public Map<String, List<SearchField>> call() {
        OpacApi api = OpacApiFactory.create(lib, "OpacApp/Test");

        if (api instanceof ApacheBaseApi) {
            ((ApacheBaseApi) api).setHttpLoggingEnabled(false);
        }

        Set<String> langs = null;
        try {
            langs = api.getSupportedLanguages();
        } catch (IOException e) {
        }

        if (langs == null) {
            // Use default language
            try {
                Map<String, List<SearchField>> map = new HashMap<>();
                map.put("default", api.getSearchFields());
                return map;
            } catch (IOException | OpacErrorException | JSONException e) {
            }
        } else {
            Map<String, List<SearchField>> map = new HashMap<>();
            for (String lang : langs) {
                api.setLanguage(lang);
                try {
                    map.put(lang, api.getSearchFields());
                } catch (IOException | OpacErrorException | JSONException e) {
                }
            }
            return map;
        }
        return null;
    }

}
