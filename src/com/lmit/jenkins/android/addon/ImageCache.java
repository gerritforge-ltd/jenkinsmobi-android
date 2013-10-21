package com.lmit.jenkins.android.addon;

import android.graphics.Bitmap;

import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;

public class ImageCache {

  private static final boolean EVICTION_DISABLED = true;
  private static LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(
      50);
  private static Logger log = Logger.getInstance();

  private static String getBaseName(String path){
    if(path==null){
      return null;
    }
    int i = path.lastIndexOf("/");
    if(i < 0) {
    	return path;
    }
    
    String result = path.substring(i+1);
    return result;
  }
  
  public static Bitmap get(String path) {

    path = getBaseName(path);
    
    Bitmap result = null;

    boolean inMemory = true;
    synchronized (cache) {
      result = cache.get(path);

      if (result == null) {
    	  inMemory = false;
        result = LocalStorage.getInstance().getIcon(path);
        if (result != null) {
          cache.put(path, result);
        }
      }
    }
    
		log.debug("Cache "
				+ (result == null ? "MISS" : (inMemory ? "MEM-HIT"
						: "STORAGE-HIT")) + ":" + path);

    return result;
  }

  public static void put(String path, Bitmap bitmap) {
    synchronized (cache) {
      if (path != null && bitmap != null) {
        path = getBaseName(path);
        cache.put(path, bitmap);
		log.debug("Cache PUT:" + path);
        LocalStorage.getInstance().putIcon(path, bitmap);
      }
    }
  }
  
	public static void evict(String path) {
	  if(EVICTION_DISABLED) {
	    return;
	  }
	  
		if (path == null) {
			return;
		}

		if(!Configuration.getInstance().isConnected()) {
			log.debug("NOT-CONNECTED => Ignoring Cache EVICT " + path);
			return;
		}

		synchronized (cache) {
			path = getBaseName(path);
			cache.remove(path);
			log.debug("Cache EVICT:" + path);
		}
	}

  public static void clean() {
    cache = new LruCache<String, Bitmap>(50);
  }
}
