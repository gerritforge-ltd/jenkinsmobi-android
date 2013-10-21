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

import java.util.LinkedList;
import java.util.List;

import android.graphics.Bitmap;
import android.view.View;

public abstract class RowData {

  public static final int MULTI_CHOICE_FIELD = 2;
  public static final int BOOLEAN_FIELD = 3;
  public static final int ACTION_FIELD = 6;
  public static final int HEADER_FIELD = 7;
  public static final int DESCRIPTIVE_FIELD = 10;
  public static final int BUTTON_FIELD = 12;
  public static final int MAVEN2_HEADER_FIELD = 13;
  public static final int LOAD_MORE_DATA = 14;
  public static final int FULL_DESC_DATA = 15;
  public static final int BIG_IMAGE_DATA = 16;
  
  public static final String ROW_KEY = "row";

  protected String description;
  protected String value;
  protected String defaultValue;
  protected String imageName;
  protected Bitmap image;
  protected String imageUrl;
  protected Bitmap descriptionImage;
  protected String descriptionImageUrl;
  protected String subCaption;
  protected String accessoryIcon;
  protected List<String> choices;
  protected int fieldType = DESCRIPTIVE_FIELD;
  protected boolean hiddenValue = false;
  protected boolean showDefault = true;
  protected boolean editable = true;
  protected boolean onlyDescriptiveRow = false;
  protected String captionForDefaultValue;
  protected int alternativeValueFontColor = -1;
  protected int alternativeDesxcriptionFontColor = -1;
  protected boolean useBoldFontForDescription = false;
  protected boolean useBoldFontForValue = false;
  protected boolean centerTextInDescription = false;
  protected boolean useDisclosureSign = false;
  protected Object tag;
  protected View view;

  private int position = 0;

  private int androidSysRowId;

  private List<RowDataSelectionChangeListener> selectionChangeListenerList =
      new LinkedList<RowDataSelectionChangeListener>();
  private List<RowDataChangeListener> dataChangeListenerList =
      new LinkedList<RowDataChangeListener>();

  public RowData(String description, String value) {

    this.description = description;
    this.value = value;
  }

  public RowData(String description, String value, boolean hiddenValue) {

    this.description = description;
    this.value = value;
    this.hiddenValue = hiddenValue;
  }

  public RowData(String description, String value, boolean hiddenValue,
      boolean showDefault) {

    this.description = description;
    this.value = value;
    this.hiddenValue = hiddenValue;
  }

  public RowData() {
    // TODO Auto-generated constructor stub
  }



  public Object getTag() {
    return tag;
  }

