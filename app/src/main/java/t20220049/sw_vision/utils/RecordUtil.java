package t20220049.sw_vision.utils;

import android.content.Context;

import java.io.File;


public class RecordUtil {
    Context context;
    String srcPath= context.getFilesDir().getAbsolutePath() + "/";;
    public RecordUtil( Context c){
        context=c;
    }
    public void clear(){
        File file = new File(srcPath + "local.y4m");
        if (file.isFile() && file.exists()) {
            file.delete();
        }
    }
}
