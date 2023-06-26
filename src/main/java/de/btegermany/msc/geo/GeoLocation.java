package de.btegermany.msc.geo;

public class GeoLocation {

    double lat;
    double lon;
    String country;
    String state;
    String city;
    String continent;

    public GeoLocation(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }



    // Setters, Getters
    public void setContinent(String continent) {
        this.continent = continent;
    }
    public void setCountry(String country) {
        this.country = country;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContinent() {
        return continent;
    }
    public String getCountry() {
        return country;
    }

    public String getState() {
        return state;
    }

    public String getCity() {
        return city;
    }
}