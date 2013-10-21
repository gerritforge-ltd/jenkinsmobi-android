package com.lmit.jenkins.android.networking;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import com.lmit.jenkins.android.activity.JenkinsMobi;
import com.lmit.jenkins.android.logger.Logger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBAdapter 
{
    public static final String STATE = "state";
    private static final String FLAG = "flag";

    private static final String DATABASE_NAME = "localstorage";
    private static final String DATABASE_NAME_SWP = "localstorage.swap";
    public static final int DATABASE_VERSION = 5;
    
    public static final String JSON_TABLE_NAME = "json";
    private static final String JSON_TABLE_CREATE = "CREATE TABLE "	 + JSON_TABLE_NAME + " (id TEXT PRIMARY KEY UNIQUE, data BLOB, tag TEXT, pluginid TEXT);";
    private static final String JSON_TABLE_DELETE = "DROP TABLE IF EXISTS "    + JSON_TABLE_NAME;
    
    
    public static final String ICONS_TABLE_NAME = "icons";
    private static final String ICONS_TABLE_CREATE = "CREATE TABLE " + ICONS_TABLE_NAME  + " (id TEXT PRIMARY KEY UNIQUE, data BLOB, tag TEXT, pluginid TEXT);";
    private static final String ICONS_TABLE_DELETE = "DROP TABLE IF EXISTS "  + ICONS_TABLE_NAME;
    private  Context context; 
    
    private DatabaseHelper dBHelper;
    private DatabaseHelperSwap databaseHelperSwap;
    private SQLiteDatabase db;
    private static Semaphore dbSem = new Semaphore(1);

  
	public SQLiteDatabase getConnection() throws SQLException {
	    try {
        dbSem.acquire();
      } catch (InterruptedException e) {
        return null;
      }
		this.context = JenkinsMobi.getAppContext();
		dBHelper = new DatabaseHelper(context);
		db = dBHelper.getWritableDatabase();
		return db;
	}
	
	public void closeConnection() {
	  if(db == null) {
	    return;
	  }
	  
	  db.close();
	  db = null;
      dbSem.release();
	}

	public SQLiteDatabase getSWAPConnection() throws SQLException {
		this.context = JenkinsMobi.getAppContext();
		databaseHelperSwap = new DatabaseHelperSwap(context);
		db = databaseHelperSwap.getWritableDatabase();
		return db;
	}

	public SQLiteDatabase getProductionConnection(Context ctx)
			throws SQLException {
		this.context = JenkinsMobi.getAppContext();
		dBHelper = new DatabaseHelper(context);
		db = dBHelper.getWritableDatabase();
		return db;
	}

    private static class DatabaseHelper extends SQLiteOpenHelper 
    {
        DatabaseHelper(Context context) 
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
          // It seems that upgrading from an older version, the create is invoked instead of the onUpgrade :-O
          try {
            db.execSQL(JSON_TABLE_DELETE);
            db.execSQL(ICONS_TABLE_DELETE);            
          } catch(Throwable e) {
            // Explicitly ignoring this: DELETE was issued as kind of workaround to an upgrade situation
          }
          
          db.execSQL(JSON_TABLE_CREATE);
          db.execSQL(ICONS_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
            Log.w("", "Upgrading database from version " + oldVersion 
                    + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL(JSON_TABLE_DELETE);
            db.execSQL(ICONS_TABLE_DELETE);
            onCreate(db);
        }
    }    
    
    
    private static class DatabaseHelperSwap extends SQLiteOpenHelper 
    {
    	DatabaseHelperSwap(Context context) 
        {
            super(context, DATABASE_NAME_SWP, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) 
        {
            db.execSQL(JSON_TABLE_CREATE);
            db.execSQL(ICONS_TABLE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, 
        int newVersion) 
        {
            Log.w("", "Upgrading database from version " + oldVersion 
                    + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL(JSON_TABLE_DELETE);
            db.execSQL(ICONS_TABLE_DELETE);
            onCreate(db);
        }
    }    
    
    public synchronized SQLiteDatabase swap() throws IOException {
        Logger.getInstance().debug("Swapping");
        
		databaseHelperSwap = new DatabaseHelperSwap(context);
		SQLiteDatabase dbSwap = databaseHelperSwap.getWritableDatabase();
		
		dbSwap.execSQL(JSON_TABLE_DELETE);
		dbSwap.execSQL(ICONS_TABLE_DELETE);
        
		dbSwap.execSQL("INSERT INTO "+DATABASE_NAME_SWP+"."+JSON_TABLE_NAME+" SELECT * FROM "+DATABASE_NAME+"."+JSON_TABLE_NAME);
		
        return getSWAPConnection();
      }
    
    public synchronized void swapBack() throws IOException {
        Logger.getInstance().debug("Swapping back");
		db.execSQL(JSON_TABLE_DELETE);
		db.execSQL(ICONS_TABLE_DELETE);
        
		db.execSQL("INSERT INTO "+DATABASE_NAME+"."+JSON_TABLE_NAME+" SELECT * FROM "+DATABASE_NAME_SWP+"."+JSON_TABLE_NAME);      
	}
}