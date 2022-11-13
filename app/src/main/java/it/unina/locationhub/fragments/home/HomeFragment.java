package it.unina.locationhub.fragments.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import it.unina.locationhub.MainActivity;
import it.unina.locationhub.MainViewModel;
import it.unina.locationhub.R;
import it.unina.locationhub.model.User;
import it.unina.locationhub.network.RequestConverter;

public class HomeFragment extends Fragment {

    private MainViewModel mainViewModel;

    //Used to display connected users list
    private ConnectedUserViewAdapter listAdapter;
    private RecyclerView recyclerView;

    //Used to check if users list is changed after an update
    private List<User> previousUsersList;

    //Image, button and textViews used when there are no connected users
    private ImageView noUserConnectedImage;
    private TextView noUserConnectedTextView;
    private TextView noUserConnectedWithinDistanceTextView;
    private Button noUserConnectedButton ;

    public HomeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ((MainActivity)requireActivity()).showBottomNavigation();
        recyclerView = view.findViewById(R.id.recycler_users);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_view);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            mainViewModel.sendRequest(RequestConverter.RequestTypes.GETUSR);
            swipeRefreshLayout.setRefreshing(false);
        });

        setupNoConnectedUsersGui(view, swipeRefreshLayout);

        //Used to check if some users or some location is changed
        previousUsersList = mainViewModel.getAllUsers().getValue();

        mainViewModel.getAllUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                if(users.size() == 0){
                    showNoConnectedUsersGui();
                    mainViewModel.setIsUserListChanged(isUserListChanged(users));
                }else {
                    List<User> usersWithinDistanceLimit = removeUsersBeyondDistanceLimit(users);
                    if(usersWithinDistanceLimit.size() == 0)
                        showNoConnectedUserWithinDistanceGui();
                    else
                        showConnectedUsersGui(usersWithinDistanceLimit);

                    mainViewModel.setIsUserListChanged(isUserListChanged(usersWithinDistanceLimit));
                }
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerView.setAdapter(listAdapter);
            }
        });

        return view;

    }

    @SuppressLint("SetTextI18n")
    private void showNoConnectedUserWithinDistanceGui() {
        noUserConnectedWithinDistanceTextView.setText(requireContext().getString(R.string.no_user_connected_within_distance_label)
                + " " + mainViewModel.getDistanceLimitPreference().getValue() + " Km");
        noUserConnectedWithinDistanceTextView.setVisibility(View.VISIBLE);
        noUserConnectedButton.setVisibility(View.VISIBLE);
        noUserConnectedImage.setVisibility(View.VISIBLE);
        noUserConnectedTextView.setVisibility(View.INVISIBLE);
    }

    private void setupNoConnectedUsersGui(View view, SwipeRefreshLayout swipeRefreshLayout) {
        //Image, button and textViews used when there are no connected users
        noUserConnectedImage = view.findViewById(R.id.no_users_connected_image);
        noUserConnectedTextView = view.findViewById(R.id.no_user_connected_label);
        noUserConnectedWithinDistanceTextView = view.findViewById(R.id.no_user_connected_within_distance_label);
        noUserConnectedButton = view.findViewById(R.id.no_user_connected_button);
        noUserConnectedWithinDistanceTextView.setVisibility(View.INVISIBLE);
        noUserConnectedTextView.setVisibility(View.INVISIBLE);
        noUserConnectedButton.setVisibility(View.INVISIBLE);
        noUserConnectedImage.setVisibility(View.INVISIBLE);
        noUserConnectedButton.setOnClickListener(v -> {
            noUserConnectedWithinDistanceTextView.setVisibility(View.INVISIBLE);
            noUserConnectedTextView.setVisibility(View.INVISIBLE);
            noUserConnectedButton.setVisibility(View.INVISIBLE);
            noUserConnectedImage.setVisibility(View.INVISIBLE);
            swipeRefreshLayout.setRefreshing(true);
            mainViewModel.sendRequest(RequestConverter.RequestTypes.GETUSR);
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void showNoConnectedUsersGui() {
        noUserConnectedTextView.setVisibility(View.VISIBLE);
        noUserConnectedButton.setVisibility(View.VISIBLE);
        noUserConnectedImage.setVisibility(View.VISIBLE);
        noUserConnectedWithinDistanceTextView.setVisibility(View.INVISIBLE);
        listAdapter = new ConnectedUserViewAdapter(null, getContext(), getActivity());
    }

    private void showConnectedUsersGui(List<User> users) {
        noUserConnectedTextView.setVisibility(View.INVISIBLE);
        noUserConnectedButton.setVisibility(View.INVISIBLE);
        noUserConnectedImage.setVisibility(View.INVISIBLE);
        noUserConnectedWithinDistanceTextView.setVisibility(View.INVISIBLE);
        listAdapter = new ConnectedUserViewAdapter(users, getContext(), requireActivity());
    }

    @SuppressLint("SetTextI18n")
    private List<User> removeUsersBeyondDistanceLimit(List<User> users) {
        int currentLimitDistancePreference = mainViewModel.getDistanceLimitPreference().getValue();
        List<User> allUsers = new ArrayList<>();
        int unlimited = Integer.MAX_VALUE;
        if (currentLimitDistancePreference != unlimited) {
            for (User u : users)
                if (u.getDistanceFromMyLocation() <= currentLimitDistancePreference)
                    allUsers.add(u);
        } else
            allUsers = users;
        return allUsers;
    }

    private boolean isUserListChanged(List<User> currentUsersList){
        if (previousUsersList.size() != currentUsersList.size())
            return true;
        else {
            for (int i = 0; i < currentUsersList.size(); i++) {
                if (!currentUsersList.get(i).equals(previousUsersList.get(i))) {
                    return true;
                }
            }
            return false;
        }
    }


}