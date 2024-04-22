package io.github.qqlittleice233.magic.util;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.ContentResolver;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;

import org.jetbrains.annotations.Nullable;

import hidden.HiddenApiBridge;

public class FakeContext extends ContextWrapper {
    static ApplicationInfo systemApplicationInfo = null;
    static Resources.Theme theme = null;
    private String packageName = "android";
    private static final String opPkg = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
            "android" : "com.android.settings";
    public FakeContext() {
        super(null);
    }

    public FakeContext(String packageName) {
        super(null);
        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public Resources getResources() {
        return Resources.getSystem();
    }

    @Override
    public String getOpPackageName() {
        return opPkg;
    }

    private static IPackageManager getIPackageManager() {
        IBinder binder = ServiceManager.getService("package");
        if (binder == null) return null;
        return IPackageManager.Stub.asInterface(binder);
    }

    private static ApplicationInfo getApplicationInfoInternal(String packageName, int flags, int userId) throws RemoteException {
        IPackageManager pm = getIPackageManager();
        if (pm == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return pm.getApplicationInfo(packageName, (long) flags, userId);
        }
        return pm.getApplicationInfo(packageName, flags, userId);
    }

    @Override
    public ApplicationInfo getApplicationInfo() {
        try {
            if (systemApplicationInfo == null)
                systemApplicationInfo = getApplicationInfoInternal("android", 0, 0);
        } catch (Throwable e) {
            Log.e("Magic-Notification", "getApplicationInfo", e);
        }
        return systemApplicationInfo;
    }

    @Override
    public ContentResolver getContentResolver() {
        return new ContentResolver(this) {};
    }

    public int getUserId() {
        return 0;
    }

    public UserHandle getUser() {
        return HiddenApiBridge.UserHandle(0);
    }

    @Override
    public Resources.Theme getTheme() {
        if (theme == null) theme = getResources().newTheme();
        return theme;
    }

    @Nullable
    @Override
    public String getAttributionTag() {
        return null;
    }

    @Override
    public Context createPackageContext(String packageName, int flags) throws PackageManager.NameNotFoundException {
        throw new PackageManager.NameNotFoundException(packageName);
    }
}