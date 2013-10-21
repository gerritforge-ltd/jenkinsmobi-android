package com.lmit.jenkins.android.networking;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableRow;

import com.lmit.jenkins.android.addon.ImageCache;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkinscloud.commons.JenkinsCloudDataNode;
import com.lmit.jenkinscloud.commons.JenkinsCloudNode;

public class ImageDownloader {

  private LinkedBlockingQueue<Executor> execution =
      new LinkedBlockingQueue<Executor>();

  private static ImageDownloader instance;
  
  private boolean stop = false;
  
  public static ImageDownloader getInstance() {
    if (instance == null) {
      instance = new ImageDownloader();
    }
    return instance;
  }

  public void stop(){
    stop = true;
  }
  
  private ImageDownloader() {

    new Thread(new Runnable() {

      @Override
      public void run() {

        while (!stop) {
          try {
            Executor e = execution.take();
            e.start();
          } catch (InterruptedException e1) {
            ;
          }
        }
      }
    }).start();
  }

  class Executor extends Thread {

    private String url;
    private Handler handler;

    public Executor(String url, Handler handler) {
      this.url = url;
      this.handler = handler;
    }

    @Override
    public void run() {
      Looper.prepare();
      try {
        Bitmap bm = downloadImage(url);
        Message msg = new Message();
        msg.what = 0;
        msg.obj = bm;
        handler.sendMessage(msg);
      } catch (Exception e) {
        ;
      }
    }
  }

  class SetMenutItemIconHandler extends Handler {

    private MenuItem menuItem;

    public SetMenutItemIconHandler(MenuItem item) {
      this.menuItem = item;
    }

    @Override
    public void handleMessage(Message msg) {
      Bitmap bm = (Bitmap) msg.obj;
      menuItem.setIcon(new BitmapDrawable(bm));
    }
  }

  class SetImageViewIconHandler extends Handler {

    private ImageView imageView;

    public SetImageViewIconHandler(ImageView view) {
      this.imageView = view;
    }

    @Override
    public void handleMessage(Message msg) {
      Bitmap bm = (Bitmap) msg.obj;
      this.imageView.setImageBitmap(bm);
      this.imageView.setVisibility(View.VISIBLE);
    }
  }
  
  class SetNodeIconHandler extends Handler {

    private JenkinsCloudDataNode node;

    public SetNodeIconHandler(JenkinsCloudDataNode node) {
      this.node = node;
    }

    @Override
    public void handleMessage(Message msg) {
      Bitmap bm = (Bitmap) msg.obj;
      this.node.setIconBmp(bm);
    }
  }

  public void setImageBitmap(final MenuItem menuItem, final String url) {
    execution.offer(new Executor(url, new SetMenutItemIconHandler(menuItem)));
  }
  
  public void setImageBitmap(final JenkinsCloudDataNode node, final String url) {
    
    try {
      Bitmap bm = downloadImage(url);
      node.setIconBmp(bm);
    } catch (Exception e) {
      ;
    }
  }

  public void setImageBitmap(final ImageView imageView, final String url) {
    execution.offer(new Executor(url, new SetImageViewIconHandler(imageView)));
  }

  private Bitmap downloadImage(String url) throws ClientProtocolException,
      IOException {
    Bitmap bm;
    bm = ImageCache.get(url);
    if (bm == null) {
      AbstractSecureHttpClient client =
          new ServerAuthenticationDefaultHttpClient(url);
      HttpResponse response = client.executeGetQuery(false);
      InputStream is = response.getEntity().getContent();
      BufferedInputStream bis = new BufferedInputStream(is);
      bm = BitmapFactory.decodeStream(bis);
      ImageCache.put(url, bm);
      bis.close();
      is.close();
    }

    return bm;
  }

	public void preloadImage(String basePath, JenkinsCloudNode genericNode) {
		if(!(genericNode instanceof JenkinsCloudDataNode)) {
			return;
		}
		
		JenkinsCloudDataNode node = (JenkinsCloudDataNode) genericNode;
		String nodePath = node.getPath();
		if (node.getIcon() != null) {
			setImageBitmap(node, getImageUrl(basePath, node, nodePath));
		}

		List<JenkinsCloudDataNode> payload = node.getPayload();
		if (payload != null) {
			for (JenkinsCloudDataNode subNode : payload) {
				preloadImage(basePath + nodePath, subNode);
			}
		}
	}

  private String getImageUrl(String basePath, JenkinsCloudDataNode node,
      String nodePath) {
    String iconPath = node.getIcon();
    if (iconPath.startsWith("http")) {
      return iconPath;
    } else if (iconPath.startsWith("/")) {
      return iconPath;
    } else {
      return basePath + nodePath + iconPath;
    }
  }
}
