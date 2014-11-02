package rs.crtaci.crtaci.entities;

import java.io.Serializable;


public class Character implements Serializable {

    public String name;

    public String altname;

    public String altname2;

    public String duration;

    public String query;

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String NL = System.getProperty("line.separator");

        result.append(((Object)this).getClass().getName() + " {" + NL);
        result.append("  name: " + this.name + NL);
        result.append("  altname: " + this.altname + NL);
        result.append("  altname2: " + this.altname2 + NL);
        result.append("  duration: " + this.duration + NL);
        result.append("  query: " + this.query + NL);
        result.append("}" + NL);

        return result.toString();
    }

}
