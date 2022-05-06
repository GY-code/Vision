package t20220049.sw_vision.arm_controller.widget;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import t20220049.sw_vision.R;
//import t20220049.sw_vision.arm_controller.BluetoothActivity;
import t20220049.sw_vision.arm_controller.uitls.BluetoothUtils;
import t20220049.sw_vision.arm_controller.uitls.LogUtil;

import java.util.ArrayList;
import java.util.Set;

/**
 * 蓝牙搜索对话框
 *
 * @author Lucas
 */
public class SearchDialog extends DialogFragment implements OnClickListener, OnItemClickListener {

    private static final String TAG = SearchDialog.class.getSimpleName();
    /**
     * 搜索时间，60s
     */
    private static final int SCAN_TIMEOUT = 60000;

    private TextView titleTV;

    private CircularProgressView progressView;

    private BluetoothDataAdapter mAdapter;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static OnDeviceSelectedListener onDeviceSelectedListener;
    private boolean scanning = false;
    private Handler mHandler;

    private BluetoothAdapter.LeScanCallback leCallBack = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            LogUtil.i(TAG, "found device : " + device.getName() + ", address = " + device.getAddress());
            if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                mAdapter.add(device);
        }
    };

    /**
     * 搜索BLE蓝牙设备
     */

    public void scanBLEDevice() {
        if (mHandler != null && mBluetoothAdapter != null) {
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    stopScan();
                }
            }, SCAN_TIMEOUT);
            mBluetoothAdapter.startLeScan(leCallBack);
            titleTV.setText(R.string.dialog_searching);
            progressView.setVisibility(View.VISIBLE);
            progressView.resetAnimation();
            scanning = true;
        }
    }

    /**
     * 停止搜索
     */
    public void stopScan() {
        if (mBluetoothAdapter != null) {
            if (scanning) {
                mBluetoothAdapter.stopLeScan(leCallBack);
                titleTV.setText(R.string.dialog_search_finished);
                progressView.setVisibility(View.GONE);
                titleTV.setText(R.string.dialog_search_finished);
                scanning = false;
            }
        }
    }

    public static void createDialog(FragmentManager fragmentManager, OnDeviceSelectedListener onDeviceSelectedListener) {
        SearchDialog.onDeviceSelectedListener = onDeviceSelectedListener;
        SearchDialog dialog = new SearchDialog();
        dialog.show(fragmentManager, "searchDialog");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, getTheme());
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_dialog, container, false);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleTV = (TextView) view.findViewById(R.id.dialog_title);
        ListView listView = (ListView) view.findViewById(R.id.device_list);
        progressView = (CircularProgressView) view.findViewById(R.id.dialog_search_progress);
        Button cancelBtn = (Button) view.findViewById(R.id.dialog_left_btn);
        Button restartBtn = (Button) view.findViewById(R.id.dialog_right_btn);

        mAdapter = new BluetoothDataAdapter(getActivity());
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        titleTV.setText(R.string.dialog_searching);
        cancelBtn.setOnClickListener(this);
        restartBtn.setOnClickListener(this);

        scanBLEDevice();
    }

    public void onDestroyView() {
        super.onDestroyView();
        stopScan();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dialog_right_btn:
                stopScan();
                mAdapter.clear();
                scanBLEDevice();
                break;
            case R.id.dialog_left_btn:
                dismissAllowingStateLoss();
                break;
        }
    }

    class BluetoothDataAdapter extends BaseAdapter {

        private ArrayList<BluetoothDevice> devices;

        private Context context;

        public BluetoothDataAdapter(Context context) {
            this.context = context;
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            devices = new ArrayList<>();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    devices.add(device);
                }
            }
        }

        public void add(BluetoothDevice device) {
            if (!devices.contains(device)) {
                this.devices.add(device);
                notifyDataSetChanged();
            }
        }

        public void removePair(int pos) {
            devices.remove(pos);
            notifyDataSetChanged();
        }

        public void clear() {
            devices.clear();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            devices = new ArrayList<>();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    devices.add(device);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder holder = new ViewHolder();

            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                view = inflater.inflate(R.layout.item_search_device, parent,
                        false);
                holder.titleView = (TextView) view.findViewById(R.id.item_device_title);
                holder.contentView = (TextView) view.findViewById(R.id.item_device_content);
                holder.deleteBtn = (ImageButton) view.findViewById(R.id.item_device_delete);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = devices.get(position);
            String title;
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                title = getString(R.string.dialog_search_item_bonded, device.getName());
                holder.deleteBtn.setVisibility(View.VISIBLE);
                holder.deleteBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = (int) v.getTag();
                        if (BluetoothUtils.removePaired(getItem(pos))) {
                            removePair(pos);
                        }
                    }
                });
            } else {
                title = device.getName();
                holder.deleteBtn.setVisibility(View.INVISIBLE);
            }
            holder.titleView.setText(title);
            holder.contentView.setText(device.getAddress());
            holder.deleteBtn.setTag(position);
            return view;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return devices.get(position);
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        class ViewHolder {
            TextView titleView;
            TextView contentView;
            ImageButton deleteBtn;
        }
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(BluetoothDevice device);
    }

    /**
     * @see OnItemClickListener#onItemClick(AdapterView,
     * View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        if (onDeviceSelectedListener != null) {
            BluetoothDevice device = mAdapter.getItem(position);
            onDeviceSelectedListener.onDeviceSelected(device);
        }
        dismissAllowingStateLoss();
    }


    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        mBluetoothAdapter.cancelDiscovery();
    }
}
