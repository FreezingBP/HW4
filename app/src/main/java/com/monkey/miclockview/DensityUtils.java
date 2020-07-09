package com.monkey.miclockview;

import android.content.Context;
import android.util.TypedValue;


public class DensityUtils {
    private DensityUtils() {  

        throw new UnsupportedOperationException("cannot be instantiated");
    }
    public static int sp2px(Context context, float spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                spVal, context.getResources().getDisplayMetrics());
    }
}