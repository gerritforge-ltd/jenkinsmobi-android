// Copyright (C) 2012 LMIT Limited
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.lmit.jenkins.android.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;

import com.lmit.jenkins.android.configuration.Configuration;

public class Logger {

  private static Logger instance;

  private static boolean enabled = true; // set this to true to enable the
                                         // logging features

  private String className = "QAExplorer";

  private FileWriter fout = null;
  public final static String TRACE_FILE_NAME = "qaexplorer.log";
  public final static String TRACE_ERROR_NAME = "qaexplorer.err";
  public final static String TRACE_ERROR_NAME_SWP = "qaexplorer.err.log";
  public final static String TRACE_FILE_NAME_SWP = "qaexplorer.support.log";

  private Logger() {

    if (enabled) {

      try {

        if (new File(Configuration.getInstance().getPrivateFolderPath())
            .exists() == false) {

          new File(Configuration.getInstance().getPrivateFolderPath()).mkdirs();
        }

        fout =
            new FileWriter(Configuration.getInstance().getPrivateFolderPath()
                + File.separator + TRACE_FILE_NAME);
      } catch (IOException e) {

        error(e.getMessage(), e);
      }
    }
  }

  public File getLogFile() {

    File result = null;

    if (enabled) {

      try {
        fout.close();
      } catch (IOException e) {
        ;
      }

      new File(Configuration.getInstance().getPrivateFolderPath()
          + File.separator + TRACE_FILE_NAME).renameTo(new File(Configuration
          .getInstance().getPrivateFolderPath()
          + File.separator
          + TRACE_FILE_NAME_SWP));

      result =
          new File(Configuration.getInstance().getPrivateFolderPath()
              + File.separator + TRACE_FILE_NAME_SWP);

      try {
        fout =
            new FileWriter(Configuration.getInstance().getPrivateFolderPath()
                + File.separator + TRACE_FILE_NAME);
      } catch (IOException e) {
        error(e.getMessage(), e);
      }
    }

    return result;
  }

  public static boolean checkForUncleanShutdown() {

    boolean result = false;

    if (enabled) {
      if (new File(Configuration.getInstance().getPrivateFolderPath()
          + File.separator + TRACE_ERROR_NAME).exists()) {

        new File(Configuration.getInstance().getPrivateFolderPath()
            + File.separator + TRACE_ERROR_NAME).renameTo(new File(
            Configuration.getInstance().getPrivateFolderPath() + File.separator
                + TRACE_ERROR_NAME_SWP));

        result = true;
      }
    }

    return result;
  }

  public static void stopLogger() {

    if (enabled && instance != null) {
      instance.__stopLoggerAndLeaveErrorFile();
      instance = null;
    }
  }

  public static void stopLoggerAndCleanup() {

    if (enabled && instance != null) {
      instance.__stopLoggerAndCleanup();
      instance = null;
    }
  }

  private void __stopLoggerAndLeaveErrorFile() {

    if (enabled) {
      if (fout != null) {
        try {
          fout.close();

          new File(Configuration.getInstance().getPrivateFolderPath()
              + File.separator + TRACE_FILE_NAME).renameTo(new File(
              Configuration.getInstance().getPrivateFolderPath()
                  + File.separator + TRACE_ERROR_NAME));

        } catch (IOException e) {

          error(e.getMessage(), e);
        }
      }
    }
  }

  private void __stopLoggerAndCleanup() {

    if (enabled) {
      if (fout != null) {
        try {
          fout.close();
          new File(Configuration.getInstance().getPrivateFolderPath()
              + File.separator + TRACE_FILE_NAME).delete();

          new File(Configuration.getInstance().getPrivateFolderPath()
              + File.separator + TRACE_ERROR_NAME).delete();

          new File(Configuration.getInstance().getPrivateFolderPath()
              + File.separator + TRACE_ERROR_NAME_SWP).delete();

        } catch (IOException e) {

          error(e.getMessage(), e);
        }
      }
    }
  }

  public static void setEnabled(boolean enabled) {

    Logger.enabled = enabled;
  }

  public static Logger getInstance() {

    if (instance == null) {

      instance = new Logger();
    }

    return instance;
  }

  public void debug(String msg) {

    if (enabled) {

      if (msg == null) {
        msg = className;
      }

      Log.d(className, msg);
      logToFile("DEBUG", msg);
    }
  }

  public void error(String msg) {

    if (enabled) {
      if (msg == null) {
        msg = className;
      }
      Log.e(className, msg);
      logToFile("ERROR", msg);
    }
  }

  public void error(String msg, Throwable e) {

    if (enabled) {
      if (msg == null) {
        msg = className;
      }

      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      String stacktrace = sw.toString();
      Log.e(className, msg + "\nStackTrace:\n" + stacktrace);
      logToFile("ERROR", msg + "\nStackTrace:\n" + stacktrace);
    }
  }

  public void info(String msg) {

    if (enabled) {
      if (msg == null) {
        msg = className;
      }
      Log.i(className, msg);
      logToFile("INFO", msg);
    }
  }

  public void warn(String msg) {

    if (enabled) {
      if (msg == null) {
        msg = className;
      }
      Log.w(className, msg);

      logToFile("WARN", msg);
    }
  }

  private void logToFile(String level, String msg) {

    if (enabled) {
      if (fout != null) {

        try {
          fout.write(formatMessage(level, msg));
        } catch (IOException e) {

          /*
           * IF logToFile is failing ... probably error() will fail as well ;-)
           * let's avoid at least a stackOverflow error(e.getMessage(), e);
           */
        }
      }
    }
  }

  private String formatMessage(String level, String msg) {

    StringBuilder result = new StringBuilder();

    Date now = new Date();
    result.append("[");
    result.append(DateFormat.format("ddMMyyyy:hhmmss", now));
    result.append("] ");
    result.append(level);
    result.append(" - ");
    result.append(msg);
    result.append("\n");

    return result.toString();
  }

  public void logConfig() {
    Configuration conf = Configuration.getInstance();
    if (enabled) {
        debug("--------- Client configuration -----------");
        debug("Product name   :" + conf.productName);
        debug("Product version:" + conf.productVersion);
        debug("Schema version :" + conf.schema_version);
        debug("Remote URL     :" + conf.hudsonHostname);
        debug("Last refresh TS:" + conf.lastRefreshTimestamp);
        debug("--------- Phone settings -----------");
        debug("Device locale  :" + conf.deviceLocale);
        debug("Private path   :" + conf.privateFolderPath);
        debug("User agent     :" + conf.userAgent);
        debug("---------  User info -----------");    
        debug("MSISDN         :" + conf.msisdn);
        debug("Subscriber id  :" + conf.subscriberId);
        debug("--------------------------------");    
    }
  }

  public static boolean checkForExternalStorageState(Context context) {

    return Environment.MEDIA_MOUNTED.equals(Configuration.getInstance()
        .getExternalStorageState());
  }
}
