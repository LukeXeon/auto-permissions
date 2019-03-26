package org.kexie.android.autopermissions;

import java.util.List;

public interface OnRequestPermissionsCallback {
    void onResult(List<String> deniedPermissions);
}
