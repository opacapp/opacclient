package meanings;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.json.JSONException;

import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.tests.apitests.LibraryApiTestCases;

public class GetSearchFieldsCallable implements Callable<List<SearchField>> {
	private Library lib;
	
	public GetSearchFieldsCallable(Library lib) {
		this.lib = lib;
	}

	@Override
	public List<SearchField> call() {
		OpacApi api = LibraryApiTestCases.getApi(lib);
		try {
			return api.getSearchFields();
		} catch (IOException | OpacErrorException | JSONException e) {
			// Fail silently
		}
		return null;
	}

}
