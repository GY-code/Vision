package t20220049.sw_vision;

public class Device {
    public String ip = null;
    float currentX;
    float currentY;
    String deviceName="Oneplus 9 RT";
    String nickName="Lily";
//    0-未连接 1-正在连接 2-已连接
    int status;
    Device() { }

    Device(int x,int y){
        currentX=x;
        currentY=y;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
