package t20220049.sw_vision.arm_controller.uitls;

import java.util.UUID;

/**
 * 配置信心常量类
 * Created by lucas on 2022/4/22.
 */
public interface Configs {
    /**
     * 字符串编码
     */
    String CHAR_SET = "UTF-8";
    /**
     * 蓝牙串口连接的UUID
     */
    UUID LOROT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    String BLUETOOTH_PIN_STRING = "1234";

    int CHANGE_INTERVAL = 50;
    int SEND_INTERVAL = 400;
    String NEW_LINE = "\r\n";
    /**
     * 命令，变换
     */
    String COMMAND_CHANGE = "PL0" +NEW_LINE;
    /**
     * 命令，左
     */
    String COMMAND_LEFT = "PL0SQ15SM100ONCE" +NEW_LINE;
    /**
     * 命令，上（准备）
     */
    String COMMAND_START_UP = "PL0SQ1SM100ONCE" +NEW_LINE;
    /**
     * 命令，上
     */
    String COMMAND_UP = "PL0SQ2SM100ONCE" +NEW_LINE;
    /**
     * 命令，右
     */
    String COMMAND_RIGHT = "PL0SQ16SM100ONCE" +NEW_LINE;
    /**
     * 命令，收腿
     */
    String COMMAND_STOP = "PL0SQ3SM100ONCE" +NEW_LINE;
    /**
     * 命令，下（准备）
     */
    String COMMAND_START_DOWN = "PL0SQ4SM100ONCE" +NEW_LINE;
    /**
     * 命令，下
     */
    String COMMAND_DOWN = "PL0SQ5SM100ONCE" +NEW_LINE;
    /**
     * 命令，下（停止）
     */
    String COMMAND_DOWN_STOP = "PL0SQ6SM100ONCE" +NEW_LINE;
    /**
     * 命令，左转
     */
    String COMMAND_TURN_LEFT = "PL0SQ7SM100ONCE" +NEW_LINE;
    /**
     * 命令，右转
     */
    String COMMAND_TURN_RIGHT = "PL0SQ8SM100ONCE" +NEW_LINE;

}
