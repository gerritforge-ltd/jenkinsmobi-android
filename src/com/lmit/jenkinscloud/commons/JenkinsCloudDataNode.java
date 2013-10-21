package com.lmit.jenkinscloud.commons;

import java.util.List;

import android.graphics.Bitmap;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.lmit.jenkins.android.logger.Logger;

public class JenkinsCloudDataNode extends JenkinsCloudNode {
  public static final String HTTP_GET = "GET";
  public static final String HTTP_POST = "POST";
  
private static final Logger log = Logger.getInstance();
  public final static String API_VERSION = "1.0";
  
  @Expose
  @SerializedName("layout")
  protected Layout layout;

  @SerializedName("payload")
  protected List<JenkinsCloudDataNode> payload;

  @Expose
  @SerializedName("menu")
  protected List<JenkinsCloudDataNode> menu;

  @Expose
  @SerializedName("version")
  private String version;

  @Expose
  @SerializedName("type")
  protected Type type;
  
  @Expose
  @SerializedName("title")
  protected String title;
  
  @Expose
  @SerializedName("description")
  protected String description;
  
  @Expose
  @SerializedName("descriptionAlign")
  protected Alignment descriptionAlign;
  
  @Expose
  @SerializedName("iconAlign")
  protected Alignment iconAlign;
  
  @Expose
  @SerializedName("icon")
  protected String icon;
  
  protected transient Bitmap iconBmp;


  @Expose
  @SerializedName("path")
  protected String path;
  
  @Expose
  @SerializedName("titleColor")
  protected String titleColor;
  
  @Expose
  @SerializedName("descriptionColor")
  protected String descriptionColor;
  
  @Expose
  @SerializedName("action")
  protected String action;
  
  @SerializedName("moredata")
  protected boolean hasMoreData;
  
  @SerializedName("modified")
  protected String modified;
  
  @SerializedName("viewtitle")
  protected String viewTitle;
  
	@SerializedName("preload")
	protected boolean preload;

	private String httpMethod = HTTP_GET;
	private byte[] postData;

	public byte[] getPostData() {
		return postData;
	}

	public String getPostContentType() {
		return postContentType;
	}

	private String postContentType;

	public boolean isHttpGet() {
		return httpMethod.equalsIgnoreCase(HTTP_GET);
  }
  
  
  
  


  public JenkinsCloudDataNode(String title, String description, String icon) {
    this(title, description);
    this.icon = icon;
  }

  public JenkinsCloudDataNode(String title, String description) {
    this(title);
    this.description = description;
  }

  public JenkinsCloudDataNode(String title) {
    this();
    this.title = title;
  }

  public JenkinsCloudDataNode(Layout layout) {
    this();
    this.layout = layout;
  }

  public JenkinsCloudDataNode() {
    ;
  }

  public Layout getLayout() {
    return layout;
  }

  public void setLayout(Layout layout) {
    this.layout = layout;
  }

  public List<JenkinsCloudDataNode> getPayload() {
    return payload;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Alignment getDescriptionAlign() {
    return descriptionAlign;
  }

  public void setDescriptionAlign(Alignment descriptionAlign) {
    this.descriptionAlign = descriptionAlign;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getTitleColor() {
    return titleColor;
  }

  public void setTitleColor(String titleColor) {
    this.titleColor = titleColor;
  }

  public String getDescriptionColor() {
    return descriptionColor;
  }

  public void setDescriptionColor(String descriptionColor) {
    this.descriptionColor = descriptionColor;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String apiversion) {
    this.version = apiversion;
  }




  public void setPayload(List<JenkinsCloudDataNode> payload) {
    this.payload = payload;
  }

  public List<JenkinsCloudDataNode> getMenu() {
    return menu;
  }

  public void setMenu(List<JenkinsCloudDataNode> menu) {
    this.menu = menu;
  }

  public Bitmap getIconBmp() {
    return iconBmp;
  }

  public void setIconBmp(Bitmap iconBmp) {
    this.iconBmp = iconBmp;
  }
  
  public boolean hasMoreData() {
    return hasMoreData;
  }

  public void setHasMoreData(boolean hasMoreData) {
    this.hasMoreData = hasMoreData;
  }

  public boolean isModified() {
    return "true".equals(modified);
  }
  
  public String getViewTitle() {
    return viewTitle;
  }

  public void setViewTitle(String viewTitle) {
    this.viewTitle = viewTitle;
  }

  public Alignment getIconAlign() {
    return iconAlign;
  }

  public void setIconAlign(Alignment iconAlign) {
    this.iconAlign = iconAlign;
  }
  
  @Override
	public String toString() {
	  return layout + "/" + path + "/" + title;
	}

  public boolean isPreload() {
    return preload;
  }
  
  public void setPost(byte[] postData, String contentType) {
	  this.httpMethod = HTTP_POST;
	  this.postData = postData;
	  this.postContentType = contentType;
  }
}
