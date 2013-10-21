package com.lmit.jenkins.android.networking;

import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

public class ServerAuthenticationDefaultHttpClient extends
    AbstractSecureHttpClient {
  
  private static final int HTTP_PARALLEL_CONNECTIONS = 8;
  private static final long HTTP_POOL_REQUEST_TIMEOUT = 10000L;
  private static final int HTTP_CONN_TIMEOUT = 10000 *50;
  private static final int HTTP_READ_TIMEOUT = 10000 *50;
  private static HttpParams params = null;
  private static SchemeRegistry schemeRegistry = null;
  static ClientConnectionManager connManager = null;
    
  static synchronized public void init() {
    if(connManager != null) {
      connManager.shutdown();
      connManager = null;
    }
    params = new BasicHttpParams();
    ConnManagerParams.setMaxTotalConnections(params, HTTP_PARALLEL_CONNECTIONS);
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
    ConnManagerParams.setTimeout(params, HTTP_POOL_REQUEST_TIMEOUT);
    params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, HTTP_CONN_TIMEOUT);
    params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, HTTP_READ_TIMEOUT);
    
    schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(
            new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    connManager = new ThreadSafeClientConnManager(params, schemeRegistry);
  }

  public ServerAuthenticationDefaultHttpClient(String url) {
    this(new DefaultHttpClient(getConnectionManager(), params), url, new HttpCredentials(), null);
  }

  private static synchronized ClientConnectionManager getConnectionManager() {
    if(connManager == null) {
      init();
    }
    return connManager;
  }

  public ServerAuthenticationDefaultHttpClient(DefaultHttpClient httpClient,
      String url, HttpCredentials credentials, String customUserAgentExtention) {

    super(httpClient, url, credentials, customUserAgentExtention);
    
    if (performAuthentication) {
      this.wrappedDefaultHttpClient.getCredentialsProvider().setCredentials(
          new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT,
              AuthScope.ANY_REALM, AuthScope.ANY_SCHEME),
          new UsernamePasswordCredentials(credentials.getUsername(),
              credentials.getPassword()));
    }

    if ("https".equals(protocol)) {

      TrustAllSSLSocketFactory.trustX509(this.wrappedDefaultHttpClient, port);
    }
  }
}
