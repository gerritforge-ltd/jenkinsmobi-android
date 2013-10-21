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
package com.lmit.jenkins.android.adapter;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.lmit.jenkins.android.activity.R;
import com.lmit.jenkins.android.addon.Utils;
import com.lmit.jenkins.android.networking.ImageDownloader;

public class AbstractHudsonDroidListAdapter extends SimpleAdapter {

  private Context appContext;

  public AbstractHudsonDroidListAdapter(Context context,
      List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
    super(context, data, resource, from, to);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    LayoutInflater mInflater = LayoutInflater.from(appContext);

    Map<String, RowData> itemData = (Map<String, RowData>) getItem(position);
    RowData dataRow = itemData.get(RowData.ROW_KEY);

    // if (convertView == null) {
    // convertView = inflateProperLayout(mInflater, dataRow);
    // dataRow.setAndroidSysRowId(convertView.getId());
    // } else {
    //
    // if (dataRow.getAndroidSysRowId() != convertView.getId()) {
    //
    // convertView = inflateProperLayout(mInflater, dataRow);
    // dataRow.setAndroidSysRowId(convertView.getId());
    // }
    // }

    convertView = inflateProperLayout(mInflater, dataRow);
    dataRow.setAndroidSysRowId(convertView.getId());
    dataRow.setView(convertView);

    setDescription(convertView, dataRow);

    setValueAndFlags(position, convertView, dataRow);

    return convertView;
  }

