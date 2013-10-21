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

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;

public class AnimatedGif2 extends AnimationDrawable {
	
	/*
	 * Frame array must be set as: 1 2 3 4 3 2 1 with 200 millis delay
	 */
	public AnimatedGif2(Context context, int[] resIds) {
		
		// infinite loop
		this.setOneShot(false);

		for (int i = 0; i < resIds.length; i++) {

			Drawable frame = context.getResources().getDrawable(resIds[i]);
			addFrame(frame, 200);
		}

		for (int i = resIds.length - 2; i > 0; i--) {

			Drawable frame = context.getResources().getDrawable(resIds[i]);
			addFrame(frame, 200);
		}
	}
}
