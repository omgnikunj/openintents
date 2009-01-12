/* 
 * Copyright (C) 2007-2009 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintents.notepad.util;

import java.io.File;

import android.net.Uri;

public class FileUriUtils {

	/**
	 * Convert File into Uri.
	 * @param file
	 * @return uri
	 */
	public static Uri getUri(File file) {
		return Uri.parse("file://" + file.getAbsolutePath());
	}


	/**
	 * Convert Uri into File.
	 * @param uri
	 * @return file
	 */
	public static File getFile(Uri uri) {
		if (uri != null) {
			String filepath = uri.toString();
			if (filepath.startsWith("file://")) {
				filepath = filepath.substring(7);
	  		}
			return new File(filepath);
		}
		return null;
	}

	/**
	 * Convert String into Uri.
	 * @param file
	 * @return uri
	 */
	public static Uri getUri(String filename) {
		return Uri.parse("file://" + filename);
	}

	/**
	 * Convert Uri into String.
	 * @param uri
	 * @return file
	 */
	public static String getFilename(Uri uri) {
		if (uri != null) {
			String filepath = uri.toString();
			if (filepath.startsWith("file://")) {
				filepath = filepath.substring(7);
	  		}
			return filepath;
		}
		return null;
	}

	/**
	 * Constructs a file from a path and file name.
	 * 
	 * @param curdir
	 * @param file
	 * @return
	 */
	public static File getFile(String curdir, String file) {
		String separator = "/";
		  if (curdir.endsWith("/")) {
			  separator = "";
		  }
		   File clickedFile = new File(curdir + separator
		                       + file);
		return clickedFile;
	}
	
	public static File getFile(File curdir, String file) {
		return getFile(curdir.getAbsolutePath(), file);
	}

}
