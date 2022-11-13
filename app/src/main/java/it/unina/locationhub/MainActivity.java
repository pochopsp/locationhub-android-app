package it.unina.locationhub;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import it.unina.locationhub.network.RequestConverter;

import static android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;

public class MainActivity extends AppCompatActivity {

    //Gui navigation fields
    private BottomNavigationView bottomNavigationView;
    private NavController navController;

    private MainViewModel mainViewModel;

    //Used to notify the user to turn on location
    private MutableLiveData<Boolean> isLocationEnabled;

    //Used to request location and get its updates
    private LocationRequest locationRequest;
    private LocationManager locationManager;
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isLocationNotEnabledDialogShowing = false;

    //Used to notify user that connection or server error has occurred inside the login fragment
    private Toast connectionErrorToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupGUI();
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupConnectionStatusObserver();

        checkAndRequestFineLocationPermission();

        //Setup location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        buildLocationRequest();
        fusedLocationClient.requestLocationUpdates(locationRequest, buildLocationCallBack(), null);

        setupLocationEnabledObserver();

        //Task running in background to check if the location is enabled or not
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        new CheckLocationStatusTask().execute();

        setupWillingToShareLocationObserver();

    }

//------------------------------  Overrides of activity methods --------------------------------

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        isLocationNotEnabledDialogShowing = false;
        if (requestCode == LocationRequest.PRIORITY_HIGH_ACCURACY) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    //GPS Enabled by user
                    isLocationEnabled.postValue(true);
                    break;
                case Activity.RESULT_CANCELED:
                    //User rejected GPS request
                    isLocationEnabled.postValue(false);
                    break;
                default:
                    break;
            }
        }
    }

    private long backPressedTime;
    private Toast backToast;

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel();
            super.onBackPressed();
            return;
        } else {
            LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast_layout, findViewById(R.id.custom_toast_container));
            backToast = new Toast(getApplicationContext());
            backToast.setGravity(Gravity.BOTTOM, 0, 190);
            backToast.setDuration(Toast.LENGTH_SHORT);
            backToast.setView(layout);
            backToast.show();
        }
        backPressedTime = System.currentTimeMillis();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //If we close the app in login fragment connection status will be "IDLE" and we won't need to release resources
        if(mainViewModel.getConnectionStatus().getValue() != UserRepository.ConnectionStatus.IDLE)
            mainViewModel.releaseResources();
    }

