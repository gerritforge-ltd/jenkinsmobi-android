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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.lmit.jenkins.android.addon.AlertBox;
import com.lmit.jenkins.android.addon.ApplicationStatus;
import com.lmit.jenkins.android.addon.BackgroundAnimationScheduler;
import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.addon.NavigationStack;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;

public abstract class AbstractActivity extends Activity {

  protected abstract boolean updateUI();

  // set this to true to avoid message-box showing for an error when exiting
  protected boolean exitGracefully = false;

  // if an error occurs while loading it will be set to true
  protected boolean errorOccurred = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    getWindow().setFormat(PixelFormat.RGBA_8888);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);

    errorOccurred = false;

    BackgroundAnimationScheduler.getInstance().clear();
    BackgroundAnimationScheduler.getInstance().pushHostActivity(this);
  }

  protected void setUpNavigationBar() {

    if (findViewById(R.id.navigationBarLeftButton) != null) {
      Button leftButton = (Button) findViewById(R.id.navigationBarLeftButton);
      TextView title = (TextView) findViewById(R.id.navigationBarTitle);

      Resources res = getResources();
      Drawable moreIcon = res.getDrawable(R.drawable.more_topbar);
      moreIcon.setBounds(0, 0, getPixelsFromDp(30), getPixelsFromDp(30));

      leftButton.setCompoundDrawables(moreIcon, null, null, null);

      leftButton.setOnClickListener(new LeftButtonOnClickListener(this));
      title.setText(Configuration.getInstance().getProductName());
    }
  }

  protected Button getLeftButton() {

    return (Button) findViewById(R.id.navigationBarLeftButton);
  }

  protected Button getRightButton() {

    return (Button) findViewById(R.id.navigationBarRightButton);
  }
  
  protected TextView getTitleView() {

    return (TextView) findViewById(R.id.navigationBarTitle);
  }

  protected void setLeftButtonText(String text) {

    Button b = getLeftButton();

    b.setCompoundDrawables(null, null, null, null);
    b.setText(text);
    b.setTextAppearance(this, android.R.style.TextAppearance_WindowTitle);
    b.setTextColor(Color.WHITE);
  }
  
  protected void setLeftButtonImage(int resId) {

    Resources res = getResources();
    Drawable image = res.getDrawable(resId);
    image.setBounds(0, 0, getPixelsFromDp(30), getPixelsFromDp(30));
    getLeftButton().setCompoundDrawables(image, null, null, null);
  }
  
  protected int getPixelsFromDp(int dp) {
    final float scale = getResources().getDisplayMetrics().density;
    return (int) (dp * scale + 0.5f);
  }

  protected void setRightButtonText(String text) {

    Button b = getRightButton();

    b.setCompoundDrawables(null, null, null, null);
    b.setText(text);
    b.setTextAppearance(this, android.R.style.TextAppearance_WindowTitle);
    b.setTextColor(Color.WHITE);
  }

  public void updateRightButtonImage() {
    boolean connected = Configuration.getInstance().isConnected();
    int imageId = connected
    ? R.drawable.connected : R.drawable.refresh;
        
    Button b = getRightButton();
    if(b == null) {
      return;
    }
    
    Resources res = getResources();
    Drawable icon = res.getDrawable(imageId);
    icon.setBounds(0, 0, getPixelsFromDp(30), getPixelsFromDp(30));
    b.setCompoundDrawables(icon, null, null, null);
    b.setVisibility(View.VISIBLE);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {

    super.onPostCreate(savedInstanceState);

    postSetupUI();
  }

  protected void postSetupUI() {

    ;
  }

  @Override
  public void onBackPressed() {

    exitGracefully = true;

    BackgroundAnimationScheduler.getInstance().clear();

    BackgroundAnimationScheduler.getInstance().popHostActivity();

    super.onBackPressed();
  }

  protected void showExceptionOccurredAlert(Throwable e) {

    String errorMessage = (e == null ? "Cannot complete download":e.getMessage());
    if(e != null) {
      Logger.getInstance().error(
          "An error has occurred during loading of : '" + getClass().getName()
              + "'", e);
    }
    errorOccurred = true;

    if (!exitGracefully) {
      AlertBox box = new AlertBox(this);
      box.show(errorMessage);
    }
  }

  private class LeftButtonOnClickListener implements OnClickListener {

    private Context parent;

    public LeftButtonOnClickListener(Context parent) {
      this.parent = parent;
    }

    @Override
    public void onClick(View v) {
      GenericListActivity.ignoreLoading();
      NavigationStack.pop();
      JenkinsCloudDataNode rootNode = (JenkinsCloudDataNode) LocalStorage.getInstance().getNode("/qaexplorer");
      rootNode.setPath("/qaexplorer/");
      GenericListActivity.setNodeToRender(rootNode);
      Intent myIntent = new Intent();
      myIntent.putExtra("caller.activity", HudsonDroidHomeActivity.class.getName());
      myIntent.setClassName(Configuration.ACTIVITY_PACKAGE_NAME,
          GenericListActivity.class.getName());
      myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(myIntent);
      overridePendingTransition(R.anim.hold, R.anim.shrink_to_middle);
      finish();
    }
  }
}
