package de.geeksfactory.opacclient.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DummyMetaDataSource implements MetaDataSource {

	private Map<String, Map<String, List<Map<String, String>>>> data = new HashMap<String, Map<String, List<Map<String, String>>>>();

	@Override
	public void open() throws Exception {
	}

	@Override
	public void close() {
	}

	@Override
	public long addMeta(String type, String library, String key, String value) {
		Map<String, List<Map<String, String>>> libData;
		if (data.containsKey(library))
			libData = data.get(library);
		else
			libData = new HashMap<String, List<Map<String, String>>>();
		List<Map<String, String>> typeData;
		if (libData.containsKey(type))
			typeData = libData.get(type);
		else
			typeData = new ArrayList<Map<String, String>>();
		Map<String, String> meta = new HashMap<String, String>();
		meta.put("key", key);
		meta.put("value", value);
		typeData.add(meta);
		libData.put(type, typeData);
		data.put(library, libData);
		return 0;
	}

	@Override
	public List<Map<String, String>> getMeta(String library, String type) {
		if (data.containsKey(library)) {
			Map<String, List<Map<String, String>>> libData = data.get(library);
			if (libData.containsKey(type)) {
				return libData.get(type);
			}
		}
		return new ArrayList<Map<String, String>>();
	}

	@Override
	public boolean hasMeta(String library) {
		return data.containsKey(library);
	}

	@Override
	public boolean hasMeta(String library, String type) {
		return data.containsKey(library) && data.get(library).containsKey(type);
	}

	@Override
	public void clearMeta(String library) {
		if (data.containsKey(library))
			data.get(library).clear();
	}

	@Override
	public void clearMeta() {
		data.clear();
	}

}
