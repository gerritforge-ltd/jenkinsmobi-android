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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;

import com.lmit.jenkins.android.activity.R;

public class AlertBox {

	private Context context;
	private OnDismissListener onDismissListener;

	public AlertBox(Context context) {
		this.context = context;
	}

	public AlertBox(Context context, OnDismissListener onDismissListener) {
		this.context = context;
		this.onDismissListener = onDismissListener;
	}

	public void show(String message) {

		AlertDialog alert = new AlertDialog.Builder(context).create();
		alert.setMessage(message);
		alert.setButton(Dialog.BUTTON_POSITIVE, "Ok", (OnClickListener) null);
		alert.setCanceledOnTouchOutside(true);

		if (onDismissListener != null) {
			alert.setOnDismissListener(onDismissListener);
		}

		alert.show();
	}

	public void showQuestion(String message, OnClickListener onClickListener) {

		showQuestion(message, context.getText(R.string.yes).toString(), context
				.getText(R.string.no).toString(), onClickListener);
	}

	public void showQuestion(String message, String positiveMessage,
			String negativeMessage, OnClickListener onClickListener) {

		AlertDialog alert = new AlertDialog.Builder(context).create();
		alert.setMessage(message);
		alert.setButton(Dialog.BUTTON_POSITIVE, positiveMessage,
				onClickListener);
		alert.setButton(Dialog.BUTTON_NEGATIVE, negativeMessage,
				onClickListener);
		alert.setCanceledOnTouchOutside(true);
		alert.show();
	}

	public void show(int unsupportedParamsAlert) {

		show(context.getText(unsupportedParamsAlert).toString());
	}
}
