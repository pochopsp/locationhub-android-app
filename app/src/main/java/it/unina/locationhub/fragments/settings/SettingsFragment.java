package it.unina.locationhub.fragments.settings;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

import it.unina.locationhub.MainViewModel;
import it.unina.locationhub.R;

public class SettingsFragment extends Fragment {

    private MainViewModel mainViewModel;

    //User distance and willing to share location preferences
    private int currentDistanceLimitValue;
    private boolean isWillingToShareLocationOnCreateView;

    SwitchMaterial willingToShareLocationSwitch;

    public SettingsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        willingToShareLocationSwitch = view.findViewById(R.id.share_position_agreement_switch);
        isWillingToShareLocationOnCreateView = mainViewModel.getWillingToShareLocation().getValue();
        willingToShareLocationSwitch.setChecked(isWillingToShareLocationOnCreateView);

        TextView appInfo = view.findViewById(R.id.info_app_textview);
        appInfo.setOnClickListener(v -> {
                assert getFragmentManager() != null;
                new InformationDialog().show(getFragmentManager(),"Information dialog");
        });

        final TextView sliderValueTextView = view.findViewById(R.id.distance_number_textview);
        Slider distancePreferenceSlider = view.findViewById(R.id.slider_limit_distance);

        if (mainViewModel.getDistanceLimitPreference().getValue() != Integer.MAX_VALUE) {
            currentDistanceLimitValue = mainViewModel.getDistanceLimitPreference().getValue();
            distancePreferenceSlider.setValue(currentDistanceLimitValue);
            sliderValueTextView.setText(currentDistanceLimitValue + " Km");
        }
        distancePreferenceSlider.addOnChangeListener((slider, value, fromUser) -> {
            currentDistanceLimitValue = (int) slider.getValue();

            if (currentDistanceLimitValue == 0) {
                sliderValueTextView.setText(R.string.no_limit_label);
                mainViewModel.setDistanceLimitPreference(Integer.MAX_VALUE);
            }else {
                sliderValueTextView.setText(currentDistanceLimitValue + " Km");
                mainViewModel.setDistanceLimitPreference(currentDistanceLimitValue);
            }
            mainViewModel.setIsUserListChanged(true);
        });
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(isWillingToShareLocationOnCreateView != willingToShareLocationSwitch.isChecked()) {
            mainViewModel.setWillingToShareLocation(!isWillingToShareLocationOnCreateView);
        }
    }
}