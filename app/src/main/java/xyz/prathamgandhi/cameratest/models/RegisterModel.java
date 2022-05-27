package xyz.prathamgandhi.cameratest.models;

import android.os.Bundle;

public class RegisterModel {

    private String imageBase64, first_name, last_name, password, phone, stationId;

    public RegisterModel(String imageBase64, Bundle extras) {

        this.imageBase64 = imageBase64;
        this.first_name = extras.getString("firstName");
        this.last_name = extras.getString("lastName");
        this.password = extras.getString("passWord");
        this.phone = extras.getString("phoneNum");
        this.stationId = extras.getString("stationId");
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStation_id() {
        return stationId;
    }

    public void setStation_id(String station_id) {
        this.stationId = station_id;
    }
}
