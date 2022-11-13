package it.unina.locationhub.fragments.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import it.unina.locationhub.MainActivity;
import it.unina.locationhub.R;
import it.unina.locationhub.model.User;

public class ConnectedUserViewAdapter extends RecyclerView.Adapter<ConnectedUserViewAdapter.ConnectedUserHolder> {

    private final List<User> usersList;

    private final Context mContext;
    private final FragmentActivity activity;

    private final Toast clickedUserWithoutLocationToast;

    public ConnectedUserViewAdapter(List<User> userArrayList, Context context, FragmentActivity activity) {
        this.usersList = userArrayList;
        this.mContext = context;
        this.activity = activity;
        clickedUserWithoutLocationToast = ((MainActivity)activity).setupClickedUserWithoutLocationToast();
    }

    // This method creates views for the RecyclerView by inflating the layout
    // Into the viewHolders which helps to display the items in the RecyclerView
    @Override
    public @NotNull ConnectedUserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        View view = layoutInflater.inflate(R.layout.connected_user_viewholder, parent, false);
        return new ConnectedUserHolder(view);
    }

    @Override
    public int getItemCount() {
        return usersList == null? 0: usersList.size();
    }

    // This method is called when binding the data to the views being created in RecyclerView
    @Override
    public void onBindViewHolder(@NonNull ConnectedUserHolder holder, final int position) {
        final User user = usersList.get(position);

        // Set the data to the views here
        holder.setUsername(user.getUsername());
        holder.setDistance(user.getDistanceFromMyLocation());
        holder.setLocation(user.getAddress());

        // You can set click listners to indvidual items in the viewholder here
        holder.itemView.setOnClickListener(v -> {
            //Display a dialog if user is not willing to share position
            if(user.isWillingToShareLocation()) {
                BottomNavigationView mBottomNavigationView = activity.findViewById(R.id.bttm_nav);
                mBottomNavigationView.setSelectedItemId(R.id.nav_map);
                Navigation.findNavController(v).popBackStack();
                Bundle arg = new Bundle();
                arg.putParcelable("clickedUserLocation", user.getLocationCoordinates());
                Navigation.findNavController(v).navigate(R.id.nav_map,arg);
            }
            else{
                clickedUserWithoutLocationToast.show();
            }
        });
    }

    // This is your ViewHolder class that helps to populate data to the view
    public class ConnectedUserHolder extends RecyclerView.ViewHolder {

        private final TextView txtUsername;
        private final TextView txtDistance;
        private final TextView txtLocation;

        @SuppressLint("UseCompatLoadingForDrawables")
        public ConnectedUserHolder(View itemView) {
            super(itemView);

            txtUsername = itemView.findViewById(R.id.username_textview);
            txtDistance = itemView.findViewById(R.id.distance_textview);
            txtLocation = itemView.findViewById(R.id.location_textview);

            ImageView userIcon = itemView.findViewById(R.id.user_icon);
            int nightModeFlags = mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

            if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO) {
                userIcon.setImageDrawable(mContext.getDrawable(R.drawable.user));
            }
        }

        public void setUsername(String name) {
            txtUsername.setText(name);
        }

        @SuppressLint("SetTextI18n")
        public void setDistance(int number) {
            txtDistance.setText(mContext.getString(R.string.viewholder_distance) + " " + number + " Km");
        }

        @SuppressLint("SetTextI18n")
        public void setLocation(String address) {
            if(address.equals(""))
                txtLocation.setText(mContext.getString(R.string.viewholder_no_location));
            else if(address.length() <= 70)
                txtLocation.setText(address);
            else
                txtDistance.setText(address.substring(0,68) + "...");
        }
    }
}
