package com.lmit.jenkins.android.addon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.gson.GsonBuilder;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkins.android.networking.DBAdapter;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudPage;

public class LocalStorage {
	private static final Logger log = Logger.getInstance();
  private static final boolean IMAGE_EVICTION_DISABLED = true;
  private static LocalStorage instance;

  public static LocalStorage getInstance() {
    if (instance == null) {
      instance = new LocalStorage();
    }

    return instance;
  }

  private LocalStorage() {
    ;
  }

  public JenkinsCloudNode getNode(String path) {
    JenkinsCloudNode node = getNode(path, JenkinsCloudNode.class);
    if(node != null) {
      try {
        if(node.className == null) {
          node.className = JenkinsCloudDataNode.class.getName();
        }
        return (JenkinsCloudNode) getNode(path, Class.forName(node.className));
      } catch (ClassNotFoundException e) {
        log.error("Unsupported cached node " + node.className);
        return null;
      }
    }
    else {
      return null;
    }
  }
  
  public <T> T getNode(String path, Class<T> nodeClass) {
    T root = null;
    DBAdapter helper = new DBAdapter();
    if(path.endsWith("/")) {
      path = path.substring(0, path.length()-1);
    }
    
    try {
      SQLiteDatabase db = tryGettingReadableDatabase(helper);
      if (db == null) {
        return null;
      }
      Cursor cursor =
          db.query(DBAdapter.JSON_TABLE_NAME,
              new String[] {"data, tag"}, "id=?", new String[] {path}, null, null,
              null);
      try {
        if (cursor.moveToFirst()) {
        	String jsonText = cursor.getString(0);
            try {
                GsonBuilder gbuilder = new GsonBuilder();
                gbuilder.disableHtmlEscaping();
                root = gbuilder.create().fromJson(jsonText, nodeClass);
                } catch(Exception e) {
            		  log.error("Malformed JSON detected in input stream\n" + jsonText, e);
            		  return null;
            	  }

            if(JenkinsCloudNode.class.isAssignableFrom(nodeClass)) {
            	JenkinsCloudNode jenkinsNode = (JenkinsCloudNode) root;
            	jenkinsNode.setEtag(cursor.getString(1));
            	jenkinsNode.setCached(true);
            }
        }
      } finally {
        cursor.close();
        helper.closeConnection();
      }
    } catch (Exception e) {
      Logger.getInstance().error("Error during getNode", e);
    }
    
    log.debug("DISK-CACHE " + path + " "+ (root == null ? "MISS":"HIT - " + root.getClass()));
    return root;
  }

  public Bitmap getIcon(String path) {

    DBAdapter helper = new DBAdapter();

    SQLiteDatabase db = tryGettingReadableDatabase(helper);
    if (db == null) {
      return null;
    }
    Cursor cursor =
        db.query(DBAdapter.ICONS_TABLE_NAME,
            new String[] {"data"}, "id=?", new String[] {path}, null, null,
            null);
    try {
      Bitmap result = null;
      if (cursor.moveToFirst()) {
        String base64 = cursor.getString(0);
        db.close();
        byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
        ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
        result = BitmapFactory.decodeStream(bis);
      }
      return result;
    } finally {
      cursor.close();
      helper.closeConnection();
    }
  }

  private SQLiteDatabase tryGettingReadableDatabase(
      DBAdapter helper) {
    int retries = 5;
    SQLiteDatabase db = null;
    while (retries > 0) {
      try {
        db = helper.getConnection();
        break;
      } catch (SQLiteException e) {
        retries--;
        try {
          Thread.sleep(50);
        } catch (InterruptedException e1) {
          ;
        }
      }
    }
    return db;
  }

  private SQLiteDatabase tryGettingWritableDatabase(
      DBAdapter helper) {
    int retries = 5;
    SQLiteDatabase db = null;
    while (retries > 0) {
      try {
        db = helper.getConnection();
        break;
      } catch (SQLiteException e) {
        retries--;
        try {
          Thread.sleep(50);
        } catch (InterruptedException e1) {
          ;
        }
      }
    }
    return db;
  }

