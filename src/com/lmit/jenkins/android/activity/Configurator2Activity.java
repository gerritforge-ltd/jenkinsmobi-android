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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import com.lmit.jenkins.android.addon.ImageCache;
import com.lmit.jenkins.android.addon.LocalStorage;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkins.android.networking.TwoPhaseAuthenticationRequiredException;

public class Configurator2Activity extends PreferenceActivity {

  private static final long SLEEP_BETWEEN_PROGRESS_MESSAGES = 500L;


  private class PreferenceChangeListener implements OnPreferenceChangeListener {
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
      if (!((EditTextPreference) preference).getText().equals(newValue)) {
        configurationChanged = true;
      }

      setPreferenceSummary(preference, newValue);
      return true;
    }

  }

  private boolean configurationChanged = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle(R.string.configurator_view_title);
    addPreferencesFromResource(R.layout.preferences_layout);

    loadFromConfiguration();
  }

  private void loadFromConfiguration() {
    Configuration conf = Configuration.getInstance();
    loadPreference(Configuration.KEY_JENKINS_URL,
        R.string.conf_entry_hostname_hint, conf.jenkinsUrl);
    loadPreference(Configuration.KEY_JENKINS_USERNAME,
        R.string.conf_entry_username_hint, conf.jenkinsUsername);
    loadPreference(Configuration.KEY_JENKINS_PASSWORD,
        R.string.conf_entry_password_hint, conf.jenkinsPassword);
  }

  private void loadPreference(String prefKey, int summaryStringKey, String value) {
    EditTextPreference editTextPref;
    editTextPref = (EditTextPreference) findPreference(prefKey);
    editTextPref.getEditText().setHint(summaryStringKey);
    setPreferenceSummary(editTextPref, value);
    editTextPref.setText(value);
    editTextPref.setOnPreferenceChangeListener(new PreferenceChangeListener());
  }

  private void setPreferenceSummary(Preference preference, Object newValue) {
    if (preference.getTitle().toString().toLowerCase().indexOf("password") >= 0) {
      preference.setSummary("***********");
    } else {
      preference.setSummary(newValue != null ? newValue.toString() : "");
    }
  }

  private String getPreference(String prefKey) {
    return ((EditTextPreference) findPreference(prefKey)).getText();
  }

  private void doSave(final String otp) {
    final ProgressDialog saveProgress =
        new ProgressDialog(Configurator2Activity.this);

    AsyncTask<Void, String, Boolean> saveTask =
        new AsyncTask<Void, String, Boolean>() {
      
      String validationResult = "";
      TwoPhaseAuthenticationRequiredException otpRequest;

          @Override
          protected void onPreExecute() {
            super.onPreExecute();
            saveProgress.setMessage(getString(R.string.conf_save_progress));
            saveProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            saveProgress.show();
          }

          @Override
          protected Boolean doInBackground(Void... params) {
            Configuration conf = Configuration.getInstance();
            conf.jenkinsUrl = getPreference(Configuration.KEY_JENKINS_URL);
            conf.jenkinsUsername =
                getPreference(Configuration.KEY_JENKINS_USERNAME);
            conf.jenkinsPassword =
                getPreference(Configuration.KEY_JENKINS_PASSWORD);

            sleep();
            publishProgress(getString(R.string.conf_save_validation));
            try {
              validationResult = conf.validate(otp);
            } catch (TwoPhaseAuthenticationRequiredException e) {
              otpRequest = e;
              return false;
            }
            sleep();
            if (validationResult != null && validationResult.equals(conf.VALIDATION_SUCCEDED)) {
              publishProgress(getString(R.string.conf_save_to_profile));
              conf.save();
              LocalStorage.getInstance().cleanAll();
              ImageCache.clean();
              sleep();
              return true;
            } else {
              return false;
            }
          }

          private void sleep() {
            try {
              Thread.sleep(SLEEP_BETWEEN_PROGRESS_MESSAGES);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }

          @Override
          protected void onProgressUpdate(String... progressMessage) {
            super.onProgressUpdate(progressMessage);
            saveProgress.setMessage(progressMessage[0]);
          }

          @Override
          protected void onPostExecute(Boolean validationSucceded) {
            saveProgress.dismiss();
            if (validationSucceded) {
              restartHomeActivity();
            } else {
              if (otpRequest != null) {
                final EditText input = new EditText(Configurator2Activity.this);

                Builder otpAlert =
                    new AlertDialog.Builder(Configurator2Activity.this)
                        .setMessage(otpRequest.getLocalizedMessage()).setView(
                            input);

                final String appId = otpRequest.getAuthAppId();
                if (appId == null) {
                  otpAlert.setNeutralButton("Verify",
                      new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                            int whichButton) {
                          Editable value = input.getText();
                          doSave(value.toString());
                        }
                      });
                } else {
                  otpAlert.setPositiveButton("Verify",
                      new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                            int whichButton) {
                          Editable value = input.getText();
                          doSave(value.toString());
                        }
                      }).setNegativeButton("Auth App", null);
                  AlertDialog dialog = otpAlert.show();

                  Button theButton =
                      dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                  theButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                      Intent i = new Intent();
                      PackageManager manager = getPackageManager();
                      i = manager.getLaunchIntentForPackage(appId);
                      i.addCategory(Intent.CATEGORY_LAUNCHER);
                      startActivity(i);
                    }
                  });
                }
              } else {
              AlertDialog.Builder alertbox = new AlertDialog.Builder(Configurator2Activity.this)
              .setMessage(getString(R.string.conf_save_validation_failed, validationResult))
              .setNeutralButton("OK", null);
              alertbox.show();
              }
            }
          }
        };

    saveTask.execute(null);
  }

  
  private void restartHomeActivity() {
    finish();
    Configuration.getInstance().setConnected(true, true);
    Intent intent = new Intent(this, HudsonDroidHomeActivity.class);
    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem menuItem;

    menuItem = menu.add("Send feedback");
    menuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
      @Override
      public boolean onMenuItemClick(MenuItem paramMenuItem) {
        showMailerToSendCrashReport();
        return true;
      }
    });

    return super.onCreateOptionsMenu(menu);
  }

  private void showMailerToSendCrashReport() {

    /* Create the Intent */
    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);

    /* Fill it with Data */
    emailIntent.setType("plain/text");
    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
        new String[] {Configuration.SUPPORT_MAIL_ADDRESS});
    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
        "JenkinsMobi log");
    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
        "JenkinsMobi log attached to this message");
    emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,
        Uri.fromFile(Logger.getInstance().getLogFile()));
    /* Send it off to the Activity-Chooser */
    startActivity(Intent.createChooser(emailIntent,
        getText(R.string.crash_email_subject).toString()));
  }

  @Override
  public void onBackPressed() {
    if (configurationChanged) {
      askConfirmationAndSaveChanges();
    } else {
      super.onBackPressed();
    }
  }

  public void askConfirmationAndSaveChanges() {
    AlertDialog alert = new AlertDialog.Builder(this).create();
    alert.setTitle(R.string.conf_save_question);
    alert.setButton(Dialog.BUTTON_POSITIVE, getText(R.string.yes).toString(),
        new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            doSave(null);
          }
        });
    alert.setButton(Dialog.BUTTON_NEGATIVE, getText(R.string.no).toString(),
        new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            finish();
          }
        });
    alert.setCanceledOnTouchOutside(false);
    alert.show();
  }

  protected void resetLocalStorage() {
    LocalStorage.getInstance().cleanAll();
    ImageCache.clean();
  }

  private void createAccountWebView() {
    Dialog dialog = new Dialog(this);
    dialog.setContentView(R.layout.create_account);
    WebView wb = (WebView) dialog.findViewById(R.id.webview);
    wb.getSettings().setJavaScriptEnabled(true);
    wb.setWebViewClient(new CreateAccountWebViewClient(dialog, this));
    wb.loadUrl(getCreateServiceAccountURL());
    dialog.setCancelable(true);
    dialog.setTitle("Create Account");
    dialog.show();
  }


  private String getCreateServiceAccountURL() {
    String baseUrl =
        ((EditTextPreference) findPreference(Configuration.KEY_SERVICE_HOSTNAME))
            .getText();
    if (!baseUrl.endsWith("/")) {
      baseUrl += "/";
    }
    return baseUrl + "signup.jsp";
  }
}
