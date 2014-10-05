package meanings;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Security;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONException;
import org.json.JSONObject;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.searchfields.JavaMeaningDetector;
import de.geeksfactory.opacclient.searchfields.SearchField;
import de.geeksfactory.opacclient.searchfields.SearchField.Meaning;

public class Main {

	public static void main(String[] args) throws IOException, JSONException {
		Security.addProvider(new BouncyCastleProvider());
		Collection<String[]> libraries = libraries();
		Set<String> ignored = new JavaMeaningDetector(null,
				"../../assets/meanings").getIgnoredFields();
		Scanner in = new Scanner(System.in);
		final ExecutorService service = Executors.newFixedThreadPool(25);
		List<Entry<Library, Future<List<SearchField>>>> tasks = new ArrayList<Entry<Library, Future<List<SearchField>>>>();
		for (String[] libraryNameArray : libraries) {
			String libraryName = libraryNameArray[0];
			Library library;
			try {
				library = Library.fromJSON(
						libraryName,
						new JSONObject(readFile("../../assets/bibs/"
								+ libraryName + ".json",
								Charset.defaultCharset())));
				Future<List<SearchField>> future = service
						.submit(new GetSearchFieldsCallable(library));
				tasks.add(new AbstractMap.SimpleEntry<Library, Future<List<SearchField>>>(
						library, future));
			} catch (JSONException | IOException e) {
				// e.printStackTrace();
			}
		}

		for (Entry<Library, Future<List<SearchField>>> entry : tasks) {
			Library library = entry.getKey();
			try {
				List<SearchField> fields = entry.getValue().get();
				if (fields == null)
					continue;

				System.out.println("Bibliothek: " + library.getIdent());
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
					String name;
					if (field.getData() != null
							&& field.getData().has("meaning")) {
						name = field.getData().getString("meaning");
						System.out.print("Unbekanntes Feld: '" + name
								+ "' (Anzeigename: " + field.getDisplayName() + ") ");
					} else {
						name = field.getDisplayName();
						System.out.print("Unbekanntes Feld: '" + name + "' ");
					}
					Meaning meaning = null;
					boolean ignoredField = false;
					while (meaning == null && !ignoredField) {
						String str = in.nextLine();
						if (str.equals("")
								|| str.toLowerCase().equals("ignore")) {
							ignoredField = true;
							detector.addIgnoredField(name);
							ignored.add(name);
						} else {
							try {
								meaning = Meaning.valueOf(str.toUpperCase());
							} catch (IllegalArgumentException e) {
								meaning = null;
							}
						}
					}
					if (meaning != null) {
						detector.addMeaning(name, meaning);
					}
				}
			} catch (JSONException | IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		in.close();
		service.shutdown();
	}

	private static Collection<String[]> libraries() {
		List<String[]> libraries = new ArrayList<String[]>();
		for (String file : new File("../../assets/bibs/").list()) {
			libraries.add(new String[] { file.replace(".json", "") });
		}
		return libraries;
	}

	private static String readFile(String path, Charset encoding)
			throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

}
