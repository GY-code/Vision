package t20220049.sw_vision.transfer;

import android.app.Application;

import t20220049.sw_vision.transfer.app.AppMaster;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppMaster.getInstance().setApp(MyApplication.this);
    }

}
