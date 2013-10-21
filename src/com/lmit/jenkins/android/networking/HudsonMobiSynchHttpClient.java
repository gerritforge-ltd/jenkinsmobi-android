package com.lmit.jenkins.android.networking;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;

import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudPageReset;
import com.lmit.jenkinscloud.commons.SyncCallback;

public class HudsonMobiSynchHttpClient {
  
  public static final String X_AUTH_OTP_HEADER = "X-Auth-OTP";
  protected static Logger log = Logger.getInstance();
  protected Header[] lastHttpHeaders;
  protected Map<String, String> userHeaders;

  public HttpResponse callPost(byte[] postData, String path)
      throws ClientProtocolException, IOException {
    AbstractSecureHttpClient client =
        new ServerAuthenticationDefaultHttpClient(path);
    HttpResponse response = client.executePostQuery(postData, userHeaders);
      lastHttpHeaders = response.getAllHeaders().clone();
      return response;
  }
  
  public void call(boolean forceRefresh, String path,
      final SyncCallback<JenkinsCloudNode> callback) {
    
    AbstractSecureHttpClient client = new ServerAuthenticationDefaultHttpClient(path);

    try {
      
      HttpResponse response = client.executeGetQuery(forceRefresh, userHeaders);
      
      JenkinsCloudNode result = null;
      lastHttpHeaders = response.getAllHeaders().clone();
      
      switch (response.getStatusLine().getStatusCode()) {

      case HttpURLConnection.HTTP_OK:
        InputStream responseInStream = response.getEntity().getContent();
        try {
          result =
              JenkinsCloudNode.fromStream(responseInStream, response.getEntity().getContentType().getValue());
          setETag(result, response);
        } finally {
          responseInStream.close();
        }
        callback.onSuccess(result);
        break;
        
      case HttpURLConnection.HTTP_NOT_MODIFIED:
    	callback.onSuccess(client.getCachedNode());
    	break;
    	
      case HttpURLConnection.HTTP_RESET:
        callback.onSuccess(new JenkinsCloudPageReset());
        break;
    	  
	  default:
	  		Configuration.getInstance().setConnected(false, true);
	  		StatusLine statusLine = response.getStatusLine();
	        callback.onFailure(new IOException("HTTP Error-Code: " + statusLine.getStatusCode() + "\n" + statusLine.getReasonPhrase()));
      }

    } catch (Exception e) {
		log.error("Cannot connect to " + path + ": setting client to Disconnected mode");
		Configuration.getInstance().setConnected(false, true);
		callback.onFailure(new IOException("Network error\n" + e.getLocalizedMessage(), e));
    } finally {
      client.releaseConnection();
    }
  }
  
	public void doPost(String url, byte[] postData, String postContentType,
			SyncCallbackWrapper<JenkinsCloudNode> callback) {

		JenkinsCloudNode result = null;
		AbstractSecureHttpClient client = new ServerAuthenticationDefaultHttpClient(
				url);

		log.debug("Posting URL: '" + url + "'");

		try {
			HttpPost post = client.getPost();
			post.setEntity(new ByteArrayEntity(postData));
			post.setHeader("Content-Type", postContentType);
			client.setHeaders(false, userHeaders, post);
			HttpResponse response = client.execute(post);
			lastHttpHeaders = response.getAllHeaders().clone();

			switch (response.getStatusLine().getStatusCode()) {

			case HttpURLConnection.HTTP_OK:
				InputStream responseInStream = response.getEntity()
						.getContent();
				try {
					result = JenkinsCloudDataNode.fromStream(responseInStream,
							response.getEntity().getContentType().getValue());
					setETag(result, response);
				} finally {
					responseInStream.close();
				}
				callback.onSuccess(result);
				break;

			case HttpURLConnection.HTTP_RESET:
				callback.onSuccess(new JenkinsCloudPageReset());
				break;

			default:
				Configuration.getInstance().setConnected(false, true);
				Configuration.getInstance().setConnected(false, true);
				StatusLine statusLine = response.getStatusLine();
				callback.onFailure(new IOException("HTTP Error: "
						+ statusLine.getStatusCode() + " - "
						+ statusLine.getReasonPhrase()));
			}
		} catch (Exception e) {
			callback.onFailure(e);
		}

	}
  
  static void setETag(JenkinsCloudNode result, HttpResponse response) {
	  Header etag = response.getFirstHeader("ETag");
	  if(etag != null && result != null) {
		  result.setEtag(etag.getValue());
	  }
  }

  public void call2(boolean forceRefresh, String path,
      final SyncCallback<InputStream> callback) {
    
    AbstractSecureHttpClient client = new ServerAuthenticationDefaultHttpClient(path);

    try {
      HttpResponse response = client.executeGetQuery(forceRefresh, userHeaders);
         
      lastHttpHeaders = response.getAllHeaders().clone();
      
      if (response.getStatusLine().getStatusCode() == 200) {

        InputStream responseInStream = response.getEntity().getContent();
        callback.onSuccess(responseInStream);
      }
      else{
        
        throw new IOException("Error "+response.getStatusLine().getReasonPhrase());
      }

    } catch (Exception e) {
      callback.onFailure(e);
    }
  }

  public boolean stop() {
    // TODO Auto-generated method stub
    return false;
  }

  public Header[] getLastHttpHeaders() {
    // TODO Auto-generated method stub
    return lastHttpHeaders;
  }

  public Map<String, String>getUserHeaders() {
    return userHeaders;
  }

  public void setUserHeaders(Map<String, String> userHeaders) {
    this.userHeaders = userHeaders;
  }
}
