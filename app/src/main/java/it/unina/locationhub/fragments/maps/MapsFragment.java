package it.unina.locationhub.fragments.maps;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.List;

import it.unina.locationhub.MainViewModel;
import it.unina.locationhub.R;
import it.unina.locationhub.model.User;


public class MapsFragment extends Fragment {

    private MainViewModel mainViewModel;

    private Marker myMarker;
    private MarkerOptions myMarkerOptions;
    private List<MarkerOptions> allUsersMarkers;

    private LatLng clickedUserLocation;

    private GoogleMap myGoogleMap;
    private boolean mapReady = false;


    private final OnMapReadyCallback callback = googleMap -> {
        mapReady = true;
        myGoogleMap = googleMap;
        myMarker = myGoogleMap.addMarker(myMarkerOptions);

        for(MarkerOptions m : allUsersMarkers)
            myGoogleMap.addMarker(m).showInfoWindow();

        boolean isUsersListChanged = mainViewModel.getIsUserListChanged().getValue();
        boolean aUserHasBeenClicked = (clickedUserLocation != null);
        boolean havePreviousCameraPosition = (mainViewModel.getCameraPosition().getValue() != null);

        if(isUsersListChanged &&  !aUserHasBeenClicked) {
            fitZoomToMarkers();
        } else if(havePreviousCameraPosition && !aUserHasBeenClicked) {
            myGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(mainViewModel.getCameraPosition().getValue()));
        }else if (aUserHasBeenClicked){
            myGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(clickedUserLocation,15));
        }else
            fitZoomToMarkers();
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        mainViewModel.getMyCurrentLocation().observe(getViewLifecycleOwner(),latLng -> {
            if(mapReady)
                animateMarker(myMarker,latLng,false);
        });

        int distanceLimitPreference = mainViewModel.getDistanceLimitPreference().getValue();
        allUsersMarkers = new ArrayList<>();
        mainViewModel.getAllUsers().observe(getViewLifecycleOwner(),allUsers -> {
            if (allUsers != null) {
                for (User u : allUsers)
                    if (u.isWillingToShareLocation() && u.getDistanceFromMyLocation() <= distanceLimitPreference) {
                        allUsersMarkers.add(new MarkerOptions().position(u.getLocationCoordinates())
                                .icon(BitmapDescriptorFactory.fromBitmap(generateBitmapIcon(u.getUsername()))));
                    }
            }
        });

        setClickedUserMarker();

        setupMyMarkerOptions();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null)
            mapFragment.getMapAsync(callback);

    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(clickedUserLocation != null)
            mainViewModel.setCameraPosition(null);
        else
            mainViewModel.setCameraPosition(myGoogleMap.getCameraPosition());
    }

    //--------------------------------------- Setup markers methods---------------------------------------

    private void setupMyMarkerOptions() {
        LatLng myLocation = mainViewModel.getMyCurrentLocation().getValue();
        if(myLocation==null)
            myLocation = new LatLng(0,0);

        myMarkerOptions = new MarkerOptions().position(myLocation);
        IconGenerator icg = new IconGenerator(getContext());
        icg.setColor(Color.rgb(0,128,255));
        icg.setTextAppearance(R.style.MyLocationTextStyle); // black text
        Bitmap bm = icg.makeIcon(getContext().getString(R.string.my_marker_label));
        myMarkerOptions.icon(BitmapDescriptorFactory.fromBitmap(bm));
    }

    private void setClickedUserMarker() {
        try {
            if(getArguments().getParcelable("clickedUserLocation") != null) {
                clickedUserLocation = (LatLng)getArguments().getParcelable("clickedUserLocation");
            }
        } catch (Exception e) {
            //if no user has been clicked catch the NullPointer
            e.printStackTrace();
        }
    }

    //--------------------------------------- Methods to adjust camera position -------------------------

    public void animateMarker(final Marker marker, final LatLng toPosition, final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = myGoogleMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    marker.setVisible(!hideMarker);
                }
            }
        });
    }

    public void fitZoomToMarkers() {
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (MarkerOptions m : allUsersMarkers) {
            b.include(m.getPosition());
        }
        b.include(myMarkerOptions.getPosition());
        LatLngBounds bounds = b.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 280);
        myGoogleMap.animateCamera(cu);
    }

    public Bitmap generateBitmapIcon(String text) {
        IconGenerator icg = new IconGenerator(getContext());
        icg.setColor(Color.WHITE);
        icg.setTextAppearance(R.style.UsernameTextStyle); // black text
        return icg.makeIcon(text);
    }

}