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
package com.lmit.jenkins.android.addon;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class BackgroundWorker extends Thread {

  private Handler handler;
  private IDataLoader dataloader;
  private int repeatTimes = 0;
  private boolean forceRefresh = false;
  private int delay = 0;

  public BackgroundWorker(IDataLoader dataLoader,
      IThreadNotificationListener threadNotificatonListener) {

    this(dataLoader, threadNotificatonListener, 0);
  }

  public BackgroundWorker(IDataLoader dataLoader,
      IThreadNotificationListener threadNotificatonListener, int repeatTimes) {
    this.repeatTimes = repeatTimes;
    this.dataloader = dataLoader;
    this.handler = new BackgroundWorkerHandler(threadNotificatonListener);
  }

  public boolean isForceRefresh() {
    return forceRefresh;
  }

  public void setForceRefresh(boolean forceRefresh) {
    this.forceRefresh = forceRefresh;
  }

  @Override
  public void run() {
    Looper.prepare();

    if(delay>0){
      
      try {
        Thread.sleep(delay);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    
    while (repeatTimes > -1) {
      
      if (dataloader != null) {
        if (forceRefresh) {
          dataloader.loadDataForceRefresh();
        } else {
          dataloader.loadData();
        }
      }

      handler.sendEmptyMessage(0);

      repeatTimes--;
    }

    Looper.loop();
  }

  private class BackgroundWorkerHandler extends Handler {

    private IThreadNotificationListener listener;

    public BackgroundWorkerHandler(IThreadNotificationListener listener) {

      this.listener = listener;
    }

    @Override
    public void handleMessage(Message msg) {

      listener.onThreadEnded(0, null);
    }
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }
}
