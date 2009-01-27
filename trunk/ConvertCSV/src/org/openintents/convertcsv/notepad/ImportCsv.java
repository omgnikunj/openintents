/* 
 * Copyright (C) 2008 OpenIntents.org
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

package org.openintents.convertcsv.notepad;

import java.io.IOException;
import java.io.Reader;

import org.openintents.convertcsv.common.WrongFormatException;
import org.openintents.convertcsv.opencsv.CSVReader;

import android.content.Context;
import android.util.Log;

public class ImportCsv {
	private final static String TAG = "ImportCsv";
	
	Context mContext;
	
	public static final String FORMAT_OUTLOOK_NOTES = "outlook notes";
	public static final String FORMAT_PALM_CSV = "palm";
	
	public ImportCsv(Context context) {
		mContext = context;
	}
	
	/**
	 * @param dis
	 * @throws IOException
	 */
	public void importCsv(Reader reader, String format) throws IOException, 
		WrongFormatException {
		
		//boolean isPalm = format.equals(FORMAT_PALM_CSV); // Palm is default.
		boolean isOutlookNotes = format.equals(FORMAT_OUTLOOK_NOTES);
		
		String note;
		long encrypted;
		String tags;
		
		CSVReader csvreader = new CSVReader(reader);
	    String [] nextLine;
	    if (isOutlookNotes) {
	    	// OutlookNotes has a header line that we ignore
	    	nextLine = csvreader.readNext();
	    	if (nextLine.length != 5) {
	    		throw new WrongFormatException();
	    	}
	    }
	    while ((nextLine = csvreader.readNext()) != null) {
	        // nextLine[] is an array of values from the line
	    	if (isOutlookNotes) {
		    	if (nextLine.length != 5) {
		    		throw new WrongFormatException();
		    	}
		    	note = nextLine[0];
		    	
		    	// We read encrypted ID from the Sensitivity column
		    	String sensitivity = nextLine[4];
		    	encrypted = 0;
		    	if (sensitivity.startsWith("Encrypted ")) {
			    	try {
			    		encrypted = Long.parseLong(sensitivity.substring(10));
			    	} catch (NumberFormatException e) {
			    		Log.e(TAG, "Error parsing 'encrypted' input: " + nextLine[1]);
			    	}
		    	}
		    	
		    	tags = nextLine[1];
	    	} else {
	    		// Default: Palm format
		    	if (nextLine.length != 3) {
		    		throw new WrongFormatException();
		    	}
		    	note = nextLine[0];
		    	 	
		    	// Palm windows inserts double carriage returns, 
		    	// so we try to get rid of them:
		    	note = note.replaceAll("\n\n", "\n");
		    	
		    	// Second column is encrypted
		    	encrypted = 0;
		    	try {
		    		encrypted = Long.parseLong(nextLine[1]);
		    	} catch (NumberFormatException e) {
		    		Log.e(TAG, "Error parsing 'encrypted' input: " + nextLine[1]);
		    	}
		    	
		    	// Third column would be category.
		    	tags = nextLine[2];
	    	}
	    	

	    	NotepadUtils.addNote(mContext, note, encrypted, tags);
	    }
	}

}
