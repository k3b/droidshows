package nl.asymmetrics.droidshows.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

//import android.util.Log;
public class Utils
{
	@NonNull
	public static SimpleDateFormat createDateFormat() {
		return new SimpleDateFormat("yyyy-MM-dd");
	}

	@NonNull
	public static String formatDate(@NonNull Date date) {
		return createDateFormat().format(date);
	}
	public boolean isNetworkAvailable(Activity mActivity) {
		Context context = mActivity.getApplicationContext();
		ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {
			// Log.d(TAG," connectivity is null");
			return false;
		} else {
			// Log.d(TAG," connectivity is not null");
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				// Log.d(TAG," info is not null");
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						return true;
					} else {
						// Log.d(TAG," info["+i+"] is not connected");
					}
				}
			} else {
				// Log.d(TAG," info is null");
			}
		}
		return false;
	}
}