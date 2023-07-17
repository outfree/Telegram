package org.telegram.messenger;

import com.google.firebase.messaging.FirebaseMessaging;
import com.huawei.hms.push.HmsMessaging;

import org.telegram.messenger.huawei.BuildConfig;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.GoogleLocationProvider;
import org.telegram.messenger.GoogleMapsProvider;
import org.telegram.messenger.ILocationServiceProvider;
import org.telegram.messenger.IMapsProvider;
import org.telegram.messenger.PushListenerController;

public class HuaweiApplicationLoader extends ApplicationLoader {
    @Override
    protected boolean isHuaweiBuild() {
        return true;
    }

    @Override
    protected PushListenerController.IPushListenerServiceProvider onCreatePushProvider() {
        if (PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices()) {
            HmsMessaging.getInstance(this).setAutoInitEnabled(false);
            FirebaseMessaging.getInstance().setAutoInitEnabled(true);
            return PushListenerController.GooglePushListenerServiceProvider.INSTANCE;
        }
        HmsMessaging.getInstance(this).setAutoInitEnabled(true);
        FirebaseMessaging.getInstance().setAutoInitEnabled(false);
        return HuaweiPushListenerProvider.INSTANCE;
    }

    @Override
    protected ILocationServiceProvider onCreateLocationServiceProvider() {
        if (PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices()) {
            return new GoogleLocationProvider();
        }
        return new HuaweiLocationProvider();
    }

    @Override
    protected IMapsProvider onCreateMapsProvider() {
        if (PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices()) {
            return new GoogleMapsProvider();
        }
        return new HuaweiMapsProvider();
    }

    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
}
