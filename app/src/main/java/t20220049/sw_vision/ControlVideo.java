package t20220049.sw_vision;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.util.ArrayList;
import java.util.List;

public class ControlVideo extends AppCompatActivity {
    public class Device {
        String type;
        String name;
    }

    RelativeLayout bottomSheet;
    BottomSheetBehavior behavior;
    RecyclerView v1;
    deviceAdapter deviceAdapter;
    List<Device> mDevicesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acticity_video);
        //底部抽屉栏展示地址
        bottomSheet = findViewById(R.id.bottom_sheet);

//        bottomSheet.getBackground().setAlpha(60);
        behavior = BottomSheetBehavior.from(bottomSheet);
        v1 = findViewById(R.id.recyclerview);

        for (int i = 0; i < 20; i++) {
            Device device = new Device();
            device.type = "标题" + i;
            device.name = "内容" + i;
            mDevicesList.add(device);
        }

        deviceAdapter = new deviceAdapter();
        v1.setAdapter(deviceAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(ControlVideo.this);
        v1.setLayoutManager(layoutManager);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, @BottomSheetBehavior.State int newState) {
//                String state = "null";
//                switch (newState) {
//                    case 1:
//                        state = "STATE_DRAGGING";//过渡状态此时用户正在向上或者向下拖动bottom sheet
//                        behavior.setState(newState);
//                        break;
//                    case 2:
//                        state = "STATE_SETTLING"; // 视图从脱离手指自由滑动到最终停下的这一小段时间
//                        break;
//                    case 3:
//                        state = "STATE_EXPANDED"; //处于完全展开的状态
//                        break;
//                    case 4:
//                        state = "STATE_COLLAPSED"; //默认的折叠状态
//                        break;
//                    case 5:
//                        state = "STATE_HIDDEN"; //下滑动完全隐藏 bottom sheet
//                        break;
//                }
                behavior.setState(newState);

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                Log.d("BottomSheetDemo", "slideOffset:" + slideOffset);
            }
        });



    }

    class deviceAdapter extends RecyclerView.Adapter<MyViewHolder> {
        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = View.inflate(ControlVideo.this, R.layout.device_list, null);
            MyViewHolder myViewHolder = new MyViewHolder(view);
            return myViewHolder;
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Device device = mDevicesList.get(position);
            holder.mType.setText(device.type);
            holder.mName.setText(device.name);
        }

        @Override
        public int getItemCount() {
            return mDevicesList.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView mType;
        TextView mName;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mType = itemView.findViewById(R.id.txt_mType);
            mName = itemView.findViewById(R.id.txt_mName);
        }
    }
}
