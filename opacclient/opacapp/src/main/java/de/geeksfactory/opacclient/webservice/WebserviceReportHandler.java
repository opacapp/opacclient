package de.geeksfactory.opacclient.webservice;

import android.util.Log;

import java.io.IOException;

import de.geeksfactory.opacclient.BuildConfig;
import de.geeksfactory.opacclient.reporting.Report;
import de.geeksfactory.opacclient.reporting.ReportHandler;

public class WebserviceReportHandler implements ReportHandler {
    @Override
    public void sendReport(Report report) {
        WebService service = WebServiceManager.getInstance();
        report.setApp(BuildConfig.APPLICATION_ID);
        report.setVersion(BuildConfig.VERSION_CODE);
        if (BuildConfig.DEBUG) Log.d("OpacClient", report.toString());
        try {
            service.sendReport(report).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
