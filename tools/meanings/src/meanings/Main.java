package meanings;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.OpacApi.OpacErrorException;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.JavaMeaningDetector;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;
import de.geeksfactory.opacclient.tests.apitests.LibraryApiTestCases;

public class Main {

	public static void main(String[] args) throws IOException, JSONException {
		Collection<String[]> libraries = libraries();
		Set<String> ignored = new JavaMeaningDetector(null,
				"../../assets/meanings").getIgnoredFields();
		Scanner in = new Scanner(System.in);
		for (String[] libraryNameArray : libraries) {
			String libraryName = libraryNameArray[0];
			System.out.println("Bibliothek: " + libraryName);
			try {
				Library library = Library.fromJSON(
						libraryName,
						new JSONObject(readFile("../../assets/bibs/"
								+ libraryName + ".json",
								Charset.defaultCharset())));
				OpacApi api = LibraryApiTestCases.getApi(library);
				List<SearchField> fields = api.getSearchFields();
				JavaMeaningDetector detector = new JavaMeaningDetector(library,
						"../../assets/meanings");
				for (int i = 0; i < fields.size(); i++) {
					fields.set(i, detector.detectMeaning(fields.get(i)));
				}
				for (SearchField field : fields) {
					if (field.getMeaning() != null
							|| ignored.contains(field.getDisplayName())
							|| field.getData() != null
							&& field.getData().has("meaning")
							&& ignored.contains(field.getData().getString(
									"meaning")))
						continue;
					System.out.print("Unbekanntes Feld: '"
							+ field.getDisplayName() + "' ");
					Meaning meaning = null;
					boolean ignoredField = false;
					while (meaning == null && !ignoredField) {
						String str = in.nextLine();
						if (str.equals("") || str.toLowerCase().equals("ignore")) {
							ignoredField = true;
							detector.addIgnoredField(field.getDisplayName());
							ignored.add(field.getDisplayName());
						} else {
							try {
								meaning = Meaning.valueOf(str.toUpperCase());
							} catch (IllegalArgumentException e) {
								meaning = null;
							}
						}
					}
					if (meaning != null) {
						detector.addMeaning(field.getDisplayName(), meaning);
					}
				}
			} catch (JSONException | IOException e) {
			} catch (OpacErrorException e) {
			}
		}
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	private static Collection<String[]> libraries() {
		List<String[]> libraries = new ArrayList<String[]>();
		for (String file : new File("../../assets/bibs/").list()) {
			libraries.add(new String[] { file.replace(".json", "") });
		}
		return libraries;
	}

}
