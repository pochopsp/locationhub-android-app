package it.unina.locationhub.network;

import java.util.List;

import it.unina.locationhub.model.User;

public abstract class ResponseConverter {

    public abstract List<User> convertAllUsers(String response);

    public abstract boolean isUsernameAlreadyUsed(String response);

    public enum ResponseTypes{
        ACKUSR, SNDUSR
    }

}

