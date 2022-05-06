package t20220049.sw_vision.webRTC_utils;

/**
 * Created by GuYi on 2022/7/24.
 * android_shuai@163.com
 */
public enum CallState {
    Idle,
    Outgoing,
    Incoming,
    Connecting,
    Connected;

    private CallState() {
    }
}