package org.kexie.android.autopermissions;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.ArraySet;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings({"WeakerAccess","deprecation"})
public final class AutoPermissions {

    //AppOpsManager.OP_SYSTEM_ALERT_WINDOW = 24
    private final static int OP_SYSTEM_ALERT_WINDOW = 24;

    private static final class LifecycleEventTrigger
            extends EmptyActivityLifecycleCallbacks {
        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            requestAll(activity);
            activity.getApplication()
                    .unregisterActivityLifecycleCallbacks(this);
        }
    }

    private static final AtomicBoolean sInit = new AtomicBoolean(false);

    static final CopyOnWriteArrayList<OnRequestPermissionsCallback> sCallbacks = new CopyOnWriteArrayList<>();

    private AutoPermissions() {
        throw new AssertionError("Can't initialize");
    }

    public static void requestAll(Activity activity) {
        selfCheck();
        List<String> permissions = getDeniedPermissions(activity);
        if (!permissions.isEmpty()) {
            RequestFragment requestFragment = RequestFragment.newInstance(permissions);
            activity.getFragmentManager()
                    .beginTransaction()
                    .add(requestFragment, RequestFragment.class.getCanonicalName())
                    .commitAllowingStateLoss();
        }
    }

    static void init(Context context) {
        if (sInit.compareAndSet(false, true)) {
            ((Application) context.getApplicationContext())
                    .registerActivityLifecycleCallbacks(new LifecycleEventTrigger());
        }
    }

    public static void addCallback(OnRequestPermissionsCallback callback) {
        selfCheck();
        if (callback != null) {
            sCallbacks.add(callback);
        }
    }

    public static void removeCallback(OnRequestPermissionsCallback callback) {
        selfCheck();
        if (callback != null) {
            sCallbacks.remove(callback);
        }
    }

    private static void selfCheck() {
        if (!sInit.get()) {
            throw new IllegalStateException("Without initialization, " +
                    "you need to use this method after Application#attachBaseContext");
        }
    }

    @NonNull
    public static List<String> getDefinedPermissions(@NonNull Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(),
                            PackageManager.GET_PERMISSIONS);
            if (packageInfo.requestedPermissions != null) {
                return Collections.unmodifiableList(Arrays.asList(packageInfo.requestedPermissions));
            } else {
                return Collections.emptyList();
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static List<String> getDeniedPermissions(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return Collections.emptyList();
        }
        List<String> requestedPermissions = getDefinedPermissions(context);
        Set<String> requestedPermissionsList = new ArraySet<>();
        if (requestedPermissions.size() != 0) {
            for (String permission : requestedPermissions) {
                if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
                    if (!hasWindowPermission(context)) {
                        requestedPermissionsList.add(Manifest.permission.SYSTEM_ALERT_WINDOW);
                    }
                    continue;
                }
                if (PackageManager.PERMISSION_GRANTED != context
                        .checkSelfPermission(permission)) {
                    requestedPermissionsList.add(permission);
                    // 进入到这里代表没有权限.
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(requestedPermissionsList));
    }

    public static boolean hasWindowPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            return hasWindowPermissionInO(context);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else {
            return hasWindowPermissionBelowM(context);
        }
    }

    /**
     * 6.0以下判断是否有权限
     * 理论上6.0以上才需处理权限，但有的国内rom在6.0以下就添加了权限
     * 其实此方式也可以用于判断6.0以上版本，只不过有更简单的canDrawOverlays代替
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    @SuppressWarnings("JavaReflectionMemberAccess")
    private static boolean hasWindowPermissionBelowM(Context context) {
        try {
            AppOpsManager manager = (AppOpsManager) context
                    .getSystemService(Context.APP_OPS_SERVICE);
            Method dispatchMethod = AppOpsManager.class
                    .getMethod("checkOp",
                            Integer.TYPE,
                            Integer.TYPE,
                            String.class);
            return AppOpsManager.MODE_ALLOWED == (int) dispatchMethod.invoke(
                    manager,
                    OP_SYSTEM_ALERT_WINDOW,
                    Binder.getCallingUid(),
                    context.getApplicationContext().getPackageName());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 用于判断8.0时是否有权限，仅用于OnActivityResult
     * 针对8.0官方bug:在用户授予权限后Settings.canDrawOverlays或checkOp方法判断仍然返回false
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static boolean hasWindowPermissionInO(Context context) {
        try {
            WindowManager mgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (mgr == null) return false;
            View viewToAdd = new View(context);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(0, 0,
                    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT);
            viewToAdd.setLayoutParams(params);
            mgr.addView(viewToAdd, params);
            mgr.removeView(viewToAdd);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}