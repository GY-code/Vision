package t20220049.sw_vision.view;

public class VideoFragment {
    public double startTime;
    public double durance;

    public String filename;

    public VideoFragment(double startTime, double durance, String filename) {
        this.startTime = startTime;
        this.durance = durance;
        this.filename = filename;
    }
}
