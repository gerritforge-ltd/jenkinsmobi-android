package com.lmit.jenkins.android.addon;

import java.util.Stack;

import com.lmit.jenkins.android.logger.Logger;

public class NavigationStack {
  private static Logger log = Logger.getInstance();
  private static Stack<String> stack = new Stack<String>();

  public static void push(String path) {
    try {
      stack.push(path);
    } finally {
      log.debug("Navigation-GO " + path + ":  " + stack);
    }
  }

  public static String pop() {
    try {
      if (!stack.isEmpty()) {
        return stack.pop();
      }
      return null;
    } finally {
      log.debug("Navigation-BACK: " + stack);
    }
  }
  
	public static String getPathInStack() {
		String[] elems = stack.toArray(new String[] {});

		if (elems.length > 0) {
			if (elems[elems.length - 1].startsWith("/qaexplorer/")
					|| elems[elems.length - 1].startsWith("qaexplorer/")
					|| elems[elems.length - 1].startsWith("/qaexplorer_root/")
					|| elems[elems.length - 1].startsWith("qaexplorer_root/")) {
				return elems[elems.length - 1];
			}
		}

		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < elems.length; i++) {

			if (elems[i].startsWith("/")) {
			  if(!elems[i].startsWith("/qaexplorer")) {
				buff = new StringBuffer("/qaexplorer" + elems[i]);
			  } else {
			    buff = new StringBuffer(elems[i]);
			  }
			} else {
				buff.append(elems[i]);

				if (!elems[i].endsWith("/")) {
					buff.append('/');
				}
			}
		}

		log.debug("Navigation-POS " + buff + ": " + stack);
		return buff.toString();
	}

  public static void reset() {
    stack.clear();
  }
}
