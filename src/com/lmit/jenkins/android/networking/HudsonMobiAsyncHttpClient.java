                      package com.lmit.jenkins.android.networking;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import android.os.AsyncTask;

import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudPageReset;
import com.lmit.jenkinscloud.commons.SyncCallback;

public class HudsonMobiAsyncHttpClient extends HudsonMobiSynchHttpClient {

	@Override
  public void call(boolean forceRefresh, String path,
      final SyncCallback<JenkinsCloudNode> callback) {

    new AsynchHttpGetTask(callback).execute("" + forceRefresh, path);
  }

  public void call2Synch(boolean forceRefresh, String url, final SyncCallback<InputStream> callback) {
    InputStream result = null;
    AbstractSecureHttpClient client =
        new ServerAuthenticationDefaultHttpClient(url);

    log.debug("Getting URL: '" + url + "'");

    try {
      HttpResponse response = client.executeGetQuery(forceRefresh, userHeaders);
      lastHttpHeaders = response.getAllHeaders().clone();

      if (response.getStatusLine().getStatusCode() == 200) {
        result = response.getEntity().getContent();
        callback.onSuccess(result);
      } else {
        throw new IOException("HTTP-Error " + response.getStatusLine().getStatusCode() + ":"
                + response.getStatusLine().getReasonPhrase());
      }
    } catch (Exception e) {
      callback.onFailure(e);
    }
  }

    @Override
  public void call2(boolean forceRefresh, String path, final SyncCallback<InputStream> callback) {

    new InternalAsynchTask2(callback).execute("" + forceRefresh, path);
  }

  private class InternalAsynchTask2 extends
      AsyncTask<String, Void, InputStream> {

    private SyncCallback<InputStream> callback;
    private Throwable e;

    public InternalAsynchTask2(SyncCallback<InputStream> callback) {
      this.callback = callback;
    }

    @Override
    protected InputStream doInBackground(String... paramArrayOfParams) {

      InputStream result = null;
      boolean forceRefresh = Boolean.parseBoolean(paramArrayOfParams[0]);
      String url = paramArrayOfParams[1];
      AbstractSecureHttpClient client =
          new ServerAuthenticationDefaultHttpClient(url);

      log.debug("Getting URL: '" + url + "'");

      try {
        HttpResponse response = client.executeGetQuery(forceRefresh, userHeaders);
        lastHttpHeaders = response.getAllHeaders().clone();

        if (response.getStatusLine().getStatusCode() == 200) {

          result = response.getEntity().getContent();
          
        } else {

          this.e =
              new Throwable("Error "
                  + response.getStatusLine().getReasonPhrase());
        }
      } catch (Exception e) {
        this.e = e;
      }

      return result;
    }

    @Override
    protected void onPostExecute(InputStream result) {

      if (e == null) {
        callback.onSuccess(result);
      } else {
        callback.onFailure(e);
      }
    }
  }

  private class AsynchHttpGetTask extends
      AsyncTask<String, Void, JenkinsCloudNode> {

    private SyncCallback<JenkinsCloudNode> callback;
    private Throwable e;

    public AsynchHttpGetTask(SyncCallback<JenkinsCloudNode> callback) {
      this.callback = callback;
    }

    @Override
    protected JenkinsCloudNode doInBackground(String... paramArrayOfParams) {

      JenkinsCloudNode result = null;
      boolean forceRefresh = Boolean.parseBoolean(paramArrayOfParams[0]);
      String url = paramArrayOfParams[1];
      AbstractSecureHttpClient client =
          new ServerAuthenticationDefaultHttpClient(url);

      log.debug("Getting URL: '" + url + "'");

      try {
        HttpResponse response = client.executeGetQuery(forceRefresh, userHeaders);
        lastHttpHeaders = response.getAllHeaders().clone();

        switch (response.getStatusLine().getStatusCode()) {

        case HttpURLConnection.HTTP_OK:
          InputStream responseInStream = response.getEntity().getContent();
          try {
            result = JenkinsCloudDataNode.fromStream(responseInStream, response.getEntity().getContentType().getValue());
            setETag(result, response);
          } finally {
            responseInStream.close();
          }
          break;
          
        case HttpURLConnection.HTTP_NOT_MODIFIED:
        	result = client.getCachedNode();
        	break;
        	
        case HttpURLConnection.HTTP_RESET:
          callback.onSuccess(new JenkinsCloudPageReset());
          break;
        	
        default:
            Configuration.getInstance().setConnected(false, true);
            StatusLine statusLine = response.getStatusLine();
            this.e =
                new IOException("HTTP Error: " + statusLine.getStatusCode()
                    + " - " + statusLine.getReasonPhrase());
        } 
      } catch (Exception e) {
        this.e = e;
		Configuration.getInstance().setConnected(false, true);
      }

      return result;
    }

    @Override
    protected void onPostExecute(JenkinsCloudNode result) {

      if (e == null) {
        callback.onSuccess(result);
      } else {
        callback.onFailure(e);
      }
		}
	}
}
