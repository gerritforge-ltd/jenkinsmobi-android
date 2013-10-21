package com.lmit.jenkins.android.addon;

public class ApplicationStatus {

  public static String getCurrentPath() {
    return NavigationStack.getPathInStack();
  }

  public static void moveOnPath(String path) {
    if(!path.endsWith("/")) {
      path += "/";
    }
    NavigationStack.push(path);
  }

  public static void moveBackPath() {

    NavigationStack.pop();
  }

  public static void reset() {
    NavigationStack.reset();
  }
}
