package de.geeksfactory.opacclient.webservice;

import de.geeksfactory.opacclient.reporting.Report;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface WebService {
    @POST("reports")
    Call<Report> sendReport(@Body Report report);
}
