package com.lmit.jenkins.android.networking;

import com.lmit.jenkins.android.configuration.Configuration;

public class HttpCredentials {

  private String username;
  private String password;

  public HttpCredentials() {
    this.username = Configuration.getInstance().getUsername();
    this.password = Configuration.getInstance().getPassword();
  }

  public HttpCredentials(String username, String password) {

    if (username == null) {
      this.username = Configuration.getInstance().getUsername();
    } else {
      this.username = username;

    }
    if (password == null) {
      this.password = Configuration.getInstance().getPassword();
    } else {
      this.password = password;
    }
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }
}
