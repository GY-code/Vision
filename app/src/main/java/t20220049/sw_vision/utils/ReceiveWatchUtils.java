package t20220049.sw_vision.utils;

import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import t20220049.sw_vision.ui.CollectActivity;

public class ReceiveWatchUtils {
    public static boolean isActive = false;
    private static WeakReference<CollectActivity> CollectActivityWeakRef;
    private static final String TAG = "WatchUtils";

    public static void setBaseActivityWeakRef(CollectActivity activity) {
        CollectActivityWeakRef = new WeakReference<>(activity);
    }

    static Boolean pollThreadFlag = true;
    static Thread pollThread ;

    public static void activeWatch() {
        if (isActive)
            return;
        pollThreadFlag = true;
        pollThread = new Thread(() -> {
            while (pollThreadFlag) {
                try {
                    PollRequest();
                    Thread.sleep(500);
                    //这里可以写你自己要运行的逻辑代码
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        });
        pollThread.start();
        isActive=true;
    }

    public static void inactiveWatch() {
        if (!isActive)
            return;
        pollThreadFlag = false;
        pollThread.interrupt();
        isActive=false;
    }


    static void PollRequest() {
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("poll", "polling");
        String json = jsonParam.toJSONString();
        MediaType mediaType = MediaType.Companion.parse("application/json;charset=utf-8");
        RequestBody requestBody = RequestBody.Companion.create(json, mediaType);
        sendOkHttpResponse("http://192.168.3.45:8000/server/polling/", requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure ");
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String data = response.body().string();
                //服务器返回信息做对应处理
                if (data.equals("photo")) {
                    CollectActivityWeakRef.get().CallTakePicture(true, false);
                } else if (data.equals("video")) {
                    if (!CollectActivity.activateVideo) {
                        CollectActivityWeakRef.get().CallSetVideoStart();
                        CollectActivity.activateVideo = true;
                    } else {
                        CollectActivityWeakRef.get().CallSetVideoEnd(true, false);
                        CollectActivity.activateVideo = false;
                    }
                }
            }
        });
    }

    //提供一个静态方法，当别的地方需要发起网络请求时，简单的调用这个方法即可
    //请求实例
    //OKHttp请求
    //callback是okhttp自带的回调接口，这里写的是使用GET方式获取服务器数据
    private static int TimeOut = 120;
    //单例获取ohttp3对象
    private static OkHttpClient client = null;

    private static synchronized OkHttpClient getInstance() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .readTimeout(TimeOut, TimeUnit.SECONDS)
                    .connectTimeout(TimeOut * 10, TimeUnit.SECONDS)
                    .writeTimeout(TimeOut * 10, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    //使用POST方式向服务器提交数据并获取返回提示数据
    public static void sendOkHttpResponse(final String address,
                                          final RequestBody requestBody, final Callback callback) {
        client = getInstance();
        //JSONObject这里是要提交的数据部分
        Request request = new Request.Builder()
                .url(address)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
