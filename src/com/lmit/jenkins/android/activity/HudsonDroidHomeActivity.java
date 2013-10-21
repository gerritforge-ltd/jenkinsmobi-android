// Copyright (C) 2012 LMIT Limited
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.lmit.jenkins.android.activity;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.Header;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.exceptions.HudsonMobiGlobalExceptionHandler;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkins.android.networking.PostError;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;

public class HudsonDroidHomeActivity extends Activity {

	public static final long MIN_SLEEP_MSEC = 800L;

	static Logger log = Logger.getInstance();

	private boolean initialized = false;

	private boolean stopEverything;

	JenkinsCloudDataNode nextNodeToRender;

	private Handler connectToCloudHandler;

	Context ctx;

	ProgressBar progress;

	TextView text;

	/* Begin Async Tasck var */
	String path = Configuration.getInstance().getHomeNode();
	boolean cacheHit = false;
	boolean forceRefresh = false;
	boolean appendMode = false;
	String messageDisplayLog = "";
	boolean progressGone = false;
	Header[] lastHttpHeaders;
	Throwable e;

	long currTime;

	/* End Async Tasck var */

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		initialized = false;

		// TODO: better daemons clean-up
		// ImageDownloader.getInstance().stop();
		// BackgroundLoader.getInstance().stop();

		super.onActivityResult(requestCode, resultCode, data);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		currTime = System.currentTimeMillis();

		ctx = this;

		setTitle("");

		setContentView(R.layout.main);
		progress = (ProgressBar) findViewById(R.id.loadingProgress);
		progress.setVisibility(View.VISIBLE);
		text = (TextView) findViewById(R.id.log_text_view);
		text.setText(R.string.loading_stage_init);

		internalInit(this, false);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {

		super.onPostCreate(savedInstanceState);

		if (stopEverything) {
			return;
		}

		if (Configuration.getInstance().getHudsonHostname() == null) {
			onConfigurationButtonClick(null);
		}
	}

	void saveResultInLocalDB(final String path, final JenkinsCloudDataNode result, boolean refresh) {
		if (result != null) {
			if (appendMode) {
				JenkinsCloudDataNode existingNode = (JenkinsCloudDataNode) LocalStorage.getInstance().getNode(path);
				List<JenkinsCloudDataNode> newPayload = new LinkedList<JenkinsCloudDataNode>();
				newPayload.addAll(existingNode.getPayload());
				newPayload.addAll(result.getPayload());
				existingNode.setPayload(newPayload);
				LocalStorage.getInstance().replaceNode(path, existingNode);
			} else {
				if (refresh) {
					LocalStorage.getInstance().evictNode(path, result);
				}
				LocalStorage.getInstance().putNode(path, result);
			}
		}
	}

	// This gets executed in a non-UI thread:
	public void connectToCloud() {

		HomeActivityLoadTask callCloud = new HomeActivityLoadTask(this);
		callCloud.execute();

	}

	@Override
	public boolean equals(Object obj) {

		boolean result = false;

		if (obj != null) {

			result = obj instanceof HudsonDroidHomeActivity;
		}

		return result;
	}

	/**
	 * [HUDSONDROID-47] Pay attention that this could never be called So to
	 * enforce our crash reporter function we need to rename the log file in
	 * case of crashes and check for renamed file when the app starts again
	 */
	@Override
	protected void onDestroy() {

		super.onDestroy();

		if (initialized) {
			initialized = false;
			// Logger.stopLoggerAndCleanup();
		}
	}

	boolean fromWidget;

	public void internalInit(Context ctx, boolean fromWidget) {
		this.fromWidget = fromWidget;

		if (!initialized) {

			HudsonMobiGlobalExceptionHandler.set();

			if (!Logger.checkForExternalStorageState(ctx)) {

				Logger.setEnabled(false); // disable logger
			}

			if (!fromWidget && Logger.checkForUncleanShutdown()) {

				// alert the user
				PostError postError = new PostError();
				postError.send();
			}

			Logger.getInstance();

			initialized = true;
		}
		
		connectToCloud();
	}

	private static void showMailerToSendCrashReport(Context ctx) {

		/* Create the Intent */
		final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

		/* Fill it with Data */
		emailIntent.setType("plain/text");
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { Configuration.SUPPORT_MAIL_ADDRESS });
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, ctx.getText(R.string.crash_email_subject));
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, ctx.getText(R.string.crash_email_body));
		emailIntent.putExtra(android.content.Intent.EXTRA_STREAM, Uri.fromFile(new File(Configuration.getInstance().getPrivateFolderPath() + File.separator + Logger.TRACE_ERROR_NAME_SWP)));
		/* Send it off to the Activity-Chooser */
		ctx.startActivity(Intent.createChooser(emailIntent, ctx.getText(R.string.crash_email_subject).toString()));
	}

	public void onInfoButtonClick(View v) {
		Intent myIntent = new Intent();
		myIntent.setClass(this, ProductInfoActivity.class);
		startActivity(myIntent);
	}

	public void onConfigurationButtonClick(View v) {
		Intent myIntent = new Intent();
		myIntent.setClass(this, Configurator2Activity.class);
		startActivity(myIntent);
	}

	boolean connectedToCellularData() {
		Logger.getInstance().debug("Checking if connected to cellular data");
		ConnectivityManager mConnectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = mConnectivity.getActiveNetworkInfo();
		if (info == null) {
			return false;
		}
		int netType = info.getType();

		Logger.getInstance().debug("Connected to cellular data " + (netType == ConnectivityManager.TYPE_MOBILE));

		return netType == ConnectivityManager.TYPE_MOBILE;
	}

	void moveOn() {
		Intent myIntent = new Intent();
		nextNodeToRender.setPath(Configuration.getInstance().getHomeNode());
		GenericListActivity.setNodeToRender(nextNodeToRender);
		GenericListActivity.ignoreLoading();
		myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		myIntent.putExtra("caller.activity", HudsonDroidHomeActivity.class.getName());
		myIntent.setClassName(Configuration.ACTIVITY_PACKAGE_NAME, GenericListActivity.class.getName());
		startActivity(myIntent);
		finish();
	}

	class CrashReportClickListener implements OnClickListener {

		private Context ctx;

		public CrashReportClickListener(Context ctx) {

			this.ctx = ctx;
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {

			if (which == DialogInterface.BUTTON_POSITIVE) {
				// send email
				showMailerToSendCrashReport(ctx);
				stopEverything = false;
				finish();
			} else {

				progress.setVisibility(View.VISIBLE);
				stopEverything = false;
				onPostCreate(null);
			}
		}
	}
}
