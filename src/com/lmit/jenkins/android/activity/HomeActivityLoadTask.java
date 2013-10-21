package com.lmit.jenkins.android.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.view.View;

import com.lmit.jenkins.android.addon.ApplicationStatus;
import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkins.android.networking.AbstractSecureHttpClient;
import com.lmit.jenkins.android.networking.ImageDownloader;
import com.lmit.jenkins.android.networking.ServerAuthenticationDefaultHttpClient;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;

class HomeActivityLoadTask extends AsyncTask<String, Void, String> {

	/**
 * 
 */
private final HudsonDroidHomeActivity HomeScreenLoadTask;

/**
 * @param hudsonDroidHomeActivity
 */
public HomeActivityLoadTask(HudsonDroidHomeActivity hudsonDroidHomeActivity) {
  HomeScreenLoadTask = hudsonDroidHomeActivity;
}

private static final long ERROR_MESSAGE_DELAY_MSEC = 10000L;

	private void setLogLoading(String message) {
		HomeScreenLoadTask.messageDisplayLog = message;
		publishProgress();
	}
	
	private void showFailureAlert(Throwable e) {
		setLogLoading("Error on Server connection: " + e.getMessage());
	}
	
    public void onSuccessclientHudsonMobiAsyncHttpClient(
        JenkinsCloudDataNode result) {

      JenkinsCloudDataNode cachedResult =
          LocalStorage.getInstance().getNode("/qaexplorer",
              JenkinsCloudDataNode.class);
      
      if (result == null && cachedResult != null) {
        setLogLoading("INVALID SERVER RESPONSE\n"
            + "Network call was possibly redirected.\n"
            + "Going OFF-LINE with local cache");
        Configuration.getInstance().setConnected(false, true);
        result = cachedResult;
        try {
          Thread.sleep(1000L);
        } catch (InterruptedException e) {
        }
        
      } else if (result == null && cachedResult == null) {
        if (Configuration.getInstance().isConnected()) {
          setLogLoading("INVALID SERVER RESPONSE\n"
              + "Network call was possibly redirected.\n"
              + "Close the App, check your network settings, "
              + "open your Browser and authenticate to Wi-Fi");
        } else {
          setLogLoading("No connectivity or local cached data.\n"
              + "check your network settings, "
              + "open your Browser and check connectivity");
          Configuration.getInstance().setConnected(true, true);
          Configuration.getInstance().save();
        }
        stopProgress();
        return;
        
      } else {
        Configuration.getInstance().setLastRefreshTimestamp(
            "" + new Date().getTime());
        Configuration.getInstance().save();
        HomeScreenLoadTask.saveResultInLocalDB(HomeScreenLoadTask.path, result, HomeScreenLoadTask.forceRefresh);
        ImageDownloader.getInstance().preloadImage(
            "/qaexplorer/" + ApplicationStatus.getCurrentPath(), result);
      }

      HomeScreenLoadTask.nextNodeToRender = result;
      HomeScreenLoadTask.moveOn();

    }

	private void stopProgress() {
		HomeScreenLoadTask.progressGone = true;
		publishProgress();
	}
	
	public void onFailureclientHudsonMobiAsyncHttpClient(Throwable e) {
		stopProgress();
		HudsonDroidHomeActivity.log.error("HTTP Client failure", e);
		if (LocalStorage.getInstance().getNode("qaexplorer") == null) {
			setLogLoading(getExceptionErrorMessage(e) + "\n" +
					"Close the application, check your network settings and try again");
		} else {
			setLogLoading("Connection Error: using latest local cached data");
			Configuration.getInstance().setConnected(false, true);
			HomeScreenLoadTask.connectToCloud();
		}
	}

	private String getExceptionErrorMessage(Throwable e) {
		String[] exceptionNameParts = e.getClass().getName().split("\\.");
		String exceptionName = exceptionNameParts[exceptionNameParts.length-1];
		StringBuilder outBuff = new StringBuilder();
		for (int pos=0; pos < exceptionName.length(); pos++) {
			char currChar = exceptionName.charAt(pos);
			if(pos > 0 && Character.isUpperCase(currChar)) {
				outBuff.append(" ");
				currChar = Character.toLowerCase(currChar);
			}
			outBuff.append(currChar);
		}
		outBuff.append(" - ");
		outBuff.append(e.getMessage());
		return outBuff.toString();
	}
	



