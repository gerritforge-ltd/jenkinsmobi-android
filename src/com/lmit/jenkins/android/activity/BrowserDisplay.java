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

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lmit.jenkins.android.activity.R;
import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkinscloud.commons.JenkinsCloudPage;

public class BrowserDisplay extends Activity {

  private WebView myWebView;
  private String url;

  private boolean running;
  private Menu actionMenu;
  private static final int MENU_STOP_REFRESH = 1;
  private boolean showStopReloadControls = true;
private JenkinsCloudPage htmlPage;
private String title;
private MenuItem menuItem;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    if (showStopReloadControls) {
      actionMenu = menu;

      if (running) {
        menuItem = menu.add(0, MENU_STOP_REFRESH, 0, getText(R.string.stop));
        menuItem.setIcon(
            R.drawable.navigation_cancel_dark);
      } else {
        menuItem = menu.add(0, MENU_STOP_REFRESH, 0, getText(R.string.reload));
        menuItem.setIcon(
            R.drawable.refresh_dark);
      }
      return true;
    } else {

      return false;
    }

  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {

    super.onMenuItemSelected(featureId, item);

    if (!running) {

      // we are stopped, restart
      setProgressBarIndeterminateVisibility(true);
      setProgressBarVisibility(true);

      running = true;
      item.setTitle(R.string.stop);
      menuItem.setIcon(
          R.drawable.navigation_cancel_dark);

      myWebView.reload();

    } else {

      myWebView.stopLoading();

      setProgressBarIndeterminateVisibility(false);
      setProgressBarVisibility(false);
      running = false;
      item.setTitle(R.string.reload);
      menuItem.setIcon(
          R.drawable.refresh_dark);
    }

    return true;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    requestWindowFeature(Window.FEATURE_PROGRESS);

    showStopReloadControls =
        getIntent().getBooleanExtra("showStopReloadControls", true);
    url = getIntent().getStringExtra("url");
    title = getIntent().getStringExtra("title");
    htmlPage = LocalStorage.getInstance().getPage(url);

    myWebView = new WebView(this);
    setContentView(myWebView);

    setProgressBarIndeterminateVisibility(true);
    setProgressBarVisibility(true);

    if (title != null) {
      setTitle(title);
    } else {
      try {
        setTitle(new URL(url).getFile());
      } catch (MalformedURLException e) {
        setTitle(url);
      }
    }

    running = true;

    new WebViewTask(this).execute();
  }

  private class WebViewTask extends AsyncTask<Void, Void, Boolean> {

    Activity parent;

    public WebViewTask(Activity parent) {
      this.parent = parent;
    }

    @Override
    protected void onPreExecute() {

      super.onPreExecute();
    }

    protected Boolean doInBackground(Void... param) {

      SystemClock.sleep(1000);

      return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {

      WebSettings webSettings = myWebView.getSettings();
      webSettings.setJavaScriptEnabled(true);
      webSettings.setBuiltInZoomControls(true);
      webSettings.setUseWideViewPort(true);

      myWebView.setPadding(0, 0, 0, 0);
      myWebView.setInitialScale(100);

      myWebView.setWebChromeClient(new WebChromeClient() {
        public void onProgressChanged(WebView view, int progress) {
          parent.setProgress(progress * 100);
        }
      });

      myWebView.setWebViewClient(new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
          // TODO Auto-generated method stub
          return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view,
            HttpAuthHandler handler, String host, String realm) {

          String username = Configuration.getInstance().getUsername();
          String password = Configuration.getInstance().getPassword();

          handler.proceed(username, password);
        }

        @Override
        public void onPageFinished(WebView view, String url) {

          running = false;
          if (actionMenu != null) {
            actionMenu.getItem(0).setTitle(R.string.reload);
          }
        }
      });

      if (htmlPage == null) {
		if (!url.startsWith("http")) {
			String hudsonHostname = Configuration.getInstance()
					.getHudsonHostname();
			if (!hudsonHostname.endsWith("/") && !url.startsWith("/")) {
				url = "/" + url;
			} else if (hudsonHostname.endsWith("/")
					&& url.startsWith("/")) {
				url = url.substring(1);
			}
			url = hudsonHostname + url;
		}
        myWebView.loadUrl(url);
      } else {
		String[] contentTypeParts = htmlPage.contentType.split(";");
		String contentType = contentTypeParts[0].trim();
		String charSet = "utf-8";
		for (int i = 1; i < contentTypeParts.length; i++) {
			String contentTypePart = contentTypeParts[i].trim();
			if (contentTypePart.startsWith("charset=")) {
				charSet = contentTypePart.substring(contentTypePart
						.indexOf('=') + 1);
			}
		}
        myWebView.loadDataWithBaseURL(url, htmlPage.html, contentType, charSet, null);
      }
    }
  }
}
