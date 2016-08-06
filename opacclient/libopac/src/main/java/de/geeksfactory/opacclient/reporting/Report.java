package de.geeksfactory.opacclient.reporting;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a report an {@link de.geeksfactory.opacclient.apis.OpacApi} might generate if an error
 * occurs or other unexpected things happen (such as needing to use a fallback behaviour that should
 * normally not need to be used), containing further information to debug the issue. You may access
 * these reports by implementing the {@link ReportHandler} interface.
 */
public class Report {
    private final String library;
    private final String api;
    private final String type;
    private final DateTime date;
    private final JSONObject data;
    private String app;
    private int version;

    public Report(String library, String api, String type, DateTime date, JSONObject data) {
        this.library = library;
        this.api = api;
        this.type = type;
        this.date = date;
        this.data = data;
    }

    public String getLibrary() {
        return library;
    }

    public String getApi() {
        return api;
    }

    public String getType() {
        return type;
    }

    public DateTime getDate() {
        return date;
    }

    public JSONObject getData() {
        return data;
    }

    @Override
    public String toString() {
        try {
            return "Report{" +
                    "library='" + library + '\'' +
                    ", api='" + api + '\'' +
                    ", type='" + type + '\'' +
                    ", date=" + date +
                    ", data=" + data.toString(2) +
                    '}';
        } catch (JSONException e) {
            return null;
        }
    }

    public String getApp() {
        return app;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
