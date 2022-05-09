package t20220049.sw_vision.arm_controller;

import android.util.Log;

import t20220049.sw_vision.ui.SendFileActivity;

public class ControlCenter {

    private final static ControlCenter INSTANCE = new ControlCenter();
    private ControlCenter(){}
    public static ControlCenter getInstance(){
        return INSTANCE;
    }
    //arm control
    private int btmKp = 10;
    private int topKp = 10;
    private double offsetX;
    private double offsetY;
    private double offsetDeadBlock = 0.1; //偏移量死区大小
    private int lastBtmDegree = 90; //上一次底部舵机的角度
    private int lastTopDegree = 0; //上一次顶部舵机的角度
    public static final String TAG = "ControlCenter";

    //500~2500: 0x01f4~0x09c4
    //常量
    final byte FRAME_HEAD = 0x55;
    final byte FRAME_LENGTH = (byte) 0x0b;
    final byte FRAME_TYPE = 0x03;
    final byte SERVO_NUM = 0x02;
    final byte TIME_LOW = (byte) 0xe8;
    final byte TIME_HIGH = (byte) 0x03;
    //变量
    byte servoId1 = 0x01;
    byte positionLow1 = (byte)0x00;
    byte positionHigh1 = (byte)0x00;

    byte servoId2 = 0x02;
    byte positionLow2 = (byte)0xdc;
    byte positionHigh2 = (byte)0x05;

    private int calBtmServoDegree(){
        //设置阈值
        if (Math.abs(offsetX) < offsetDeadBlock) {
            offsetX = 0;
        }
        //delta 范围-50~50
        double deltaDegree = offsetX * btmKp;
        Log.e(TAG,"deltaBtm: "+deltaDegree);
        //计算更新的底部舵机角度
        double nextBtmDegree = lastBtmDegree - deltaDegree;
        Log.e(TAG,"nBtm: "+nextBtmDegree);
        //边界检测
        if(nextBtmDegree < 0){
            nextBtmDegree = 0;
        }else if(nextBtmDegree >180){
            nextBtmDegree = 180;
        }
        lastBtmDegree = (int) nextBtmDegree;
        return (int) (11.11*nextBtmDegree+500);
    }

    private int calTopServoDegree(){
        //设置阈值
        if (Math.abs(offsetY) < offsetDeadBlock) {
            offsetY = 0;
        }
        //delta 范围-50~50
        double deltaDegree = offsetY * topKp;
        Log.e(TAG,"deltaTop: "+deltaDegree);
        //计算更新的顶部舵机角度
        double nextTopDegree = lastTopDegree + deltaDegree;
        Log.e(TAG,"nTop: "+nextTopDegree);
        //边界检测
        if(nextTopDegree < 0){
            nextTopDegree = 0;
        }else if(nextTopDegree >180){
            nextTopDegree = 180;
        }
        lastTopDegree = (int) nextTopDegree;
        return (int) (11.11*nextTopDegree+500);
    }

    public void moveArm(double x, double y) {
        x = 1 - x;
        offsetX = (x - 0.5) * 2;
        offsetY = (y - 0.5) * 2;
        Log.e(TAG, "Detect face width: " + offsetX + ", height: " + offsetY);
        int top = calTopServoDegree();
        int btm = calBtmServoDegree();
        Log.e(TAG,"topDe: "+top +", btmDe: "+btm);
        updatePositionValue(top,btm);
        makeArmsMove();
    }

    private void updatePositionValue(int nextTopDegree,int nextBtmDegree){
        //1500
        positionHigh1 = (byte) (nextTopDegree >> 8);
        positionLow1 = (byte) (nextTopDegree & 0b11111111);

        positionHigh2 = (byte) (nextBtmDegree >> 8);
        positionLow2 = (byte) (nextBtmDegree & 0b11111111);
    }

    public void updatePosition(byte pl1,byte ph1, byte pl2,byte ph2){
        positionLow1 = pl1;
        positionHigh1 = ph1;
        positionLow2 = pl2;
        positionHigh2 = ph2;
    }

    public void makeArmsMove(){
        byte[] bytes = {
                FRAME_HEAD,FRAME_HEAD,FRAME_LENGTH,FRAME_TYPE,SERVO_NUM,TIME_LOW,TIME_HIGH,
                servoId1,positionLow1,positionHigh1,
                servoId2,positionLow2,positionHigh2
        };
        SendFileActivity.sendActionCmd(bytes);
    }

    private int cal(int a){
        return (int)(a*11.11+500);
    }

    public void moveUp(){
        int nextTopDegree = lastTopDegree - 20;
        if(nextTopDegree < 0){
            nextTopDegree = 0;
        }
        updatePositionValue(cal(nextTopDegree),cal(lastBtmDegree));
        lastTopDegree = nextTopDegree;
        makeArmsMove();
    }

    public void moveDown(){
        int nextTopDegree = lastTopDegree + 20;
        if(nextTopDegree > 180){
            nextTopDegree = 180;
        }
        updatePositionValue(cal(nextTopDegree),cal(lastBtmDegree));
        lastTopDegree = nextTopDegree;
        makeArmsMove();
    }

    public void moveLeft(){
        int nextBtmDegree = lastBtmDegree + 20;
        if(nextBtmDegree > 180){
            nextBtmDegree = 180;
        }
        updatePositionValue(cal(lastTopDegree),cal(nextBtmDegree));
        lastBtmDegree = nextBtmDegree;
        makeArmsMove();
    }

    public void moveRight(){
        int nextBtmDegree = lastBtmDegree - 20;
        if(nextBtmDegree < 0){
            nextBtmDegree = 0;
        }
        updatePositionValue(cal(lastTopDegree),cal(nextBtmDegree));
        lastBtmDegree = nextBtmDegree;
        makeArmsMove();
    }
}
