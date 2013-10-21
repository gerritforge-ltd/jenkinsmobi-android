package com.lmit.jenkins.android.networking;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.lmit.jenkins.android.activity.GenericListActivity;
import com.lmit.jenkins.android.activity.R;
import com.lmit.jenkins.android.addon.ApplicationStatus;
import com.lmit.jenkins.android.addon.LoadingView;
import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;

public class ReconnectRootNode extends AsyncTask<Void, Void, Boolean> {

  GenericListActivity ctx;
  private ProgressDialog dialog;
  private static Logger log = Logger.getInstance();
  private Configuration config;
  private static final String ROOT_PATH = "/qaexplorer";
  private static final long DISCONNECT_DELAY_MSEC = 2000L;
  
  public static class ConnectNodeOnClickListener implements OnClickListener {
    
    private GenericListActivity ctx;

    public ConnectNodeOnClickListener(GenericListActivity ctx) {
      this.ctx = ctx;
    }

    @Override
    public void onClick(View v) {
      boolean isConnected = Configuration.getInstance().isConnected();
      ProgressDialog connectProgress = new ProgressDialog(ctx);
      connectProgress.setCancelable(true);
      connectProgress
          .setMessage(
              ctx.getString(isConnected
                  ? R.string.online_disconnect : R.string.offline_connect));
      
      ReconnectRootNode refreshNode =
          new ReconnectRootNode(ctx,
              connectProgress);
      refreshNode.execute();
    }
    
  }

  public ReconnectRootNode(GenericListActivity ctx, ProgressDialog dialog) {
    this.ctx = ctx;
    config = Configuration.getInstance();
    this.dialog = dialog;
  }

  boolean cacheHit = false;
  boolean appendMode = false;
  String messageDisplayLog = "";
  Header[] lastHttpHeaders;
  Throwable e;

  @Override
  protected Boolean doInBackground(Void... params) {
    if (config.isConnected()) {
      return goOffLine();
    } else {
      return goOnLine();
    }
  }

  private Boolean goOffLine() {
    try {
      Thread.sleep(DISCONNECT_DELAY_MSEC);
    } catch (InterruptedException e) {
    }
    Configuration.getInstance().setConnected(false, true);
    Configuration.getInstance().save();
    return false;
  }

  private Boolean goOnLine() {
    config.setConnected(true, true);
    
    Map<String, String> headers = Configuration.getInstance().getRequestHeaders();

    JenkinsCloudDataNode result = null;
    AbstractSecureHttpClient client =
        new ServerAuthenticationDefaultHttpClient(ApplicationStatus.getCurrentPath());

    try {
      HttpResponse response = client.executeGetQuery(false, headers);

      switch (response.getStatusLine().getStatusCode()) {

        case HttpURLConnection.HTTP_OK:
          InputStream responseInStream = response.getEntity().getContent();
          try {
            result =
                (JenkinsCloudDataNode) JenkinsCloudDataNode.fromJson(
                    responseInStream, JenkinsCloudDataNode.class);
            Header etag = response.getFirstHeader("ETag");
            if (etag != null && result != null) {
              result.setEtag(etag.getValue());
            }
          } finally {
            responseInStream.close();
          }
          
          break;

        case HttpURLConnection.HTTP_NOT_MODIFIED:
          result = (JenkinsCloudDataNode) client.getCachedNode();
          break;


        default:
          config.setConnected(false, true);
          e =
              new Throwable("Error "
                  + response.getStatusLine().getReasonPhrase());
      }
    } catch (Exception exec) {
      e = exec;
      config.setConnected(false, true);
    }

    if (e == null && result != null) {
      onSuccessclientHudsonMobiAsyncHttpClient(result);
      
      BackgroundLoader.getInstance().enqueue(
          new BackgroundLoader.ExecutionRequest(ctx, "", 0, 0));

    } else {
      onFailureclientHudsonMobiAsyncHttpClient(e);
    }
    
    if(config.isConnected()) {
      config.save();
    }

    return config.isConnected();
  }

  @Override
  protected void onPostExecute(Boolean connected) {
    if (dialog != null && dialog.isShowing()) {
      dialog.cancel();
    }
    ctx.updateRightButtonImage();
  }

  @Override
  protected void onPreExecute() {
    if (dialog != null) {
      dialog.show();
    }
  }

  @Override
  protected void onProgressUpdate(Void... values) {
  }


  public void onSuccessclientHudsonMobiAsyncHttpClient(
      JenkinsCloudDataNode result) {
    Configuration.getInstance().setLastRefreshTimestamp(
        "" + new Date().getTime());
    Configuration.getInstance().save();
    Logger.getInstance().debug("Connected to cloud");

    if (!result.isCached()) {
      LocalStorage.getInstance().replaceNode(ROOT_PATH, result);
      ImageDownloader.getInstance().preloadImage(ROOT_PATH, result);
    }

    LoadingView.remove();
  }

  public void onFailureclientHudsonMobiAsyncHttpClient(Throwable e) {
    LoadingView.remove();
  }

}
