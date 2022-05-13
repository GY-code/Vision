package t20220049.sw_vision.arm_controller;

import android.util.Log;

import t20220049.sw_vision.ui.SendFileActivity;

public class ControlCenter {

    private int count = 0;

    private final static ControlCenter INSTANCE = new ControlCenter();
    private ControlCenter(){}
    public static ControlCenter getInstance(){
        return INSTANCE;
    }


    //arm control
    private int btmKp = 4;
    private int topKp = 4;

    private double offsetX;
    private double offsetY;
    private double offsetDeadBlock = 0.1; //偏移量死区大小
    private int lastBtmDegree = 90; //上一次底部舵机的角度
    private int lastTopDegree = 90; //上一次顶部舵机的角度 初始90（1500）映射后范围：1900-800
    public static final String TAG = "ControlCenter";

    //十六进制

    //500~2500: 0x01f4~0x09c4
    //常量
    final byte FRAME_HEAD = 0x55;
    final byte FRAME_LENGTH = (byte) 0x0b;
    final byte FRAME_TYPE = 0x03;
    final byte SERVO_NUM = 0x02;

    //变量
    byte servoId1 = 0x01;
    byte positionLow1 = (byte)0xdc;
    byte positionHigh1 = (byte)0x05;


    byte servoId2 = 0x02;
    byte positionLow2 = (byte)0xdc;
    byte positionHigh2 = (byte)0x05;

    private final int MOVE_STATE = 0;
    private final int CONTROL_STATE = 1;
    /*
     * 根据offsetX返回更新后的下舵机角度（十进制映射到500-2500）
     * */

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
        int result = (int) (11.11*nextBtmDegree+500);
        return result;
    }

    /*
    * 根据offsetY返回更新后的上舵机角度（十进制映射到500-2500）
    * */
     private int calTopServoDegree(){

        //设置阈值
        if (Math.abs(offsetY) < offsetDeadBlock) {
            offsetY = 0;
        }
        //delta 范围-50~50
        double deltaDegree = offsetY * topKp;
        Log.e(TAG,"deltaTop: "+deltaDegree);
        //计算更新的顶部舵机角度
        double nextTopDegree = lastTopDegree - deltaDegree;

        Log.e(TAG,"nTop: "+nextTopDegree);
        //边界检测
        if(nextTopDegree < 0){
            nextTopDegree = 0;
        }else if(nextTopDegree >180){
            nextTopDegree = 180;
        }
        lastTopDegree = (int) nextTopDegree;
        int result = (int) (11.11*nextTopDegree+500);
        if(result > 1900){
            result = 1900;
        }else if(result < 800){
            result = 800;
        }
        return result;
    }

    public void moveArm(double x, double y) {
        count++;
        count = count % 30000;
        if (count % 3 != 0){
             return;
        }

        x = 1 - x;
        offsetX = (x - 0.5) * 2;
        offsetY = (y - 0.5) * 2;
        Log.e(TAG, "Detect face width: " + offsetX + ", height: " + offsetY);
        int top = calTopServoDegree();
        int btm = calBtmServoDegree();
        Log.e(TAG,"topDe: "+top +", btmDe: "+btm);
        updatePositionValue(top,btm);
        makeArmsMove(CONTROL_STATE);
    }

    /*
    * 更新舵机角度（十六进制）
    * */

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

    public void makeArmsMove(int type){
        byte TIME_LOW,TIME_HIGH;
        if(type == CONTROL_STATE){
             TIME_LOW = (byte) 0x32;
             TIME_HIGH = (byte) 0x00;
        }else {
             TIME_LOW = (byte) 0x20;
             TIME_HIGH = (byte) 0x03;
        }

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
        int nextTopDegree = lastTopDegree - 15;

        if(nextTopDegree < 0){
            nextTopDegree = 0;
        }
        updatePositionValue(cal(nextTopDegree),cal(lastBtmDegree));
        lastTopDegree = nextTopDegree;
        makeArmsMove(MOVE_STATE);
    }

    public void moveDown(){
        int nextTopDegree = lastTopDegree + 15;

        if(nextTopDegree > 180){
            nextTopDegree = 180;
        }
        updatePositionValue(cal(nextTopDegree),cal(lastBtmDegree));
        lastTopDegree = nextTopDegree;
        makeArmsMove(MOVE_STATE);
    }

    public void moveLeft(){
        int nextBtmDegree = lastBtmDegree + 15;

        if(nextBtmDegree > 180){
            nextBtmDegree = 180;
        }
        updatePositionValue(cal(lastTopDegree),cal(nextBtmDegree));
        lastBtmDegree = nextBtmDegree;
        makeArmsMove(MOVE_STATE);
    }

    public void moveRight(){
        int nextBtmDegree = lastBtmDegree - 15;

        if(nextBtmDegree < 0){
            nextBtmDegree = 0;
        }
        updatePositionValue(cal(lastTopDegree),cal(nextBtmDegree));
        lastBtmDegree = nextBtmDegree;
        makeArmsMove(MOVE_STATE);

    }
}
