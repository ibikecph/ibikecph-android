package com.spoiledmilk.ibikecph.tracking;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.util.Log;
import com.spoiledmilk.ibikecph.IbikeApplication;
import com.spoiledmilk.ibikecph.R;
import com.spoiledmilk.ibikecph.persist.Track;
import com.spoiledmilk.ibikecph.persist.TrackLocation;
import io.realm.Realm;
import io.realm.RealmResults;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by jens on 3/13/15.
 */
public class MilestoneManager {
    public enum LengthNotification {
        KM_10,
        KM_50,
        KM_100,
        KM_250,
        KM_500,
        KM_750
    }


    public static void checkForMilestones() {
        Realm realm = Realm.getInstance(IbikeApplication.getContext());

        // Total length
        int totalLength = getTotalLength();
        int lengthMilestoneOrdinal = IbikeApplication.getSettings().getLengthNotificationOrdinal();
        LengthNotification notificationToCreate  = null;

        // We compare the lengthMilestoneOrdinal to the ordinal of the potential milestones. If we don't have a milestone
        // yet the call will return -1, which is lower than 0, the ordinal of LengthNotification.KM_10.
        if (totalLength > 750*1000 && lengthMilestoneOrdinal < LengthNotification.KM_750.ordinal()) {
            notificationToCreate = LengthNotification.KM_750;
        } else if (totalLength > 500*1000 && lengthMilestoneOrdinal < LengthNotification.KM_500.ordinal()) {
            notificationToCreate = LengthNotification.KM_500;
        } else if (totalLength > 250*1000 && lengthMilestoneOrdinal < LengthNotification.KM_250.ordinal()) {
            notificationToCreate = LengthNotification.KM_250;
        } else if (totalLength > 100*1000 && lengthMilestoneOrdinal < LengthNotification.KM_100.ordinal()) {
            notificationToCreate = LengthNotification.KM_100;
        } else if (totalLength > 50*1000 && lengthMilestoneOrdinal < LengthNotification.KM_50.ordinal()) {
            notificationToCreate = LengthNotification.KM_50;
        } else if (totalLength > 10*1000 && lengthMilestoneOrdinal < LengthNotification.KM_10.ordinal()) {
            notificationToCreate = LengthNotification.KM_10;
        }

        if (notificationToCreate != null) {
            makeLengthNotification(notificationToCreate);

            // Update the settings so we get persistence
            IbikeApplication.getSettings().setLengthNotificationOrdinal(notificationToCreate.ordinal());
        }

        // Streak
        int curStreak = daysInARow();
        int maxStreak = IbikeApplication.getSettings().getMaxStreakLength();

        if (curStreak > maxStreak) {
            switch(curStreak) {
                case 3:
                case 5:
                case 10:
                case 15:
                case 20:
                case 25:
                case 30:
                    makeStreakNotification(curStreak);
                    break;

                default:
                    break;
            }

            IbikeApplication.getSettings().setMaxStreakLength(curStreak);
        }

        Log.d("JC", "Current streak: " + curStreak);

    }

    // TODO: DRY
    private static void makeStreakNotification(int streakLength) {
        Context context = IbikeApplication.getContext();

        Notification.Builder notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setContentTitle(IbikeApplication.getString("app_name"));

        String message = "";

        switch(streakLength) {
            case 3:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_daystreak_1_description"), streakLength));
                break;
            case 5:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_daystreak_2_description"), streakLength));
                break;
            case 10:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_daystreak_3_description"), streakLength));
                break;
            case 15:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_daystreak_4_description"), streakLength));
                break;
            case 20:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_daystreak_5_description"), streakLength));
                break;
            case 25:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_daystreak_6_description"), streakLength));
                break;
            case 30:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_daystreak_7_description"), streakLength));
                break;
            default:
                break;
        }


        notificationBuilder.setSmallIcon(R.drawable.logo);

        Notification n = notificationBuilder.build();
        NotificationManager mNotificationManager =
                (NotificationManager) IbikeApplication.getContext().getSystemService(context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(1, n);
    }

    public static void makeLengthNotification(LengthNotification length) {
        Context context = IbikeApplication.getContext();

        Notification.Builder notificationBuilder = new Notification.Builder(context);
        notificationBuilder.setContentTitle(IbikeApplication.getString("app_name"));

        switch (length) {
            case KM_10:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_distance_1_description"), 10));
                break;
            case KM_50:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_distance_2_description"), 50));
                break;
            case KM_100:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_distance_3_description"), 100));
                break;
            case KM_250:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_distance_4_description"), 250));
                break;
            case KM_500:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_distance_5_description"), 500));
                break;
            case KM_750:
                notificationBuilder.setContentText(String.format(IbikeApplication.getString("milestone_distance_6_description"), 750));
                break;
        }

        notificationBuilder.setSmallIcon(R.drawable.logo);

        Notification n = notificationBuilder.build();
        NotificationManager mNotificationManager =
                (NotificationManager) IbikeApplication.getContext().getSystemService(context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(0, n);
    }


    public static int getTotalLength() {
        Realm realm = Realm.getInstance(IbikeApplication.getContext());
        RealmResults<Track> results = realm.allObjects(Track.class);

        double totalDist = 0;

        for (Track t : results) {
            totalDist += t.getLength();
        }

        return (int) totalDist;
    }

    public static int daysInARow() {
        // Go though all TrackLocation objects
        Realm realm = Realm.getInstance(IbikeApplication.getContext());

        int curStreak = 0;
        boolean stop = false;

        // Get the length of the current streak
        for (int i = 0; !stop; i++) {
            Date start = getDateStart(daysAgo(i));
            Date end = getDateEnd(daysAgo(i));

            long numTrackLocationsForParticularDay = realm.where(TrackLocation.class).between("timestamp", start, end).count();

            if (numTrackLocationsForParticularDay > 0 ) {
                curStreak++;
            } else {
                stop = true;
            }
        }

        // Calculate number of days from span
        return curStreak;
    }

    public static Date daysAgo(int i) {
        Date d = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(d);

        // Subtract the desired number of days
        c.add(Calendar.DAY_OF_MONTH, i * -1);

        return c.getTime();
    }

    public static Date getDateAtTime(Date d, int hours, int minutes) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        return cal.getTime();
    }

    public static Date getDateStart(Date d) {
        return getDateAtTime(d, 0, 0);
    }

    public static Date getDateEnd(Date d) {
        return getDateAtTime(d, 23, 59);
    }

}
