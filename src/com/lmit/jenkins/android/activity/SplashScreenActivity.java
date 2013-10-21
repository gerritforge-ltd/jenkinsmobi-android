package com.lmit.jenkins.android.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.lmit.jenkins.android.configuration.Configuration;
import com.lmit.jenkins.android.logger.Logger;
import com.lmit.jenkins.android.networking.ServerAuthenticationDefaultHttpClient;

public class SplashScreenActivity extends Activity {
	long m_dwSplashTime = 200;
	boolean m_bPaused = false;
    boolean m_bSplashActive = true;
    Intent intent;
    String pkg;
    Context ctx;
    int flag=0;
    
    public SplashScreenActivity() {
      JenkinsMobi.setContext(this);
    }
    
    @Override
    public void onCreate(Bundle icicle)
    {
    	super.onCreate(icicle);
    	
		final Uri intentUri = getIntent().getData();
		if (intentUri != null) {
			Configuration.getInstance().setHomeNode(intentUri);
			Logger.getInstance().info(
					"************************\n"
							+ "Activated from intent URL: " + intentUri + "\n"
							+ "************************\n");
		} else {
			Configuration.getInstance().setHomeNode(Configuration.DEFAULT_HOME_NODE);
		}

        setContentView(R.layout.spalsh);
       
        intent=new Intent(getApplicationContext(), HudsonDroidHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        		Thread splashTimer = new Thread()
    		{
    			public void run()
    		     {
    				flag=1;
    				
    				try
    				{
    			        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    					//Wait loop
    					long ms = 0;
    					while(m_bSplashActive && ms < m_dwSplashTime)
    					{
    						sleep(100);
    						//Advance the timer only if we're running.
    						if(!m_bPaused)
    							ms += 100;
    					}
    					//Advance to the next screen.
    					startActivity(intent);
    					SplashScreenActivity.this.finish(); 	
    					SplashScreenActivity.super.onDestroy();
    					}
    				catch(Exception e)
    				{
    					Log.e("Splash", e.toString());
    				}
    				finally
    				{
    					finish();
    				}
    			 }
    		};
    		
    		if (flag==0)
    		{
    	   splashTimer.start();
    		}
    }
    
    protected void onPause()
    {
    super.onPause();
    m_dwSplashTime = 400;
    intent=new Intent(getApplicationContext(), HudsonDroidHomeActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    }    
    
    protected void onResume()
    {
    super.onResume();
    m_dwSplashTime = 400;
    intent=new Intent(getApplicationContext(), HudsonDroidHomeActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
	}
    
  @Override
  protected void onStart() {
    super.onStart();
    Configuration.getInstance().load();
    ServerAuthenticationDefaultHttpClient.init();
  }
    
}