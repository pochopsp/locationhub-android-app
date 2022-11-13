package it.unina.locationhub.network;

import com.google.android.gms.maps.model.LatLng;

import it.unina.locationhub.model.User;

public abstract class RequestConverter {

    public abstract String requestAllUsers();

    public abstract String insertUser(User user);

    public abstract String updateShareLocationPreference(boolean willingToShare);

    public abstract String updateCurrentLocation(LatLng latLng, String address);

    public abstract String stillConnected();

    @SafeVarargs
    public final <T> String convertRequest(RequestTypes reqType, T... parameter){
        switch (reqType){
            case GETUSR:
                return requestAllUsers();
            case ADDUSR:
                return insertUser((User) parameter[0]);
            case UPDUSR:
                return updateShareLocationPreference((Boolean) parameter[0]);
            case UPDLOC:
                return updateCurrentLocation((LatLng)parameter[0],(String)parameter[1]);
            case CONUSR:
                return stillConnected();
        }
        return "";
    }

    public enum RequestTypes {
         GETUSR, ADDUSR, UPDUSR, UPDLOC, CONUSR
    }

}
