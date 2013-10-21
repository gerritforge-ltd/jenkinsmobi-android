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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class NotificationMessageFactory {

	public final static int MESSAGE_TYPE = 0x00000001;
	public final static int LED_TYPE = 0x00000010;
	public final static int VIBRATE_TYPE = 0x00000100;
	public final static int SOUND_TYPE = 0x00001000;

	public static class NotificationObject {

		private int type = MESSAGE_TYPE;
		private int notificationBarIcon;
		private int notificationId;
		private boolean persistent = false;
		private String title;
		private String notificationBarMessage;
		private String notificationDescriptionMessage;
		private String notificationDescriptionTitle;
		private Class<?> activityClassToInvoke;

		public boolean isPersistent() {
			return persistent;
		}

		public void setPersistent(boolean persistent) {
			this.persistent = persistent;
		}

		public int getNotificationId() {
			return notificationId;
		}

		public void setNotificationId(int notificationId) {
			this.notificationId = notificationId;
		}

		public Class<?> getActivityClassToInvoke() {
			return activityClassToInvoke;
		}

		public void setActivityClassToInvoke(Class<?> activityClassToInvoke) {
			this.activityClassToInvoke = activityClassToInvoke;
		}

		public String getNotificationDescriptionTitle() {
			return notificationDescriptionTitle;
		}

		public void setNotificationDescriptionTitle(
				String notificationDescriptionTitle) {
			this.notificationDescriptionTitle = notificationDescriptionTitle;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getNotificationBarMessage() {
			return notificationBarMessage;
		}

		public void setNotificationBarMessage(String notificationBarMessage) {
			this.notificationBarMessage = notificationBarMessage;
		}

		public String getNotificationDescriptionMessage() {
			return notificationDescriptionMessage;
		}

		public void setNotificationDescriptionMessage(
				String notificationDescriptionMessage) {
			this.notificationDescriptionMessage = notificationDescriptionMessage;
		}

		public int getNotificationBarIcon() {
			return notificationBarIcon;
		}

		public void setNotificationBarIcon(int notificationBarIcon) {
			this.notificationBarIcon = notificationBarIcon;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}
	}

	private static NotificationManager nm = null;

	public static void showNotification(Context ctx,
			NotificationObject notificationObject) {

		getNotificationManager(ctx);

		removeNotification(ctx, notificationObject.getNotificationId());

		Notification notification = new Notification();

		if (notificationObject.isPersistent()) {

			notification.flags |= Notification.FLAG_NO_CLEAR;
		} else {
			notification.flags |= Notification.FLAG_AUTO_CANCEL;
		}

		if ((notificationObject.getType() & MESSAGE_TYPE) != 0) {
			notification.icon = notificationObject.getNotificationBarIcon();
			notification.tickerText = notificationObject
					.getNotificationBarMessage();
			notification.when = System.currentTimeMillis();

			CharSequence contentTitle = notificationObject
					.getNotificationDescriptionTitle();
			CharSequence contentText = notificationObject
					.getNotificationDescriptionMessage();
			Intent notificationIntent = new Intent(ctx,
					notificationObject.getActivityClassToInvoke());
			PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
					notificationIntent, Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);

			notification.setLatestEventInfo(ctx, contentTitle, contentText,
					contentIntent);
		}

		if ((notificationObject.getType() & LED_TYPE) != 0) {
			notification.flags |= Notification.FLAG_SHOW_LIGHTS;
			notification.ledARGB = 0xFFFF0000;
			notification.ledOffMS = 300;
			notification.ledOnMS = 1000;
		}

		if ((notificationObject.getType() & VIBRATE_TYPE) != 0) {

			long[] vibrate = { 0, 100, 200, 300 };
			notification.vibrate = vibrate;
		}

		if ((notificationObject.getType() & SOUND_TYPE) != 0) {

			notification.defaults |= Notification.DEFAULT_SOUND; 
		}

		nm.notify(notificationObject.getNotificationId(), notification);
	}

	private static void getNotificationManager(Context ctx) {
		if (nm == null) {
			nm = (NotificationManager) ctx
					.getSystemService(Context.NOTIFICATION_SERVICE);
		}
	}

	public static void removeNotification(Context ctx, int id) {

		getNotificationManager(ctx);

		nm.cancel(id);
	}
}
