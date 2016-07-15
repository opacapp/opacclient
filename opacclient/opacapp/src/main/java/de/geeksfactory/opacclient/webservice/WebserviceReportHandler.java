package de.geeksfactory.opacclient.webservice;

import android.util.Log;

import java.io.IOException;

import de.geeksfactory.opacclient.BuildConfig;
import de.geeksfactory.opacclient.reporting.Report;
import de.geeksfactory.opacclient.reporting.ReportHandler;

public class WebserviceReportHandler implements ReportHandler {
    @Override
    public void sendReport(Report report) {
        if (BuildConfig.DEBUG) {
            Log.d("OpacClient", report.toString());
        }
        WebService service = WebServiceManager.getInstance();
        try {
            service.sendReport(report).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
