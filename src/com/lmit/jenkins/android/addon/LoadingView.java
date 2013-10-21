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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import com.lmit.jenkins.android.activity.R;

public class LoadingView extends ProgressDialog {

	private static String defaultTitle;
	private static LoadingView instance;
	private static LoadingView pendingInstance;
	private Context parent;

	public static LoadingView getInstance(Context context) {

		return getInstance(context, null);
	}

	public static LoadingView getInstance(Context context, String title) {

		if (instance == null) {
			
			instance = new LoadingView(context, title);
		} else {

			pendingInstance = new LoadingView(context, title);
		}

		return instance;
	}

	@Override
	public void onBackPressed() {

		super.onBackPressed();
		
		remove();

		((Activity) (parent)).onBackPressed();
	}

	private LoadingView(Context context, String title) {
		super(context);

		parent = context;

		defaultTitle = context.getText(R.string.loading).toString();

		this.setIndeterminate(true);
		this.setProgressStyle(STYLE_SPINNER);
		if (title == null) {
			this.setMessage(defaultTitle);
		} else {
			this.setMessage(title);
		}
	}

	public static void remove() {
		if (instance != null) {

			instance.dismiss();
			instance = null;
		}

		if (pendingInstance != null) {

			instance = pendingInstance;
			pendingInstance = null;
			instance.show();
		}
	}
}
