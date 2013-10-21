package com.lmit.jenkins.android.activity;

import java.util.concurrent.Semaphore;

import android.content.Context;

import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;

public class JenkinsMobi extends android.app.Application {
    private Logger log;
	private static Context context = null;
	private static String statusApp;

	public JenkinsMobi() {
		super();
	}

	@Override
	public void onCreate() {

		super.onCreate();

		Configuration.getInstance(this);

		LocalStorage.getInstance();
	}

	public static String getStatusApp() {
		return JenkinsMobi.statusApp;
	}

	public static void setStatusApp(boolean connected) {
		JenkinsMobi.statusApp = connected ? "Updated":"Cache";
	}

	public static void setContext(Context context) {
	  if(context != null) {
		JenkinsMobi.context = context;
	  }
	}

    public static Context getAppContext() {
      if (context == null) {
        throw new IllegalArgumentException(
            "Cannot get a valid application context");
      }
      
      return JenkinsMobi.context;
    }
}
