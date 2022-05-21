package t20220049.sw_vision.webRTC_utils;

import android.os.Handler;

import org.webrtc.VideoSink;


public interface mVideoSink extends VideoSink {

    void setTarget(VideoSink target);
    void setSurfaceHandler(Handler handler);

}
