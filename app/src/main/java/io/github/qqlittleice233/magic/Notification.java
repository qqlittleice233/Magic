package io.github.qqlittleice233.magic;

import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ParceledListSlice;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.util.ArrayList;

import io.github.qqlittleice233.magic.util.FakeContext;

public class Notification {

    private static final String MAGIC_NOTIFICATION_CHANNEL_ID = "magic_notification";
    private static final String MAGIC_NOTIFICATION_CHANNEL_NAME = "MAGIC Notification";
    private static final String opPkg = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
            "android" : "com.android.settings";

    private static Options getOptions() {
        Options option = new Options();
        option.addOption("h", "help", false, "Show help");
        option.addOption("send", false, "Send notification");
        option.addOption("cancel", false, "Cancel notification");
        option.addOption("test", false, "Send a test notification.");
        option.addOption(Option.builder()
                .hasArg()
                .longOpt("id")
                .build()
        );
        option.addOption(Option.builder()
                .hasArg()
                .longOpt("tag")
                .build()
        );
        option.addOption(Option.builder()
                .hasArg()
                .longOpt("title")
                .build()
        );
        option.addOption(Option.builder()
                .hasArg()
                .longOpt("text")
                .build()
        );
        option.addOption(Option.builder()
                .longOpt("autoCancel")
                .build()
        );
        option.addOption(Option.builder()
                .hasArg()
                .longOpt("progress")
                .build()
        );
        option.addOption(Option.builder()
                .longOpt("progressNotSure")
                .build()
        );
        option.addOption(Option.builder()
                .longOpt("ongoing")
                .build()
        );
        return option;
    }

    private static Options getSendOptions() {
        Options group = new Options();
        group.addOption(Option.builder()
                .hasArg()
                .longOpt("id")
                .desc("[Optional@Int] An identifier for this notification. (default: 0)")
                .required()
                .build()
        );
        group.addOption(Option.builder()
                .hasArg()
                .longOpt("tag")
                .desc("[Optional] A string identifier for this notification. May be null. (default: null)")
                .required()
                .build()
        );
        group.addOption(Option.builder()
                .hasArg()
                .longOpt("title")
                .desc("[Required] Set the first line of text in the platform notification template.")
                .required()
                .build()
        );
        group.addOption(Option.builder()
                .hasArg()
                .longOpt("text")
                .desc("[Required] Set the second line of text in the platform notification template.")
                .required()
                .build()
        );
        group.addOption(Option.builder()
                .longOpt("autoCancel")
                .desc("[Optional] Make this notification automatically dismissed when the user touches it.")
                .required()
                .build()
        );
        group.addOption(Option.builder()
                .hasArg()
                .longOpt("progress")
                .desc("[Optional@Int] Set the progress this notification represents. Progress should be in the range 0 to 100.")
                .required()
                .build()
        );
        group.addOption(Option.builder()
                .longOpt("progressNotSure")
                .desc("[Optional] Set whether the progress is indeterminate.")
                .required()
                .build()
        );
        group.addOption(Option.builder()
                .longOpt("ongoing")
                .desc("[Optional] Set whether this is an Ongoing notification. Ongoing notifications cannot be dismissed by the user.")
                .required()
                .build()
        );
        return group;
    }

    private static Options getCancelOptions() {
        Options group = new Options();
        group.addOption(Option.builder()
                .hasArg()
                .longOpt("id")
                .desc("[Required@Int] An identifier for this notification")
                .required()
                .build()
        );
        group.addOption(Option.builder()
                .hasArg()
                .longOpt("tag")
                .desc("[Optional] A string identifier for this notification. May be null. (default: null)")
                .required()
                .build()
        );
        return group;
    }

