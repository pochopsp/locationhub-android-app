package it.unina.locationhub.model;

import com.google.android.gms.maps.model.LatLng;

import java.util.Objects;

public class User implements Comparable{

    private String username;
    private boolean willingToShareLocation;
    private int distanceFromMyLocation;
    private String address;
    private LatLng locationCoordinates;

    public User(String username, int distanceFromClient, String address, LatLng locationCoordinates, boolean willingToShareLocation){
        this.username = username;
        this.willingToShareLocation = willingToShareLocation;
        this.distanceFromMyLocation = distanceFromClient;
        this.address = address;
        this.locationCoordinates = locationCoordinates;
    }

    public String getUsername() {
        return username;
    }

    public boolean isWillingToShareLocation() {
        return willingToShareLocation;
    }

    public int getDistanceFromMyLocation() {
        return distanceFromMyLocation;
    }

    public String getAddress() {
        return address;
    }

    public LatLng getLocationCoordinates() { return locationCoordinates; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        if( Objects.equals(username, user.username) && willingToShareLocation == user.willingToShareLocation)
            return  Objects.equals(locationCoordinates, user.locationCoordinates);
        else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, willingToShareLocation, distanceFromMyLocation, address, locationCoordinates);
    }

    @Override
    public int compareTo(Object o) {
        return Integer.compare(distanceFromMyLocation, ((User) o).distanceFromMyLocation);
    }
}
