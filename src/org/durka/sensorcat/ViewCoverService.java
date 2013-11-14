package org.durka.sensorcat;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;


public class ViewCoverService extends Service implements SensorEventListener {
	
	static final String TAG = "ViewCoverService";
	
	private SensorManager mSensorManager;

	private boolean mClosed;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "service started");
		
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY), SensorManager.SENSOR_DELAY_NORMAL);
		
		return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "service stopped");
		
		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// I don't care
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_PROXIMITY:
			if (event.values[0] > 0) {
				if (mClosed) {
					if (!hall_is_closed()) {
						mClosed  = false;
						PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
						PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
			            wl.acquire();
			            wl.release();
					}
				}
			} else {
				if (!mClosed) {
					if (hall_is_closed()) {
						mClosed = true;
						DevicePolicyManager dpm = (DevicePolicyManager) getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
						dpm.lockNow();
					}
				}
			}
			Log.d(TAG, String.format("mClosed = %b", mClosed));
			break;
		}
	}

	private boolean hall_is_closed() {
		
		String status = "";
		try {
			Scanner sc = new Scanner(new File(getString(R.string.hall_file)));
			status = sc.nextLine();
			sc.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Hall effect sensor device file not found!");
		}
		
		return status.equals("CLOSE");
	}

}
