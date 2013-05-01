package com.android.settings.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

// don't show unavoidable warnings
@SuppressWarnings({
        "UnusedDeclaration",
        "MethodWithMultipleReturnPoints",
        "ReturnOfNull",
        "NestedAssignment",
        "DynamicRegexReplaceableByCompiledPattern",
        "BreakStatement"})
public class Helpers {
    // avoids hardcoding the tag
    private static final String TAG = Thread.currentThread().getStackTrace()[1].getClassName();

    public Helpers() {
        // dummy constructor
    }

    /**
     * Checks device for SuperUser permission
     *
     * @return If SU was granted or denied
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static boolean checkSu() {
        if (!new File("/system/bin/su").exists()
                && !new File("/system/xbin/su").exists()) {
            Log.e(TAG, "su binary does not exist!!!");
            return false; // tell caller to bail...
        }
        try {
            if (CMDProcessor.runSuCommand("ls /data/app-private").success()) {
                Log.i(TAG, " SU exists and we have permission");
                return true;
            } else {
                Log.i(TAG, " SU exists but we don't have permission");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "NullPointer throw while looking for su binary", e);
            return false;
        }
    }

    /**
     * Checks device for network connectivity
     *
     * @return If the device has data connectivity
    */
    public static boolean isNetworkAvailable(Context context) {
        boolean state = false;
        if (context != null) {
            ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                Log.i(TAG, "The device currently has data connectivity");
                state = true;
            } else {
                Log.i(TAG, "The device does not currently have data connectivity");
                state = false;
            }
        }
        return state;
    }

    /**
     * Checks to see if Busybox is installed in "/system/"
     *
     * @return If busybox exists
     */
    public static boolean checkBusybox() {
        if (!new File("/system/bin/busybox").exists()
                && !new File("/system/xbin/busybox").exists()) {
            Log.e(TAG, "Busybox not in xbin or bin!");
            return false;
        }
        try {
            if (!CMDProcessor.runSuCommand("busybox mount").success()) {
                Log.e(TAG, "Busybox is there but it is borked! ");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "NullpointerException thrown while testing busybox", e);
            return false;
        }
        return true;
    }

    public static String[] getMounts(CharSequence path) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("/proc/mounts"), 256);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains(path)) {
                    return line.split(" ");
                }
            }
        } catch (FileNotFoundException ignored) {
            Log.d(TAG, "/proc/mounts does not exist");
        } catch (IOException ignored) {
            Log.d(TAG, "Error reading /proc/mounts");
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException ignored) {
                    // ignored
                }
            }
        }
        return null;
    }

    public static boolean getMount(String mount) {
        String[] mounts = getMounts("/system");
        if (mounts != null && mounts.length >= 3) {
            String device = mounts[0];
            String path = mounts[1];
            String point = mounts[2];
            String preferredMountCmd = new String("mount -o " + mount + ",remount -t " + point + ' ' + device + ' ' + path);
            if (CMDProcessor.runSuCommand(preferredMountCmd).success()) {
                return true;
            }
        }
        String fallbackMountCmd = new String("busybox mount -o remount," + mount + " /system");
        return CMDProcessor.runSuCommand(fallbackMountCmd).success();
    }

    public static String readOneLine(String fname) {
        BufferedReader br = null;
        String line = null;
        try {
            br = new BufferedReader(new FileReader(fname), 1024);
            line = br.readLine();
        } catch (FileNotFoundException ignored) {
            Log.d(TAG, "File was not found! trying via shell...");
            return readFileViaShell(fname, true);
        } catch (IOException e) {
            Log.d(TAG, "IOException while reading system file", e);
            return readFileViaShell(fname, true);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                    // failed to close reader
                }
            }
        }
        return line;
    }

    public static String readFileViaShell(String filePath, boolean useSu) {
        String command = new String("cat " + filePath);
        return useSu ? CMDProcessor.runSuCommand(command).getStdout()
                : CMDProcessor.runShellCommand(command).getStdout();
    }

    public static boolean writeOneLine(String filename, String value) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(filename);
            fileWriter.write(value);
        } catch (IOException e) {
            String Error = "Error writing { " + value + " } to file: " + filename;
            Log.e(TAG, Error, e);
            return false;
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException ignored) {
                    // failed to close writer
                }
            }
        }
        return true;
    }

    public static String[] getAvailableIOSchedulers() {
        String[] schedulers = null;
        String[] aux = readStringArray("/sys/block/mmcblk0/queue/scheduler");
        if (aux != null) {
            schedulers = new String[aux.length];
            for (int i = 0; i < aux.length; i++) {
                schedulers[i] = aux[i].charAt(0) == '['
                        ? aux[i].substring(1, aux[i].length() - 1)
                        : aux[i];
            }
        }
        return schedulers;
    }

    private static String[] readStringArray(String fname) {
        String line = readOneLine(fname);
        if (line != null) {
            return line.split(" ");
        }
        return null;
    }

    public static String getIOScheduler() {
        String scheduler = null;
        String[] schedulers = readStringArray("/sys/block/mmcblk0/queue/scheduler");
        if (schedulers != null) {
            for (String s : schedulers) {
                if (s.charAt(0) == '[') {
                    scheduler = s.substring(1, s.length() - 1);
                    break;
                }
            }
        }
        return scheduler;
    }

    /**
     * Long toast message
     *
     * @param context Application Context
     * @param msg Message to send
     */
    public static void msgLong(Context context, String msg) {
        if (context != null && msg != null) {
            Toast.makeText(context, msg.trim(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Short toast message
     *
     * @param context Application Context
     * @param msg Message to send
     */
    public static void msgShort(Context context, String msg) {
        if (context != null && msg != null) {
            Toast.makeText(context, msg.trim(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Long toast message
     *
     * @param context Application Context
     * @param msg Message to send
     */
    public static void sendMsg(Context context, String msg) {
        if (context != null && msg != null) {
            msgLong(context, msg);
        }
    }

    /**
     * Return a timestamp
     *
     * @param context Application Context
     */
    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    public static String getTimestamp(Context context) {
        String timestamp = "unknown";
        Date now = new Date();
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(context);
        if (dateFormat != null && timeFormat != null) {
            timestamp = dateFormat.format(now) + ' ' + timeFormat.format(now);
        }
        return timestamp;
    }

    public static boolean isPackageInstalled(String packageName, PackageManager pm) {
        try {
            String mVersion = pm.getPackageInfo(packageName, 0).versionName;
            if (mVersion == null) {
                return false;
            }
        } catch (NameNotFoundException notFound) {
            Log.e(TAG, "Package could not be found!", notFound);
            return false;
        }
        return true;
    }

    public static void restartSystemUI() {
        CMDProcessor.startSuCommand("pkill -TERM -f com.android.systemui");
    }

    public static void setSystemProp(String prop, String val) {
        CMDProcessor.startSuCommand("setprop " + prop + " " + val);
    }

    public static String getSystemProp(String prop, String def) {
        String result = null;
        try {
            result = SystemProperties.get(prop, def);
        } catch (IllegalArgumentException iae) {
            Log.e(TAG, "Failed to get prop: " + prop);
        }
        return result == null ? def : result;
    }

    /*
     * Mount System partition
     *
     * @param read_value ro for ReadOnly and rw for Read/Write
     *
     * @returns true for successful mount
     */
    public static boolean mountSystem(String read_value) {
        String REMOUNT_CMD = "busybox mount -o %s,remount -t yaffs2 /dev/block/mtdblock1 /system";
        Log.d(TAG, "Remounting /system " + read_value);
        return CMDProcessor.runSuCommand(String.format(REMOUNT_CMD, read_value)).success();
    }
    
    /*
     * Find value of build.prop item (/system can be ro or rw)
     *
     * @param prop /system/build.prop property name to find value of
     *
     * @returns String value of @param:prop
     */
    public static String findBuildPropValueOf(String prop) {
        String mBuildPath = "/system/build.prop";
        String DISABLE = "disable";
        String value = null;
        try {
            //create properties construct and load build.prop
            Properties mProps = new Properties();
            mProps.load(new FileInputStream(mBuildPath));
            //get the property
            value = mProps.getProperty(prop, DISABLE);
            Log.d(TAG, String.format("Helpers:findBuildPropValueOf found {%s} with the value (%s)", prop, value));
        } catch (IOException ioe) {
            Log.d(TAG, "failed to load input stream");
        } catch (NullPointerException npe) {
            //swallowed thrown by ill formatted requests
        }
        
        if (value != null) {
            return value;
        } else {
            return DISABLE;
        }
    }
    
    // find value of /sys/kernel/fast_charge/force_fast_charge
    public static int isFastCharge() {
        int onOff = 0;
        String line = "";
        final String filename = "/sys/kernel/fast_charge/force_fast_charge";
        final File f = new File(filename);
        
        if (f.exists() && f.canRead()) {
            try {
                final BufferedReader br = new BufferedReader(new FileReader(f), 256);
                String buffer = null;
                while ((buffer = br.readLine()) != null) {
                    line += buffer + "\n";
                    try {
                        onOff = Integer.parseInt(buffer);
                    } catch (NumberFormatException nfe) {
                        onOff = 0;
                    }
                }
                br.close();
            } catch (final Exception e) {
                Log.e(TAG, "Error reading file: " + filename, e);
                onOff = 0;
            }
        }
        return onOff;
    }
}
