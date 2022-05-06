package t20220049.sw_vision.arm_controller.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 指令模型类
 * Created by lucas on 2022/4/25.
 */
public class Command {

    String commandStr;

    long delay;

    public static class Builder {

        private List<Command> commandList;

        public Builder() {
            this.commandList = new ArrayList<>();
        }

        public Builder addCommand(String commandStr, long delay) {
            commandList.add(new Command(commandStr, delay));
            return this;
        }

        public List<Command> createCommands() {
            return commandList;
        }
    }

    public Command() {
    }

    public Command(String commandStr) {
        this.commandStr = commandStr;
        this.delay = 0;
    }

    public Command(String commandStr, long delay) {
        this.commandStr = commandStr;
        this.delay = delay;
    }

    public String getCommandStr() {
        return commandStr;
    }

    public void setCommandStr(String commandStr) {
        this.commandStr = commandStr;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    @Override
    public String toString() {
        return "{ command = " + commandStr + ", delay = " + delay + "}";
    }
}