	@Override
	protected String doInBackground(String... params) {
	  Logger.getInstance().logConfig();
		
	  boolean simChange = Configuration.getInstance().detectSubscriberId();

      if (Configuration.getInstance().isConnected()) {
        if (HomeScreenLoadTask.connectedToCellularData() && simChange
            && Configuration.getInstance().getMsisdn() == null) {
        }
      }

		// final JenkinsCloudAPIClient client = new
		// JenkinsCloudAPIClient(ctx);
		Map<String, String> headers = Configuration.getInstance().getRequestHeaders();
		ApplicationStatus.reset();

		JenkinsCloudDataNode result = null;

        AbstractSecureHttpClient client = new ServerAuthenticationDefaultHttpClient(
            HomeScreenLoadTask.path);
		try {
			HomeScreenLoadTask.currTime = sleep(HomeScreenLoadTask.currTime);
			setLogLoading(HomeScreenLoadTask.getString(Configuration.getInstance().isConnected() ? R.string.loading_stage_connect:R.string.loading_stage_offline));
			
			if(Configuration.getInstance().isConnected()) {
	            HudsonDroidHomeActivity.log.debug("Getting URL: '" + HomeScreenLoadTask.path + "'");
				HttpResponse response = client.executeGetQuery(false, headers);

				HomeScreenLoadTask.currTime = sleep(HomeScreenLoadTask.currTime);
				switch (response.getStatusLine().getStatusCode()) {

				case HttpURLConnection.HTTP_OK:
				  if(Configuration.getInstance().isConnected()) {
					setLogLoading(HomeScreenLoadTask.getString(R.string.loading_stage_receive));
				  }

					InputStream responseInStream = response.getEntity()
							.getContent();
					try {
						result = (JenkinsCloudDataNode) JenkinsCloudDataNode
								.fromJson(responseInStream,
										JenkinsCloudDataNode.class);
						Header etag = response.getFirstHeader("ETag");
						if (etag != null && result != null) {
							result.setEtag(etag.getValue());
						}
					} finally {
						responseInStream.close();
					}
					break;

				case HttpURLConnection.HTTP_NOT_MODIFIED:
				  if(Configuration.getInstance().isConnected()) {
					setLogLoading(HomeScreenLoadTask.getString(R.string.loading_stage_notmodified));
				  }
					result = (JenkinsCloudDataNode) client.getCachedNode();
					break;

				default:
			  		Configuration.getInstance().setConnected(false, true);
					throw new IOException("Error "
							+ response.getStatusLine().getReasonPhrase());
				}
			} else {
			  result = (JenkinsCloudDataNode) client.getCachedNode(HomeScreenLoadTask.path);
			}
		} catch (IOException exec) {
			HomeScreenLoadTask.e = exec;
			Configuration.getInstance().setConnected(false, true);
		}

		HomeScreenLoadTask.currTime = sleep(HomeScreenLoadTask.currTime);
		if (HomeScreenLoadTask.e == null) {
		  if(Configuration.getInstance().isConnected()) {
			setLogLoading(HomeScreenLoadTask.getString(R.string.loading_stage_images));
		  }
			onSuccessclientHudsonMobiAsyncHttpClient(result);
		} else {
			result = (JenkinsCloudDataNode) client.getCachedNode();
			if (result == null) {
				setLogLoading(HomeScreenLoadTask.getString(R.string.loading_stage_failure));
				HomeScreenLoadTask.currTime = sleep(HomeScreenLoadTask.currTime);
				onFailureclientHudsonMobiAsyncHttpClient(HomeScreenLoadTask.e);
			} else {
				setLogLoading(HomeScreenLoadTask.getString(R.string.loading_stage_offline));
				HomeScreenLoadTask.currTime = sleep(HomeScreenLoadTask.currTime);
				onSuccessclientHudsonMobiAsyncHttpClient(result);
			}
		}
		return null;
	}

	private long sleep(long currTime) {
		long now = System.currentTimeMillis();
		long sleepTime = HudsonDroidHomeActivity.MIN_SLEEP_MSEC - (now - currTime);
		if(sleepTime > 10) {
			try {
				HudsonDroidHomeActivity.log.debug("Sleeping " + sleepTime + " msec");
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
			}
		}
		return System.currentTimeMillis();
	}

	@Override
	protected void onPostExecute(String result) {
	}

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected void onProgressUpdate(Void... values) {
		HomeScreenLoadTask.text.setText(HomeScreenLoadTask.messageDisplayLog);
		if (HomeScreenLoadTask.progressGone) {
			HomeScreenLoadTask.progress.setVisibility(View.INVISIBLE);
		}
	}
}
