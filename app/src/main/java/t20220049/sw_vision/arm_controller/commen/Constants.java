package t20220049.sw_vision.arm_controller.commen;

/**
 * 常量类
 */
public class Constants {

    /**
     * 消息id类
     *
     * @author lucas
     */
    public static class MessageID {
        /**
         * 请求连接
         */
        public static final int MSG_REQUEST_CONNECT = 1;
        /**
         * 蓝牙状态改变
         */
        public static final int MSG_STATE_CHANGE = MSG_REQUEST_CONNECT + 1;
        /**
         * 连接成功
         */
        public static final int MSG_CONNECT_SUCCEED = MSG_STATE_CHANGE + 1;
        /**
         * 连接失败
         */
        public static final int MSG_CONNECT_FAILURE = MSG_CONNECT_SUCCEED + 1;
        /**
         * 重新连接
         */
        public static final int MSG_CONNECT_RECONNECT = MSG_CONNECT_FAILURE + 1;
        /**
         * 连接断开
         */
        public static final int MSG_CONNECT_LOST = MSG_CONNECT_RECONNECT + 1;
        /**
         * 发送数据
         */
        public static final int MSG_SEND_DATA = MSG_CONNECT_LOST + 1;
        /**
         * 发送字符串
         */
        public static final int MSG_SEND_STRING = MSG_SEND_DATA + 1;
        /**
         * 发送指令集
         */
        public static final int MSG_SEND_COMMAND_LIST = MSG_SEND_STRING + 1;
        /**
         * 发送指令集，不可取消
         */
        public static final int MSG_SEND_COMMAND_STOP = MSG_SEND_COMMAND_LIST + 1;
        /**
         * 发送指令时，蓝牙未连接
         */
        public static final int MSG_SEND_NOT_CONNECT = MSG_SEND_COMMAND_STOP + 1;
        /**
         * 发送成功
         */
        public static final int MSG_SEND_SUCCESS = MSG_SEND_NOT_CONNECT + 1;
        /**
         * 已处理（可能未处理完成）
         */
        public static final int MSG_SEND_HANDLED = MSG_SEND_SUCCESS + 1;
        /**
         * 发送失败
         */
        public static final int MSG_SEND_FAILURE = MSG_SEND_HANDLED + 1;
        /**
         * 接收到数据
         */
        public static final int MSG_RECEIVE_DATA = MSG_SEND_FAILURE + 1;
        /**
         * 发送指令
         */
        public static final int MSG_SEND_COMMAND = MSG_RECEIVE_DATA + 1;
        /**
         * 隐藏语音结果显示文本
         */
        public static final int MSG_HIDE_RESULT = MSG_SEND_COMMAND + 1;
        /**
         * 更新语音结果显示文本
         */
        public static final int MSG_UPDATE_RESULT = MSG_HIDE_RESULT + 1;


        public static final int MSG_SEND_BYTECOMMAND = MSG_UPDATE_RESULT + 1;

    }
}
