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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableRow;
import android.widget.TextView;

import com.lmit.jenkins.android.adapter.AbstractHudsonDroidListAdapter;
import com.lmit.jenkins.android.adapter.DescriptiveRowData;
import com.lmit.jenkins.android.adapter.FullDescRowData;
import com.lmit.jenkins.android.adapter.HeaderLikeRowData;
import com.lmit.jenkins.android.adapter.LoadMoreDataRowData;
import com.lmit.jenkins.android.adapter.RowData;
import com.lmit.jenkins.android.addon.ApplicationStatus;
import com.lmit.jenkins.android.addon.LoadingView;
import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkins.android.networking.BackgroundLoader;
import com.lmit.jenkins.android.networking.BackgroundLoader.ExecutionRequest;
import com.lmit.jenkins.android.networking.HudsonMobiAsyncHttpClient;
import com.lmit.jenkins.android.networking.ImageDownloader;
import com.lmit.jenkins.android.networking.JenkinsCloudAPIClient;
import com.lmit.jenkins.android.networking.ReconnectRootNode;
import com.lmit.jenkinscloud.commons.Alignment;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudPage;
import com.lmit.jenkinscloud.commons.Layout;
import com.lmit.jenkinscloud.commons.SyncCallback;
import com.lmit.jenkinscloud.commons.Type;
import com.markupartist.android.widget.OnIsBottomOverScrollListener;
import com.markupartist.android.widget.PullToRefreshListView;
import com.markupartist.android.widget.PullToRefreshListView.EndlessScrollListener;
import com.markupartist.android.widget.PullToRefreshListView.OnRefreshListener;

public class GenericListActivity extends DataLoadActivity {
	private final class NodeLoadedCallback implements
			SyncCallback<JenkinsCloudNode> {
		@Override
          public void onSuccess(JenkinsCloudNode result) {
            nodeToRender = (JenkinsCloudDataNode) result;
          }

		@Override
          public void onFailure(Throwable e) {
            showExceptionOccurredAlert(e);
          }
	}

	protected static final long PAGE_DOWNLOAD_TIMEOUT = 30 * 1000L;

  private static Logger log = Logger.getInstance();

  private static JenkinsCloudDataNode nodeToRenderStatic;

  private JenkinsCloudDataNode nodeToRender;

  private boolean alreadyLoadedUI = false;

  private boolean forceRefresh = false;

  private boolean reloadMoreData = false;

  private static boolean ignoreLoading = false;
  
  private ProgressDialog dialog;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Trick to pass an Object in a static way: but makeing it as soon as
    // possible an instance object
    if (nodeToRenderStatic != null) {
      this.nodeToRender = nodeToRenderStatic;
      nodeToRenderStatic = null;
    }

    if (nodeToRender != null) {
      ApplicationStatus.moveOnPath(nodeToRender.getPath());
    }

    Logger.getInstance().debug(ApplicationStatus.getCurrentPath());

    if (ignoreLoading) {
      if (Configuration.getInstance().isConnected()) {
        BackgroundLoader.getInstance().enqueue(
            new ExecutionRequest(this, "qaexplorer", 0, 50));
      }
    }

