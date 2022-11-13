package it.unina.locationhub.fragments.login;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

import it.unina.locationhub.MainActivity;
import it.unina.locationhub.MainViewModel;
import it.unina.locationhub.R;
import it.unina.locationhub.UserRepository;
import it.unina.locationhub.model.User;
import it.unina.locationhub.network.RequestConverter;

public class LoginFragment extends Fragment {

    private MainViewModel mainViewModel;

    AlertDialog connectingToServerDialog;

    TextInputEditText usernameTextInput;
    TextInputEditText ipTextInput;
    TextInputEditText portTextInput;

    TextView usernameErrorTextView;
    TextView ipOrPortErrorTextView;
    TextView usernameAlreadyUsedErrorTextView;

    public LoginFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_login, container, false);

        setupGui(view);

        mainViewModel.getAllUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                connectingToServerDialog.dismiss();
                NavController navController = Navigation.findNavController(requireView());
                navController.popBackStack();
                navController.navigate(R.id.nav_home);
            }
        });

        mainViewModel.getConnectionStatus().observe(getViewLifecycleOwner(),connectionStatus -> {
            if(connectionStatus == UserRepository.ConnectionStatus.DISCONNECTED)
                connectingToServerDialog.dismiss();
        });


        mainViewModel.getIsUsernameAlreadyUsed().observe(getViewLifecycleOwner(), isUsernameAlreadyUsed -> {
            if (isUsernameAlreadyUsed != null) {
                if (isUsernameAlreadyUsed) {
                    mainViewModel.releaseResources();
                    usernameAlreadyUsedErrorTextView.setVisibility(View.VISIBLE);
                    connectingToServerDialog.dismiss();
                } else {
                    mainViewModel.sendRequest(RequestConverter.RequestTypes.GETUSR);
                    mainViewModel.setSignedIn();
                }
            }
        });

        Button continueButton = view.findViewById(R.id.continue_btn);
        continueButton.setOnClickListener(v -> {
            if (usernameErrorTextView.getVisibility() == View.VISIBLE || ipOrPortErrorTextView.getVisibility() == View.VISIBLE) {
                assert getFragmentManager() != null;
                new LoginErrorDialog().show(getFragmentManager(), "Error username dialog");
            } else {
                //check if the username is already in use
                boolean ipValid= isIpAddressValid(ipTextInput.getText().toString());
                boolean portValid= isPortNumberValid(portTextInput.getText().toString());
                boolean usernameValid=isUsernameValid(usernameTextInput.getText().toString());
                if (ipValid && portValid && usernameValid) {
                    String ip = ipTextInput.getText().toString();
                    int port = Integer.parseInt(portTextInput.getText().toString());
                    mainViewModel.connect(ip,port);
                    connectingToServerDialog.show();
                    SwitchMaterial sw = view.findViewById(R.id.share_position_agreement_switch);
                    addUserThread(sw.isChecked(), usernameTextInput);
                } else if(!usernameValid && (!ipValid || !portValid) ){
                    usernameErrorTextView.setVisibility(View.VISIBLE);
                    ipOrPortErrorTextView.setVisibility(View.VISIBLE);
                }else if(!usernameValid)
                    usernameErrorTextView.setVisibility(View.VISIBLE);
                else
                    ipOrPortErrorTextView.setVisibility(View.VISIBLE);
            }
        });

        return view;
    }

    private void setupGui(View view) {
        setupConnectingToServerDialog();

        ((MainActivity) requireActivity()).hideBottomNavigation();

        setupUsernameTextInputEditText(view);
        setupIpAndPortTextInputEditText(view);

        usernameAlreadyUsedErrorTextView = view.findViewById(R.id.error_username_already_used_text_view);
        usernameAlreadyUsedErrorTextView.setVisibility(View.INVISIBLE);
    }

    private void setupIpAndPortTextInputEditText(View view) {
        portTextInput = view.findViewById(R.id.port_text_edit);
        ipOrPortErrorTextView = view.findViewById(R.id.error_ip_and_port);
        ipOrPortErrorTextView.setVisibility(View.INVISIBLE);
        ipTextInput = view.findViewById(R.id.ip_text_edit);
        ipTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isIpAddressValid(s.toString()))
                    ipOrPortErrorTextView.setVisibility(View.VISIBLE);
                else if (isPortNumberValid(portTextInput.getText().toString()))
                    ipOrPortErrorTextView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isIpAddressValid(s.toString()))
                    ipOrPortErrorTextView.setVisibility(View.VISIBLE);
                else if (isPortNumberValid(portTextInput.getText().toString()))
                    ipOrPortErrorTextView.setVisibility(View.INVISIBLE);
            }
        });

        portTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isPortNumberValid(s.toString()))
                    ipOrPortErrorTextView.setVisibility(View.VISIBLE);
                else if(isIpAddressValid(ipTextInput.getText().toString()))
                    ipOrPortErrorTextView.setVisibility(View.INVISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isPortNumberValid(s.toString()))
                    ipOrPortErrorTextView.setVisibility(View.VISIBLE);
                else if(isIpAddressValid(ipTextInput.getText().toString()))
                    ipOrPortErrorTextView.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void setupUsernameTextInputEditText(View view) {
        usernameErrorTextView = view.findViewById(R.id.error_text_view);
        usernameErrorTextView.setVisibility(View.INVISIBLE);

        usernameTextInput = view.findViewById(R.id.username_text_edit);
        usernameTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                usernameAlreadyUsedErrorTextView.setVisibility(View.INVISIBLE);
                if (!isUsernameValid(s.toString()))
                    usernameErrorTextView.setVisibility(View.VISIBLE);
                else
                    usernameErrorTextView.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void addUserThread(boolean willingToShareLocation, TextInputEditText usernameTextInput) {
        new Thread(() -> {
            while (mainViewModel.getConnectionStatus().getValue() == UserRepository.ConnectionStatus.IDLE) {
            }
            if (mainViewModel.getConnectionStatus().getValue() == UserRepository.ConnectionStatus.CONNECTED) {
                mainViewModel.sendStillConnected();
                mainViewModel.setWillingToShareLocation(willingToShareLocation);
                while (mainViewModel.getMyCurrentLocation().getValue() == null) {
                }
                User userClient = new User(usernameTextInput.getText().toString(), 0, ((MainActivity) requireActivity()).getAddressFromLatLng(mainViewModel.getMyCurrentLocation().getValue()),
                        mainViewModel.getMyCurrentLocation().getValue(), willingToShareLocation);
                mainViewModel.sendRequest(RequestConverter.RequestTypes.ADDUSR, userClient);
            }
        }).start();
    }

    public void setupConnectingToServerDialog() {

        int llPadding = 30;
        LinearLayout ll = new LinearLayout(getContext());
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(llPadding, llPadding, llPadding, llPadding);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        ProgressBar progressBar = new ProgressBar(getContext());
        progressBar.setIndeterminate(true);
        progressBar.setPadding(0, 0, llPadding, 0);
        progressBar.setLayoutParams(llParam);

        llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        TextView tvText = new TextView(getContext());
        tvText.setText(requireContext().getString(R.string.loading_dialog));
        tvText.setTextColor(Color.parseColor("#000000"));
        tvText.setTextSize(20);
        tvText.setLayoutParams(llParam);

        ll.addView(progressBar);
        ll.addView(tvText);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.CustomAlertDialog);
        builder.setCancelable(true);
        builder.setView(ll);

        connectingToServerDialog = builder.create();
        Window window = connectingToServerDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(connectingToServerDialog.getWindow().getAttributes());
            layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            connectingToServerDialog.getWindow().setAttributes(layoutParams);
        }
    }

    private boolean isUsernameValid(String username) {

        final Pattern VALID_USERNAME_REGEX =
                Pattern.compile("^(?=.{4,10}$)(?![_.])(?!.*[_.]{2})[a-zA-Z0-9._]+(?<![_.])$");

        return VALID_USERNAME_REGEX.matcher(username).matches();

    }

    private boolean isPortNumberValid(String port){
        try {
            return !(port.length() == 0 || Integer.parseInt(port) < 1024 || Integer.parseInt(port) > 65535);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isIpAddressValid(String ip) {

        if(ip.length() < 7 || ip.length() > 16)
            return false;
        final Pattern IP_ADDRESS = Pattern.compile(
                "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                        + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                        + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                        + "|[1-9][0-9]|[0-9]))");
        return IP_ADDRESS.matcher(ip).matches();

    }


}