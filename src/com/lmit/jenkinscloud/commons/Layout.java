package com.lmit.jenkinscloud.commons;

public enum Layout {

  LIST ("List"),
  COMPOSITE ("Composite"),
  ICONS ("Icons"),
  WEB_VIEW ("WebView"),
  MENU ("Menu");
  
  private String desc;
  
  private Layout(String desc) {
    this.desc= desc;
  }
  
  @Override
  public String toString() {
   
    return desc;
  }
}
