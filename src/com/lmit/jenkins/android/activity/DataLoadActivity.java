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

import java.util.ArrayList;
import java.util.HashMap;

import android.os.Bundle;

import com.lmit.jenkins.android.adapter.AbstractHudsonDroidListAdapter;
import com.lmit.jenkins.android.adapter.RowData;
import com.lmit.jenkins.android.addon.BackgroundAnimationScheduler;
import com.lmit.jenkins.android.addon.BackgroundWorker;
import com.lmit.jenkins.android.addon.IDataLoader;
import com.lmit.jenkins.android.addon.IThreadNotificationListener;
import com.lmit.jenkins.android.addon.IUIUpdateable;
import com.lmit.jenkins.android.addon.LoadingView;

public abstract class DataLoadActivity extends AbstractActivity implements
    IDataLoader, IUIUpdateable {

  protected ArrayList<HashMap<String, RowData>> list;
  protected AbstractHudsonDroidListAdapter adapter;
  protected String loadingProgressMessage = null;
  protected boolean loadDataAfterUISetup = true;
  protected boolean loaded = false;
  protected boolean neverUseFeedbackMsg = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

  }
  
  @Override
  protected void onStart() {
    super.onStart();

    if (loadDataAfterUISetup && !loaded) {
      reloadData();
    }
  }

  public void setLoadingProgressMessage(String loadingProgressMessage) {
    this.loadingProgressMessage = loadingProgressMessage;
  }

  protected void reloadData(String newMessage, boolean withFeedback,
      boolean forceRefresh, int delay) {
    errorOccurred = false;

    BackgroundAnimationScheduler.getInstance().clear();

    if (withFeedback && !neverUseFeedbackMsg) {
      LoadingView loadingView = LoadingView.getInstance(this);
      if (newMessage != null) {
        loadingView.setMessage(newMessage);
      }
      loadingView.show();
    }

    BackgroundWorker bw =
        new BackgroundWorker(this, new DataLoadThreadNotificationListener());
    bw.setDelay(delay);
    bw.setForceRefresh(forceRefresh);
    bw.start();
  }

  protected void reloadData(String newMessage, int delay) {

    reloadData(newMessage, true, false,delay);
  }
  
  protected void reloadData(String newMessage) {

    reloadData(newMessage, true, false,0);
  }

  protected void reloadData() {

    reloadData(loadingProgressMessage, true, false,0);
  }

  protected void reloadData(boolean force) {

    reloadData(loadingProgressMessage, false, force,0);
  }

  protected void reloadDataNoFeedback() {

    reloadData(loadingProgressMessage, false, false,0);
  }

  class DataLoadThreadNotificationListener implements
      IThreadNotificationListener {

    @Override
    public void onThreadEnded(int code, String msg) {

      if (!exitGracefully) {

        if (updateUI()) {
          setUpNavigationBar();
        }
        
        if(!errorOccurred){
          update();
        }

        LoadingView.remove();
        loaded = true;
      }
    }
  }
}
