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
package com.lmit.jenkins.android.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.webkit.WebView;

import com.lmit.jenkins.android.activity.GenericListActivity;
import com.lmit.jenkins.android.activity.JenkinsMobi;
import com.lmit.jenkins.android.activity.R;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkins.android.networking.DBAdapter;
import com.lmit.jenkins.android.networking.HudsonMobiSynchHttpClient;
import com.lmit.jenkins.android.networking.TwoPhaseAuthenticationRequiredException;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;
import com.lmit.jenkinscloud.commons.SyncCallback;


public class Configuration {
  public static final Uri DEFAULT_HOME_NODE = new Uri.Builder().path("/qaexplorer/").build();
private static final String JENKINSCLOUD_USERNAME = "jenkinsmobi";
  private static final String JENKINSCLOUD_PASSWORD = "jenkinsmobi";
//  private static final String JENKINSCLOUD_URL = "http://dev.gitent-scm.com:9446/core";
  private static final String JENKINSCLOUD_URL = "http://jenkinscloud.com:9446/core";
  private static final String JENKINSCLOUD_SECRET = "WpoHdtbmnQwMdzhErIWtkZWsgF7tKyOIMcIGhn+n0FHo3Thp/9Dcgg";
  
  /*BEGIN POST ERROR CONFIGURATION*/
  public static final int POST_ERROR_TIME_OUT_CONNECT = 300;
  public static final int POST_ERROR_TIME_OUT_RESPONSE = 300;
  public static final String POST_ERROR_URL = "http://jenkinscloud.com/faults";
  /*END POST ERROR CONFIGURATION*/

	
  public static final float REQUIRED_API_LEVEL = 2.0f;
  
  public static final String SUPPORT_MAIL_ADDRESS = "support@hudson-mobi.com";

  public static final String CONFIGURATION_FILENAME = "hudsonmobile.conf";
  public static final String KEY_SERVICE_HOSTNAME = "service.hostname";
  public static final String KEY_USERNAME = "username";
  public static final String KEY_PASSWORD = "password";
  public static final String KEY_EULA_ACCEPTED = "eula.accepted";
  public static final String KEY_AUDIT_ACCEPTED = "audit.accepted";

  public static final String KEY_PRODUCT_NAME = "product.name";
  public static final String KEY_PRODUCT_VERSION = "product.version";
  public static final String KEY_API_VERSION = "api.version";
  public static final String KEY_MSIS_DN = "msisdn";
  public static final String KEY_SUBSCRIBER_ID = "subscriberid";
  public static final String KEY_LAST_REFRESH_TS = "last.refresh.ts";
  public static final String KEY_SCHEMA_VERSION = "schema.version";
  public static final String KEY_CONNECTED = "connected";
  
  public static final String KEY_JENKINS_URL = "jenkins.url";
  public static final String KEY_JENKINS_USERNAME = "jenkins.username";
  public static final String KEY_JENKINS_PASSWORD = "jenkins.password";
  


  private static Configuration instance;

  private Context appContext;
  public String productVersion;
  public String productName;
  public String privateFolderPath;
  public String targetCIProduct;
  public String deviceLocale;
  public String hudsonHostname;
  private String username;
  private String password;
  public String msisdn;
  public String subscriberId;
  public String schema_version;
  public boolean connected;
  public boolean connectedChanged;
  public String lastRefreshTimestamp;
  public String userAgent; 
  
  public String jenkinsUrl;
  public String jenkinsUsername;
  public String jenkinsPassword;

  public final static String[] ANDROID_RELEASE_CODENAMES = {"", "Base",
      "Base 1.1", "Cupcake", "Donut", "Eclair", "Eclair", "Eclair", "Froyo",
      "Gingerbread", "Gingerbread", "HoneyComb", "HoneyComb", "HoneyComb",
      "Ice Cream Sandwich", "Ice Cream Sandwich"};
  public static final String ACTIVITY_PACKAGE_NAME =
      GenericListActivity.class.getPackage().getName();
  public final String VALIDATION_SUCCEDED;
  public final String OTP_TOKEN_REQUEST;
public String apkFileMD5;
	private String homeNode = DEFAULT_HOME_NODE.getPath();
  private String packageName;

  public Context getAppContext() {
    return appContext;
  }
  
  public String getUserAgent() {
      return userAgent;
  }

  public static String getDeviceSoftwareVersion() {
    return android.os.Build.VERSION.RELEASE;
  }

