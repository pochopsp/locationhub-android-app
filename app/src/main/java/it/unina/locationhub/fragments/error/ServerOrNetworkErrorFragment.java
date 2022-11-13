package it.unina.locationhub.fragments.error;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import it.unina.locationhub.MainActivity;
import it.unina.locationhub.MainViewModel;
import it.unina.locationhub.R;

public class ServerOrNetworkErrorFragment extends Fragment {

    private MainViewModel mainViewModel;
    private Toast errorToast;
    private long toastShowTime;

    public ServerOrNetworkErrorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_server_or_network_error, container, false);

        ((MainActivity)requireActivity()).hideBottomNavigation();

        Button tryAgainButton = view.findViewById(R.id.continue_btn);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.popBackStack();
                    BottomNavigationView mBottomNavigationView = ((MainActivity)requireActivity()).findViewById(R.id.bttm_nav);
                    mBottomNavigationView.setSelectedItemId(R.id.nav_home);
                    navController.popBackStack();
                    navController.navigate(R.id.nav_login);
                }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}