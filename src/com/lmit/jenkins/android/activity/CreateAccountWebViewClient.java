package com.lmit.jenkins.android.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.lmit.jenkins.android.logger.Logger;

public class CreateAccountWebViewClient extends WebViewClient {
  private Logger log = Logger.getInstance();
  private Dialog webViewDialog;
  private Configurator2Activity configActivity;
  
  public CreateAccountWebViewClient(Dialog webViewDialog, Configurator2Activity configurator) {
    this.webViewDialog = webViewDialog;
    this.configActivity = configurator;
  }
  
  @Override
  public void onPageStarted(WebView view, String url, Bitmap favicon) {
    view.addJavascriptInterface(this, "createAccount");
  }

  public void success(String user, String password) {
    log.debug("Sign-Up of user " + user + " SUCCEDED");
    webViewDialog.dismiss();
    // FIXME: to be enabled again when the preferences will only point to the JenkinsCloud instance
//    configActivity.setUsername(user);
//    configActivity.setPassword(password);
//    configActivity.saveConfiguration();
  }
  
  public void failed(String reason) {
    webViewDialog.dismiss();
    log.error("Sign-Up failed: " + reason);
    new AlertDialog.Builder(configActivity)
        .setTitle(configActivity.getString(R.string.signup_failed))
        .setMessage(reason).setNeutralButton("Close", null).show();
  }
}
