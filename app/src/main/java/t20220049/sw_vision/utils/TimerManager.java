package t20220049.sw_vision.utils;

public class TimerManager {
    private static double startTime;
    private static double recordTime;
    private static TimerManager instance;

    private TimerManager() {
        startTime = System.currentTimeMillis();
        recordTime = 0;
    }

    public static TimerManager getInstance() {
        if (instance == null) {
            synchronized (TimerManager.class) {
                if (instance == null) {
                    instance = new TimerManager();
                }
            }
        }

        return instance;
    }

    public void restart() {
        startTime = System.currentTimeMillis();
        recordTime = 0;
    }

    public double[] cut() {
        double endTime = System.currentTimeMillis();
        double durance = (endTime - startTime) / 1000;

        double recordOut = recordTime;
        recordTime += durance;
        startTime = endTime;

        return new double[]{recordOut, durance};
    }

}
