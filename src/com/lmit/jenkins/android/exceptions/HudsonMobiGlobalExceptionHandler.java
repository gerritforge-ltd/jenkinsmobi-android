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
package com.lmit.jenkins.android.exceptions;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import android.os.Environment;
import android.os.StatFs;

import com.lmit.jenkins.android.logger.Logger;

public class HudsonMobiGlobalExceptionHandler implements
		UncaughtExceptionHandler {

	private UncaughtExceptionHandler defaultHandler;

	private static boolean set = false;

	public HudsonMobiGlobalExceptionHandler(
			UncaughtExceptionHandler defaultHandler) {

		this.defaultHandler = defaultHandler;
	}

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		Logger.getInstance().error(
				"Unexpected error occurred, application is exiting", ex);
		Logger.getInstance().info(
				"Available memory: '" + getAvailableInternalMemorySize() + "'");
		Logger.getInstance().info(
				"Total memory: '" + getTotalInternalMemorySize() + "'");
		Logger.getInstance().info("Device model '" + android.os.Build.MODEL + "'");
		Logger.stopLogger();

		defaultHandler.uncaughtException(thread, ex);
	}

	public static void set() {

		if (!set) {
			UncaughtExceptionHandler defaultHandler = Thread
					.getDefaultUncaughtExceptionHandler();

			Thread.setDefaultUncaughtExceptionHandler(new HudsonMobiGlobalExceptionHandler(
					defaultHandler));

			set = true;
		}
	}

	private long getAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize;
	}

	private long getTotalInternalMemorySize() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return totalBlocks * blockSize;
	}
}
