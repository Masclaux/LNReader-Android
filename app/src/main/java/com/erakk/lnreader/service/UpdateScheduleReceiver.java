package com.erakk.lnreader.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.erakk.lnreader.Constants;

import java.util.Calendar;

public class UpdateScheduleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        reschedule(context);
    }

    public static void reschedule(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String updatesIntervalStr = preferences.getString(Constants.PREF_UPDATE_INTERVAL, "0");
        int updatesInterval = Integer.parseInt(updatesIntervalStr);

        reschedule(context, updatesInterval);
    }

    public static void reschedule(Context context, int updatesInterval) {
        long repeatTime = UpdateService.GetUpdateInterval("" + updatesInterval);

        AlarmManager service = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, UpdateStartServiceReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        if (repeatTime > 0) {
            Log.d(UpdateService.TAG, "Setting up schedule");

            // Start repeatTime seconds after boot completed
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, 60);

            // InexactRepeating allows Android to optimize the energy consumption
            Log.i(UpdateService.TAG, "Repeating in: " + repeatTime);
            // service.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), repeatTime, pending);
            service.set(AlarmManager.RTC, cal.getTimeInMillis() + repeatTime, pending);
        } else {
            Log.i(UpdateService.TAG, "Canceling Schedule");
            service.cancel(pending);
        }
    }
}