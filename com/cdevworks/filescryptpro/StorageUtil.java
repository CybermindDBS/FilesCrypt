package com.cdevworks.filescryptpro;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StorageUtil {
    private static final String SECONDARY_STORAGES = System.getenv("SECONDARY_STORAGE");
    private static final String EMULATED_STORAGE_TARGET = System.getenv("EMULATED_STORAGE_TARGET");

    public static String[] getStorageDirectories(Context context) {
        // Final set of paths
        final Set<String> availableDirectoriesSet = new HashSet<>();

        if (!TextUtils.isEmpty(EMULATED_STORAGE_TARGET)) {
            // Device has an emulated storage
            availableDirectoriesSet.add(getEmulatedStorageTarget());
        } else {
            // Device doesn't have an emulated storage
            availableDirectoriesSet.addAll(getExternalStorage(context));
        }

        // Add all secondary storages
        Collections.addAll(availableDirectoriesSet, getAllSecondaryStorages());

        String[] storagesArray = new String[availableDirectoriesSet.size()];
        String[] toBeFormatted = availableDirectoriesSet.toArray(storagesArray);
        String[] formatted = new String[toBeFormatted.length];
        for (int i = 0; i < toBeFormatted.length; i++) {
            StringBuilder sb = new StringBuilder(toBeFormatted[i]);
            sb.deleteCharAt(0);
            sb.deleteCharAt(sb.length() - 1);
            formatted[i] = sb.toString();
        }
        ArrayList<String> validStorages = new ArrayList<>();
        for (String storageDirectory : formatted) {
            if (new File(File.separator + storageDirectory).canRead()) {
                validStorages.add(storageDirectory);
            }
        }
        return validStorages.toArray(new String[0]);
    }

    private static Set<String> getExternalStorage(Context context) {
        final Set<String> availableDirectoriesSet = new HashSet<>();
        // Solution of empty raw emulated storage for android version >= marshmallow
        // because the EXTERNAL_STORAGE become something like: "/Storage/A5F9-15F4",
        // so we can't access it directly
        File[] files = getExternalFilesDirs(context);
        for (File file : files) {
            if (file != null) {
                String applicationSpecificAbsolutePath = file.getAbsolutePath();
                String rootPath = applicationSpecificAbsolutePath.substring(0, applicationSpecificAbsolutePath.indexOf("Android/data"));
                availableDirectoriesSet.add(rootPath);
            }
        }
        return availableDirectoriesSet;
    }

    private static String getEmulatedStorageTarget() {
        String rawStorageId = "";
        // External storage paths should have storageId in the last segment
        // i.e: "/storage/emulated/storageId" where storageId is 0, 1, 2, ...
        final String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        final String[] folders = path.split("\\|/|" + File.separator);
        final String lastSegment = folders[folders.length - 1];
        if (!TextUtils.isEmpty(lastSegment) && TextUtils.isDigitsOnly(lastSegment)) {
            rawStorageId = lastSegment;
        }

        if (TextUtils.isEmpty(rawStorageId)) {
            return EMULATED_STORAGE_TARGET;
        } else {
            return EMULATED_STORAGE_TARGET + File.separator + rawStorageId;
        }
    }

    private static String[] getAllSecondaryStorages() {
        if (!TextUtils.isEmpty(SECONDARY_STORAGES)) {
            // All Secondary SD-CARDs split into array
            return SECONDARY_STORAGES.split(File.pathSeparator);
        }
        return new String[0];
    }

    private static File[] getExternalFilesDirs(Context context) {
        return context.getExternalFilesDirs(null);
    }
}