  // TO BE CALLED FIRST IN THE MAIN APP
  public static Configuration getInstance(Context appContext) {
    if (instance == null) {
      instance = new Configuration(appContext);
      instance.load();
    }

    return instance;
  }

  // WARN: DO NOT call this method prior than getInstance(Context)
  public static Configuration getInstance() {

    if (instance == null) {
      throw new RuntimeException("Instance cannot be null now");
    }

    return instance;
  }

	private Configuration(Context context) {
		this.appContext = context;
		apkFileMD5 = computeApkChecksum(context);
		VALIDATION_SUCCEDED = context
				.getString(R.string.conf_save_validation_succeded);
		OTP_TOKEN_REQUEST = context
				.getString(R.string.conf_save_validation_otp_request);
		packageName = context.getApplicationContext().getPackageName();
	}

	private String computeApkChecksum(Context context) {
		ApplicationInfo appInfo = context.getApplicationInfo();
		FileInputStream apkIn = null;
		try {
			apkIn = new FileInputStream(appInfo.sourceDir);
			try {
				byte[] apkBuff = new byte[6 * 1024];
				MessageDigest md5 = MessageDigest.getInstance("MD5");
				int apkRead = 0;
				while (0 < (apkRead = apkIn.read(apkBuff))) {
					md5.update(apkBuff, 0, apkRead);
				}
				byte[] apkDigest = md5.digest();
				return toHex(apkDigest);
			} catch (NoSuchAlgorithmException e) {
				// This would never happen as MD5 is hardcoded
				Logger.getInstance().error("Cannot compute APK MD5", e);
				return "";
			} finally {
				apkIn.close();
			}
		} catch (IOException e1) {
			// This would never happen as we are in the APK we're pointing to
			Logger.getInstance().error("Cannot read my own APK", e1);
			return "";
		}
	}

	private String toHex(byte[] a) {
		StringBuilder sb = new StringBuilder();
		for (byte b : a)
			sb.append(String.format("%02x", b & 0xff));
		return sb.toString();
	}

  public String getTargetCIProduct() {
    return targetCIProduct;
  }

  public String getDeviceLocale() {
    return deviceLocale;
  }

  public String getPrivateFolderPath() {
    return privateFolderPath;
  }

  public String getExternalStorageState() {
    return Environment.getExternalStorageState();
  }

  public String getProductVersion() {
    return productVersion;
  }

  public String getProductName() {
    return productName;
  }

  public void load() {

    SharedPreferences prefs =
        appContext.getSharedPreferences(CONFIGURATION_FILENAME,
            Context.MODE_PRIVATE);

    deviceLocale =
        appContext.getResources().getConfiguration().locale.getCountry()
            .toLowerCase();
    productName = appContext.getResources().getString(R.string.app_name);
    productVersion = appContext.getResources().getString(R.string.app_version);

    privateFolderPath = appContext.getFilesDir().getAbsolutePath();
    if(Environment.getExternalStorageState()==Environment.MEDIA_MOUNTED){
      privateFolderPath =
          Environment.getExternalStorageDirectory().getAbsolutePath()
              + File.separator + "Android" + File.separator + "data"
              + File.separator + "com.lmit.qaexplorer.android" + File.separator
              + "cache";
    }
    if(!new File(privateFolderPath).exists()){
      boolean writeResult = new File(privateFolderPath).mkdirs();
      Logger.getInstance().debug("Created data dirs: "+ writeResult);
    }

    username = prefs.getString(Configuration.KEY_USERNAME, JENKINSCLOUD_USERNAME);
    password = prefs.getString(Configuration.KEY_PASSWORD, JENKINSCLOUD_PASSWORD);
//    hudsonHostname = prefs.getString(Configuration.KEY_SERVICE_HOSTNAME, JENKINSCLOUD_URL);
  hudsonHostname = JENKINSCLOUD_URL;
    msisdn = prefs.getString(Configuration.KEY_MSIS_DN, "no-msisdn-supplied");
    subscriberId = prefs.getString(Configuration.KEY_SUBSCRIBER_ID, null);
    lastRefreshTimestamp =
        prefs.getString(Configuration.KEY_LAST_REFRESH_TS, "0");
    schema_version =
        prefs.getString(Configuration.KEY_SCHEMA_VERSION, "" + DBAdapter.DATABASE_VERSION);
    connected = prefs.getBoolean(Configuration.KEY_CONNECTED, true);
    connectedChanged = false;

    userAgent =
        new WebView(appContext).getSettings().getUserAgentString() + " APK-MD5/" + apkFileMD5 + " Package/" + packageName;
    
    jenkinsUrl = prefs.getString(Configuration.KEY_JENKINS_URL, "http://hudson-mobi.com/hudson");
    jenkinsUsername = prefs.getString(Configuration.KEY_JENKINS_USERNAME, "guest");
    jenkinsPassword = prefs.getString(Configuration.KEY_JENKINS_PASSWORD, "guest");
    
		Logger.getInstance().info(
				String.format("PackageName:%s APK:%s",
						packageName,
						apkFileMD5));
  }

