package com.moulberry.flashback.compat;

import com.moulberry.flashback.LoaderCompat;

public class IrisApiWrapper {

    public static boolean isIrisAvailable() {
        return LoaderCompat.isModLoaded("oculus");
    }

    public static boolean isShaderPackInUse() {
        return isIrisAvailable();
    }

}
