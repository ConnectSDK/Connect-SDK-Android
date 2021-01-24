package com.connectsdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class ConnectedDeviceDialog {
    private AlertDialog pickerDialog;
    private IConnectedDeviceDialogListener deviceDialogListener;
    private Activity activity;

    public AlertDialog getConnectedDeviceDialog(Activity activity, String deviceName) {
        ViewGroup titleContainer = (ViewGroup) activity.getLayoutInflater().inflate(R.layout.header_layout, null);
        this.activity = activity;
        TextView title = titleContainer.findViewById(R.id.headerView);
        title.setText(deviceName);
        pickerDialog = new AlertDialog.Builder(activity)
                .setCustomTitle(titleContainer)
                .setCancelable(false)
                .setPositiveButton("STOP CASTING", positiveButtonListener)
                .setNegativeButton("CANCEL", negativeButtonListener)
                .create();
        pickerDialog.setOnShowListener(onShowListener);
        return pickerDialog;
    }

    private DialogInterface.OnClickListener positiveButtonListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            deviceDialogListener.disconnectClick();
            dialogInterface.dismiss();
        }
    };

    private DialogInterface.OnClickListener negativeButtonListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
        }
    };

    private DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {

        @Override
        public void onShow(DialogInterface dialog) {
            Button nbutton = pickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            if (nbutton != null) {
                nbutton.setTextAppearance(activity, R.style.buttonText);
            }
            Button pbutton = pickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (pbutton != null) {
                pbutton.setTextAppearance(activity, R.style.buttonText);
            }
        }
    };

    public void setDeviceDialogListener(IConnectedDeviceDialogListener deviceDialogListener) {
        this.deviceDialogListener = deviceDialogListener;
    }

    public interface IConnectedDeviceDialogListener {
        public void disconnectClick();
    }
}
