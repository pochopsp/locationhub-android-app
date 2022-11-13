package it.unina.locationhub;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import it.unina.locationhub.model.User;
import it.unina.locationhub.network.RequestConverter;

public class MainViewModel extends ViewModel {

    private final UserRepository userRepository;

    //Users retrieved from server
    private final LiveData<List<User>> allUsers;

    private final LiveData<UserRepository.ConnectionStatus> connectionStatus;

    //flag used to check if the username is already taken by another user on server
    private final LiveData<Boolean> isUsernameAlreadyUsed;

    //used to setup map in maps fragment
    private final MutableLiveData<LatLng> myCurrentLocation;
    private final MutableLiveData<CameraPosition> cameraPosition;
    private final MutableLiveData<Boolean> isUsersListChanged;

    //used to store user preferences from settings fragment
    private final MutableLiveData<Integer> distanceLimitPreference;
    private final MutableLiveData<Boolean> willingToShareLocation;

    public MainViewModel() {

        userRepository = new UserRepository();
        connectionStatus = userRepository.getConnectionStatus();
        allUsers = userRepository.getAllUsers();
        isUsernameAlreadyUsed = userRepository.isUsernameAlreadyUsed();
        myCurrentLocation = new MutableLiveData<>();
        cameraPosition = new MutableLiveData<>();
        willingToShareLocation = new MutableLiveData<>();
        distanceLimitPreference = new MutableLiveData<>(Integer.MAX_VALUE);
        isUsersListChanged = new MutableLiveData<>(true);
    }


    public LiveData<List<User>> getAllUsers() {

        return allUsers;
    }

    public void updateAndSendCurrentLocation(LatLng newLocation, String addressFromLatLng){
        myCurrentLocation.postValue(newLocation);
        if(userRepository.amISignedIn()) {
            userRepository.sendRequest(RequestConverter.RequestTypes.UPDLOC, newLocation, addressFromLatLng);
        }
    }

//-------------------------------User preferences methods-------------------------------

    public void setWillingToShareLocation(boolean willingToShareLocation){
        this.willingToShareLocation.postValue(willingToShareLocation);
    }

    public LiveData<Boolean> getWillingToShareLocation(){
        return willingToShareLocation;
    }

    public LiveData<Integer> getDistanceLimitPreference() {
        return distanceLimitPreference;
    }

    public void setDistanceLimitPreference(int limitPreference){
        distanceLimitPreference.postValue(limitPreference);
    }

//-------------------------------Maps fragment utilities-------------------------------

    public MutableLiveData<CameraPosition> getCameraPosition() {
        return cameraPosition;
    }

    public void setCameraPosition(CameraPosition cameraPosition){
        this.cameraPosition.postValue(cameraPosition);
    }

    public LiveData<LatLng> getMyCurrentLocation() {
        return myCurrentLocation;
    }

    public LiveData<Boolean> getIsUserListChanged(){
        return isUsersListChanged;
    }

    public void setIsUserListChanged(Boolean isListChanged){
        isUsersListChanged.postValue(isListChanged);
    }

//-------------------------------User login methods-------------------------------

    public LiveData<Boolean> getIsUsernameAlreadyUsed() {
        return isUsernameAlreadyUsed;
    }

    public boolean amISignedIn() {
        return userRepository.amISignedIn();
    }

    public void setSignedIn() {
        userRepository.setSignedIn(true);
    }

    public void clearPreviousLoginData(){
        userRepository.setAllUsers(null);
        userRepository.setIsUsernameAlreadyUsed(null);
        userRepository.setSignedIn(false);
    }

//-------------------------------Connection and communication methods-------------------------------

    public void connect(String ip, int port){
        userRepository.connect(ip,port);
    }

    public void resetConnectionStatus(){
        userRepository.setConnectionStatus(UserRepository.ConnectionStatus.IDLE);
    }

    public void sendStillConnected(){
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(connectionStatus.getValue() == UserRepository.ConnectionStatus.CONNECTED)
                    userRepository.sendRequest(RequestConverter.RequestTypes.CONUSR);
                else{
                    timer.cancel();
                    timer.purge();
                }

            }
        };
        timer.schedule(task,3000,5000);
    }

    public LiveData<UserRepository.ConnectionStatus> getConnectionStatus() {

        return connectionStatus;
    }

    public void  releaseResources(){
        userRepository.disconnect();
    }

    @SafeVarargs
    public final <T> void sendRequest(RequestConverter.RequestTypes type, T... parameter) {
        userRepository.sendRequest(type, parameter);
    }

}