//------------------------------  GUI methods --------------------------------

    private void setupGUI() {
        Objects.requireNonNull(getSupportActionBar()).hide();

        setupConnectionErrorToast();

        bottomNavigationView = findViewById(R.id.bttm_nav);
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            navController.popBackStack();
            navController.navigate(item.getItemId());
            return true;
        });

        int nightModeFlags = getApplicationContext().getResources().getConfiguration().uiMode &
                                Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getWindow().getInsetsController().setSystemBarsAppearance(APPEARANCE_LIGHT_STATUS_BARS, APPEARANCE_LIGHT_STATUS_BARS);
            } else {
                View decor = getWindow().getDecorView();
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        }
    }

    public void showBottomNavigation() {
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    public void hideBottomNavigation() {
        bottomNavigationView.setVisibility(View.GONE);
    }

//-------------------------------Observer Methods-------------------------------

    private void setupWillingToShareLocationObserver() {
        mainViewModel.getWillingToShareLocation().observe(this, willingToShareLocation ->{
            if(mainViewModel.amISignedIn()){
                mainViewModel.sendRequest(RequestConverter.RequestTypes.UPDUSR, willingToShareLocation);
            }
        } );
    }

    private void setupLocationEnabledObserver() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

        isLocationEnabled = new MutableLiveData<>();
        isLocationEnabled.observe(this, isLocationEnabled -> {

            if (!isLocationEnabled && !isLocationNotEnabledDialogShowing) {
                Task<LocationSettingsResponse> taskTurnOnLocationDialog = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
                isLocationNotEnabledDialogShowing = true;
                taskTurnOnLocationDialog.addOnCompleteListener(this::checkLocationSettingsWithErrorSolving);
            }
        });
    }

    private void setupConnectionStatusObserver() {
        mainViewModel.getConnectionStatus().observe(this, connectionStatus -> {
            if (connectionStatus == UserRepository.ConnectionStatus.DISCONNECTED && mainViewModel.amISignedIn()) {
                //In case of error we must clear all users live data as we use it to check if we are logged in or not
                // and set the username already used flag to false
                mainViewModel.clearPreviousLoginData();
                navController.popBackStack();
                navController.navigate(R.id.nav_server_or_network_error);
                mainViewModel.resetConnectionStatus();
            }
            else if(connectionStatus == UserRepository.ConnectionStatus.DISCONNECTED){
                //We are here either due to
                // 1) connection or server error
                // 2) Inserted username is already taken by another user
                boolean usernameAlreadyUsed = (mainViewModel.getIsUsernameAlreadyUsed().getValue()!=null);
                if(!usernameAlreadyUsed) {
                    // 1) has occurred
                    connectionErrorToast.show();
                }
                mainViewModel.clearPreviousLoginData();
                mainViewModel.resetConnectionStatus();
            }
        });
    }

    //------------------------------- Location Methods --------------------------------

    private void checkAndRequestFineLocationPermission() {
        int accessFineLocationPermission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (accessFineLocationPermission != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String @NotNull [] permissions, int @NotNull [] grantResults) {
        if (requestCode == 1 && permissions.length > 0 && grantResults.length > 0) {
            String permission = permissions[0];
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                navController.popBackStack();
                boolean askAgain = shouldShowRequestPermissionRationale(permission);
                Bundle arg = new Bundle();
                if (!askAgain) {
                    arg.putBoolean("checked", true);
                    navController.navigate(R.id.nav_location_error, arg);
                } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
                    arg.putBoolean("checked", false);
                    navController.navigate(R.id.nav_location_error, arg);
                    // user denied but did NOT check "never ask again"
                }
            } else {
                //user granted the permission, so we can request the location
                fusedLocationClient.requestLocationUpdates(locationRequest, buildLocationCallBack(), null);
            }
        }
    }

    private void checkLocationSettingsWithErrorSolving(Task<LocationSettingsResponse> task) {
        try {
            task.getResult(ApiException.class);
            // All location settings are satisfied. The client can initialize location requests here.
        } catch (ApiException exception) {
            switch (exception.getStatusCode()) {
                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                    // Location settings are not satisfied. But could be fixed by showing the user a dialog.
                    try {
                        // Cast to a resolvable exception.
                        ResolvableApiException resolvable = (ResolvableApiException) exception;
                        // Show the dialog by calling startResolutionForResult() and check the result in onActivityResult().
                        isLocationNotEnabledDialogShowing = true;
                        resolvable.startResolutionForResult(MainActivity.this, LocationRequest.PRIORITY_HIGH_ACCURACY);
                    } catch (IntentSender.SendIntentException e) {
                        // Ignore the error.
                    } catch (ClassCastException e) {
                        // Ignore, should be an impossible error.
                    }
                    break;
                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                    //Happens when Airplane mode is turned on, Wi-fi is disabled and Location is turned on
                    break;
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class CheckLocationStatusTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            while (true) {
                isLocationEnabled.postValue(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
                SystemClock.sleep(1000);
            }
        }

    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(3000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(50);
    }

    //Build the location callback object and obtain the location results
    private LocationCallback buildLocationCallBack() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(@NotNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
                mainViewModel.updateAndSendCurrentLocation(newLocation, getAddressFromLatLng(newLocation));
            }
        };
    }

    public String getAddressFromLatLng(LatLng currentLocation) {
        String userAddress = null;
        try {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(currentLocation.latitude, currentLocation.longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                userAddress = addresses.get(0).getAddressLine(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (userAddress != null) {
            if (userAddress.length() > 90)
                return userAddress.substring(0, 88) + "...";
            else
                return userAddress;
        } else {
            userAddress = getString(R.string.no_address_retrieved);
        }
        return userAddress;
    }

//-------------------------------Setup error toasts methods-------------------------------

    public Toast setupClickedUserWithoutLocationToast() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout, findViewById(R.id.custom_toast_container));
        TextView tv = layout.findViewById(R.id.textview_toast);
        tv.setText(R.string.clicked_on_user_not_willing_to_share);
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, 190);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        return toast;
    }

    public void setupConnectionErrorToast() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast_layout, findViewById(R.id.custom_toast_container));
        TextView tv = layout.findViewById(R.id.textview_toast);
        tv.setText(getApplicationContext().getString(R.string.error_connection_toast));
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        connectionErrorToast = new Toast(getApplicationContext());
        connectionErrorToast.setGravity(Gravity.BOTTOM, 0, 190);
        connectionErrorToast.setDuration(Toast.LENGTH_LONG);
        connectionErrorToast.setView(layout);
    }
}