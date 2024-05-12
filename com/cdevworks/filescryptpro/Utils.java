package com.cdevworks.filescryptpro;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
    public static String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        DecimalFormat df = new DecimalFormat("#.00");
        if (size >= gb) {
            return df.format((double) size / gb) + " GB";
        } else if (size >= mb) {
            return df.format((double) size / mb) + " MB";
        } else if (size >= kb) {
            return df.format((double) size / kb) + " KB";
        } else {
            if (size == 0)
                return "0.00 B";
            return df.format((double) size) + " B";
        }
    }

    public static String getDateAndTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy_HH-mm");
        LocalDateTime now = LocalDateTime.now();
        return now.format(formatter);
    }
}

