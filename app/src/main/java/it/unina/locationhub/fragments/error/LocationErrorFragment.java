package it.unina.locationhub.fragments.error;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import it.unina.locationhub.R;
public class LocationErrorFragment extends Fragment {


    public LocationErrorFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_location_error, container, false);

        TextView checked_text_view = view.findViewById(R.id.checked_text_view);
        TextView not_checked_text_View = view.findViewById(R.id.not_checked_text_view);
        checked_text_view.setVisibility(View.INVISIBLE);
        not_checked_text_View.setVisibility(View.INVISIBLE);

        assert getArguments() != null;
        if(getArguments().getBoolean("checked"))
            checked_text_view.setVisibility(View.VISIBLE);
        else
            not_checked_text_View.setVisibility(View.VISIBLE);

        return view;
    }
}