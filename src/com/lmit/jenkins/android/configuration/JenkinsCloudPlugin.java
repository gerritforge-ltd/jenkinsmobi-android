package com.lmit.jenkins.android.configuration;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class JenkinsCloudPlugin {
  public String parentAccountUsername;
  public String name;
  public String description;
  public String type;
  public String url;
  public String username;
  public String password;
  public Map<String, String> options;
  
  public JenkinsCloudPlugin(String name) {
    this.name = name;
    this.options = new HashMap<String, String>();
  }
  
  public byte[] getData() {
    try {
      return new Gson().toJson(this).getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Will never be thrown as encoding is hardcoded
      throw new IllegalArgumentException(e);
    }
  }
}
