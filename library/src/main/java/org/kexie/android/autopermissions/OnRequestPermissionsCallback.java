package org.kexie.android.autopermissions;

import java.util.List;

public interface OnRequestPermissionsCallback {
    void onRequestPermissionsResult(List<String> deniedPermissions);
}
