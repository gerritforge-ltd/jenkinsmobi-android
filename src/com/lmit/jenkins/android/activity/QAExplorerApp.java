package com.lmit.jenkins.android.activity;

import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;


public class QAExplorerApp extends android.app.Application {

	public QAExplorerApp() {
		super();
	}
	
	@Override
	public void onCreate() {

		super.onCreate();
		
		Configuration.getInstance(this);
		
		LocalStorage.getInstance();
	}
}
