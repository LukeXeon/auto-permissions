package org.kexie.android.autopermissions;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
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
import java.util.Objects;


@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressWarnings("deprecation")
public final class AutoPermission
        extends Fragment {

    private static final int REQUEST_WINDOWS_PERMISSION = 10001;
    private static final int REQUEST_NORMAL_PERMISSIONS = 10002;

    private static final class LifecycleTrigger
            extends EmptyActivityLifecycleCallbacks {

        private final List<String> permission;

        private LifecycleTrigger(List<String> permission) {
            this.permission = permission;
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(AutoPermission.class.getCanonicalName(),
                    new ArrayList<>(permission));
            AutoPermission autoPermission = new AutoPermission();
            autoPermission.setArguments(bundle);
            activity.getFragmentManager()
                    .beginTransaction()
                    .add(autoPermission, AutoPermission.class.getCanonicalName())
                    .commit();
            Application application = activity.getApplication();
            application.registerActivityLifecycleCallbacks(this);
        }
    }

    static void init(Context context) {
        List<String> requestedPermissions = Permissions.getDeniedPermissions(context);
        if (requestedPermissions.size() != 0) {
            ((Application) context.getApplicationContext())
                    .registerActivityLifecycleCallbacks(new LifecycleTrigger(requestedPermissions));
        }
    }

    private List<String> requests;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requests = Objects.requireNonNull(getArguments()
                .getStringArrayList(AutoPermission.class.getCanonicalName()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(requests.toArray(new String[0]), REQUEST_NORMAL_PERMISSIONS);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WINDOWS_PERMISSION) {
            if (Permissions.hasWindowPermission(getContext())) {
                requests.remove(Manifest.permission.SYSTEM_ALERT_WINDOW);
            }
            finish();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private void finish() {
        Context context = getContext().getApplicationContext();
        if (context instanceof OnRequestPermissionsCallback) {
            ((OnRequestPermissionsCallback) context).onResult(requests.isEmpty()
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(requests));
        }
        getFragmentManager()
                .beginTransaction()
                .remove(this)
                .commit();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
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
}
