package rs.crtaci.crtaci.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;


public class Cartoon implements Serializable {

    @SerializedName("Id")
    public String id;

    @SerializedName("Character")
    public String character;

    @SerializedName("Title")
    public String title;

    @SerializedName("FormattedTitle")
    public String formattedTitle;

    @SerializedName("Episode")
    public Integer episode;

    @SerializedName("Season")
    public Integer season;

    @SerializedName("Service")
    public String service;

    @SerializedName("Url")
    public String url;

    @SerializedName("Thumbnails")
    public Thumbnails thumbnails;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NL = System.getProperty("line.separator");

        result.append(((Object)this).getClass().getName() + " {" + NL);
        result.append("  id: " + this.id + NL);
        result.append("  character: " + this.character + NL);
        result.append("  title: " + this.title + NL);
        result.append("  formattedTitle: " + this.formattedTitle + NL);
        result.append("  episode: " + this.episode + NL);
        result.append("  season: " + this.season + NL);
        result.append("  service: " + this.service + NL);
        result.append("  url: " + this.url + NL);
        result.append("  thumbnail: " + this.thumbnails.large + NL);
        result.append("}" + NL);

        return result.toString();
    }

}
