package org.kexie.android.autopermissions;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings("deprecation")
public final class RequestFragment
        extends Fragment {

    private static final int REQUEST_WINDOWS_PERMISSION = 10001;
    private static final int REQUEST_NORMAL_PERMISSIONS = 10002;

    private List<String> requests;

    public static RequestFragment newInstance(List<String> permissions) {
        Bundle args = new Bundle();
        args.putStringArrayList(RequestFragment.class.getCanonicalName(),
                new ArrayList<>(permissions));
        RequestFragment fragment = new RequestFragment();
        fragment.setArguments(args);
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requests = getArguments()
                .getStringArrayList(RequestFragment.class.getCanonicalName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && requests != null) {
            requestPermissions(requests.toArray(new String[0]), REQUEST_NORMAL_PERMISSIONS);
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WINDOWS_PERMISSION) {
            if (AutoPermissions.hasWindowPermission(getContext())) {
                requests.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
            }
            finish();
        }
    }

    private void finish() {
        getFragmentManager()
                .beginTransaction()
                .remove(this)
                .commitAllowingStateLoss();
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );
        if (requestCode == REQUEST_NORMAL_PERMISSIONS) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    requests.remove(permissions[i]);
                }
            }
            if (requests.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getContext().getPackageName()));
                startActivityForResult(intent, REQUEST_WINDOWS_PERMISSION);
            } else {
                finish();
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @Override
    public void onDestroy() {
        super.onDestroy();
        Context context = getContext().getApplicationContext();
        if (context instanceof OnRequestPermissionsCallback) {
            ((OnRequestPermissionsCallback) context).onRequestPermissionsResult(requests.isEmpty()
                    ? Collections.<String>emptyList()
                    : Collections.unmodifiableList(requests)
            );
        }
    }
}