    super.onCreate(savedInstanceState);
  }

  @Override
  public void onBackPressed() {
      ApplicationStatus.moveBackPath();
      if (ApplicationStatus.getCurrentPath() == null) {
        Intent myIntent = new Intent();
        myIntent.setClassName(HudsonDroidHomeActivity.class.getPackage().getName(),
            HudsonDroidHomeActivity.class.getName());
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(myIntent);
        finish();
      } else {
        super.onBackPressed();
      }
  }

  public static void setNodeToRender(JenkinsCloudDataNode nodeToRenderStatic) {
    GenericListActivity.nodeToRenderStatic = nodeToRenderStatic;
  }

  private void makeListView() {
    ListView listView = (ListView) findViewById(R.id.genericListView);
    
    updateRightButtonImage();
    getRightButton().setOnClickListener(new ReconnectRootNode.ConnectNodeOnClickListener(this));

    listView.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int position,
          long arg3) {

        if (isLoadMoreDataItem(arg0, position)) {
          reloadMoreDataCallback(0, true);
          return;
        }

        final JenkinsCloudDataNode object = getClickedItem(arg0, position);
        if(object == null) {
          log.error("NULL object clicked => IGNORING event");
          return;
        }
        
        if (!Configuration.getInstance().isConnected() &&
            !isCached(object)) {  
            AlertDialog.Builder connectAlert = new AlertDialog.Builder(GenericListActivity.this);
            connectAlert.setMessage(getString(R.string.offline_load_request_connect)).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
             
             @Override
             public void onClick(DialogInterface dialog, int which) {
               Configuration.getInstance().setConnected(true, false);
               updateRightButtonImage();
               moveToTargetObject(object);
             }
           }).setNegativeButton("No", null).show();
            return; 
        } else {
          moveToTargetObject(object);
        }
      }

      private void moveToTargetObject(JenkinsCloudDataNode object) {
        // check action now
        String action = object.getAction();
        if (action != null) {
          reactToAction(object, action);
        } else {
          if (object.getPath() != null) {
            GenericListActivity.setNodeToRender(object);
            Intent newConfIntent = new Intent();
            newConfIntent.setClassName(Configuration.ACTIVITY_PACKAGE_NAME,
                GenericListActivity.class.getName());
            startActivity(newConfIntent);
          }
        }
      }

      private JenkinsCloudDataNode getClickedItem(AdapterView<?> arg0,
          int position) {
        Map<String, RowData> itemData =
            (Map<String, RowData>) arg0.getItemAtPosition(position);
        RowData row = itemData.get(RowData.ROW_KEY);
        JenkinsCloudDataNode object = null;
        if (row.isUseDisclosureSign() && !(row instanceof HeaderLikeRowData)) {
          object = (JenkinsCloudDataNode) row.getTag();
        }
        return object;
      }

      private boolean isLoadMoreDataItem(AdapterView<?> arg0, int position) {
        Map<String, RowData> itemData =
            (Map<String, RowData>) arg0.getItemAtPosition(position);
        RowData row = itemData.get(RowData.ROW_KEY);
        return "loadmore".equals(row.getTag());
      }
    });

    list = new ArrayList<HashMap<String, RowData>>();
    adapter =
        new AbstractHudsonDroidListAdapter(this, list, R.layout.default_io_row,
            new String[0], new int[0]);
    adapter.setContext(getApplicationContext());
    listView.setAdapter(adapter);

    ((PullToRefreshListView) listView)
        .setOnRefreshListener(new OnRefreshListener() {
          @Override
          public void onRefresh() {
            if(!Configuration.getInstance().isConnected()) {
              Configuration.getInstance().setConnected(true, false);
            }
            reloadData(true);
          }
        });
  }
  
  private boolean isCached(final JenkinsCloudDataNode object) {
    String objectFullPath = resolvePath(object);
    return LocalStorage.getInstance().isCached(objectFullPath);
  }

  private String resolvePath(JenkinsCloudDataNode object) {
    String path = object.getAction();
    if(path == null) {
      path = object.getPath();
    }

    if(path.startsWith("http")) {
      return path;
    }
    
    if(path.startsWith("/")) {
      return "/qaexplorer" + path;
    }
    
    return ApplicationStatus.getCurrentPath() + path;
  }

  private void reloadMoreDataCallback(int delay, boolean usefeedback) {
    reloadMoreData = true;
    reloadData("Loading more data...", usefeedback, false, delay);
  }

  protected void reloadMoreData() {
    JenkinsCloudAPIClient api = new JenkinsCloudAPIClient(this);
    forceRefresh = true;
    api.setForceRefresh(forceRefresh);
    api.setAppendMode(true);
    Map<String, String> headers = Configuration.getInstance().getRequestHeaders();
    headers.put("x-jenkinscloud-accept-items", "50");
    headers.put("x-jenkinscloud-accept-from",
        "" + Integer.toString(list.size()));
    api.callSync(ApplicationStatus.getCurrentPath(),
        new SyncCallback<JenkinsCloudNode>() {
          @Override
          public void onSuccess(JenkinsCloudNode result) {
            nodeToRender = (JenkinsCloudDataNode) result;
          }

          @Override
          public void onFailure(Throwable e) {
            // TODO Auto-generated method stub
          }
        }, headers, Integer.toString(list.size()), "50");
  }

  @Override
  public void loadDataForceRefresh() {

    JenkinsCloudAPIClient api = new JenkinsCloudAPIClient(this);
    Map<String, String> headers = null;
    if (Configuration.getInstance().isConnected()) {
      forceRefresh = true;
      api.setForceRefresh(forceRefresh);
      headers = Configuration.getInstance().getRequestHeaders();
      headers.put("x-jenkinscloud-accept-items", "" + list.size());
      headers.put("x-jenkinscloud-accept-from", "0");
    }
    api.callSync(ApplicationStatus.getCurrentPath(),
        new SyncCallback<JenkinsCloudNode>() {
          @Override
          public void onSuccess(JenkinsCloudNode result) {
            nodeToRender = (JenkinsCloudDataNode) result;
          }

          @Override
          public void onFailure(Throwable e) {
            showExceptionOccurredAlert(e);
          }
        }, headers, "0", "" + list.size());
  }

  @Override
  public void loadData() {
    if (ignoreLoading) {
      ignoreLoading = false;
      return;
    }
   

    if (reloadMoreData) {
      reloadMoreData();
      return;
    }

    forceRefresh = false;
    JenkinsCloudAPIClient api = new JenkinsCloudAPIClient(this);
    Map<String, String> headers = Configuration.getInstance().getRequestHeaders();
    headers.put("x-jenkinscloud-accept-items", "50");
    headers.put("x-jenkinscloud-accept-from", "0");
    
	if (nodeToRender.isHttpGet()) {
		api.callSync(ApplicationStatus.getCurrentPath(),
				new NodeLoadedCallback(), headers, "0", "50");
	} else {
		api.doPostSynch(ApplicationStatus.getCurrentPath(),
				new NodeLoadedCallback(), headers,
				nodeToRender.getPostData(),
				nodeToRender.getPostContentType());
	}
  }

  @Override
  public void update() {
	  if(nodeToRender == null) {
		  log.debug("Nothing to render: node is null");
		  return;
	  }
    Layout layout = nodeToRender.getLayout();
    if(layout == null) {
    	log.error("Node '" + nodeToRender + "' does not declare any layout: nothing to render");
    	return;
    }
	switch (layout) {
      case ICONS:
        makeIconView();
        break;
      case COMPOSITE:
        updateCompositeListView();
        break;
      case WEB_VIEW:
        break;
      case LIST:
      default:
        updateListView();
        break;
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    updateRightButtonImage();
  }

  @Override
  protected void onStart() {
    super.onStart();
    updateRightButtonImage();
  }

  @Override
  protected boolean updateUI() {
    boolean result = false;
    updateRightButtonImage();
    if (!alreadyLoadedUI) {
      if (nodeToRender != null && nodeToRender.getLayout() != null) {
        switch (nodeToRender.getLayout()) {
          case ICONS:
            setContentView(R.layout.iconview);
            break;
          case COMPOSITE:
            setContentView(R.layout.generic_list);
            makeListView();
            break;
          case WEB_VIEW:
            break;
          case LIST:
          default:
            setContentView(R.layout.generic_list);
            makeListView();
            break;
        }
      } else {
        try {
          Thread.sleep(2000L);
        } catch (InterruptedException e) {
        }
        ApplicationStatus.moveBackPath();
        this.finish();
        return false;
      }
      alreadyLoadedUI = true;
      result = true;
    }

    return result;
  }

  private void updateCompositeListView() {
    if (errorOccurred) {
      return;
    }

    list.clear();

    List<JenkinsCloudDataNode> sections = nodeToRender.getPayload();
    for (final JenkinsCloudDataNode section : sections) {

      JenkinsCloudAPIClient client = new JenkinsCloudAPIClient(this);
      final String sectionPath = section.getPath();
      String query = ApplicationStatus.getCurrentPath() + sectionPath;
      if (sectionPath.contains(ApplicationStatus.getCurrentPath())) {
        query = sectionPath;
      }
      client.setForceRefresh(forceRefresh);
      Map<String, String> headers = Configuration.getInstance().getRequestHeaders();
      client.callSync(query, new SyncCallback<JenkinsCloudNode>() {

        @Override
        public void onSuccess(JenkinsCloudNode abstractResult) {
        	JenkinsCloudDataNode result = (JenkinsCloudDataNode) abstractResult;
          if (result.getPayload() != null) {
            for (JenkinsCloudDataNode dataObject : result.getPayload()) {
            	if(dataObject == null) {
            		log.error("Skipping null object detected in payload of " + result);
            		continue;
            	}
              String dataObjectPath = dataObject.getPath();
              if(dataObjectPath != null && !dataObjectPath.startsWith("/")) {
                dataObjectPath = sectionPath + (sectionPath.endsWith("/") ? "":"/") + dataObjectPath;
                dataObject.setPath(dataObjectPath);
              }
              HashMap<String, RowData> item = new HashMap<String, RowData>();
              RowData csRow = null;
              if (Type.HEADER.equals(dataObject.getType())) {
                csRow = new HeaderLikeRowData(dataObject.getTitle());
              } else if (dataObject.getIcon() == null && dataObject.getIconBmp() == null
                          && dataObject.getDescription() == null
                          && dataObject.getAction() == null && dataObject.getPath() == null) {
                        csRow = new FullDescRowData(dataObject.getTitle());
               }  else {
            	  
                csRow = new DescriptiveRowData(dataObject.getTitle());
                if (dataObject.getIcon() != null) {
                  ImageDownloader.getInstance().preloadImage(ApplicationStatus.getCurrentPath(), dataObject);
                  csRow.setImageUrl(ApplicationStatus.getCurrentPath()
                      + result.getPath() + dataObject.getIcon());
                }
                
                if (Alignment.RIGHT.equals(dataObject.getDescriptionAlign())) {
                    csRow.setValue(dataObject.getDescription());
                    if (dataObject.getDescriptionColor() != null
                        && dataObject.getDescriptionColor().length() > 0) {
                      csRow.setAlternativeValueFontColor(Color.parseColor(dataObject
                          .getDescriptionColor()));
                    }
                  } else {
                    csRow.setSubCaption(dataObject.getDescription());
                  }

                csRow.setUseDisclosureSign(dataObject.getPath() != null
                    || dataObject.getAction() != null);
                if ("green".equalsIgnoreCase(dataObject.getDescriptionColor())) {
                  csRow.setAlternativeDesxcriptionFontColor(Color.GREEN);
                } else if ("red".equalsIgnoreCase(dataObject
                    .getDescriptionColor())) {
                  csRow.setAlternativeDesxcriptionFontColor(Color.RED);
                }
              }

              csRow.setTag(dataObject);
              item.put(RowData.ROW_KEY, csRow);
              list.add(item);
            }
          }
        }

        @Override
        public void onFailure(Throwable e) {
          // TODO Auto-generated method stub

        }
      }, headers, null, null);
    }

    adapter.notifyDataSetChanged();

    ListView listView = (ListView) findViewById(R.id.genericListView);
    ((PullToRefreshListView) listView).onRefreshComplete();
    ((PullToRefreshListView) listView)
        .forcePullToRefreshViewHidden(list.size());
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {

    menu.clear();

    List<JenkinsCloudDataNode> cloudMenu = nodeToRender.getMenu();
    if (cloudMenu != null) {
      for (final JenkinsCloudDataNode cloudMenuNode : cloudMenu) {
        MenuItem item = menu.add(cloudMenuNode.getTitle());
        if (cloudMenuNode.getIcon() != null) {
          ImageDownloader.getInstance().setImageBitmap(
              item,
              ApplicationStatus.getCurrentPath() + nodeToRender.getPath()
                  + cloudMenuNode.getIcon());
        }
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            reactToAction(cloudMenuNode, cloudMenuNode.getAction());
            return true;
          }
        });
      }
    }

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    return super.onCreateOptionsMenu(menu);
  }

  public void updateListView(List<JenkinsCloudDataNode> nodes) {

    if (errorOccurred) {
      return;
    }

    final ListView listView = (ListView) findViewById(R.id.genericListView);

    int lastSelectedItem = -1;
    if (reloadMoreData) {
      // remove last object
      list.remove(list.size() - 1);
      lastSelectedItem = list.size();
    } else {
      list.clear();
    }

    if (nodes != null) {
      for (JenkinsCloudDataNode dataObject : nodes) {
        HashMap<String, RowData> item = new HashMap<String, RowData>();
        RowData csRow = null;
        if (Type.HEADER.equals(dataObject.getType())) {
          csRow = new HeaderLikeRowData(dataObject.getTitle());
        } else {

          if (dataObject.getIcon() == null && dataObject.getIconBmp() == null
              && dataObject.getDescription() == null
              && dataObject.getAction() == null && dataObject.getPath() == null) {
            csRow = new FullDescRowData(dataObject.getTitle());
          } else {

            csRow = new DescriptiveRowData(dataObject.getTitle());

            csRow.setUseBoldFontForDescription(dataObject.isModified());

            if (dataObject.getIcon() != null) {

              if (Alignment.BOTTOM.equals(dataObject.getIconAlign())) {
                csRow.setFieldType(RowData.BIG_IMAGE_DATA);
                if (ApplicationStatus.getCurrentPath().equals(
                    nodeToRender.getPath())) {
                  if (dataObject.getIcon().startsWith("http://")) {
                    csRow.setDescriptionImageUrl(dataObject.getIcon());
                  } else {
                    csRow.setDescriptionImageUrl(nodeToRender.getPath()
                        + dataObject.getIcon());
                  }
                } else {
                  if (dataObject.getIcon().startsWith("http://")) {
                    csRow.setDescriptionImageUrl(dataObject.getIcon());
                  } else {
                    csRow.setDescriptionImageUrl(ApplicationStatus
                        .getCurrentPath()
                        + nodeToRender.getPath()
                        + dataObject.getIcon());
                  }
                }
              } else {
                if (ApplicationStatus.getCurrentPath().equals(
                    nodeToRender.getPath())) {
                  if (dataObject.getIcon().startsWith("http://")) {
                    csRow.setImageUrl(dataObject.getIcon());
                  } else {
                    csRow.setImageUrl(nodeToRender.getPath()
                        + dataObject.getIcon());
                  }
                } else {
                  if (dataObject.getIcon().startsWith("http://")) {
                    csRow.setImageUrl(dataObject.getIcon());
                  } else {
                    csRow.setImageUrl(ApplicationStatus.getCurrentPath()
                        + nodeToRender.getPath() + dataObject.getIcon());
                  }
                }
              }
            }

            if (Alignment.RIGHT.equals(dataObject.getDescriptionAlign())) {
              csRow.setValue(dataObject.getDescription());
              if (dataObject.getDescriptionColor() != null
                  && dataObject.getDescriptionColor().length() > 0) {
                csRow.setAlternativeValueFontColor(Color.parseColor(dataObject
                    .getDescriptionColor()));
              }
            } else {
              csRow.setSubCaption(dataObject.getDescription());
            }
            csRow.setUseDisclosureSign(dataObject.getPath() != null);
          }
        }
        csRow.setTag(dataObject);
        item.put(RowData.ROW_KEY, csRow);
        list.add(item);
      }

      if (Configuration.getInstance().isConnected()
          && nodeToRender.hasMoreData()) {
        HashMap<String, RowData> item = new HashMap<String, RowData>();
        RowData csRow = new LoadMoreDataRowData();
        csRow.setTag("loadmore");
        item.put(RowData.ROW_KEY, csRow);
        list.add(item);

        ((PullToRefreshListView) listView)
            .setEndlessScrollListener(new EndlessScrollListener(0,
                (OnIsBottomOverScrollListener) listView));
        ((PullToRefreshListView) listView)
            .setOnLoadMoreDataListener(new OnLoadMoreDataListener() {
              @Override
              public void onLoadMore(int delay) {
                reloadMoreData = true;
                Map<String, RowData> itemData =
                    (Map<String, RowData>) listView.getItemAtPosition(listView
                        .getLastVisiblePosition());
                RowData dataRow = itemData.get(RowData.ROW_KEY);
                View v = dataRow.getView();
                if (v != null) {
                  v.findViewById(R.id.pull_to_refresh_progress_load_more)
                      .setVisibility(View.VISIBLE);
                  v.findViewById(R.id.downArrow).setVisibility(View.GONE);
                  ((TextView) v.findViewById(R.id.loadMoreText))
                      .setText("Loading...");
                }
                reloadMoreDataCallback(delay, false);
              }
            });
      } else {
        ((PullToRefreshListView) listView)
            .setEndlessScrollListener(new EndlessScrollListener(0,
                (OnIsBottomOverScrollListener) listView));
        ((PullToRefreshListView) listView).setOnLoadMoreDataListener(null);
      }
    }

    adapter.notifyDataSetChanged();

    if (reloadMoreData) {
      listView.setSelected(true);
      listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
      listView.setSelection(lastSelectedItem);
      ((PullToRefreshListView) listView).onRefreshComplete();
    } else {
      ((PullToRefreshListView) listView).onRefreshComplete();
      ((PullToRefreshListView) listView).forcePullToRefreshViewHidden(list
          .size());
    }

    reloadMoreData = false;

    if (nodeToRender != null && nodeToRender.getViewTitle() != null) {
      getTitleView().setText(nodeToRender.getViewTitle());
    } else {
      getTitleView().setText(Configuration.getInstance().getProductName());
    }
  }

  public void updateListView() {
    updateListView(nodeToRender.getPayload());
  }

  private void makeIconView() {

    int i = 0;
    if (nodeToRender == null || nodeToRender.getPayload() == null
        || nodeToRender.getPayload().size() <= 0) {
      log.error("No items to display on icon view");
    } else {
      for (JenkinsCloudDataNode node : nodeToRender.getPayload()) {
        ImageButton button = new ImageButton(this);
        if (node.getIconBmp() != null) {
          button.setImageBitmap(node.getIconBmp());
        } else {
          ImageDownloader downloader = ImageDownloader.getInstance();
          downloader.preloadImage(ApplicationStatus.getCurrentPath(), node);

          ImageDownloader.getInstance().setImageBitmap(button,
              ApplicationStatus.getCurrentPath() + node.getIcon());
        }
        button.setScaleType(ScaleType.FIT_CENTER);
        button.setBackgroundColor(0x00FFFFFF);
        button.setTag(node);
        button.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View paramView) {
            JenkinsCloudDataNode __node =
                (JenkinsCloudDataNode) paramView.getTag();
            String action = __node.getAction();
            if (action != null) {
              reactToAction(__node, action);
            } else {
              GenericListActivity.setNodeToRender(__node);
              Intent newConfIntent = new Intent();
              newConfIntent.setClassName(Configuration.ACTIVITY_PACKAGE_NAME,
                  GenericListActivity.class.getName());
              startActivity(newConfIntent);
            }
          }
        });

        TextView text = new TextView(this);
        text.setText(node.getTitle());
        text.setGravity(Gravity.CENTER);

        TableRow btnRow = null;
        TableRow descRow = null;
        switch (i / 3) {
          case 0:
            btnRow = (TableRow) findViewById(R.id.tableRow1);
            descRow = (TableRow) findViewById(R.id.tableRow2);
            break;
          case 1:
            btnRow = (TableRow) findViewById(R.id.tableRow3);
            descRow = (TableRow) findViewById(R.id.tableRow4);
            break;
          case 2:
            btnRow = (TableRow) findViewById(R.id.tableRow5);
            descRow = (TableRow) findViewById(R.id.tableRow6);
            break;
        }
        i++;
        int btnWidth = getScreenWidth() / 4;
        btnRow.addView(button, new TableRow.LayoutParams(btnWidth, btnWidth,
            1.f));
        descRow.addView(text, new TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT, 1.f));
      }
    }

    getTitleView().setText(Configuration.getInstance().getProductName());

    setLeftButtonImage(R.drawable.settings300);

    getLeftButton().setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        onSettingsClick(null);
      }
    });
    
    updateRightButtonImage();
    getRightButton().setOnClickListener(new ReconnectRootNode.ConnectNodeOnClickListener(this));
  }

	private int getScreenWidth() {
		Display display = getWindowManager().getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		return width < height ? width /* Portrait */ : height /* Height */;
	}

