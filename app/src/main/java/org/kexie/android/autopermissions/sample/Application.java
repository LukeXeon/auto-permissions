package org.kexie.android.autopermissions.sample;

import android.util.Log;
import org.kexie.android.autopermissions.AutoPermissions;
import org.kexie.android.autopermissions.OnRequestPermissionsCallback;

import java.util.List;

public class Application extends android.app.Application implements OnRequestPermissionsCallback {

    private static final String TAG = "Application";

    @Override
    public void onCreate() {
        super.onCreate();
        AutoPermissions.addCallback(this);
    }

    @Override
    public void onRequestPermissionsResult(List<String> permissions) {
        Log.d(TAG, "onRequestPermissionsResult: " + permissions);
    }
}
