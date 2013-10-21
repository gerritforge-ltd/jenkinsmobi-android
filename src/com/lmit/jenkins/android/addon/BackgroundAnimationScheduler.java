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

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;

public class BackgroundAnimationScheduler {

	private static BackgroundAnimationScheduler instance;

	private Timer backgroundAnimationTimer;

	private List<Activity> hostActivityStack;

	private List<List<AnimationDrawable>> animationsQueuesStack;

	private Activity currentActivity;
	private List<AnimationDrawable> currentAnimationsQueue;

	private BackgroundAnimationScheduler() {

		hostActivityStack = new LinkedList<Activity>();

		animationsQueuesStack = new LinkedList<List<AnimationDrawable>>();
	}

	public static BackgroundAnimationScheduler getInstance() {

		if (instance == null) {

			instance = new BackgroundAnimationScheduler();
		}

		return instance;
	}

	public void pushHostActivity(Activity activity) {

		hostActivityStack.add(activity);

		currentAnimationsQueue = new LinkedList<AnimationDrawable>();

		animationsQueuesStack.add(currentAnimationsQueue);

		currentActivity = activity;
	}

	public void popHostActivity() {

		if (hostActivityStack.size() > 0) {

			hostActivityStack.remove(hostActivityStack.size() - 1);

			animationsQueuesStack.remove(animationsQueuesStack.size() - 1);

			if (hostActivityStack.size() > 0) {

				currentActivity = hostActivityStack.get(hostActivityStack
						.size() - 1);

				currentAnimationsQueue = animationsQueuesStack
						.get(animationsQueuesStack.size() - 1);
				
				startAll();
				
			} else {
				currentActivity = null;

				currentAnimationsQueue = null;
			}
		}
	}

	public void enqueueAnimation(AnimationDrawable animation) {

		currentAnimationsQueue.add(animation);

		if (backgroundAnimationTimer == null) {

			backgroundAnimationTimer = new Timer("BackgroundAnimationScheduler");
			backgroundAnimationTimer.schedule(
					new BackgroundAnimationSchedulerThread(currentActivity),
					200);
		}
	}

	public void stopAll() {

		if (currentAnimationsQueue != null) {
			for (AnimationDrawable animation : currentAnimationsQueue) {

				animation.stop();
			}

			if (animationsQueuesStack.size() <= 1) {
				if (backgroundAnimationTimer != null) {
					backgroundAnimationTimer.cancel();
					backgroundAnimationTimer = null;
				}
			}
			
			currentAnimationsQueue.clear();
		}
	}

	public void startAll() {
		if (currentAnimationsQueue != null) {
			for (AnimationDrawable animation : currentAnimationsQueue) {

				animation.start();
			}
		}
	}

	public void clear() {

		stopAll();
	}

	private class BackgroundAnimationSchedulerThread extends TimerTask {

		private Activity parent;

		public BackgroundAnimationSchedulerThread(Activity parent) {

			this.parent = parent;
		}

		@Override
		public void run() {

			parent.runOnUiThread(new Runnable() {

				@Override
				public void run() {

					BackgroundAnimationScheduler.getInstance().startAll();
				}
			});
		}
	}
}
