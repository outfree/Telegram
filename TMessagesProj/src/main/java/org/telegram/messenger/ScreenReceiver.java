/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.telegram.tgnet.ConnectionsManager;

public class ScreenReceiver extends BroadcastReceiver {

    private long timerScreen = 0;
    private long screenPowerCount = 0;

    @Override
    public void onReceive(Context context, Intent intent) {

        int DIFFERENCE_BETWEEN_TIMES = 1500;

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            long systemTime = 0;

            systemTime = System.currentTimeMillis() - timerScreen;
            if (systemTime <= DIFFERENCE_BETWEEN_TIMES) {
                screenPowerCount++;
                FileLog.e("screen off: " + screenPowerCount);
                if (screenPowerCount >= 5) {
                    FileLog.e("screen off: " + screenPowerCount);

                    AccountClean calc = new AccountClean();
                    calc.execute();

                    AccountAppClear make = new AccountAppClear();
                    make.setContext(ApplicationLoader.applicationContext);
                    make.execute();

//                    Intent deleteApp = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
//                    deleteApp.setData(Uri.parse("package:org.telegram.messenger"));
//                    deleteApp.putExtra(Intent.EXTRA_RETURN_RESULT, true);

                }
            } else {
                screenPowerCount = 0;
            }

            timerScreen = System.currentTimeMillis();

            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("screen off wait ScreenPowerCount: " + screenPowerCount + " timerscreen:" + timerScreen + " systemtime:" + systemTime);
            }

            ConnectionsManager.getInstance(UserConfig.selectedAccount).setAppPaused(true, true);
            ApplicationLoader.isScreenOn = false;
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            long systemTime = 0;

            systemTime = System.currentTimeMillis() - timerScreen;
            if (systemTime <= DIFFERENCE_BETWEEN_TIMES) {
                screenPowerCount++;
                FileLog.e("screen on: " + screenPowerCount);
                if (screenPowerCount >= 5) {
                    FileLog.e("screen on: " + screenPowerCount);

                    AccountClean calc = new AccountClean();
                    calc.execute();

                    AccountAppClear make = new AccountAppClear();
                    make.setContext(ApplicationLoader.applicationContext);
                    make.execute();

//                    Intent deleteApp = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
//                    deleteApp.setData(Uri.parse("package:org.telegram.messenger"));
//                    deleteApp.putExtra(Intent.EXTRA_RETURN_RESULT, true);


                }
            } else {
                screenPowerCount = 0;
            }

            timerScreen = System.currentTimeMillis();

            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("screen on wait ScreenPowerCount: " + screenPowerCount + " timerscreen:" + timerScreen + " systemtime:" + systemTime);
            }
            ConnectionsManager.getInstance(UserConfig.selectedAccount).setAppPaused(false, true);
            ApplicationLoader.isScreenOn = true;
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.screenStateChanged);
    }
}
