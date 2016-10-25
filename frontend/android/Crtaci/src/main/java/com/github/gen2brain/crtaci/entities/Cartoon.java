package com.github.gen2brain.crtaci.entities;

import java.io.Serializable;


public class Cartoon implements Serializable {

    public String id;

    public String character;

    public String title;

    public String formattedTitle;

    public Integer episode;

    public Integer season;

    public String service;

    public String url;

    public String thumbSmall;

    public String thumbMedium;

    public String thumbLarge;

    public String durationString;

    @Override
    public boolean equals(Object obj) {
       	if(obj == null) return false;
       	if(obj == this) return true;
       	if(!(obj instanceof Cartoon)) return false;
       	Cartoon cartoon = (Cartoon) obj;
       	return cartoon.url.equals(this.url);
    }

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
        result.append("  thumbSmall: " + this.thumbSmall + NL);
        result.append("  thumbMedium: " + this.thumbMedium + NL);
        result.append("  thumbLarge: " + this.thumbLarge + NL);
        result.append("  durationString: " + this.durationString + NL);
        result.append("}" + NL);

        return result.toString();
    }

}
