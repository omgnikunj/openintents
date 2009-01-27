/* $Id$
 * 
 * Copyright 2008 Randy McEoin
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
package org.openintents.safe;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Restore extends Activity {
	
	private static boolean debug = false;
	private static final String TAG = "Restore";
	
	private DBHelper dbHelper=null;
	private String masterKey="";
	private String filename=null;
	private RestoreDataSet restoreDataSet=null;
	private boolean firstTime=false;

    public static final String KEY_FIRST_TIME = "first_time";  // Intent keys

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		if (debug) Log.d(TAG,"onCreate()");

		firstTime = icicle != null ? icicle.getBoolean(Restore.KEY_FIRST_TIME) : false;
		if (firstTime == false) {
		    Bundle extras = getIntent().getExtras();            
		    firstTime = extras != null ? extras.getBoolean(Restore.KEY_FIRST_TIME) : false;
		}

		if ((!firstTime) && (!CategoryList.isSignedIn())) {
			Intent frontdoor = new Intent(this, FrontDoor.class);
			startActivity(frontdoor);		
			finish();
		}

		setContentView(R.layout.restore);
		String title = getResources().getString(R.string.app_name) + " - " +
			getResources().getString(R.string.restore);
		setTitle(title);
		
		if (filename==null) {
			filename=CategoryList.BACKUP_FILENAME;
		}
		TextView filenameText;
		filenameText = (TextView) findViewById(R.id.restore_filename);
		filenameText.setText(filename);

		TextView restoreInfoText;
		restoreInfoText = (TextView) findViewById(R.id.restore_info);

		EditText passwordText;
		passwordText = (EditText) findViewById(R.id.restore_password);
		
		Button restoreButton;
		restoreButton = (Button) findViewById(R.id.restore_button);

		if (!backupFileExists(filename)) {
			passwordText.setVisibility(0);
			restoreButton.setVisibility(0);
			restoreInfoText.setText(R.string.restore_no_file);
			return;
		}

		restoreInfoText.setText(R.string.restore_set_password);

		passwordText.setVisibility(1);
		restoreButton.setVisibility(1);

		restoreButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View arg0) {
				EditText passwordText;
				passwordText = (EditText) findViewById(R.id.restore_password);

				String masterPassword = passwordText.getText().toString();
				read(filename, masterPassword);
			}
		});
    }

    @Override
	protected void onResume() {
		super.onResume();
		
		if (debug) Log.d(TAG,"onResume()");
		
		if ((!firstTime) && (!CategoryList.isSignedIn())) {
			Intent frontdoor = new Intent(this, FrontDoor.class);
			startActivity(frontdoor);		
			finish();
		}
	}

    private boolean backupFileExists(String filename) {
		FileReader fr;
		try {
			fr = new FileReader(filename);
			fr.close();
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
    }

    public boolean read(String filename, String masterPassword) {
		if (debug) Log.d(TAG,"read("+filename+",)");
    	
		FileReader fr;
		try {
			fr = new FileReader(filename);
		} catch (FileNotFoundException e1) {
			// e1.printStackTrace();
			Toast.makeText(Restore.this, getString(R.string.restore_unable_to_open)+
				" "+e1.getLocalizedMessage(),
				Toast.LENGTH_LONG).show();
			return false;
		}

		SAXParserFactory spf = SAXParserFactory.newInstance();
		try {
			SAXParser sp = spf.newSAXParser();

			XMLReader xr = sp.getXMLReader(); 

			RestoreHandler myRestoreHandler = new RestoreHandler();
			xr.setContentHandler(myRestoreHandler); 

			xr.parse(new InputSource(fr)); 

			restoreDataSet = myRestoreHandler.getParsedData();

		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
			Toast.makeText(Restore.this, getString(R.string.restore_unable_to_open)+
				" "+e.getLocalizedMessage(),
				Toast.LENGTH_LONG).show();
			return false;
		} catch (SAXException e) {
			//e.printStackTrace();
			Toast.makeText(Restore.this, getString(R.string.restore_unable_to_open)+
				" "+e.getLocalizedMessage(),
				Toast.LENGTH_LONG).show();
			return false;
		} catch (IOException e) {
			//e.printStackTrace();
			Toast.makeText(Restore.this, getString(R.string.restore_unable_to_open)+
				" "+e.getLocalizedMessage(),
				Toast.LENGTH_LONG).show();
			return false;
		} 

		if (restoreDataSet.getVersion() != Backup.CURRENT_VERSION) {
			Toast.makeText(Restore.this, getString(R.string.restore_bad_version)+
				" "+Integer.toString(restoreDataSet.getVersion()),
				Toast.LENGTH_LONG).show();
        	return false;
		}
		CategoryEntry firstCatEntry=restoreDataSet.getCategories().get(0);
		if (firstCatEntry==null) {
			Toast.makeText(Restore.this, getString(R.string.restore_error),
				Toast.LENGTH_LONG).show();
			return false;
		}
		CryptoHelper ch=new CryptoHelper();
		
		String salt=restoreDataSet.getSalt();
		String masterKeyEncrypted=restoreDataSet.getMasterKeyEncrypted();
		masterKey="";
		try {
			ch.init(CryptoHelper.EncryptionStrong, salt);
			ch.setPassword(masterPassword);
			masterKey = ch.decrypt(masterKeyEncrypted);
		} catch (CryptoHelperException e) {
			Log.e(TAG,e.toString());
			Toast.makeText(this, getString(R.string.crypto_error)
				+ e.getMessage(), Toast.LENGTH_SHORT).show();
			return false;
		}
		if (ch.getStatus()==false) {
			Toast.makeText(Restore.this, getString(R.string.restore_decrypt_error),
					Toast.LENGTH_LONG).show();
	        Animation shake = AnimationUtils
	        	.loadAnimation(Restore.this, R.anim.shake);
	        findViewById(R.id.restore_password).startAnimation(shake);

			return false;
		}
		ch=new CryptoHelper();
		try {
			ch.init(CryptoHelper.EncryptionMedium, salt);
			ch.setPassword(masterKey);
		} catch (CryptoHelperException e1) {
			e1.printStackTrace();
			Toast.makeText(this, getString(R.string.crypto_error)
				+ e1.getMessage(), Toast.LENGTH_SHORT).show();
			return false;
		}
		
		String firstCategory="";
		try {
			firstCategory = ch.decrypt(firstCatEntry.name);
		} catch (CryptoHelperException e) {
			Log.e(TAG,e.toString());
		}
		if (ch.getStatus() == false) {
			Toast.makeText(Restore.this, getString(R.string.restore_decrypt_error),
				Toast.LENGTH_LONG).show();
			return false;
		}
		if (debug) Log.d(TAG,"firstCategory="+firstCategory);

		dbHelper=new DBHelper(Restore.this);

		String msg=getString(R.string.restore_found)+" "+
        	Integer.toString(restoreDataSet.getTotalEntries())+" "+
        	getString(R.string.restore_passwords)+" "+
        	restoreDataSet.getDate()+".\n"+
			getString(R.string.dialog_restore_database_msg);
		Dialog confirm = new AlertDialog.Builder(Restore.this)
		.setIcon(android.R.drawable.ic_menu_manage)
		.setTitle(R.string.dialog_restore_database_title)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				restoreDatabase();
			}
		})
		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dbHelper.close();
			}
		}) 
		.setMessage(msg)
		.create();
		confirm.show();

		return true;
	}
    
	private void restoreDatabase() {
		dbHelper.beginTransaction();
		dbHelper.deleteDatabase();

		dbHelper.storeSalt(restoreDataSet.getSalt());
		dbHelper.storeMasterKey(restoreDataSet.getMasterKeyEncrypted());
		CategoryList.setSalt(restoreDataSet.getSalt());
		PassList.setSalt(restoreDataSet.getSalt());
		CategoryList.setMasterKey(masterKey);
		PassList.setMasterKey(masterKey);
		for (CategoryEntry category : restoreDataSet.getCategories()) {
			if (debug) Log.d(TAG,"category="+category.name);
			dbHelper.addCategory(category);
		}
		int totalPasswords=0;
		for (PassEntry password : restoreDataSet.getPass()) {
			totalPasswords++;
			long rowid=dbHelper.addPassword(password);
			if (password.packageAccess!=null) {
				for (String packageName : password.packageAccess) {
					if (debug) Log.d(TAG,"packageName="+packageName);
					dbHelper.addPackageAccess(rowid, packageName);
				}
			}
		}
		dbHelper.commit();
		dbHelper.close();

		Toast.makeText(Restore.this, getString(R.string.restore_complete)+
			" "+Integer.toString(totalPasswords),
			Toast.LENGTH_LONG).show();

		setResult(RESULT_OK);
		finish();
	}
}
