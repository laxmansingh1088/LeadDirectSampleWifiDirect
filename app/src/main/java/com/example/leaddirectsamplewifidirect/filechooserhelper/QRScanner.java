package com.example.leaddirectsamplewifidirect.filechooserhelper;

import android.app.Activity;
import android.content.Intent;

import com.blikoon.qrcodescanner.QrCodeActivity;

public class QRScanner {
    public static final int QRSCANNER_CODE = 70;

    public static void scan(Activity activity) {
        Intent intent = new Intent(activity, QrCodeActivity.class);
        try {
            activity.startActivityForResult(intent, QRSCANNER_CODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
