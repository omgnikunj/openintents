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

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Allows user to change the password used to unlock the application
 * as well as encrypt the database.
 * 
 * @author Randy McEoin
 */
public class ChangePass extends Activity {

	private static boolean debug = false;
	private static final String TAG = "ChangePass";
	
	ProgressDialog mChangePassProgress;
	
	private static final int CHANGE_PASS_PROGRESS_KEY = 0;
	
	protected static final int MSG_PASS_CHANGED = 0x101; 
	
	Thread changePassThread = null;
	String oldPassword;
	String newPassword;

    Handler myViewUpdateHandler = new Handler(){
		// @Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case ChangePass.MSG_PASS_CHANGED:
					Toast.makeText(ChangePass.this, R.string.password_changed,
						Toast.LENGTH_LONG).show();
					finish();
					break;
			}
			super.handleMessage(msg);
		}
    }; 

    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		if (debug) Log.d(TAG,"onCreate()");

		if (CategoryList.isSignedIn()==false) {
			finish();
		}
		
		setContentView(R.layout.chg_pass);
		String title = getResources().getString(R.string.app_name) + " - " +
			getResources().getString(R.string.change_password);
		setTitle(title);

		Button changePasswordButton = (Button) findViewById(R.id.change_password_button);
		
		changePasswordButton.setOnClickListener(new View.OnClickListener() {
		    public void onClick(View arg0) {
		    	performChangePass();
		    }
		});
		
    }
    
    @Override
    protected void onPause() {
		super.onPause();
		
		if (debug) Log.d(TAG,"onPause()");
		
		if ((changePassThread != null) && (changePassThread.isAlive())) {
			if (debug) Log.d(TAG,"wait for thread");
//			importThread.interrupt();
			int maxWaitToDie=500000;
			try { changePassThread.join(maxWaitToDie); } 
			catch(InterruptedException e){} //  ignore 
		}
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CHANGE_PASS_PROGRESS_KEY: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage("Please wait while re-encrypting...");
                dialog.setIndeterminate(false);
                dialog.setCancelable(false);
                return dialog;
            }
        }
        return null;
    }
    
    /**
     * Check the old, new and verify fields then try to re-encrypt
     * the data.
     */
    private void performChangePass() {
    	if (debug) Log.d(TAG,"performChangePass()");
    	
		EditText oldPassword = (EditText) findViewById(R.id.old_password);
		EditText newPassword = (EditText) findViewById(R.id.new_password);
		EditText verifyPassword = (EditText) findViewById(R.id.verify_password);

		String oldPlain = oldPassword.getText().toString();
		String newPlain = newPassword.getText().toString();
		String verifyPlain = verifyPassword.getText().toString();

		if (newPlain.compareTo(verifyPlain) != 0) {
            Toast.makeText(ChangePass.this, R.string.new_verify_mismatch,
                    Toast.LENGTH_SHORT).show();
            return;
		}
		if (newPlain.length() < 4) {
            Toast.makeText(ChangePass.this, R.string.notify_blank_pass,
                    Toast.LENGTH_SHORT).show();
            return;
		}
		if (!checkUserPassword(oldPlain)) {
            Toast.makeText(ChangePass.this, R.string.invalid_old_password,
                    Toast.LENGTH_SHORT).show();
            return;
		}
//		changePassThreadStart(oldPlain, newPlain);
		changeMasterPassword(oldPlain, newPlain);
    }
    
	/**
	 * Start a separate thread to import the database.   By running
	 * the import in a thread it allows the main UI thread to return
	 * and permit the updating of the progress dialog.
	 * 
	 * @param oldPass clear text old password
	 * @param newPass clear text new password
	 */
    /*
	private void changePassThreadStart(String oldPass, String newPass){
		if (debug) Log.d(TAG,"changePassThreadStart(,)");
		showDialog(CHANGE_PASS_PROGRESS_KEY);
		oldPassword=oldPass;
		newPassword=newPass;
		changePassThread = new Thread(new Runnable() {
			public void run() {
				changePassword(oldPassword, newPassword);

				dismissDialog(CHANGE_PASS_PROGRESS_KEY);
				
				Message m = new Message();
				m.what = ChangePass.MSG_PASS_CHANGED;
				ChangePass.this.myViewUpdateHandler.sendMessage(m); 

				if (debug) Log.d(TAG,"thread end");
				}
			});
		changePassThread.start();
	}
	*/

    private boolean changeMasterPassword(String oldPass, String newPass) {
    	
		DBHelper dbHelper= new DBHelper(this);

		CryptoHelper ch = new CryptoHelper();

		String encryptedMasterKey = dbHelper.fetchMasterKey();
		String decryptedMasterKey = "";
		try {
			ch.init(CryptoHelper.EncryptionStrong, dbHelper.fetchSalt());
			ch.setPassword(oldPass);
			decryptedMasterKey = ch.decrypt(encryptedMasterKey);
			if (ch.getStatus()==true) {	// successful decryption?
				ch.setPassword(newPass);
				encryptedMasterKey = ch.encrypt(decryptedMasterKey);
				if (ch.getStatus()==true) { // successful encryption?
					dbHelper.storeMasterKey(encryptedMasterKey);
					dbHelper.close();
					Toast.makeText(ChangePass.this, R.string.password_changed,
							Toast.LENGTH_LONG).show();
					setResult(RESULT_OK);
					finish();
					return true;
				}
			}

		} catch (CryptoHelperException e) {
			Log.e(TAG, e.toString());
			Toast.makeText(this,getString(R.string.crypto_error)
				+ e.getMessage(), Toast.LENGTH_SHORT).show();
		}

		dbHelper.close();

		Toast.makeText(ChangePass.this, R.string.error_changing_password,
				Toast.LENGTH_LONG).show();
		return false;
    }
    
    /**
     * This is an older function.   We'll want to re-use this when we
     * allow the user to regenerate the master key.
     * 
     * @param oldPass
     * @param newPass
     */
    public void changePassword(String oldPass, String newPass) {
    	if (debug) Log.d(TAG,"changePassword(,)");
    	
		DBHelper dbHelper= new DBHelper(this);

		CryptoHelper ch = new CryptoHelper();

	    List<CategoryEntry> categoryRows;
		categoryRows = dbHelper.fetchAllCategoryRows();

		List<PassEntry> passRows;
		passRows = dbHelper.fetchAllRows(new Long(0));
		
		/**
		 * Decrypt everything using the old password.
		 */
		if (debug) Log.d(TAG,"decrypting");
		ch.setPassword(oldPass);

		for (CategoryEntry row : categoryRows) {
		    row.plainName = "";
		    try {
				row.plainName = ch.decrypt(row.name);
		    } catch (CryptoHelperException e) {
				if (debug) Log.e(TAG,e.toString());
				Toast.makeText(this,getString(R.string.crypto_error)
					+ e.getMessage(), Toast.LENGTH_SHORT).show();
				return;
		    }
		}

		for (PassEntry row : passRows) {
		    try {
				row.plainDescription = ch.decrypt(row.description);
				row.plainWebsite = ch.decrypt(row.website);
				row.plainUsername = ch.decrypt(row.username);
				row.plainPassword = ch.decrypt(row.password);
				row.plainNote = ch.decrypt(row.note);
		    } catch (CryptoHelperException e) {
				if (debug) Log.e(TAG,e.toString());
				Toast.makeText(this,getString(R.string.crypto_error)
					+ e.getMessage(), Toast.LENGTH_SHORT).show();
				return;
		    }
		}

		/**
		 * Encrypt everything using the new password.
		 */
		if (debug) Log.d(TAG,"encrypting");
		ch.setPassword(newPass);

		for (CategoryEntry row : categoryRows) {
		    try {
				row.name = ch.encrypt(row.plainName);
		    } catch (CryptoHelperException e) {
				if (debug) Log.e(TAG,e.toString());
				Toast.makeText(this,getString(R.string.crypto_error)
					+ e.getMessage(), Toast.LENGTH_SHORT).show();
				return;
		    }
		}

		for (PassEntry row : passRows) {
		    try {
				row.description = ch.encrypt(row.plainDescription);
				row.website = ch.encrypt(row.plainWebsite);
				row.username = ch.encrypt(row.plainUsername);
				row.password = ch.encrypt(row.plainPassword);
				row.note = ch.encrypt(row.plainNote);
		    } catch (CryptoHelperException e) {
				if (debug) Log.e(TAG,e.toString());
				Toast.makeText(this,getString(R.string.crypto_error)
					+ e.getMessage(), Toast.LENGTH_SHORT).show();
				return;
		    }
		}

		/**
		 * Update the database with the newly encrypted data.
		 */
		if (debug) Log.d(TAG,"updating database");
		dbHelper.beginTransaction();

		for (CategoryEntry row : categoryRows) {
			dbHelper.updateCategory(row.id, row);
		}

		for (PassEntry row : passRows) {
			dbHelper.updatePassword(row.id, row);
		}
		
		byte[] md5Key = CryptoHelper.md5String(newPass);
		String hexKey = CryptoHelper.toHexString(md5Key);
		String cryptKey = "";
		Log.i(TAG, "Saving Password: " + hexKey);
		try {
			cryptKey = ch.encrypt(hexKey);
			dbHelper.storeMasterKey(cryptKey);
		} catch (CryptoHelperException e) {
			Log.e(TAG, e.toString());
			Toast.makeText(this,getString(R.string.crypto_error)
				+ e.getMessage(), Toast.LENGTH_SHORT).show();
			dbHelper.rollback();
			dbHelper.close();
			return;
		}

		dbHelper.commit();
		
		PassList.setMasterKey(newPass);
		CategoryList.setMasterKey(newPass);

		dbHelper.close();
    }
    
    /**
     * Check the provided clear text password with the one stored
     * in the database.
     * 
     * @param pass = clear text password
     * @return True if password is correct.
     */
    private boolean checkUserPassword(String pass) {
    	if (debug) Log.d(TAG,"checkUserPassword()");
    	
		DBHelper dbHelper= new DBHelper(this);
		String confirmKey = dbHelper.fetchMasterKey();

		CryptoHelper ch = new CryptoHelper();

		try {
			ch.init(CryptoHelper.EncryptionStrong, dbHelper.fetchSalt());
			ch.setPassword(pass);
			ch.decrypt(confirmKey);
		} catch (CryptoHelperException e) {
			Log.e(TAG, e.toString());
		}
		dbHelper.close();

		// was decryption of the master key successful?
		if (ch.getStatus()==true) {
			return true;	// then we must have a good master password
		}
		return false;
	}

}