  public void save() {

    SharedPreferences prefs =
        appContext.getSharedPreferences(CONFIGURATION_FILENAME,
            Context.MODE_PRIVATE);
    Editor prefEditor = prefs.edit();

    prefEditor.putString(Configuration.KEY_SERVICE_HOSTNAME, hudsonHostname);
    prefEditor.putString(Configuration.KEY_USERNAME, username);
    prefEditor.putString(Configuration.KEY_PASSWORD, password);
    prefEditor.putString(Configuration.KEY_MSIS_DN, msisdn);
    prefEditor.putString(Configuration.KEY_SUBSCRIBER_ID, subscriberId);
    prefEditor.putString(Configuration.KEY_SCHEMA_VERSION, schema_version);
    prefEditor.putString(Configuration.KEY_LAST_REFRESH_TS,
        lastRefreshTimestamp);
    prefEditor.putString(Configuration.KEY_JENKINS_URL, jenkinsUrl);
    prefEditor.putString(Configuration.KEY_JENKINS_USERNAME, jenkinsUsername);
    prefEditor.putString(Configuration.KEY_JENKINS_PASSWORD, jenkinsPassword);
    
    if (connectedChanged) {
      prefEditor.putBoolean(Configuration.KEY_CONNECTED, connected);
      connectedChanged = false;
    }
    
    prefEditor.commit();
  }

  public static void resetIstance() {

    instance = null;
  }

  public String getHudsonHostname() {

    return hudsonHostname;
  }

  public String getUsername() {

    return JENKINSCLOUD_SECRET;
  }

  public String getPassword() {

    return subscriberId;
  }

  public void setHudsonHostname(String host) {

    hudsonHostname = host;
  }

  @Override
  public boolean equals(Object o) {

    boolean result = false;

    if (o != null && o instanceof Configuration) {

      Configuration __o = (Configuration) o;
      if (__o.getHudsonHostname() != null && getHudsonHostname() != null) { // check
        // hostname
        result = __o.getHudsonHostname().equals(getHudsonHostname());
      } else {
        result = __o.getHudsonHostname() == null && getHudsonHostname() == null;
      }

      if (result) {
        if (__o.getUsername() != null && getUsername() != null) { // check
                                                                  // username
          result = __o.getUsername().equals(getUsername());
        } else {
          result = __o.getUsername() == null && getUsername() == null;
        }

        if (result) { // if usernames match check passwords

          if (__o.getPassword() != null && getPassword() != null) {
            result = __o.getPassword().equals(getPassword());
          } else {
            result = __o.getPassword() == null && getPassword() == null;
          }
        }
      }
    }

    return result;
  }

  public void setConnected(boolean connected, boolean saveConfig) {
    this.connected = connected;
    this.connectedChanged = saveConfig;
  }

  public boolean isConnected() {
    return this.connected;
  }

  public String getMsisdn() {
    return msisdn;
  }

  public void setMsisdn(String msisdn) {
    this.msisdn = msisdn;
  }

  public String getLastRefreshTimestamp() {
    return lastRefreshTimestamp;
  }

  public void setLastRefreshTimestamp(String lastRefreshTimestamp) {
    this.lastRefreshTimestamp = lastRefreshTimestamp;
  }

  public String getHudsonHostnameOnlyBase() {

    // http://xxxxx:xxx/core
    String host = getHudsonHostname();
    int i = host.lastIndexOf('/');
    return host.substring(0, i);
  }

  public String getSchema_version() {
    return schema_version;
  }

  public void setSchema_version(String schema_version) {
    this.schema_version = schema_version;
  }

  public String getSubscriberId() {
    return subscriberId;
  }

  public void setSubscriberID(String subscriberId) {
    this.subscriberId = subscriberId;
  }

  public HashMap<String, String> getRequestHeaders() {
    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("x-jenkinscloud-msisdn", msisdn);
    headers.put("x-jenkinscloud-subscriberid", subscriberId);
    return headers;
  }

