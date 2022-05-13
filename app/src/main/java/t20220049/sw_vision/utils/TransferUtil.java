package t20220049.sw_vision.utils;

import android.content.Context;

import java.util.ArrayList;

import t20220049.sw_vision.transfer.client.WifiClientService;
import t20220049.sw_vision.transfer.client.WifiClientTask;
import t20220049.sw_vision.transfer.server.WifiServer;
import t20220049.sw_vision.transfer.server.WifiServerService;

public class TransferUtil {
    static ArrayList<WifiServer.MyClient> clients = WifiServer.clients;

    //采集端给控制端发送信息，先指令，后内容
    public static void C2S_UserID(String id) {
        new Thread(() -> {
            if (WifiClientService.serverOut != null) {
                WifiClientService.serverOut.println("userID");
                WifiClientService.serverOut.flush();
                WifiClientService.serverOut.println(id);
                WifiClientService.serverOut.flush();
            }
        }).start();
    }

    //控制端给采集端发指令
    public static void S2C(String instruction) {
        for (WifiServer.MyClient mc : clients) {
            new Thread(() -> {
                WifiServer.sendInstruction(instruction, mc.clientIP);
            }).start();
        }
    }

    public static void C2S_Photo(String path, Context context) {
        if (WifiClientService.socket != null) {
            new WifiClientTask(context, true).execute(path, "photo");
        }
    }

    public static void C2S_Video(String path, Context context) {
        if (WifiClientService.socket != null) {
            new WifiClientTask(context, true).execute(path, "video");
        }
    }
}
