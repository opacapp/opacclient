package de.geeksfactory.opacclient.webservice;

import org.joda.time.DateTime;

import java.util.List;

import de.geeksfactory.opacclient.objects.Library;
import de.geeksfactory.opacclient.reporting.Report;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface WebService {
    @POST("reports/")
    Call<Report> sendReport(@Body Report report);

    @GET("androidconfigs/")
    Call<List<Library>> getLibraryConfigs(@Query("modified_since") DateTime modifiedSince,
            @Query("app_version") int appVersion, @Query("plus_app") Integer plusApp,
            @Query("library_id") String libraryId);
}
