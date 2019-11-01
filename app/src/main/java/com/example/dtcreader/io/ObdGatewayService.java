package com.example.dtcreader.io;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.example.dtcreader.SettingsActivity;
import com.example.dtcreader.MainActivity;
import com.example.dtcreader.R;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.Objects;

//Эта служба в первую очередь отвечает за создание и поддержание постоянное соединение между устройством, на котором работает приложение
//Во-вторых, он будет служить хранилищем ObdCommandJobs

public class ObdGatewayService extends AbstractGatewayService {

    private static final String TAG = ObdGatewayService.class.getName();
    @Inject
    SharedPreferences prefs;

    private BluetoothDevice dev = null;
    private BluetoothSocket sock = null;

    public void startService() throws IOException {
        Log.d(TAG, "Starting service..");

        // get the remote Bluetooth device
        final String remoteDevice = prefs.getString(SettingsActivity.BLUETOOTH_LIST_KEY, null);
        if (remoteDevice == null || "".equals(remoteDevice)) {
            Toast.makeText(ctx, getString(R.string.text_bluetooth_nodevice), Toast.LENGTH_LONG).show();

            // log error
            Log.e(TAG, "No Bluetooth device has been selected.");

            stopService();
            throw new IOException();
        } else {

            final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            dev = btAdapter.getRemoteDevice(remoteDevice);
            /*установка соединение Bluetooth
     * Поскольку обнаружение представляет собой сложную процедуру для адаптера Bluetooth,
     * этот метод всегда должен вызываться перед попыткой подключения к
     * устройство. Обнаружение не управляется Активити,
     * но запускается как системная служба, поэтому приложение всегда должно вызывать
     *обнаружение, даже если оно не запрашивало обнаружение напрямую, просто чтобы
     * быть уверенным. Если состояние Bluetooth не STATE_ON, этот API вернет false.*/

            Log.d(TAG, "Stopping Bluetooth discovery.");
            btAdapter.cancelDiscovery();

            showNotification(getString(R.string.notification_action), getString(R.string.service_starting), R.mipmap.ic_launcher, true, true, false);

            try {
                startObdConnection();
            } catch (Exception e) {
                Log.e(TAG, "There was an error while establishing connection. -> " + e.getMessage()
                );

                // in case of failure, stop this service.
                stopService();
                throw new IOException();
            }
            showNotification(getString(R.string.notification_action), getString(R.string.service_started), R.mipmap.ic_launcher, true, true, false);
        }
    }
/*Запуск и настройте соединение с интерфейсом OBD*/

    private void startObdConnection() throws IOException {
        Log.d(TAG, "Starting OBD connection..");
        isRunning = true;
        try {
            sock = BluetoothManager.connect(dev);
        } catch (Exception e2) {
            Log.e(TAG, "\n" + "Произошла ошибка при установлении соединения Bluetooth.", e2);
            stopService();
            throw new IOException();
        }

        // Let's configure the connection.
        Log.d(TAG, "Queueing jobs for connection configuration..");
        queueJob(new ObdCommandJob(new ObdResetCommand()));


        try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }

        queueJob(new ObdCommandJob(new EchoOffCommand()));
        queueJob(new ObdCommandJob(new EchoOffCommand()));
        queueJob(new ObdCommandJob(new LineFeedOffCommand()));
        queueJob(new ObdCommandJob(new TimeoutCommand(62)));

        // Get protocol from preferences
        final String protocol = prefs.getString(SettingsActivity.PROTOCOLS_LIST_KEY, "AUTO");
        queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.valueOf(protocol))));
        queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));

        queueCounter = 0L;
        Log.d(TAG, "Initialization jobs queued.");


    }

    @Override
    public void queueJob(ObdCommandJob job) {
        // This is a good place to enforce the imperial units option
        job.getCommand().useImperialUnits(prefs.getBoolean(SettingsActivity.IMPERIAL_UNITS_KEY, false));

        // Now we can pass it along
        super.queueJob(job);
    }

    protected void executeQueue() throws InterruptedException {
        Log.d(TAG, "Executing queue..");
        while (!Thread.currentThread().isInterrupted()) {
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();

                // log job
                Log.d(TAG, "Taking job[" + job.getId() + "] from queue..");

                if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NEW)) {
                    Log.d(TAG, "Job state is NEW. Run it..");
                    job.setState(ObdCommandJob.ObdCommandJobState.RUNNING);
                    if (sock.isConnected()) {
                        job.getCommand().run(sock.getInputStream(), sock.getOutputStream());
                    } else {
                        job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                        Log.e(TAG, "Can't run command on a closed socket.");
                    }
                } else
                    // log not new job
                    Log.e(TAG,
                            "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
            } catch (UnsupportedCommandException u) {
                if (job != null) {
                    job.setState(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED);
                }
                Log.d(TAG, "Command not supported. -> " + u.getMessage());
            } catch (IOException io) {
                if(io.getMessage().contains("Broken pipe"))
                    job.setState(ObdCommandJob.ObdCommandJobState.BROKEN_PIPE);
                else
                    job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                Log.e(TAG, "IO error. -> " + io.getMessage());
            } catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR);
                }
                Log.e(TAG, "Failed to run command. -> " + e.getMessage());
            }

            if (job != null) {
                final ObdCommandJob job2 = job;
                ((MainActivity) ctx).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((MainActivity) ctx).stateUpdate(job2);
                    }
                });
            }
        }
    }

    public void stopService() {
        Log.d(TAG, "Stopping service..");

        notificationManager.cancel(NOTIFICATION_ID);
        jobsQueue.clear();
        isRunning = false;

        if (sock != null)
            // close socket
            try {
                sock.close();
            } catch (IOException e) {
                Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            }

        // kill service
        stopSelf();
    }

    public boolean isRunning() {
        return isRunning;
    }


}
