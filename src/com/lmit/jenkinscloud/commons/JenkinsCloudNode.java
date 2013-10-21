package com.lmit.jenkinscloud.commons;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.util.ByteArrayBuffer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.lmit.jenkins.android.logger.Logger;

public class JenkinsCloudNode {
	private static final String CONTENT_TYPE_CHARSET = "charset=";
	private static final Logger log = Logger.getInstance();
	protected transient String etag;
	protected boolean cached;
	
	@Expose
	@SerializedName("className")
    public String className;

	public boolean isCached() {
		return cached;
	}

	public void setCached(boolean cached) {
		this.cached = cached;
	}

	public JenkinsCloudNode() {
		super();
	}

	public String getEtag() {
		return etag;
	}

	public void setEtag(String etag) {
		this.etag = etag;
	}

	public String toJson() {
		GsonBuilder gbuilder = new GsonBuilder();
		gbuilder.disableHtmlEscaping();
		return gbuilder.create().toJson(this);
	}

	public JsonElement toJsonTree() {
		GsonBuilder gbuilder = new GsonBuilder();
		gbuilder.disableHtmlEscaping();
		return gbuilder.create().toJsonTree(this);
	}

	public static JenkinsCloudNode fromJson(InputStream json,
			Class<? extends JenkinsCloudNode> targetClass) {
	  JenkinsCloudNode outNode = null;
		try {
			GsonBuilder gbuilder = new GsonBuilder();
			gbuilder.disableHtmlEscaping();
			return outNode = gbuilder.create().fromJson(new InputStreamReader(json),
					targetClass);
		} catch (Exception e) {
			log.error("Malformed JSON detected in input stream", e);
			return null;
		} finally {
		  if(outNode != null && outNode.className == null) {
		    outNode.className = targetClass.getName();
		  }
		}
	}

	public static JenkinsCloudNode fromJson(String json,
			Class<? extends JenkinsCloudNode> targetClass) {
      JenkinsCloudNode outNode = null;
		try {
			GsonBuilder gbuilder = new GsonBuilder();
			gbuilder.disableHtmlEscaping();
			return outNode = gbuilder.create().fromJson(json, targetClass);
		} catch (Exception e) {
			log.error("Malformed JSON detected in input stream\n" + json, e);
			return null;
		} finally {
          if(outNode != null && outNode.className == null) {
            outNode.className = targetClass.getName();
          }
        }
	}

	public static JenkinsCloudNode fromStream(InputStream is, String contentType) {
		if (contentType.toLowerCase().indexOf("html") < 0) {
			return fromJson(is, JenkinsCloudDataNode.class);
		} else {
			return fromHtml(is, contentType);
		} 
	}

	private static JenkinsCloudNode fromHtml(InputStream is, String contentType) {
		String charSet = "UTF-8";
		int charSetPos = contentType.indexOf(CONTENT_TYPE_CHARSET);
		if (charSetPos >= 0) {
			int charSetEnd = contentType.indexOf(";", charSetPos);
			if (charSetEnd >= 0) {
				charSet = contentType.substring(charSetPos
						+ CONTENT_TYPE_CHARSET.length(), charSetEnd);
			} else {
				charSet = contentType.substring(charSetPos
						+ CONTENT_TYPE_CHARSET.length());
			}
		}
		try {
			byte[] htmlBin = readFully(is);
			String html = new String(htmlBin, charSet);
			return new JenkinsCloudPage(contentType, html);
		} catch (IOException e) {
			log.error("I/O Error while reading HTTP input stream", e);
			return null;
		}
	}

	private static byte[] readFully(InputStream is) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte[] buff = new byte[64*1024];
		int read;
		while((read = is.read(buff)) >= 0) {
			if(read > 0) {
			bout.write(buff, 0, read);
			}
		}
		bout.close();
		return bout.toByteArray();
	}
}