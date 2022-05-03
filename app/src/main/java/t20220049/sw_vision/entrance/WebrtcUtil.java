package t20220049.sw_vision.entrance;

import android.app.Activity;
import android.text.TextUtils;

import t20220049.sw_vision.ui.CollectActivity;
import t20220049.sw_vision.webRTC_utils.WebRTCManager;
import t20220049.sw_vision.bean.MediaType;
import t20220049.sw_vision.bean.MyIceServer;
import t20220049.sw_vision.ui.ControlActivity;
import t20220049.sw_vision.ws.IConnectEvent;


public class WebrtcUtil {


//    public static final String HOST = "47.93.186.97";
    public static final String HOST = "106.13.236.207";

    // turn and stun
    private static MyIceServer[] iceServers = {
            new MyIceServer("stun:stun.l.google.com:19302"),

            // 测试地址1
            new MyIceServer("stun:" + HOST + ":3000?transport=udp"),
            new MyIceServer("turn:" + HOST + ":3000?transport=udp",
                    "ddssingsong",
                    "123456"),
            new MyIceServer("turn:" + HOST + ":3000?transport=tcp",
                    "ddssingsong",
                    "123456"),
    };

    // signalling
//    private static String WSS = "wss://" + HOST + "/wss";
    //本地测试信令地址
     private static String WSS = "ws://106.13.236.207:3000";

    // one to one
    public static void callSingle(Activity activity, String wss, String roomId, boolean videoEnable,boolean watchMode) {
        if (TextUtils.isEmpty(wss)) {
            wss = WSS;
        }
        WebRTCManager.getInstance().init(wss, iceServers, new IConnectEvent() {
            @Override
            public void onSuccess() {
                CollectActivity.openActivity(activity, videoEnable,watchMode);
            }

            @Override
            public void onFailed(String msg) {

            }
        });
        WebRTCManager.getInstance().connect(videoEnable ? MediaType.TYPE_VIDEO : MediaType.TYPE_AUDIO, roomId);
    }

    // Videoconferencing
    public static void call(Activity activity, String wss, String roomId) {
        if (TextUtils.isEmpty(wss)) {
            wss = WSS;
        }
        WebRTCManager.getInstance().init(wss, iceServers, new IConnectEvent() {
            @Override
            public void onSuccess() {
                ControlActivity.openActivity(activity);
//                ControlVideo.openActivity(activity);
            }

            @Override
            public void onFailed(String msg) {

            }
        });
        WebRTCManager.getInstance().connect(MediaType.TYPE_MEETING, roomId);
    }


}
