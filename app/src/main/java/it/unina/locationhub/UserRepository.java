package it.unina.locationhub;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.List;

import it.unina.locationhub.model.User;
import it.unina.locationhub.network.RequestConverter;
import it.unina.locationhub.network.RequestConverterImpl;
import it.unina.locationhub.network.ResponseConverter;
import it.unina.locationhub.network.ResponseConverterImpl;
import it.unina.locationhub.network.ServerOrNetworkDisconnectedException;
import it.unina.locationhub.network.TcpClient;

public class UserRepository implements TcpClient.TcpListener {

    private TcpClient tcpClient;
    private final MutableLiveData<ConnectionStatus> connectionStatus;

    //Used to convert requests and responses in our application protocol format
    private final RequestConverter requestConverter;
    private final ResponseConverter responseConverter;

    //Used to store server's responses
    private final MutableLiveData<List<User>> allUsers;
    private final MutableLiveData<Boolean> isUsernameAlreadyUsed;

    private boolean signedIn = false;

    public UserRepository() {

        connectionStatus = new MutableLiveData<>(ConnectionStatus.IDLE);
        allUsers = new MutableLiveData<>();
        isUsernameAlreadyUsed = new MutableLiveData<>();
        requestConverter = new RequestConverterImpl();
        responseConverter = new ResponseConverterImpl();
    }

    public enum ConnectionStatus {

        CONNECTED, DISCONNECTED, IDLE
    }

//-------------------------------Connection methods-------------------------------

    public void connect(String ip, int port) {
        tcpClient = new TcpClient(this,ip,port);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tcpClient.run();
                } catch (ServerOrNetworkDisconnectedException | IOException e) {
                    tcpClient.stopClient();
                    setConnectionStatus(ConnectionStatus.DISCONNECTED);
                }
            }
        }).start();

    }

    public void disconnect() {
        tcpClient.stopClient();
        setConnectionStatus(ConnectionStatus.DISCONNECTED);
    }

    public LiveData<ConnectionStatus> getConnectionStatus() {

        return connectionStatus;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {

        this.connectionStatus.postValue(connectionStatus);
    }

//-------------------------------Live data getters and setters-------------------------------

    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }

    public void setAllUsers(List<User> allUsers){
        this.allUsers.postValue(allUsers);
    }

    public LiveData<Boolean> isUsernameAlreadyUsed() {

        return isUsernameAlreadyUsed;
    }

    public void setIsUsernameAlreadyUsed(Boolean isUsernameAlreadyUsed){
        this.isUsernameAlreadyUsed.postValue(isUsernameAlreadyUsed);
    }

//-------------------------------Communication with server methods-------------------------------
    @SafeVarargs
    public final <T> void sendRequest(RequestConverter.RequestTypes type, T... parameter){

        tcpClient.sendMessage(requestConverter.convertRequest(type,parameter));
    }

    @Override
    public void onMessageReceived(String message) {

        try {
            handleResponse(message);
        } catch (StringIndexOutOfBoundsException e) {
            //If server disconnected while was sending a message
            tcpClient.stopClient();
            setConnectionStatus(ConnectionStatus.DISCONNECTED);

        }
    }

    private void handleResponse(String message){
        ResponseConverter.ResponseTypes responseType =ResponseConverter.ResponseTypes.valueOf(message.substring(0,6));
        switch (responseType){
            case ACKUSR:
                isUsernameAlreadyUsed.postValue(responseConverter.isUsernameAlreadyUsed(message.substring(7,8)));
                break;
            case SNDUSR:
                List<User> users = responseConverter.convertAllUsers(message.substring(6));
                allUsers.postValue(users);
                break;
        }
    }

    @Override
    public void onConnectionEstablished() {
        setConnectionStatus(ConnectionStatus.CONNECTED);
    }

//-------------------------------User login status methods-------------------------------

    public boolean amISignedIn() {
        return signedIn;
    }

    public void setSignedIn(boolean signedIn) {
        this.signedIn = signedIn;
    }

}
