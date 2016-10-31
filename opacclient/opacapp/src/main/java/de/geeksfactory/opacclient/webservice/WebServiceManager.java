package de.geeksfactory.opacclient.webservice;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Set;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.utils.DebugTools;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class WebServiceManager {
    private static final String BASE_URL = "https://info.opacapp.net";
    private static WebService service;

    public static WebService getInstance() {
        if (service == null) {
            Moshi moshi = new Moshi.Builder()
                    .add(new JSONAdapterFactory())
                    .add(new DateTimeAdapter())
                    .add(new LibraryAdapter())
                    .build();
            Retrofit retrofit = new Retrofit.Builder()
                    .client(DebugTools.prepareHttpClient(new OkHttpClient.Builder()).build())
                    .baseUrl(BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build();
            service = retrofit.create(WebService.class);
        }
        return service;
    }

    private static class LibraryAdapter {

        @FromJson
        public Library fromJson(JsonReader reader) throws IOException {
            JSONObject json = new JSONObjectAdapter().fromJson(reader);
            try {
                return Library.fromJSON(json.getString("_id"), json);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @ToJson
        public void toJson(JsonWriter writer, Library value) throws IOException {
            try {
                JSONObject json = value.toJSON();
                json.put("_id", value.getIdent());
                new JSONObjectAdapter().toJson(writer, json);
            } catch (JSONException e) {
                throw new IOException(e);
            }
        }
    }

    private static class JSONObjectAdapter extends JsonAdapter<JSONObject> {
        @Override
        public JSONObject fromJson(JsonReader reader) throws IOException {
            try {
                JSONObject object = new JSONObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    JsonReader.Token token = reader.peek();
                    if (token == JsonReader.Token.NULL) {
                        reader.nextNull();
                        object.put(name, null);
                    } else if (token == JsonReader.Token.BEGIN_ARRAY) {
                        object.put(name, new JSONArrayAdapter().fromJson(reader));
                    } else if (token == JsonReader.Token.BEGIN_OBJECT) {
                        object.put(name, new JSONObjectAdapter().fromJson(reader));
                    } else if (token == JsonReader.Token.BOOLEAN) {
                        object.put(name, reader.nextBoolean());
                    } else if (token == JsonReader.Token.STRING) {
                        object.put(name, reader.nextString());
                    } else if (token == JsonReader.Token.NUMBER) {
                        object.put(name, reader.nextDouble());
                    }
                }
                reader.endObject();
                return object;
            } catch (JSONException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void toJson(JsonWriter writer, JSONObject value) throws IOException {
            try {
                writer.beginObject();
                Iterator<String> iterator = value.keys();
                while (iterator.hasNext()) {
                    String name = iterator.next();
                    writer.name(name);
                    Object val = value.get(name);
                    if (val == null) {
                        writer.nullValue();
                    }
                    if (val instanceof JSONArray) {
                        new JSONArrayAdapter().toJson(writer, (JSONArray) val);
                    } else if (val instanceof JSONObject) {
                        new JSONObjectAdapter().toJson(writer, (JSONObject) val);
                    } else if (val instanceof Boolean) {
                        writer.value((Boolean) val);
                    } else if (val instanceof String) {
                        writer.value((String) val);
                    } else if (val instanceof Number) {
                        writer.value((Number) val);
                    }
                }
                writer.endObject();
            } catch (JSONException e) {
                throw new IOException(e);
            }
        }
    }

    private static class JSONArrayAdapter extends JsonAdapter<JSONArray> {
        @Override
        public JSONArray fromJson(JsonReader reader) throws IOException {
            try {
                JSONArray array = new JSONArray();
                reader.beginArray();
                while (reader.hasNext()) {
                    JsonReader.Token token = reader.peek();
                    if (token == JsonReader.Token.BEGIN_ARRAY) {
                        array.put(new JSONArrayAdapter().fromJson(reader));
                    } else if (token == JsonReader.Token.BEGIN_OBJECT) {
                        array.put(new JSONObjectAdapter().fromJson(reader));
                    } else if (token == JsonReader.Token.BOOLEAN) {
                        array.put(reader.nextBoolean());
                    } else if (token == JsonReader.Token.NULL) {
                        reader.nextNull();
                        array.put(null);
                    } else if (token == JsonReader.Token.STRING) {
                        array.put(reader.nextString());
                    } else if (token == JsonReader.Token.NUMBER) {
                        array.put(reader.nextDouble());
                    }
                }
                reader.endArray();
                return array;
            } catch (JSONException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void toJson(JsonWriter writer, JSONArray value) throws IOException {
            try {
                writer.beginArray();
                for (int i = 0; i < value.length(); i++) {
                    Object val = value.get(i);
                    if (val == null) {
                        writer.nullValue();
                    }
                    if (val instanceof JSONArray) {
                        new JSONArrayAdapter().toJson(writer, (JSONArray) val);
                    } else if (val instanceof JSONObject) {
                        new JSONObjectAdapter().toJson(writer, (JSONObject) val);
                    } else if (val instanceof Boolean) {
                        writer.value((Boolean) val);
                    } else if (val instanceof String) {
                        writer.value((String) val);
                    } else if (val instanceof Number) {
                        writer.value((Number) val);
                    }
                }
                writer.endArray();
            } catch (JSONException e) {
                throw new IOException(e);
            }
        }
    }

    private static class DateTimeAdapter {
        @ToJson
        String toJson(DateTime dateTime) {
            return ISODateTimeFormat.dateTime().print(dateTime);
        }

        @FromJson
        DateTime fromJson(String string) {
            return ISODateTimeFormat.dateTime().parseDateTime(string);
        }
    }

    private static class JSONAdapterFactory implements JsonAdapter.Factory {
        @Override
        public JsonAdapter<?> create(Type type, Set<? extends Annotation> annotations,
                Moshi moshi) {
            if (type == JSONObject.class) {
                return new JSONObjectAdapter();
            } else if (type == JSONArray.class) {
                return new JSONArrayAdapter();
            } else {
                return null;
            }
        }
    }
}
