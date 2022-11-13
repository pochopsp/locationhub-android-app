package it.unina.locationhub.fragments.login;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import it.unina.locationhub.R;

public class LoginErrorDialog extends AppCompatDialogFragment {

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),R.style.CustomAlertDialog);
        builder.setTitle(R.string.username_error_title).setMessage(R.string.login_dialog_error).setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                Button b = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                b.setTextColor(getResources().getColor(R.color.light_blue_600));
            }
        });
        return dialog;
    }
}
