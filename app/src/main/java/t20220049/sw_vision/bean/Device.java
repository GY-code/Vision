package t20220049.sw_vision.bean;

public class Device {
    public String ip = null;
    public float currentX;
    public float currentY;
    public String deviceName = "Oneplus 9 RT";
    public String nickName = "Lily";
    //    0-未连接 1-正在连接 2-已连接
    public int status;

    Device() {
    }

    public Device(int x, int y) {
        currentX = x;
        currentY = y;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
