package com.eljaviluki.grindrplus;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static String toReadableDate(long timestamp){
        return SimpleDateFormat.getDateTimeInstance().format(new Date(timestamp));
    }
}
