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

public class QueryStringNormalizer {

	public static String normalize(String queryString) {

		byte[] chBuffer = queryString.getBytes();

		StringBuilder result = new StringBuilder();
		/*
		 * / --> 0x2F --> 0x2A & --> 0x26 | --> 0x7C ' --> 0x27
		 */

		for (int i = 0; i < chBuffer.length; i++) {

			byte ch = chBuffer[i];
			switch (ch) {
			case 0x2F:
				result.append("%2F");
				break;
			case 0x2A:
				result.append("%2A");
				break;
			// case 0x26:
			// [result appendString:@"%26"];
			// break;
			case 0x7C:
				result.append("%7C");
				break;
			case 0x20:
				result.append("%20");
				break;
			case 0x27:
				result.append("%27");
				break;
			default:
				result.append((char)ch);
				break;
			}
		}

		return result.toString();
	}

	public static String normalizeBlanks(String queryString) {

		byte[] chBuffer = queryString.getBytes();

		StringBuilder result = new StringBuilder();

		for (int i = 0; i < chBuffer.length; i++) {

			byte ch = chBuffer[i];
			switch (ch) {
			case 0x20:
				result.append("%20");
				break;
			default:
				result.append((char)ch);
				break;
			}
		}

		return result.toString();
	}
}
