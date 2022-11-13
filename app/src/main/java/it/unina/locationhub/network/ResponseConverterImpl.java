package it.unina.locationhub.network;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.unina.locationhub.model.User;

public class ResponseConverterImpl extends ResponseConverter{
    @Override
    public List<User> convertAllUsers(String response) {
        List<User> allUsers = new ArrayList<>();
        while (response.length()>1) {
            response = response.substring(1);
            String username = response.substring(0, response.indexOf(" "));
            response = response.substring(response.indexOf(" ") + 1);
            String distanceFromClient;
            User user;
            boolean willingToShare;
            LatLng location;
            String address;

            //the user didn't agree to share location
            if (response.startsWith("0")) {
                willingToShare = false;
                response = response.substring(2);
                address = "";
                location = null;
            } else { // the user agreed to share location
                willingToShare = true;
                response = response.substring(2);
                double latitude = Double.parseDouble(response.substring(0, response.indexOf(" ")));
                response = response.substring(response.indexOf(" ") + 1);
                double longitude = Double.parseDouble(response.substring(0, response.indexOf(" ")));
                location = new LatLng(latitude, longitude);
                response = response.substring(response.indexOf(" ") + 2);
                address = response.substring(0, response.indexOf("-"));
                response = response.substring(response.indexOf("-") + 2);
            }

            //the message is finished
            if (!response.contains("#")) {
                distanceFromClient = response;
                response = "";
            } else { //this isn't the last user to convert
                distanceFromClient = response.substring(0, response.indexOf("#"));
                response = response.substring(response.indexOf("#"));
            }

            user = new User(username, Integer.parseInt(distanceFromClient), address, location, willingToShare);
            allUsers.add(user);

        }
        Collections.sort(allUsers);

        return allUsers;
    }

    @Override
    public boolean isUsernameAlreadyUsed(String response) {
        return !response.equals("1");
    }

}