  public void putNode(String path, JenkinsCloudNode node) {
    if (node.isCached()) {
      return;
    }

    if (node.className == null) {
      node.className = node.getClass().getName();
    }

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
	  
    cleanPath(path);
    DBAdapter helper = new DBAdapter();
    try {
      SQLiteDatabase db = tryGettingWritableDatabase(helper);
      if (db == null) {
        return;
      }
      ContentValues values = new ContentValues();
      if (!path.startsWith("/") && !path.startsWith("http")) {
        path = "/" + path;
      }
      values.put("id", path);
      values.put("data", node.toJson());
      values.put("tag", node.getEtag());
      values.put("pluginid", "");
      db.beginTransaction();
      try {
        long insertResult =
            db.insertWithOnConflict(DBAdapter.JSON_TABLE_NAME,
                null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Logger.getInstance().debug("InsertData result=" + insertResult);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
        helper.closeConnection();
      }
    } catch (Exception e) {
      Logger.getInstance().error("Error during putNode", e);
    }
    
    log.debug("DISK-CACHE PUT " + path + " " + node.getClass());
  }


  public void replaceNode(String path, JenkinsCloudNode node) {
	  if(node.isCached()) {
		  return;
	  }
	  
    putNode(path, node);
  }

  public void putIcon(String path, Bitmap icon) {

    DBAdapter helper = new DBAdapter();

    try {
      SQLiteDatabase db = tryGettingWritableDatabase(helper);
      if (db == null) {
        return;
      }
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
      byte[] byteArray = stream.toByteArray();
      String base64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
      ContentValues values = new ContentValues();
      values.put("id", path);
      values.put("data", base64);
      values.put("pluginid", "");
      db.beginTransaction();
      try {
        long insertResult =
            db.insertWithOnConflict(DBAdapter.ICONS_TABLE_NAME,
                null, values, SQLiteDatabase.CONFLICT_REPLACE);
        Logger.getInstance().debug("InsertData result=" + insertResult);
        db.setTransactionSuccessful();
      } finally {
        db.endTransaction();
        helper.closeConnection();
      }
    } catch (Exception e) {
      Logger.getInstance().error("Error during putIcon", e);
    }
  }

  public void cleanAll() {
    DBAdapter helper = new DBAdapter();
    SQLiteDatabase db = helper.getConnection();
    try {
      db.delete(DBAdapter.JSON_TABLE_NAME, null, null);
      db.delete(DBAdapter.ICONS_TABLE_NAME, null, null);
    } finally {
      helper.closeConnection();
    }
  }

  public void cleanPath(String path) {
    DBAdapter helper = new DBAdapter();
    SQLiteDatabase db = helper.getConnection();
    try {
      db.delete(DBAdapter.JSON_TABLE_NAME, "id=?",
          new String[] {path + "%"});
      db.delete(DBAdapter.ICONS_TABLE_NAME, "id=?",
          new String[] {path + "%"});
    } finally {
      helper.closeConnection();
    }
    
    log.debug("DISK-CACHE EVICT " + path);
  }

  private SQLiteDatabase transactionDB;
  private DBAdapter transactionHelper;

  public void startTransaction() throws IOException {
    transactionHelper = new DBAdapter();
    transactionDB = transactionHelper.swap();
    transactionDB.beginTransaction();
    transactionDB.setLockingEnabled(false);
  }

  // make sure to call this method after startTransaction in case of success
  public void commitTransaction() throws IOException {
    transactionDB.setTransactionSuccessful();
    transactionDB.endTransaction();
    transactionDB.close();
    transactionHelper = new DBAdapter();
    transactionHelper.swapBack();
    transactionHelper = null;
    transactionDB = null;
  }

  // make sure to call this method after startTransaction in case of error
  public void abortTransaction() throws IOException {
	  if(transactionDB == null) {
		  return;
	  }
	transactionHelper = new DBAdapter();
    transactionDB.endTransaction();
    transactionDB.close();
    transactionHelper.swapBack();
    transactionHelper = null;
    transactionDB = null;
  }

	public void evictNode(String path, JenkinsCloudNode result) {
		cleanPath(path);

		if (result instanceof JenkinsCloudDataNode) {
			evictImages((JenkinsCloudDataNode) result);
		}
	}

	public void evictImages(JenkinsCloudDataNode result) {
		cleanImage(result.getIcon());
		List<JenkinsCloudDataNode> payload = result.getPayload();
		if(payload == null) {
			return;
		}
		for (JenkinsCloudDataNode subNode : payload) {
			evictImages(subNode);
		}
	}

	private void cleanImage(String icon) {
	  if(IMAGE_EVICTION_DISABLED) {
	    return;
	  }
	  
		if(icon == null) {
			return;
		}
		
		DBAdapter helper = new DBAdapter();
		SQLiteDatabase db = helper.getConnection();
		try {
			String imageId = icon.substring(icon.lastIndexOf('/')+1);
			log.debug("Cleanup Image " + imageId);
			db.delete(DBAdapter.ICONS_TABLE_NAME, "id=?",
					new String[] { imageId });
			ImageCache.evict(imageId);
		} finally {
			helper.closeConnection();
		}
	}

	public JenkinsCloudPage getPage(String url) {
		return getNode(url, JenkinsCloudPage.class);
	}

  public boolean isCached(String path) {
    return getNode(path) != null;
  }
}
