package de.geeksfactory.opacclient.webservice;

import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class WebServiceManager {
    private static final String BASE_URL = "https://info.opacapp.net";
    private static WebService service;

    public static WebService getInstance() {
        if (service == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build();
            service = retrofit.create(WebService.class);
        }
        return service;
    }
}
