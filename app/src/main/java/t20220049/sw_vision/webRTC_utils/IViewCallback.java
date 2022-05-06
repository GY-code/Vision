package t20220049.sw_vision.webRTC_utils;

import org.webrtc.MediaStream;

/**
 * Created by GuYi on 2017/10/23.
 */

public interface IViewCallback {

    void onSetLocalStream(MediaStream stream, String socketId);

    void onAddRemoteStream(MediaStream stream, String socketId);

    void onCloseWithId(String socketId);

}
