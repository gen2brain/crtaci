package rs.crtaci.crtaci.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


public class Character implements Serializable {

    @SerializedName("Name")
    public String name;

    @SerializedName("AltName")
    public String altname;

    @SerializedName("Duration")
    public String duration;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NL = System.getProperty("line.separator");

        result.append(((Object)this).getClass().getName() + " {" + NL);
        result.append("  name: " + this.name + NL);
        result.append("  altname: " + this.altname + NL);
        result.append("  duration: " + this.duration + NL);
        result.append("}" + NL);

        return result.toString();
    }

}
