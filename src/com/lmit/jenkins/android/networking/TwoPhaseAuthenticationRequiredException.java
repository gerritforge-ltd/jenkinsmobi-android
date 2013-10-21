package com.lmit.jenkins.android.networking;

import javax.security.auth.login.LoginException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

public class TwoPhaseAuthenticationRequiredException extends LoginException {
  private static final long serialVersionUID = 6390763647784727072L;
  private String authAppId;

  public String getAuthAppId() {
    return authAppId;
  }

  public void setAuthAppId(String authAppId) {
    this.authAppId = authAppId;
  }

  public TwoPhaseAuthenticationRequiredException(String string) {
    super(string);
  }

  public TwoPhaseAuthenticationRequiredException(HttpResponse response) {
    super(response.getStatusLine().getReasonPhrase());
    Header otpAppHeader = response.getFirstHeader("X-Auth-OTP-AppId");
    if(otpAppHeader != null) {
      authAppId = otpAppHeader.getValue();
    }
  }
}
