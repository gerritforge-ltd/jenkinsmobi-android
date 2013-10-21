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
package com.lmit.jenkins.android.activity;

import java.text.MessageFormat;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.lmit.jenkins.android.activity.R;
import com.lmit.jenkins.android.addon.Utils;
import com.lmit.jenkins.android.configuration.Configuration;

public class ProductInfoActivity extends Activity {

    public void onLmitLogoClick(View v){
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.lmitsoftware.com"));
      startActivity(browserIntent);
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.product_info);

		Button legalInfoButton = (Button) findViewById(R.id.legalInfoButton);
		legalInfoButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent myIntent = new Intent();
				myIntent.setClassName(Configuration.ACTIVITY_PACKAGE_NAME,
						LegalNoticeActivity.class.getName());
				startActivity(myIntent);
			}
		});
		
		TextView buildNumberText = (TextView) findViewById(R.id.buildNumberField);
		buildNumberText.setText(Utils.readAssetTextFile(this, "jenkins.version", "jenkins.version"));
		
        TextView deviceInfoText = (TextView) findViewById(R.id.deviceInfoField);
        StringBuilder builder = new StringBuilder();
        builder.append(MessageFormat.format(getText(R.string.info_device_name)
                .toString(), Build.PRODUCT));
        builder.append("\n");
        builder.append(MessageFormat.format(getText(R.string.info_device_model)
                .toString(), Build.MODEL));
        builder.append("\n");
        builder.append(MessageFormat.format(
                getText(R.string.info_device_manufacturer).toString(),
                Build.MANUFACTURER));
        builder.append("\n");
        builder.append(MessageFormat.format(
                getText(R.string.info_device_soft_ver).toString(),
                getDeviceSoftwareVersion()));

        deviceInfoText.setText(builder.toString());

//TODO
//        TextView versionText = (TextView) findViewById(R.id.versionField);
//        versionText.setText(Configuration.getInstance()
//                .getProductVersion()+" - " + result.getVersion().getVersion());
	}

	private String getDeviceSoftwareVersion() {

		if (Build.VERSION.SDK_INT >= Configuration.ANDROID_RELEASE_CODENAMES.length) {

			return Build.VERSION.RELEASE;
		} else {

			return Build.VERSION.RELEASE
					+ " - "
					+ Configuration.ANDROID_RELEASE_CODENAMES[Build.VERSION.SDK_INT];
		}
	}
}
