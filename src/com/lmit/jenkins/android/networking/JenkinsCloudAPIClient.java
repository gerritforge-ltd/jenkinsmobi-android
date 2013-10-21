package com.lmit.jenkins.android.networking;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;
import com.lmit.jenkinscloud.commons.SyncCallback;

public class JenkinsCloudAPIClient {

  private static Logger log = Logger.getInstance();
  private Context ctx;
  private boolean cacheHit = false;
  private boolean forceRefresh = false;
  private boolean appendMode = false;

  public JenkinsCloudAPIClient(Context ctx) {
    this.ctx = ctx;
  }

  public void setForceRefresh(boolean refresh) {
    forceRefresh = refresh;
  }

  public void setAppendMode(boolean mode) {
    appendMode = mode;
  }

  public boolean isCacheHit() {
    return cacheHit;
  }

  public void doGet(final boolean forceRefresh, final String path,
      final SyncCallback<JenkinsCloudNode> syncCallback, Class<? extends JenkinsCloudNode> targetClass,
      Map<String, String> userHeaders, String itemsFrom, String items) {

    JenkinsCloudNode localData =
        LocalStorage.getInstance().getNode(path, targetClass);

    if(!Configuration.getInstance().isConnected()) {
      cacheModeQuery(syncCallback, localData);
      return;
    }

    if (!forceRefresh) {
      if (localData != null) {
        String eTag = localData.getEtag();
        log.debug("HTTP-Cache HIT! " + (eTag == null ? "" : "eTag=" + eTag));

        if (eTag != null) {
          userHeaders.put("If-None-Match", eTag);
        } else {
          syncCallback.onSuccess(localData);
          cacheHit = true;
          return;
        }
      } else {
        log.debug("Cache MISS!");
      }
    } else {
      log.debug("Force fetching of new data");
    }

    final HudsonMobiAsyncHttpClient client = new HudsonMobiAsyncHttpClient();
    client.setUserHeaders(userHeaders);
    String query =
        path;
    if (itemsFrom != null && items != null) {
    	query += (query.indexOf('?') > 0 ? "&":"?");
      query +=
          "x-jenkinscloud-accept-items=" + items
              + "&x-jenkinscloud-accept-from=" + itemsFrom;
    }
    client.call(forceRefresh, query, 
    		new SyncCallbackWrapper<JenkinsCloudNode>(
    				syncCallback) {
      @Override
      public void onSuccess(JenkinsCloudNode result) {
        Configuration.getInstance().setLastRefreshTimestamp(
            "" + new Date().getTime());
        Configuration.getInstance().save();
        saveResultInLocalDB(path, result, forceRefresh);
        ImageDownloader.getInstance().preloadImage(path, result);
        syncCallback.onSuccess(result);
      }
    });
  }
  
	public void doPostSynch(final String path,
			SyncCallback<JenkinsCloudNode> syncCallback,
			Map<String, String> userHeaders, byte[] postData, String postContentType) {

		if (!Configuration.getInstance().isConnected()) {
			syncCallback.onFailure(new ClientDisconnectedException());
			return;
		}

		final HudsonMobiSynchHttpClient client = new HudsonMobiSynchHttpClient();
		client.setUserHeaders(userHeaders);
		String query = path;
		client.doPost(query, postData, postContentType, new SyncCallbackWrapper<JenkinsCloudNode>(
				syncCallback) {
			@Override
			public void onSuccess(JenkinsCloudNode result) {
				saveResultInLocalDB(path, result, true);
				ImageDownloader.getInstance().preloadImage(path, result);
				super.onSuccess(result);
			}
		});
	}

  private void cacheModeQuery(
      final SyncCallback<JenkinsCloudNode> syncCallback,
      JenkinsCloudNode localData) {
    if(localData != null) {
      syncCallback.onSuccess(localData);
    } else {
      syncCallback.onFailure(new ClientDisconnectedException());
    }
  }
  
  public void callSync(final String path,
      final SyncCallback<JenkinsCloudNode> syncCallback,
      Map<String, String> headers, String itemsFrom, String items) {

	  JenkinsCloudNode localData = LocalStorage.getInstance()
          .getNode(path);

	    if(!Configuration.getInstance().isConnected()) {
	      cacheModeQuery(syncCallback, localData);
	      return;
	    }
	    
	if (!forceRefresh) {
		if (localData != null && !Configuration.getInstance().isConnected()) {
			log.debug("Cache HIT " + path
					+ ": returning node as working in disconnected mode");
			cacheHit = true;
			syncCallback.onSuccess(localData);
			return;
		} else if (localData == null) {
			log.debug("Cache MISS!");
		}
	} else {
		log.debug("Force fetching of new data");
	}
	
	final JenkinsCloudNode cachedData = localData;

    final HudsonMobiSynchHttpClient client = new HudsonMobiSynchHttpClient();
    client.setUserHeaders(headers);
    String query =
        path;
    if (itemsFrom != null && items != null) {
    	query += (query.indexOf('?') > 0 ? "&":"?");
      query +=
          "x-jenkinscloud-accept-items=" + items
              + "&x-jenkinscloud-accept-from=" + itemsFrom;
    }
    client.call(forceRefresh, query, new SyncCallback<JenkinsCloudNode>() {

      @Override
      public void onSuccess(JenkinsCloudNode result) {
        Configuration.getInstance().setLastRefreshTimestamp(
            "" + new Date().getTime());
        Configuration.getInstance().save();
        saveResultInLocalDB(path, result, forceRefresh);
        syncCallback.onSuccess(result);
      }

      @Override
      public void onFailure(Throwable e) {
    	  if(cachedData != null) {
    	        syncCallback.onSuccess(cachedData);    		  
    	  } else {
        syncCallback.onFailure(e);
    	  }
      }
    });
  }

  private void saveResultInLocalDB(final String path,
      final JenkinsCloudNode result, boolean refresh) {
    if (result != null) {
      if (appendMode) {
        JenkinsCloudNode existingNode =
            LocalStorage.getInstance().getNode(path);
        List<JenkinsCloudDataNode> newPayload =
            new LinkedList<JenkinsCloudDataNode>();
        
        if(existingNode instanceof JenkinsCloudDataNode) {
        newPayload.addAll(((JenkinsCloudDataNode) existingNode).getPayload());
        newPayload.addAll(((JenkinsCloudDataNode) result).getPayload());
        ((JenkinsCloudDataNode) existingNode).setPayload(newPayload);
        }
        LocalStorage.getInstance().replaceNode(path, existingNode);
      } else {
    	  if(refresh) {
    		  LocalStorage.getInstance().evictNode(path, result);
    	  }
        LocalStorage.getInstance().putNode(path, result);
      }
    }
  }

}