public void onSettingsClick(View v) {

    Intent intent = new Intent();
    intent.setClass(this, Configurator2Activity.class);
    startActivity(intent);
    overridePendingTransition(R.anim.grow_from_middle, R.anim.hold);
  }

  private void reactToAction(JenkinsCloudDataNode object, String action) {

    URI uri = null;
    String scheme = null;
    try {
      uri = new URI(action);
      scheme = uri.getScheme();
    } catch (URISyntaxException e) {
      Logger.getInstance().debug("Invalid URI " + action + ": assuming a relative path");
    }

    if(scheme == null) {
    	displayWebView(ApplicationStatus.getCurrentPath() + action);
    } else if (scheme.toLowerCase().startsWith("http")) {
      displayWebView(action);
    } else if ("select".equalsIgnoreCase(scheme)) {
      filterListView(uri);
    } else if ("mailto".equalsIgnoreCase(scheme)) {
      // TODO
    } else if ("install".equalsIgnoreCase(scheme)) {
      installAPK(uri);
    } else if ("market".equalsIgnoreCase(scheme)) {
      openGooglePlay(uri);
    } else if ("menu".equalsIgnoreCase(scheme)) {
      openMenu();
    } else if ("post".equalsIgnoreCase(scheme)) {
    	openDialogForPost(object, uri);
    }
    else {

      JenkinsCloudAPIClient client = new JenkinsCloudAPIClient(this);
      Map<String, String> headers = Configuration.getInstance().getRequestHeaders();
      client.doGet(false, ApplicationStatus.getCurrentPath() + uri.getPath(),
          new SyncCallback<JenkinsCloudNode>() {

            @Override
            public void onSuccess(JenkinsCloudNode result) {
              nodeToRender = (JenkinsCloudDataNode) result;
              reloadData(true);
            }

            @Override
            public void onFailure(Throwable e) {
              // TODO Auto-generated method stub
            }
          }, JenkinsCloudDataNode.class, headers, null, null);
    }
  }

	private void openDialogForPost(JenkinsCloudDataNode object, final URI uri) {
		final String msgQuery = uri.getQuery();
		final String msgPath = object.getPath();

		if (msgQuery != null) {
			String msgText = URLDecoder.decode(msgQuery.substring(msgQuery.indexOf('=') + 1));
			final EditText postInput = new EditText(this);
			postInput.setLines(5);
			postInput.setScrollBarStyle(EditText.SCROLLBARS_OUTSIDE_INSET);
			postInput.setGravity(Gravity.TOP | Gravity.LEFT);
			Builder postDialog = new AlertDialog.Builder(this).setMessage(
					msgText).setView(postInput);

			postDialog.setNeutralButton("Submit",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int whichButton) {
							Editable value = postInput.getText();
							
							JenkinsCloudDataNode postNode = new JenkinsCloudDataNode();
							postNode.setPath(msgPath);
							postNode.setPost(value.toString().getBytes(), "text/plain; charset=utf-8");
							
				            GenericListActivity.setNodeToRender(postNode);
				            Intent newConfIntent = new Intent();
				            newConfIntent.setClassName(Configuration.ACTIVITY_PACKAGE_NAME,
				                  GenericListActivity.class.getName());
				            startActivity(newConfIntent);
						}
					});
			postDialog.show();
		}
	}

	private void openMenu() {
		openOptionsMenu();
	}

  private void openGooglePlay(URI uri) {
    String appQuery = uri.getQuery();
    Matcher appNameMatcher =
        Pattern.compile("id=([^=&\\?]+)").matcher(appQuery);
    if (!appNameMatcher.find()) {
      log.error("Invalid GooglePlay URL='" + uri
          + "': cannot find id=<appId> part");
      return;
    }
    String appName = appNameMatcher.group(1);

    try {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri.toString())));
    } catch (android.content.ActivityNotFoundException e) {
      log.error("Cannot find Google Play client", e);
      startActivity(new Intent(Intent.ACTION_VIEW,
          Uri.parse("http://play.google.com/store/apps/details?id=" + appName)));
    }
  }

  private void displayWebView(final String url) {
    
    dialog = new ProgressDialog(this);
    dialog.setCancelable(true);
    dialog.setMessage(getString(R.string.loading));
    dialog.show();
    
    new AsyncTask<Void, Void, JenkinsCloudPage>() {
      
      JenkinsCloudPage page = null;
  
      @Override
      protected JenkinsCloudPage doInBackground(Void... params) {
        JenkinsCloudAPIClient client = new JenkinsCloudAPIClient(GenericListActivity.this);
        Map<String, String> headers = Configuration.getInstance().getRequestHeaders();
        
        final Semaphore clientPending = new Semaphore(1);
  
        try {
          clientPending.acquire();
        } catch (InterruptedException e2) {
        }
        
        client.doGet(false, url,
            new SyncCallback<JenkinsCloudNode>() {
  
              @Override
              public void onSuccess(JenkinsCloudNode result) {
                if (result instanceof JenkinsCloudPage) {
                  page = (JenkinsCloudPage) result;
                  LocalStorage.getInstance().putNode(url, result);
                  log.debug("Page retrieved: " + getPageDetails());
                }
                clientPending.release();
              }
  
              private String getPageDetails() {
                String pageDetails = "ETag=" + page.getEtag() + ", Title=" + page.getTitle() + ", Size=" + page.html.length()/1024 + " kB";
                return pageDetails;
              }
  
              @Override
              public void onFailure(Throwable e) {
                log.error("Cannot load url " + url, e);
    
                page = LocalStorage.getInstance().getPage(url);
                if (page != null) {
                  log.debug("Display latest cached data: " + getPageDetails());
                }
                clientPending.release();
              }
            }, JenkinsCloudPage.class, headers, null, null);
        
          try {
            clientPending.acquire();
          } catch (InterruptedException e1) {
            log.error("No response from web server");
          }        
        
        return page;
      }
      
      @Override
      protected void onPostExecute(JenkinsCloudPage result) {
        dialog.dismiss();
  
        if(result == null) {
          log.error("Cannot retrieve page on url " + url);
          return;
        }
        
        if (page.refreshRequest) {
          reloadData();
        } else {
          log.debug("Starting BrowserDisplay on url " + url + " (page="
              + LocalStorage.getInstance().getPage(url) + ")");
          Intent newConfIntent = new Intent();
          newConfIntent.putExtra("url", url);
          newConfIntent.putExtra("title", result.getTitle());
          newConfIntent.putExtra("contentType", result.contentType);
          newConfIntent.setClassName(Configuration.ACTIVITY_PACKAGE_NAME,
              BrowserDisplay.class.getName());
          startActivity(newConfIntent);
        }
      };
      
    }.execute();
  
    return;
  }

  private void filterListView(URI uri) {
    String field = uri.getHost();
    if(field == null) {
    	return;
    }
    
    String path = uri.getPath();
    if(path == null) {
    	return;
    }
    
    String value = URLDecoder.decode(path.substring(1)).trim();

    // supported ONLY in LIST Layout
    if (nodeToRender.getLayout() == Layout.LIST) {

      if ("*".equals(value)) {

        this.updateListView();
        return;
      }

      List<JenkinsCloudDataNode> filteredList =
          new LinkedList<JenkinsCloudDataNode>();
      for (JenkinsCloudDataNode node : nodeToRender.getPayload()) {
        try {
          Field nodeField = node.getClass().getDeclaredField(field);
          nodeField.setAccessible(true);
          String nodeFieldValue = (String) nodeField.get(node);
          if (nodeFieldValue == null || nodeFieldValue.trim().equalsIgnoreCase(value)) {
            filteredList.add(node);
          }
        } catch (SecurityException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (NoSuchFieldException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IllegalArgumentException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        } catch (IllegalAccessException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      updateListView(filteredList);
    }
  }

  private void installAPK(URI uri) {

    final LoadingView loadingView = LoadingView.getInstance(this);
    loadingView.setMessage("Init transfer ...");
    loadingView.show();
    
    AsyncTask<URI, String, Void> downloadTask = new AsyncTask<URI, String, Void>() {
      
      String downloadedAPKFileName = null;
      Throwable downloadError = null;

      @Override
      protected Void doInBackground(URI... params) {        
        URI uri = params[0];
        
        HudsonMobiAsyncHttpClient client = new HudsonMobiAsyncHttpClient();
        Map<String, String> headers = Configuration.getInstance().getRequestHeaders();
        client.setUserHeaders(headers);

        String downloadUrl;
        if (uri.getHost() != null) {
          downloadUrl = uri.toString();
        } else {
          downloadUrl =
              ApplicationStatus.getCurrentPath() + uri.getRawSchemeSpecificPart();
        }
        final String fullArtfUrl = downloadUrl;
        
        publishProgress("Connect ...");

        client.call2Synch(false, fullArtfUrl, new SyncCallback<InputStream>() {
          @Override
          public void onSuccess(InputStream result) {
            try {
              publishProgress("Download APK ...");
              String baseName = getFileBaseName(fullArtfUrl);
              Logger.getInstance().debug("basename='" + baseName + "'");
              String tmpFileName =
                  Configuration.getInstance().getPrivateFolderPath()
                      + File.separator + baseName;
              Logger.getInstance().debug(
                  "downloadedAPKFileName='" + tmpFileName + "'");
              byte[] buffer = new byte[4096];
              int read = -1;
              BufferedOutputStream fout =
                  new BufferedOutputStream(new FileOutputStream(
                      tmpFileName));
              int size = 0;
              int prevSize = 0;
              try {
                while ((read = result.read(buffer)) > 0) {
                  fout.write(buffer, 0, read);
                  size += read;
                  if((size - prevSize) > 100*1024) {
                    publishProgress("Download APK ... " + (size / 1024) + " kB");
                    prevSize = size;
                  }
                }
                Logger.getInstance().debug("Download COMPLETED");
                Thread.sleep(1000L);
              } finally {
                fout.close();
                result.close();
              }
              
              publishProgress("Authorising APK ...");

              String permission = "666";
                String command =
                    "chmod " + permission + " " + tmpFileName;
                Runtime runtime = Runtime.getRuntime();
                Process proc = runtime.exec(command);
                int exitProc = proc.waitFor();
                if(exitProc != 0) {
                  throw new IOException("Command " + command + " failed with exit code " + exitProc);
                }
                Thread.sleep(1000L);
              
              downloadedAPKFileName = tmpFileName;
              publishProgress("Setup ...");
            } catch (Exception e) {
              downloadError = e;
              showExceptionOccurredAlert(e);
            } 
          }

          private String getFileBaseName(String artfFullPath) {
            return UUID.nameUUIDFromBytes(artfFullPath.getBytes()).toString()
                + ".apk";
          }

          @Override
          public void onFailure(Throwable e) {
            publishProgress("Failed");
            downloadError = e;
          }
        });
        
        return null;
      }
      
      @Override
      protected void onProgressUpdate(String... progress) {
        log.debug("Download progress: " + progress[0]);
        loadingView.setMessage(progress[0]);
      }
      
        @Override
        protected void onPostExecute(Void v) {
          loadingView.dismiss();

          if (downloadError != null || downloadedAPKFileName == null) {
            showExceptionOccurredAlert(downloadError);
            return;
          } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(
                Uri.fromFile(new File(downloadedAPKFileName)),
                "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
          }
        }
    };
    
    downloadTask.execute(uri);
  }

  public static void ignoreLoading() {
    ignoreLoading = true;
  }

  public void onInfoButtonClick(View v) {
    Intent myIntent = new Intent();
    myIntent.setClass(this, ProductInfoActivity.class);
    startActivity(myIntent);
  }


}