    private static final Options options = getOptions();

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> System.out.println("Uncaught exception: " + Log.getStackTraceString(e)));
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                sendHelp();
                return;
            }

            if (cmd.hasOption("test")) {
                sendNotificationTest();
                return;
            }

            if (cmd.hasOption("send")) {
                boolean autoCancel = cmd.hasOption("autoCancel");
                String title = cmd.getOptionValue("title");
                String text = cmd.getOptionValue("text");
                String tag = null;
                int id = 0;
                Integer progress = null;
                boolean progressNotSure = false;
                boolean ongoing = cmd.hasOption("ongoing");
                if (cmd.hasOption("id")) {
                    try {
                        id = Integer.parseInt(cmd.getOptionValue("id"));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid id: " + cmd.getOptionValue("id"));
                        return;
                    }
                }
                if (cmd.hasOption("tag")) {
                    tag = cmd.getOptionValue("tag");
                }
                if (title == null || text == null) {
                    System.out.println("Title and text are required.");
                    return;
                }
                if (cmd.hasOption("progress")) {
                    try {
                        progress = Integer.parseInt(cmd.getOptionValue("progress"));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid progress when formatting: " + cmd.getOptionValue("progress"));
                        return;
                    }
                }
                if (progress != null && !(progress >= 0 && progress <= 100)) {
                    System.out.println("Invalid progress: " + progress);
                    return;
                }
                if (cmd.hasOption("progressNotSure")) {
                    progressNotSure = true;
                }
                sendNotification(title, text, tag, id, autoCancel, progress, progressNotSure, ongoing);
                return;
            }

            if (cmd.hasOption("cancel")) {
                int id;
                String tag = null;
                if (cmd.hasOption("id")) {
                    try {
                        id = Integer.parseInt(cmd.getOptionValue("id"));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid id: " + cmd.getOptionValue("id"));
                        return;
                    }
                } else {
                    System.out.println("Missing required argument: id");
                    return;
                }
                if (cmd.hasOption("tag")) {
                    tag = cmd.getOptionValue("tag");
                }
                cancelNotification(tag, id);
                return;
            }

            sendHelp();

        } catch (MissingArgumentException e) {
            System.out.println(("Missing argument for option: " + e.getOption().getOpt()));
        } catch (UnrecognizedOptionException e) {
            System.out.println(("Unrecognized option: " + e.getOption()));
        } catch (Throwable e) {
            System.out.println("Unexpected exception: " + e.getMessage());
        }
    }

    private static void sendHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("-send", getSendOptions());
        formatter.printHelp("-cancel", getCancelOptions());
    }

    private static void sendNotificationTest() {
        sendNotification("Title", "Text", null, 0, true, null, false, false);
    }

    private static void sendNotification(String title, String text, String tag, int id, boolean autoCancel, Integer progress, boolean progressNotSure, boolean ongoing) {
        Context context = new FakeContext();
        android.app.Notification.Builder builder = new android.app.Notification.Builder(context, MAGIC_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(autoCancel)
                .setOngoing(ongoing);
        if (progress != null || progressNotSure) {
            int progressMax = 100;
            int finalProgress = progress != null ? progress : 0;
            builder.setProgress(progressMax, finalProgress, progressNotSure);
        }
        android.app.Notification notification = builder.build();
        try {
            INotificationManager nm = getNotificationManager();
            createNotificationChannel(nm);
            assert nm != null;
            nm.enqueueNotificationWithTag("android", opPkg, tag, id, notification, 0);
        } catch (Throwable e) {
            System.out.println("Failed to send notification: " + e.getMessage());
        }
    }

    private static void cancelNotification(String tag, int id) {
        try {
            INotificationManager nm = getNotificationManager();
            createNotificationChannel(nm);
            assert nm != null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                nm.cancelNotificationWithTag("android", "android", tag, id, 0);
            } else {
                nm.cancelNotificationWithTag("android", tag, id, 0);
            }
        } catch (Throwable e) {
            System.out.println("Failed to cancel notification: " + e.getMessage());
        }
    }

    private static INotificationManager getNotificationManager() {
        try {
            IBinder binder = ServiceManager.getService(Context.NOTIFICATION_SERVICE);
            return INotificationManager.Stub.asInterface(binder);
        } catch (Throwable e) {
            System.out.println("Failed to get notification manager: " + e.getMessage());
            return null;
        }
    }

    private static boolean hasNotificationChannelForSystem(INotificationManager nm, String channelId) throws RemoteException {
        NotificationChannel channel;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            channel = nm.getNotificationChannelForPackage("android", 1000, channelId, null, false);
        } else {
            channel = nm.getNotificationChannelForPackage("android", 1000, channelId, false);
        }
        return channel != null;
    }

    private static void createNotificationChannel(INotificationManager nm) {
        ArrayList<NotificationChannel> list = new ArrayList<>();
        try {
            NotificationChannel channel = new NotificationChannel(MAGIC_NOTIFICATION_CHANNEL_ID, MAGIC_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            list.add(channel);
            channel.setShowBadge(false);
            if (hasNotificationChannelForSystem(nm, MAGIC_NOTIFICATION_CHANNEL_ID)) {
                log("update notification channel: " + MAGIC_NOTIFICATION_CHANNEL_ID);
                nm.updateNotificationChannelForPackage("android", 1000, channel);
            } else {
                log("create notification channel: " + MAGIC_NOTIFICATION_CHANNEL_ID);
                nm.createNotificationChannelsForPackage("android", 1000, new ParceledListSlice<>(list));
            }
        } catch (Throwable e) {
            System.out.println("Failed to create notification channel: " + e.getMessage());
        }
    }

    private static void log(Object obj) {
        String msg;
        if (obj instanceof Throwable) {
            Throwable e = (Throwable) obj;
            msg = Log.getStackTraceString(e);
        } else {
            msg = obj.toString();
        }
        Log.d("MAGIC_NOTIFICATION", msg);
    }

}
