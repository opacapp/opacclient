package de.geeksfactory.opacclient.tests.apitests;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import de.geeksfactory.opacclient.apis.Adis;
import de.geeksfactory.opacclient.apis.BiBer1992;
import de.geeksfactory.opacclient.apis.Bibliotheca;
import de.geeksfactory.opacclient.apis.IOpac;
import de.geeksfactory.opacclient.apis.OpacApi;
import de.geeksfactory.opacclient.apis.Pica;
import de.geeksfactory.opacclient.apis.SISIS;
import de.geeksfactory.opacclient.apis.SRU;
import de.geeksfactory.opacclient.apis.WebOpacNet;
import de.geeksfactory.opacclient.apis.Zones22;
import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.storage.DummyMetaDataSource;

@RunWith(Parameterized.class)
public class BasicLibraryTest {

	private Library library;
	private OpacApi api;

	public BasicLibraryTest(String library) throws JSONException, IOException {
		this.library = Library.fromJSON(
				library,
				new JSONObject(readFile("../assets/bibs/" + library + ".json",
						Charset.defaultCharset())));
	}

	@Parameters(name = "{0}")
	public static Collection<String[]> libraries() {
		List<String[]> libraries = new ArrayList<String[]>();
		for (String file : new File("../assets/bibs/").list()) {
			libraries.add(new String[] { file.replace(".json", "") });
		}
		return libraries;
	}

	@Before
	public void setUp() {
		api = null;
		if (library.getApi().equals("bond26")
				|| library.getApi().equals("bibliotheca"))
			// Backwardscompatibility
			api = new Bibliotheca();
		else if (library.getApi().equals("oclc2011")
				|| library.getApi().equals("sisis"))
			// Backwards compatibility
			api = new SISIS();
		else if (library.getApi().equals("zones22"))
			api = new Zones22();
		else if (library.getApi().equals("biber1992"))
			api = new BiBer1992();
		else if (library.getApi().equals("pica"))
			api = new Pica();
		else if (library.getApi().equals("iopac"))
			api = new IOpac();
		else if (library.getApi().equals("adis"))
			api = new Adis();
		else if (library.getApi().equals("sru"))
			api = new SRU();
		else if (library.getApi().equals("webopac.net"))
			api = new WebOpacNet();
		else
			api = null;
		api.init(new DummyMetaDataSource(), library);
	}

	@Test
	public void test() throws JSONException, IOException {
		api.start();
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}
}