  public String validate(String otp) throws TwoPhaseAuthenticationRequiredException {
    HudsonMobiSynchHttpClient client = new HudsonMobiSynchHttpClient();
    client.setUserHeaders(getRequestHeaders());
    final StringBuilder validationResult = new StringBuilder();
    JenkinsCloudPlugin jenkinsPlugin = getJenkinsPlugin();
    StatusLine statusLine;
    HttpResponse response = null;
    try {
      if (otp != null) {
        Map<String, String> headers = client.getUserHeaders();
        headers.put(HudsonMobiSynchHttpClient.X_AUTH_OTP_HEADER, otp);
      }
      response = client.callPost(jenkinsPlugin.getData(),
          buildUrl(hudsonHostname, "../registry/rpc/config"));
    statusLine = response.getStatusLine();
    int statusCode = statusLine.getStatusCode();
    switch (statusCode) {
      case HttpURLConnection.HTTP_OK:
        validationResult.append(VALIDATION_SUCCEDED);
        break;
      case HttpURLConnection.HTTP_PRECON_FAILED:
        throw new TwoPhaseAuthenticationRequiredException(response);
      default:
        validationResult.append(statusLine.getReasonPhrase());
        break;
    }

    return validationResult.toString();
    } catch (Exception e) {
      if(e instanceof TwoPhaseAuthenticationRequiredException) {
        throw (TwoPhaseAuthenticationRequiredException) e;
      }
      return e.getLocalizedMessage();
    } finally {
      if(response != null) {
        try {
          response.getEntity().consumeContent();
        } catch (IOException e) {
        }
      }
    }
  }

  private JenkinsCloudPlugin getJenkinsPlugin() {
    JenkinsCloudPlugin plugin = new JenkinsCloudPlugin("JenkinsCI");
    plugin.description = "JenkinsCI";
    plugin.type = "JenkinsCI";
    plugin.url = jenkinsUrl;
    plugin.username = jenkinsUsername;
    plugin.password = jenkinsPassword;
    return plugin;
  }

  private String buildUrl(String baseUrl, String path) {
    StringBuilder outPath = new StringBuilder(baseUrl);
    if(!baseUrl.endsWith("/")) {
      outPath.append("/");
    }
    while(path.startsWith("/")) {
      path = path.substring(1);
    } 
    
    outPath.append(path);
    return outPath.toString();
  }

  public String getHomeNode() {
    return this.homeNode;
  }

  public void setHomeNode(Uri intentUri) {
    this.homeNode = intentUri.getPath();
    String apkMd5 = intentUri.getQueryParameter("APK-MD5");
    if(apkMd5 != null) {
      apkFileMD5 = apkMd5;
    } else {
      computeApkChecksum(appContext);
    }
    String packageName = intentUri.getQueryParameter("PackageName");
    if(packageName != null) {
      this.packageName = packageName;
    } else {
      this.packageName = appContext.getPackageName();
    }
    
    load();
  }

    public boolean detectSubscriberId() {
      Logger log = Logger.getInstance();
      boolean simChange = false;
      TelephonyManager telephonyManager =
          (TelephonyManager) this.appContext
              .getSystemService(Context.TELEPHONY_SERVICE);
      String subscriberId = telephonyManager.getSubscriberId();
      String deviceId = telephonyManager.getDeviceId();
      if(deviceId == null || deviceId.length() <= 0) {
        WifiManager wifiManager = (WifiManager) this.appContext
            .getSystemService(Context.WIFI_SERVICE);
        WifiInfo connInfo = wifiManager.getConnectionInfo();
        if(connInfo != null) {
          deviceId = connInfo.getMacAddress();
        }
      }
      if(subscriberId == null) {
        subscriberId = "";
      }
      UUID deviceUuid = UUID.nameUUIDFromBytes((deviceId + subscriberId).getBytes());

      if (subscriberId == null || subscriberId.trim().length() <= 0) {
        log.debug("No subscriber-id detected (possibly phone without SIM or in flight-mode)");
        String storedSubscriberId = Configuration.getInstance().getSubscriberId();
        if (storedSubscriberId == null) {
          log.debug("Generating a new subscriber UUID");
          subscriberId = deviceUuid.toString();
        } else {
          subscriberId = storedSubscriberId;
        }
      } else {
        subscriberId = deviceUuid.toString();
      }
  
      String storedSubscriberId = Configuration.getInstance().getSubscriberId();
      if (!subscriberId.equals(storedSubscriberId)) {
        simChange = true;
  
        log.debug("SubscriberID=" + subscriberId);
        Configuration.getInstance().setSubscriberID(subscriberId);
        Configuration.getInstance().save();
      }
      return simChange;
    }
}