  public void setTag(Object tag) {
    this.tag = tag;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  protected List<RowDataSelectionChangeListener> getSelectionChangeListeners() {
    return selectionChangeListenerList;
  }

  public void addSelectionChangeListener(
      RowDataSelectionChangeListener selectionChangeListener) {

    this.selectionChangeListenerList.add(selectionChangeListener);
  }

  protected List<RowDataChangeListener> getDataChangeListeners() {
    return dataChangeListenerList;
  }

  public void addDataChangeListener(RowDataChangeListener dataChangeListener) {

    this.dataChangeListenerList.add(dataChangeListener);
  }

  public void update(RowData updatedData) {

    for (RowDataSelectionChangeListener listener : selectionChangeListenerList) {

      listener.onRowDataSelectionChange(updatedData);
    }
  }

  @Override
  public boolean equals(Object o) {

    if (o == null) {

      return false;
    }

    if (!(o instanceof RowData)) {

      return false;
    }

    RowData __o = (RowData) o;
    if (__o.getDescription() == null && this.description == null) {

      return true;
    }

    if (__o.getDescription() != null
        && __o.getDescription().equals(this.description)) {

      return true;
    }

    return false;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getValue() {

    String result = value;

    if (value == null || value.equals(defaultValue)) {

      if (captionForDefaultValue != null) {
        result = captionForDefaultValue;
      }
    } else if (value != null && value.equals(captionForDefaultValue)) {

      result = defaultValue;

    }

    return result;
  }

  public String getRawValue() {

    if (value == null
        || "".equals(value)
        || (captionForDefaultValue != null && captionForDefaultValue
            .equals(value))) {

      return defaultValue;
    }

    return value;
  }

  public void setValue(String value) {

    this.value = value;

    for (RowDataChangeListener listener : dataChangeListenerList) {

      listener.onRowDataChange(this);
    }
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public int getFieldType() {
    return fieldType;
  }

  public void setFieldType(int fieldType) {
    this.fieldType = fieldType;
  }

  public boolean isHiddenValue() {
    return hiddenValue;
  }

  public void setHiddenValue(boolean hiddenValue) {
    this.hiddenValue = hiddenValue;
  }

  public boolean isShowDefault() {
    return showDefault;
  }

  public void setShowDefault(boolean showDefault) {
    this.showDefault = showDefault;
  }

  public boolean isEditable() {
    return editable;
  }

  public void setEditable(boolean editable) {
    this.editable = editable;
  }

  public boolean isOnlyDescriptiveRow() {
    return onlyDescriptiveRow;
  }

  public void setOnlyDescriptiveRow(boolean onlyDescriptiveRow) {
    this.onlyDescriptiveRow = onlyDescriptiveRow;
  }

  public List<String> getChoices() {
    return choices;
  }

  public void setChoices(List<String> choices) {
    this.choices = choices;
  }

  public String getCaptionForDefaultValue() {
    return captionForDefaultValue;
  }

  public void setCaptionForDefaultValue(String captionForDefaultValue) {
    this.captionForDefaultValue = captionForDefaultValue;
  }

  public String getImageName() {
    return imageName;
  }

  public void setImageName(String imageName) {
    this.imageName = imageName;
  }
  
  public Bitmap getImage() {
    return image;
  }

  public void setImage(Bitmap image) {
    this.image = image;
  }

  public String getSubCaption() {
    return subCaption;
  }

  public void setSubCaption(String subCaption) {
    this.subCaption = subCaption;
  }

  public int getAlternativeValueFontColor() {
    return alternativeValueFontColor;
  }

  public void setAlternativeValueFontColor(int alternativeValueFontColor) {
    this.alternativeValueFontColor = alternativeValueFontColor;
  }

  public int getAlternativeDesxcriptionFontColor() {
    return alternativeDesxcriptionFontColor;
  }

  public void setAlternativeDesxcriptionFontColor(
      int alternativeDesxcriptionFontColor) {
    this.alternativeDesxcriptionFontColor = alternativeDesxcriptionFontColor;
  }

  public boolean isUseBoldFontForDescription() {
    return useBoldFontForDescription;
  }

  public void setUseBoldFontForDescription(boolean useBoldFontForDescription) {
    this.useBoldFontForDescription = useBoldFontForDescription;
  }

  public boolean isUseBoldFontForValue() {
    return useBoldFontForValue;
  }

  public void setUseBoldFontForValue(boolean useBoldFontForValue) {
    this.useBoldFontForValue = useBoldFontForValue;
  }

  public boolean isCenterTextInDescription() {
    return centerTextInDescription;
  }

  public void setCenterTextInDescription(boolean centerTextInDescription) {
    this.centerTextInDescription = centerTextInDescription;
  }

  public boolean isUseDisclosureSign() {
    return useDisclosureSign;
  }

  public void setUseDisclosureSign(boolean useDisclosureSign) {
    this.useDisclosureSign = useDisclosureSign;
  }

  public String getAccessoryIcon() {
    return accessoryIcon;
  }

  public void setAccessoryIcon(String accessoryIcon) {
    this.accessoryIcon = accessoryIcon;
  }

  public int getAndroidSysRowId() {
    return androidSysRowId;
  }

  public void setAndroidSysRowId(int androidSysRowId) {
    this.androidSysRowId = androidSysRowId;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }
  
  public String getImageUrl(){
    return this.imageUrl;
  }

  public View getView() {
    return view;
  }

  public void setView(View view) {
    this.view = view;
  }

  public Bitmap getDescriptionImage() {
    return descriptionImage;
  }

  public void setDescriptionImage(Bitmap descriptionImage) {
    this.descriptionImage = descriptionImage;
  }

  public String getDescriptionImageUrl() {
    return descriptionImageUrl;
  }

  public void setDescriptionImageUrl(String descriptionImageUrl) {
    this.descriptionImageUrl = descriptionImageUrl;
  }
}
