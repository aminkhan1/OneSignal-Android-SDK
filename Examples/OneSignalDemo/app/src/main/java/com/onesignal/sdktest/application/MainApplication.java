package com.onesignal.sdktest.application;

import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.multidex.MultiDexApplication;

import com.onesignal.OneSignal;
import com.onesignal.inAppMessages.IInAppMessage;
import com.onesignal.inAppMessages.IInAppMessageClickHandler;
import com.onesignal.inAppMessages.IInAppMessageClickResult;
import com.onesignal.inAppMessages.IInAppMessageLifecycleHandler;
import com.onesignal.debug.LogLevel;
import com.onesignal.notifications.IDisplayableNotification;
import com.onesignal.notifications.INotificationLifecycleListener;
import com.onesignal.notifications.INotificationWillDisplayEvent;
import com.onesignal.sdktest.BuildConfig;
import com.onesignal.sdktest.R;
import com.onesignal.sdktest.constant.Tag;
import com.onesignal.sdktest.constant.Text;
import com.onesignal.sdktest.notification.OneSignalNotificationSender;
import com.onesignal.sdktest.util.SharedPreferenceUtil;

import org.json.JSONObject;

public class MainApplication extends MultiDexApplication {
    private static final int SLEEP_TIME_TO_MIMIC_ASYNC_OPERATION = 2000;

    public MainApplication() {
        // run strict mode default in debug mode to surface any potential issues easier
        if(BuildConfig.DEBUG)
            StrictMode.enableDefaults();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        OneSignal.getDebug().setLogLevel(LogLevel.DEBUG);

        // OneSignal Initialization
        String appId = SharedPreferenceUtil.getOneSignalAppId(this);
        // If cached app id is null use the default, otherwise use cached.
        if (appId == null) {
            appId = getString(R.string.onesignal_app_id);
            SharedPreferenceUtil.cacheOneSignalAppId(this, appId);
        }

        OneSignalNotificationSender.setAppId(appId);
        OneSignal.initWithContext(this, appId);

        OneSignal.getInAppMessages().setInAppMessageLifecycleHandler(new IInAppMessageLifecycleHandler() {
            @Override
            public void onWillDisplayInAppMessage(@NonNull IInAppMessage message) {
                Log.v(Tag.LOG_TAG, "onWillDisplayInAppMessage");
            }

            @Override
            public void onDidDisplayInAppMessage(@NonNull IInAppMessage message) {
                Log.v(Tag.LOG_TAG, "onDidDisplayInAppMessage");
            }

            @Override
            public void onWillDismissInAppMessage(@NonNull IInAppMessage message) {
                Log.v(Tag.LOG_TAG, "onWillDismissInAppMessage");
            }

            @Override
            public void onDidDismissInAppMessage(@NonNull IInAppMessage message) {
                Log.v(Tag.LOG_TAG, "onDidDismissInAppMessage");
            }
        });

        OneSignal.getInAppMessages().setInAppMessageClickHandler(new IInAppMessageClickHandler() {
            @Override
            public void inAppMessageClicked(@Nullable IInAppMessageClickResult result) {
                Log.v(Tag.LOG_TAG, "INotificationClickListener.inAppMessageClicked");
            }
        });

        OneSignal.getNotifications().addClickListener(event ->
        {
            Log.v(Tag.LOG_TAG, "INotificationClickListener.onClick fired" +
                    " with event: " + event);
        });

        OneSignal.getNotifications().addForegroundLifecycleListener(new INotificationLifecycleListener() {
            @Override
            public void onWillDisplay(@NonNull INotificationWillDisplayEvent event) {
                Log.v(Tag.LOG_TAG, "INotificationLifecycleListener.onWillDisplay fired" +
                        " with event: " + event);

                IDisplayableNotification notification = event.getNotification();
                JSONObject data = notification.getAdditionalData();

                //Prevent OneSignal from displaying the notification immediately on return. Spin
                //up a new thread to mimic some asynchronous behavior, when the async behavior (which
                //takes 2 seconds) completes, then the notification can be displayed.
                event.preventDefault();
                Runnable r = () -> {
                    try {
                        Thread.sleep(SLEEP_TIME_TO_MIMIC_ASYNC_OPERATION);
                    } catch (InterruptedException ignored) {
                    }

                    notification.display();
                };

                Thread t = new Thread(r);
                t.start();
            }
        });

        OneSignal.getInAppMessages().setPaused(true);
        OneSignal.getLocation().setShared(false);

        Log.d(Tag.LOG_TAG, Text.ONESIGNAL_SDK_INIT);
    }

}
