package com.lmit.jenkins.android.networking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

import android.content.Context;

import com.lmit.jenkins.android.activity.JenkinsMobi;
import com.lmit.jenkins.android.addon.ApplicationStatus;
import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;
import com.lmit.jenkinscloud.commons.SyncCallback;

public class BackgroundLoader {

  private static final long LOAD_ITEMS_SLEEP_TIME_MSEC = 100L;

  private static Logger log = Logger.getInstance();

  private static LinkedBlockingDeque<ExecutionRequest> executionRequests = new LinkedBlockingDeque<ExecutionRequest>();

  private boolean stop;

  public static class ExecutionRequest {

    private Context ctx;
    private String pluginId;
    private int fromNode;
    private int maxNodes;

    public ExecutionRequest(Context ctx, String pluginId, int from, int max) {
      this.ctx = ctx;
      this.pluginId = pluginId;
      this.fromNode = from;
      this.maxNodes = max;
    }

    public int getFromNode() {
      return fromNode;
    }

    public void setFromNode(int fromNode) {
      this.fromNode = fromNode;
    }

    public int getMaxNodes() {
      return maxNodes;
    }



    public void setMaxNodes(int maxNodes) {
      this.maxNodes = maxNodes;
    }



    public Context getCtx() {
      return ctx;
    }

    public void setCtx(Context ctx) {
      this.ctx = ctx;
    }

    public String getPluginId() {
      return pluginId;
    }

    public void setPluginId(String pluginId) {
      this.pluginId = pluginId;
    }
  }

  private static BackgroundLoader instance;

  public static BackgroundLoader getInstance() {
    if (instance == null) {
      instance = new BackgroundLoader();
    }
    return instance;
  }

  private BackgroundLoader() {

    Thread th = new Thread(new Runnable() {

      @Override
      public void run() {
        while (!stop) {
          ExecutionRequest request;
          try {
            log.debug("backgroud loader waiting for new requests");
            request = executionRequests.take();
            startLoad(request.getCtx(), request.getPluginId());
          } catch (InterruptedException e) {
            ;
          }
        }
        instance = null;
      }
    });
    th.setName("Background-load");
    th.start();
  }

  public void enqueue(ExecutionRequest request) {
    log.debug("New background loading request for pluginId='"
        + request.getPluginId() + "'");
    stop = false;
    executionRequests.offer(request);
  }

  private void startLoad(final Context ctx, final String pluginId) {
    log.debug("Running background load for pluginId='" + pluginId + "'");
    
    LocalStorage store = LocalStorage.getInstance();
    JenkinsCloudAPIClient client = new JenkinsCloudAPIClient(JenkinsMobi.getAppContext());
    String rootPath = Configuration.getInstance().getHomeNode();
    JenkinsCloudDataNode rootNode = (JenkinsCloudDataNode) store.getNode(rootPath);
    Map<String, String> headers = Configuration.getInstance().getRequestHeaders();
    load(client, headers, rootPath, rootNode);
  }
  
  private void load(final JenkinsCloudAPIClient client,
      final Map<String, String> headers, final String parentPath,
      final JenkinsCloudDataNode node) {
    
    if(!Configuration.getInstance().isConnected()) {
      return;
    }
    
    if (node == null || node.getPath() == null || node.getPath().equals("menu")) {
      return;
    }

    String path = node.getPath();
    final String currentPath = getPath(parentPath, path);
    if (currentPath == null) {
      return;
    }

    log.debug("Background loading of " + currentPath);
    final List<JenkinsCloudDataNode> subNodesToLoad =
        new ArrayList<JenkinsCloudDataNode>();

    final Semaphore callFinished = new Semaphore(1);
    try {
      callFinished.acquire();
    } catch (InterruptedException e1) {
    }
    client.doGet(false, currentPath, new SyncCallback<JenkinsCloudNode>() {

      @Override
      public void onSuccess(JenkinsCloudNode result) {
        if (result instanceof JenkinsCloudDataNode) {
          JenkinsCloudDataNode resultNode = (JenkinsCloudDataNode) result;
          List<JenkinsCloudDataNode> resultSubNodes = resultNode.getPayload();
          if (resultSubNodes != null) {
            for (JenkinsCloudDataNode subNode : resultSubNodes) {
              if (subNode.isPreload()) {
                subNodesToLoad.add((JenkinsCloudDataNode) subNode);
              }
            }
          }
        }
        callFinished.release();
      }

      @Override
      public void onFailure(Throwable e) {
        log.error("Cannot load node", e);
        callFinished.release();
      }
    }, JenkinsCloudDataNode.class, headers, null, null);
    try {
      callFinished.acquire();
    } catch (InterruptedException e1) {
    }

    for (JenkinsCloudDataNode subNode : subNodesToLoad) {
      load(client, headers, currentPath, subNode);
      try {
        Thread.sleep(LOAD_ITEMS_SLEEP_TIME_MSEC);
      } catch (InterruptedException e1) {
      }
    }
  }

  private String getPath(String parentPath,
      String nodePath) {
    if(nodePath == null) {
      return null;
    }
    
    if(nodePath.equals("/")) {
      nodePath = "";
    }
    
    if(nodePath.startsWith("/")) {
      return null;
    }
    
    if(!parentPath.endsWith("/")) {
      parentPath += "/";
    }
    
    return parentPath + nodePath;
  }

  public void stop() {
    stop = true;
  }
}
