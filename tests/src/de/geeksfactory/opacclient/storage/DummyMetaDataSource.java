package de.geeksfactory.opacclient.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DummyMetaDataSource implements MetaDataSource {

	public Map<String, Map<String, List<Map<String, String>>>> data = new HashMap<String, Map<String, List<Map<String, String>>>>();

	@Override
	public void open() throws Exception {
	}

	@Override
	public void close() {
	}

	@Override
	public long addMeta(String type, String library, String key, String value) {
		return 0;
	}

	@Override
	public List<Map<String, String>> getMeta(String library, String type) {
		return new ArrayList<Map<String, String>>();
	}

	@Override
	public boolean hasMeta(String library) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasMeta(String library, String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clearMeta(String library) {
		// TODO Auto-generated method stub
	}

	@Override
	public void clearMeta() {
		// TODO Auto-generated method stub

	}

}
