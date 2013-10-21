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
package com.lmit.jenkins.android.addon;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.widget.ImageView;
import android.widget.Toast;

import com.lmit.jenkins.android.activity.R;

public class Utils {
  
  public static void showToast(String text, Context ctx){
    int duration = Toast.LENGTH_SHORT;
    Toast toast = Toast.makeText(ctx, text, duration);
    toast.show();
  }
  
  public static void setImageByName(ImageView imageView, Context context,
      String color) {

    if (imageView == null || color == null) {

      return;
    }
  }

  public static String parseHudsonDate(String elementValue) {

    // 2010-07-21T17:03:04.685020Z

    StringBuilder result = new StringBuilder();

    result.append(elementValue.substring(0, 10));
    result.append(" ");
    result.append(elementValue.substring(11, 23));

    return result.toString();
  }

  public static CharSequence readAssetTextFile(Context context,
      String filename, String fallbackFilename) {

    CharSequence result = null;

    try {
      result = internalReadAssetTextFile(context, filename);
    } catch (FileNotFoundException e) {

      try {
        result = internalReadAssetTextFile(context, fallbackFilename);
      } catch (FileNotFoundException e1) {

        result = "";
      }
    }

    return result;
  }

  private static CharSequence internalReadAssetTextFile(Context context,
      String filename) throws FileNotFoundException {

    BufferedReader in = null;
    try {

      in =
          new BufferedReader(new InputStreamReader(context.getAssets().open(
              filename)));
      String line;
      StringBuilder buffer = new StringBuilder();
      while ((line = in.readLine()) != null)
        buffer.append(line).append('\n');
      return buffer;
    } catch (FileNotFoundException e) {

      throw e;
    } catch (IOException e) {
      return "";
    } finally {
      closeStream(in);
    }
  }

  /**
   * Closes the specified stream.
   * 
   * @param stream The stream to close.
   */
  private static void closeStream(Closeable stream) {
    if (stream != null) {
      try {
        stream.close();
      } catch (IOException e) {
        // Ignore
      }
    }
  }
}
