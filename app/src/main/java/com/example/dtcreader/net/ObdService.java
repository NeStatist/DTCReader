package com.example.dtcreader.net;

import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ObdService {
    @POST("/")
    Response uploadReading(@Body ObdReading reading);
}
