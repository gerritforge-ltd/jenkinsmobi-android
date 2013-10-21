package com.lmit.jenkinscloud.commons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JenkinsCloudPage extends JenkinsCloudNode {
	public final String html;
	public final String contentType;
	public final boolean refreshRequest;
	
	public JenkinsCloudPage(String contentType, String html) {
		this.html = html;
		this.contentType = contentType;
		this.refreshRequest = false;
	}
	
	protected JenkinsCloudPage(boolean refreshRequest) {
	  this.refreshRequest = true;
	  this.html = "";
	  this.contentType = "";
	}

  public String getTitle() {
    Pattern titlePattern = Pattern.compile("<title>([^>]*)</title>", Pattern.DOTALL);
    Matcher match = titlePattern.matcher(html);
    if(match.find()) {
      return match.group(1);
    } else {
      return null;
    }
  }
}
