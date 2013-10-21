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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lmit.jenkins.android.activity.R;
import com.lmit.jenkins.android.addon.Utils;
import com.lmit.jenkins.android.configuration.Configuration;

public class LegalNoticeActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.legal_notice);

		setTitle(R.string.legal_notice_view_title);

		TextView legalNoticeText = (TextView) findViewById(R.id.fieldValue);

		legalNoticeText.setText(Utils.readAssetTextFile(this,
				"legalnotice."
						+ Configuration.getInstance().getDeviceLocale()
						+ ".txt", "legalnotice.en.txt"));
	}

	public void onOkButtonClick(View v) {

		finish();
	}
}
