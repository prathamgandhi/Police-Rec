package xyz.prathamgandhi.cameratest.APIInterface;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import xyz.prathamgandhi.cameratest.models.RegisterModel;

public interface RetrofitRegisterAPI {

    public static String BASE_URL = "https://simplifiedcoding.net/demos/";

    @POST("register")
    Call<RegisterModel> createPost(@Body RegisterModel registerModel);
}
