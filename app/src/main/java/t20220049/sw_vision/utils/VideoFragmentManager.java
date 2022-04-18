package t20220049.sw_vision.utils;

import java.util.ArrayList;

public class VideoFragmentManager {
    private static VideoFragmentManager instance;
    private volatile static ArrayList<VideoFragment> fragments;

    private VideoFragmentManager() {
        fragments = null;
    }

    public static VideoFragmentManager getInstance() {
        if (instance == null) {
            synchronized (VideoFragmentManager.class) {
                if (instance == null) {
                    instance = new VideoFragmentManager();
                }
            }
        }

        return instance;
    }

    public synchronized boolean isComplete() {
        return fragments != null;
    }

    public synchronized void setFragments(ArrayList<VideoFragment> list) {
        fragments = list;
    }

    public ArrayList<VideoFragment> getFragments() {
        return fragments;
    }

    public synchronized void clear() {
        fragments = null;
    }

}
