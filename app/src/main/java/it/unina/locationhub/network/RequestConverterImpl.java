package it.unina.locationhub.network;

import com.google.android.gms.maps.model.LatLng;

import it.unina.locationhub.model.User;

public class RequestConverterImpl extends RequestConverter{
    @Override
    public String requestAllUsers() {
        return "GETUSR";
    }

    @Override
    public String insertUser(User user) {

        String isWillingToShareLocation = user.isWillingToShareLocation() ? " 1 " : " 0 ";

        return "ADDUSR " + user.getUsername()+ isWillingToShareLocation + user.getLocationCoordinates().latitude + " " +  user.getLocationCoordinates().longitude
                + " -"+user.getAddress() + "-";
    }
    @Override
    public String updateShareLocationPreference(boolean willingToShare) {
        if(willingToShare)
            return "UPDUSR 1";
        else
            return "UPDUSR 0";
    }

    @Override
    public String updateCurrentLocation(LatLng latLng, String address) {
        return "UPDLOC " + latLng.latitude + " " + latLng.longitude + " -" + address + "-";
    }

    @Override
    public String stillConnected() {
        return "CONUSR";
    }
}