  private void setValueAndFlags(int position, View convertView, RowData dataRow) {

    Object valueField = convertView.findViewById(R.id.fieldValue);

    switch (dataRow.getFieldType()) {

      case RowData.BOOLEAN_FIELD: {
        if (valueField instanceof CheckBox) {
          CheckBox checkBox = (CheckBox) valueField;
          checkBox.setTag(dataRow);
          checkBox.setChecked("true".equals(dataRow.getValue()));

          checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                boolean isChecked) {

              RowData selectedSpinnerData = (RowData) buttonView.getTag();
              selectedSpinnerData.setValue(isChecked ? "true" : "false");
              selectedSpinnerData.update(selectedSpinnerData);
            }
          });
        }
      }
        break;
      case RowData.MULTI_CHOICE_FIELD:
        if (valueField instanceof Spinner) {
          Spinner spinner = (Spinner) valueField;
          spinner.setTag(dataRow);
          ArrayAdapter<String> adapter =
              new ArrayAdapter<String>(appContext,
                  android.R.layout.simple_spinner_item, dataRow.getChoices()
                      .toArray(new String[] {}));
          adapter
              .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
          spinner.setAdapter(adapter);

          int currentSelection = 0;
          int choices = dataRow.getChoices().size();
          for (currentSelection = 0; currentSelection < choices; currentSelection++) {

            if (dataRow.getValue().equals(
                spinner.getItemAtPosition(currentSelection))) {

              break;
            }
          }

          if (currentSelection == choices) {
            currentSelection = 1;
          }
          spinner.setSelection(currentSelection);

          spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {

              MultiChoiceRowData selectedSpinnerData =
                  (MultiChoiceRowData) parent.getTag();
              selectedSpinnerData.setValue(selectedSpinnerData.getChoices()
                  .get(position));

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
              ;
            }
          });
        }
        break;
      case RowData.DESCRIPTIVE_FIELD: {

        if (dataRow.getValue() != null && dataRow.getValue().length() > 0) {
          TextView valueView = (TextView) valueField;
          valueView.setVisibility(View.VISIBLE);
          valueView.setText(dataRow.getValue());
          valueView.setTag(dataRow);
        }

        ImageView imageView =
            (ImageView) convertView.findViewById(R.id.fieldStatusIcon);

        if (dataRow.getImageUrl() == null) {

          imageView.setVisibility(View.GONE);
        } else {
          imageView.setVisibility(View.VISIBLE);
          ImageDownloader.getInstance().setImageBitmap(imageView,
              dataRow.getImageUrl());
        }
      }
        break;
      case RowData.BIG_IMAGE_DATA: {

        if (dataRow.getValue() != null && dataRow.getValue().length() > 0) {
          TextView valueView = (TextView) valueField;
          valueView.setVisibility(View.VISIBLE);
          valueView.setText(dataRow.getValue());
          valueView.setTag(dataRow);
        }

        ImageView imageView =
            (ImageView) convertView.findViewById(R.id.descriptionIcon);

        if (dataRow.getDescriptionImageUrl() == null) {

          imageView.setVisibility(View.GONE);
        } else {
          imageView.setVisibility(View.VISIBLE);
          ImageDownloader.getInstance().setImageBitmap(imageView,
              dataRow.getDescriptionImageUrl());
        }
      }
        break;
      case RowData.ACTION_FIELD:
        break;
      case RowData.HEADER_FIELD:
        break;
      default:
        break;
    }

    if (valueField != null && valueField instanceof TextView) {

      if (dataRow.isHiddenValue()) {

        setHiddendTextEdit((TextView) valueField);
      }

      if (dataRow.getAlternativeValueFontColor() != -1) {

        setAlternativeTextColor((TextView) valueField,
            dataRow.getAlternativeValueFontColor());
      } else {

        setAlternativeTextColor((TextView) valueField, Color.BLACK);
      }
    }
  }

  private void setAlternativeTextColor(TextView valueField, int alternativeColor) {

    valueField.setTextColor(alternativeColor);
  }

  private void setDescription(View convertView, RowData dataRow) {

    View descriptionView = convertView.findViewById(R.id.fieldDescription);

    if (descriptionView != null && descriptionView instanceof TextView) {

      ((TextView) descriptionView).setText(dataRow.getDescription());

      if (dataRow.isCenterTextInDescription()) {

        ((TextView) descriptionView).setGravity(Gravity.CENTER);
      }

      if (dataRow.isUseBoldFontForDescription()) {

        ((TextView) descriptionView).setTypeface(Typeface.DEFAULT_BOLD);
      } else {

        ((TextView) descriptionView).setTypeface(Typeface.DEFAULT);
      }

      if (dataRow.getFieldType() == RowData.HEADER_FIELD) {
        descriptionView.setBackgroundColor(Color.GRAY);
        ((TextView) descriptionView).setTextColor(Color.WHITE);
      } else {

        descriptionView.setBackgroundColor(Color.TRANSPARENT);
        if (dataRow.getAlternativeDesxcriptionFontColor() != -1) {

          setAlternativeTextColor((TextView) descriptionView,
              dataRow.getAlternativeDesxcriptionFontColor());
        } else {
          ((TextView) descriptionView).setTextColor(Color.BLACK);
        }
      }

      if (dataRow.getFieldType() == RowData.DESCRIPTIVE_FIELD) {

        TextView fieldCaptionPlusView =
            (TextView) convertView.findViewById(R.id.fieldSubCaption);

        if (dataRow.getSubCaption() != null
            && dataRow.getSubCaption().length() > 0) {

          fieldCaptionPlusView.setVisibility(View.VISIBLE);
          fieldCaptionPlusView.setText(dataRow.getSubCaption());
        } else {
          fieldCaptionPlusView.setVisibility(View.GONE);
        }
      }
    } else if (descriptionView != null && descriptionView instanceof Button) {

      ((Button) descriptionView).setText(dataRow.getDescription());
    }
  }

  private View inflateProperLayout(LayoutInflater mInflater, RowData dataRow) {

    View result = null;

    switch (dataRow.getFieldType()) {
      case RowData.DESCRIPTIVE_FIELD:
        result = mInflater.inflate(R.layout.default_io_row, null);
        break;
      case RowData.MULTI_CHOICE_FIELD:
        result = mInflater.inflate(R.layout.multichoide_io_row, null);
        break;
      case RowData.HEADER_FIELD:
        result = mInflater.inflate(R.layout.header_like_io_row, null);
        break;
      case RowData.LOAD_MORE_DATA:
        result = mInflater.inflate(R.layout.load_more_io_row, null);
        break;
      case RowData.FULL_DESC_DATA:
        result = mInflater.inflate(R.layout.full_desc_io_row, null);
        break;
      case RowData.BIG_IMAGE_DATA:
        result = mInflater.inflate(R.layout.default_io_row_bigimage, null);
        break;
      default:
        break;
    }

    if (dataRow.isUseDisclosureSign()) {

      ImageView disclosure =
          (ImageView) result.findViewById(R.id.disclosureIcon);
      disclosure.setVisibility(View.VISIBLE);
    }

    if (dataRow.getAccessoryIcon() != null) {

      ImageView accessoryIcon =
          (ImageView) result.findViewById(R.id.accessoryIcon);
      if (accessoryIcon != null) {
        Utils.setImageByName(accessoryIcon, appContext,
            dataRow.getAccessoryIcon());
        accessoryIcon.setVisibility(View.VISIBLE);
      } else {
        accessoryIcon.setVisibility(View.GONE);
      }
    }

    return result;
  }

  private void setHiddendTextEdit(TextView textView) {

    textView.setTransformationMethod(new PasswordTransformationMethod());
  }

  public void setContext(Context applicationContext) {
    appContext = applicationContext;
  }
}
