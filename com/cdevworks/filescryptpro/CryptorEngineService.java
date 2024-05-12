package com.cdevworks.filescryptpro;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class CryptorEngineService extends Service {
    private ServiceHandler serviceHandler;
    private final String CHANNEL_ID = "foreground service";
    private final String CHANNEL_ID2 = "result";
    NotificationManagerCompat notificationManager, notificationManager2;
    NotificationCompat.Builder notification, resultNotification;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            // Toast.makeText(CryptorEngineService.this, "service starting", Toast.LENGTH_SHORT).show();

            //MainActivity.cryptorEngineHandler object is important to achieve thread synchronization.
            if (!com.cdevworks.filescryptpro.CryptorEngineHandler.runEnginesAndHandler) {
                Log.i("", ">>>>>>>>>>>>>>>>>>>>>>NEW CEH<<<<<<<<<<<<<<<<<<<");
                Thread cryptorEngineHandler = new Thread(() -> com.cdevworks.filescryptpro.CryptorEngine.cryptorEngineHandler.initiate());
                cryptorEngineHandler.setName("cryptorEngineHandler");
                cryptorEngineHandler.setPriority(Thread.MAX_PRIORITY);
                cryptorEngineHandler.start();
            }

            try {
                while (true) {
                    if (com.cdevworks.filescryptpro.FileOperationLog.cryptorEnginesProcessing || MainActivity.mainUIRunning || com.cdevworks.filescryptpro.CSFileChooser.filePickerUIRunning || com.cdevworks.filescryptpro.FileOperationLog.makingCryptorEngineInputs) {
                        if (com.cdevworks.filescryptpro.FileOperationLog.makingCryptorEngineInputs) {
                            notification.setProgress(0, 0, true);
                            notification.setContentTitle(getText(R.string.notification_title)).setContentText(getString(R.string.str, "One moment..."));
                            notification.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background_light)).setColorized(true);
                            if (ActivityCompat.checkSelfPermission(CryptorEngineService.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            notificationManager.notify(1, notification.build());
                            while (com.cdevworks.filescryptpro.FileOperationLog.makingCryptorEngineInputs) {
                                //noinspection BusyWait
                                Thread.sleep(500);
                            }
                        }
                        if (com.cdevworks.filescryptpro.FileOperationLog.cryptorEnginesProcessing) {
                            notificationManager2.cancel(2);
                            // Issue the initial notification with zero progress
                            notification.setProgress(Math.toIntExact(com.cdevworks.filescryptpro.FileOperationLog.totalSizeInMB), Math.toIntExact(com.cdevworks.filescryptpro.FileOperationLog.processedBytes / 1048576L), false);
                            notification.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background_light)).setColorized(true);
                            if (ActivityCompat.checkSelfPermission(CryptorEngineService.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                // TODO: Consider calling
                                //    ActivityCompat#requestPermissions
                                // here to request the missing permissions, and then overriding
                                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                //                                          int[] grantResults)
                                // to handle the case where the user grants the permission. See the documentation
                                // for ActivityCompat#requestPermissions for more details.
                                return;
                            }
                            notificationManager.notify(1, notification.build());

                            while (com.cdevworks.filescryptpro.FileOperationLog.cryptorEnginesProcessing) {
                                notification.setProgress(Math.toIntExact(com.cdevworks.filescryptpro.FileOperationLog.totalSizeInMB), Math.toIntExact(com.cdevworks.filescryptpro.FileOperationLog.processedBytes / 1048576L), false);
                                notificationManager.notify(1, notification.setContentTitle("Progress: " + com.cdevworks.filescryptpro.FileOperationLog.filesProcessed + "/" + com.cdevworks.filescryptpro.FileOperationLog.totalFiles + " files").setContentText((com.cdevworks.filescryptpro.FileOperationLog.processedBytes / 1048576L) + " MB/" + (com.cdevworks.filescryptpro.FileOperationLog.totalSizeInMB) + " MB").build());
                                if (com.cdevworks.filescryptpro.FileOperationLog.folderCryptionOnProgress) {
                                    notification.setProgress(0, 0, true);
                                    notification.setContentTitle(getText(R.string.notification_title)).setContentText(getString(R.string.str, "One moment..."));
                                    notification.setColor(ContextCompat.getColor(getApplicationContext(), R.color.background_light)).setColorized(true);
                                    if (ActivityCompat.checkSelfPermission(CryptorEngineService.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        // TODO: Consider calling
                                        //    ActivityCompat#requestPermissions
                                        // here to request the missing permissions, and then overriding
                                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                        //                                          int[] grantResults)
                                        // to handle the case where the user grants the permission. See the documentation
                                        // for ActivityCompat#requestPermissions for more details.
                                        return;
                                    }
                                    notificationManager.notify(1, notification.build());
                                    while (com.cdevworks.filescryptpro.FileOperationLog.folderCryptionOnProgress) {
                                        //noinspection BusyWait
                                        Thread.sleep(500);
                                    }
                                }
                                //noinspection BusyWait
                                Thread.sleep(1000);
                            }
                            notification.setContentTitle(getText(R.string.notification_title)).setContentText(getText(R.string.notification_message)).setProgress(0, 0, false);
                            notification.setColor(ContextCompat.getColor(getApplicationContext(), R.color.white)).setColorized(false);
                            notificationManager.notify(1, notification.build());

                            if (com.cdevworks.filescryptpro.FileOperationLog.report.status) {
                                StringBuilder sb = new StringBuilder();
                                if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToEncrypt > 0 && com.cdevworks.filescryptpro.FileOperationLog.report.filesToDecrypt > 0)
                                    sb.append("Encrypted ").append(com.cdevworks.filescryptpro.FileOperationLog.report.filesToEncrypt).append(" file(s) and Decrypted ").append(com.cdevworks.filescryptpro.FileOperationLog.report.filesToDecrypt).append(" file(s).");
                                else if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToEncrypt > 0)
                                    sb.append("Encrypted ").append(com.cdevworks.filescryptpro.FileOperationLog.report.filesToEncrypt).append(" file(s)");
                                else if (com.cdevworks.filescryptpro.FileOperationLog.report.filesToDecrypt > 0)
                                    sb.append("Decrypted ").append(com.cdevworks.filescryptpro.FileOperationLog.report.filesToDecrypt).append(" file(s)");
                                notificationManager2.notify(2, resultNotification.setContentTitle("Operation Completed Successfully.").setContentText(sb).build());
                            } else {
                                notificationManager2.notify(2, resultNotification.setContentTitle("Operation Completed with Failures.").setContentText("Tap here to see details.").build());
                            }
                            showOperationReport();
                        } else {
                            notification.setContentTitle(getText(R.string.notification_title)).setContentText(getText(R.string.notification_message)).setProgress(0, 0, false);
                            notification.setColor(ContextCompat.getColor(getApplicationContext(), R.color.white)).setColorized(false);
                            notificationManager.notify(1, notification.build());
                        }
                        //noinspection BusyWait
                        Thread.sleep(1000);
                    } else {
                        //noinspection BusyWait
                        Thread.sleep(1000);
                        if (!(com.cdevworks.filescryptpro.FileOperationLog.cryptorEnginesProcessing || MainActivity.mainUIRunning || com.cdevworks.filescryptpro.CSFileChooser.filePickerUIRunning)) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException();
            }

            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            notification.setContentTitle(getText(R.string.notification_title)).setContentText(getText(R.string.notification_message_2)).setProgress(0, 0, false);
            notification.setColor(ContextCompat.getColor(getApplicationContext(), R.color.white)).setColorized(false);
            notificationManager.notify(1, notification.build());
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        HandlerThread thread = new HandlerThread("ServiceStartArguments", -20);
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        Looper serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        createNotificationChannel();
        createResultNotificationChannel();

        final Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        notificationManager = NotificationManagerCompat.from(CryptorEngineService.this);
        notification = new NotificationCompat.Builder(CryptorEngineService.this, CHANNEL_ID);
        notification.setContentTitle(getText(R.string.notification_title)).setContentText(getText(R.string.notification_message)).setSmallIcon(R.drawable.ic_action_name).setColor(ContextCompat.getColor(this, R.color.foreground)).setContentIntent(pendingIntent).setPriority(NotificationCompat.PRIORITY_LOW);

        notificationManager2 = NotificationManagerCompat.from(CryptorEngineService.this);
        resultNotification = new NotificationCompat.Builder(CryptorEngineService.this, CHANNEL_ID2);
        resultNotification.setSmallIcon(R.drawable.ic_action_name).setColor(ContextCompat.getColor(this, R.color.foreground)).setContentIntent(pendingIntent).setPriority(NotificationCompat.PRIORITY_LOW);

        startForeground(1, notification.build());
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void createResultNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        CharSequence name = getString(R.string.channel_name2);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID2, name, importance);
        channel.setDescription(description);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    public void showOperationReport() {
        try (FileInputStream fis = new FileInputStream(MainActivity.reportFile); ObjectInputStream ois = new ObjectInputStream(fis)) {
            com.cdevworks.filescryptpro.OperationReport report = (com.cdevworks.filescryptpro.OperationReport) ois.readObject();
            Log.i("", "Status: " + report.status + ", Total Files: " + report.totalFiles + ", Files Processed: " + report.filesProcessed + ", Files to be Encrypted: " + report.filesToEncrypt + ", Files to be Decrypted: " + report.filesToDecrypt);
            Log.i("", "Log: " + report.log);
        } catch (Exception e) {
            Log.i("", ">>>>>>>>>>>>>>>>: SOME ERROR");
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onDestroy() {
        MainActivity.reportFile.delete();
        com.cdevworks.filescryptpro.CryptorEngineHandler.stop();
    }
}

