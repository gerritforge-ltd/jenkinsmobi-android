package com.lmit.jenkins.android.networking;

import java.io.File;
import java.io.FileInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;

public class PostError {

	private static Logger log = Logger.getInstance();

	public void send() {

		File file = new File(Configuration.getInstance().getPrivateFolderPath() + File.separator + Logger.TRACE_ERROR_NAME_SWP);
		try {
			log.debug("Error report sended");
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is established.
			int timeoutConnection = Configuration.POST_ERROR_TIME_OUT_RESPONSE;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
			// Set the default socket timeout (SO_TIMEOUT) 
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = Configuration.POST_ERROR_TIME_OUT_RESPONSE;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
			
			HttpClient httpclient = new DefaultHttpClient(httpParameters);

			HttpPost httppost = new HttpPost(Configuration.POST_ERROR_URL);

			InputStreamEntity reqEntity = new InputStreamEntity(new FileInputStream(file), -1);
			reqEntity.setContentType("binary/octet-stream");
			reqEntity.setChunked(true); // Send in multiple parts if needed
			httppost.setEntity(reqEntity);
			HttpResponse response = httpclient.execute(httppost);
			
		} catch (Exception e) {
			// show error
		}

	}
}