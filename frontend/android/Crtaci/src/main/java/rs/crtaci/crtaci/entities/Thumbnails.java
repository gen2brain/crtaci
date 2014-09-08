package rs.crtaci.crtaci.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


public class Thumbnails implements Serializable {

    @SerializedName("Small")
    public String small;

    @SerializedName("Medium")
    public String medium;

    @SerializedName("Large")
    public String large;

}
