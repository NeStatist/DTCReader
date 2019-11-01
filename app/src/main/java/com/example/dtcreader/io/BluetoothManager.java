package com.example.dtcreader.io;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.UUID;

public class BluetoothManager {

    private  static final  String TAG = BluetoothManager.class.getName();
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static BluetoothSocket connect(BluetoothDevice device) throws IOException{
        BluetoothSocket socket = null;
        BluetoothSocket sockFallback = null;

        Log.d(TAG, "Starting Bluetooth connection///");
        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
        }catch (Exception e1){
            Log.e(TAG, "Произошла ошибка при установлении соединения Bluetooth!!!",e1);
            Class<?>clazz = socket.getRemoteDevice().getClass();
            Class<?>[] paramTypes = new Class<?>[]{Integer.TYPE};
            try {
                Method m = clazz.getMethod("createRfcommSocket",paramTypes);
                Object[] params = new Object[]{1};
                sockFallback = (BluetoothSocket) m.invoke(socket.getRemoteDevice(),params);
                sockFallback.connect();
                socket=sockFallback;
            }catch (Exception e2){
                Log.e(TAG,"Не удалось выполнить установление соединения Bluetooth.",e2);
                throw new IOException(e2.getMessage());
            }
        }
        return socket;
    }
}
