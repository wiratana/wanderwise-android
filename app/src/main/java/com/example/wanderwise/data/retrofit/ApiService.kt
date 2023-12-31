package com.example.wanderwise.data.retrofit

import com.example.wanderwise.data.response.GetAllPostResponse
import com.example.wanderwise.data.response.LoginResponse
import com.example.wanderwise.data.response.RegisterResponse
import com.example.wanderwise.data.response.UploadImageResponse
import com.example.wanderwise.data.response.UploadPhotoResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ApiService {

    @Multipart
    @POST("posts")
    suspend fun uploadImage(
        @Part("city") title: RequestBody,
        @Part("caption") caption: RequestBody,
        @Part file: MultipartBody.Part
    ): UploadImageResponse

    @FormUrlEncoded
    @POST("auth/register")
    suspend fun register(
        @Field("username") name: String,
        @Field("email") email: String,
        @Field("password") password: String
    ): RegisterResponse

    @FormUrlEncoded
    @POST("auth/login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): LoginResponse

    @Multipart
    @PUT("auth/user")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): UploadPhotoResponse

    @FormUrlEncoded
    @PUT("auth/username")
    suspend fun updateName(
        @Field("name") name: String
    ): UploadPhotoResponse

    @FormUrlEncoded
    @PUT("auth/useremail")
    suspend fun updateEmail(
        @Field("email") email: String
    ): UploadPhotoResponse

    @GET("auth/user")
    fun getPhotoProfile(): Call<UploadPhotoResponse>

    @GET("auth/user")
    fun getUserProfile(): Call<UploadPhotoResponse>

}