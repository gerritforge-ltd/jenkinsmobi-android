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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.lmit.jenkins.android.activity.R;
import com.lmit.jenkins.android.adapter.RowData;

public class IODialog extends AlertDialog implements OnClickListener {

	private TextView parentView;
	private View inflatedView;
	private RowData parentRowData;

	public static final int ONLY_NUMBERS = 0;
	public static final int SIMPLE_TEXT = 1;
	public static final int URL = 2;
	public static final int CAPITALIZED_TEXT = 3;
	public static final int PASSWORD = 4;

	public IODialog(Context context, RowData pareRowData) {

		super(context);

		this.parentRowData = pareRowData;

		LayoutInflater mInflater = LayoutInflater.from(context);
		inflatedView = mInflater.inflate(R.layout.default_io_dialog, null);
		this.setTitle(this.parentRowData.getDescription());
		this.setView(inflatedView);
		this.setButton(BUTTON_POSITIVE, "Ok", this);
		this.setButton(BUTTON_NEGATIVE, "Cancel", this);

		setInputType(SIMPLE_TEXT);

		if (parentRowData.isHiddenValue()) {

			setInputType(PASSWORD);
		}

		EditText editText = (EditText) inflatedView
				.findViewById(R.id.valueField);
		if (parentRowData.getValue() == null
				|| parentRowData.getValue().length() == 0) {

			editText.setText(parentRowData.getDefaultValue());

		} else {
			editText.setText(parentRowData.getValue());
		}

		editText.setHint(parentRowData.getDescription());
	}

	public IODialog(Context context, TextView parentView) {

		this(context, (RowData) parentView.getTag());
		this.parentView = parentView;
	}

	public void setInputType(int inputType) {

		EditText editText = (EditText) inflatedView
				.findViewById(R.id.valueField);

		int rawInputType = InputType.TYPE_CLASS_TEXT;

		switch (inputType) {

		case SIMPLE_TEXT:
			rawInputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			break;
		case ONLY_NUMBERS:
			rawInputType = InputType.TYPE_CLASS_NUMBER
					| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			break;
		case URL:
			rawInputType = InputType.TYPE_TEXT_VARIATION_URI
					| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			break;
		case CAPITALIZED_TEXT:
			rawInputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
					| InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			break;
		case PASSWORD:
			rawInputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			editText.setTransformationMethod(new PasswordTransformationMethod());
			break;
		}

		editText.setRawInputType(rawInputType);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

		if (which == BUTTON_POSITIVE) {
			EditText editText = (EditText) inflatedView
					.findViewById(R.id.valueField);

			if (parentView != null) {
				parentView.setText(editText.getText().toString());
			}
			parentRowData.setValue(editText.getText().toString());
		}
	}
}
