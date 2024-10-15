package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.api


import com.google.android.gms.common.api.internal.ApiKey
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("photos/random")
    fun getRandomImage(@Query("client_id") apiKey: String): Call<ImageResponse>

}
