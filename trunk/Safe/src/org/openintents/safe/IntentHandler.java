/* $Id$
 * 
 * Copyright 2007-2008 Steven Osborn
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


import java.util.ArrayList;

import org.openintents.intents.CryptoIntents;
import org.openintents.safe.dialog.DialogHostingActivity;
import org.openintents.safe.service.ServiceDispatch;
import org.openintents.safe.service.ServiceDispatchImpl;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


/**
 * FrontDoor Activity
 * 
 * This activity just acts as a splash screen and gets the password from the
 * user that will be used to decrypt/encrypt password entries.
 * 
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class IntentHandler extends Activity {

	private static final boolean debug = !false;
	private static String TAG = "IntentHandler";
	
	private static final int REQUEST_CODE_ASK_PASSWORD = 1;
	private static final int REQUEST_CODE_ALLOW_EXTERNAL_ACCESS = 2;
	

	private DBHelper dbHelper;
	private String salt;
	private String masterKey;
	private CryptoHelper ch;

	// service elements
    private ServiceDispatch service;
    private ServiceDispatchConnection conn;
	private Intent mServiceIntent;

    SharedPreferences mPreferences;
	//public static String SERVICE_NAME = "org.openintents.safe.service.ServiceDispatchImpl";
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		mServiceIntent = null;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		initService(); // start up the PWS service so other applications can query.
	}

	
	//currently only handles result from askPassword function.
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (debug) Log.d(TAG, "onActivityResult: requestCode == " + requestCode + ", resultCode == " + resultCode);
	
		switch (requestCode) {
		case REQUEST_CODE_ASK_PASSWORD:
			if (resultCode == RESULT_OK) {

				if (service == null) {
					mServiceIntent = data;
					// setServiceParametersFromExtrasAndDispatchAction() is called in onServiceConnected.
					return;
				}
				
				setServiceParametersFromExtrasAndDispatchAction(data);
				
			} else { // resultCode == RESULT_CANCELED
				setResult(RESULT_CANCELED);
				finish();
			}
			break;
		case REQUEST_CODE_ALLOW_EXTERNAL_ACCESS:
			// Check again, regardless whether user pressed "OK" or "Cancel".
			// Also, DialogHostingActivity never returns a resultCode different than
			// RESULT_CANCELED.
			if (service == null) {
				if (debug) Log.i(TAG, "actionDispatch called later");
				// actionDispatch() is called in onServiceConnected.
			} else if (salt == null) {
				try {
		        	salt = service.getSalt();
					masterKey = service.getPassword();
					actionDispatch();
				} catch (RemoteException e) {
					Log.d(TAG, e.toString());
					// Not successful...
					finish();
				}
			} else {
				if (debug) Log.i(TAG, "actionDispatch called right now");
				actionDispatch();
			}
			break;
		}
			
	}

	/**
	 * @param data
	 */
	private void setServiceParametersFromExtrasAndDispatchAction(Intent data) {
		salt = data.getStringExtra("salt");
		masterKey = data.getStringExtra("masterKey");
		String timeout = mPreferences.getString(Preferences.PREFERENCE_LOCK_TIMEOUT, Preferences.PREFERENCE_LOCK_TIMEOUT_DEFAULT_VALUE); 
		int timeoutMinutes=5; // default to 5
		try {
			timeoutMinutes = Integer.valueOf(timeout);
		} catch (NumberFormatException e) {
			Log.d(TAG,"why is lock_timeout busted?");
		}
		
		try {
			service.setTimeoutMinutes(timeoutMinutes);
			service.setSalt(salt);
			service.setPassword(masterKey); // should already be connected.
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		boolean externalAccess = mPreferences.getBoolean(Preferences.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false);
		boolean isLocal = isIntentLocal();
		
		if (isLocal || externalAccess) {
			actionDispatch();
		} else {
			// ask first
			if (debug) Log.d(TAG, "onActivityResult: showDialogAllowExternalAccess()");
			showDialogAllowExternalAccess();
		}
	}

	/**
	 * 
	 */
	private void showDialogAllowExternalAccess() {
		Intent i = new Intent(this, DialogHostingActivity.class);
		i.putExtra(DialogHostingActivity.EXTRA_DIALOG_ID, DialogHostingActivity.DIALOG_ID_ALLOW_EXTERNAL_ACCESS);
		this.startActivityForResult(i, REQUEST_CODE_ALLOW_EXTERNAL_ACCESS);
	}
	
	protected void actionDispatch () {    
		final Intent thisIntent = getIntent();
        final String action = thisIntent.getAction();
    	Intent callbackIntent = getIntent(); 
    	int callbackResult = RESULT_CANCELED;
		PassList.setSalt(salt);
        CategoryList.setSalt(salt);
		PassList.setMasterKey(masterKey);
        CategoryList.setMasterKey(masterKey);
        if (ch == null) {
    		ch = new CryptoHelper();
        }
        try {
			ch.init(CryptoHelper.EncryptionMedium,salt);
    		ch.setPassword(masterKey);
		} catch (CryptoHelperException e1) {
			e1.printStackTrace();
			Toast.makeText(this, getString(R.string.crypto_error)
				+ e1.getMessage(), Toast.LENGTH_SHORT).show();
			return;
		}

        boolean externalAccess = mPreferences.getBoolean(Preferences.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false);

        if (action == null || action.equals(Intent.ACTION_MAIN)){
        	//TODO: When launched from debugger, action is null. Other such cases?
        	Intent i = new Intent(getApplicationContext(),
        			CategoryList.class);
        	startActivity(i);
        } else if (externalAccess){

        	// which action?
        	if (action.equals (CryptoIntents.ACTION_ENCRYPT)) {
        		callbackResult = encryptIntent(thisIntent, callbackIntent);
        	} else if (action.equals (CryptoIntents.ACTION_DECRYPT)) {
        		callbackResult = decryptIntent(thisIntent, callbackIntent);
        	} else if (action.equals (CryptoIntents.ACTION_GET_PASSWORD)
        			|| action.equals (CryptoIntents.ACTION_SET_PASSWORD)) {
        		try {
        			callbackIntent = getSetPassword (thisIntent, callbackIntent);
                	callbackResult = RESULT_OK;
        		} catch (CryptoHelperException e) {
        			Log.e(TAG, e.toString(), e);
        			Toast.makeText(IntentHandler.this,
        					"There was a crypto error while retreiving the requested password: " + e.getMessage(),
        					Toast.LENGTH_SHORT).show();
        		} catch (Exception e) {
        			Log.e(TAG, e.toString(), e);
        			//TODO: Turn this into a proper error dialog.
        			Toast.makeText(IntentHandler.this,
        					"There was an error in retreiving the requested password: " + e.getMessage(),
        					Toast.LENGTH_SHORT).show();
        		}
        	}
        	setResult(callbackResult, callbackIntent);
        }
        finish();
	}


	/**
	 * Encrypt all supported fields in the intent and return the result in callbackIntent.
	 * 
	 * This is supposed to be called by outside functions, so we encrypt using a random session key.
	 * 
	 * @param thisIntent
	 * @param callbackIntent
	 * @return callbackResult
	 */
	private int encryptIntent(final Intent thisIntent, Intent callbackIntent) {
		if (debug)
			Log.d(TAG, "encryptIntent()");
		
		int callbackResult = RESULT_CANCELED;
		try {
			if (thisIntent.hasExtra(CryptoIntents.EXTRA_TEXT)) {
				// get the body text out of the extras.
				String inputBody = thisIntent.getStringExtra (CryptoIntents.EXTRA_TEXT);
				String outputBody = "";
				outputBody = ch.encryptWithSessionKey (inputBody);
				// stash the encrypted text in the extra
				callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT, outputBody);
			}
			
			if (thisIntent.hasExtra(CryptoIntents.EXTRA_TEXT_ARRAY)) {
				String[] in = thisIntent.getStringArrayExtra(CryptoIntents.EXTRA_TEXT_ARRAY);
				String[] out = new String[in.length];
				for (int i = 0; i < in.length; i++) {
					if (in[i] != null) {
						out[i] = ch.encryptWithSessionKey(in[i]);
					}
				}
				callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY, out);
			}
			
			// Support for binary fields could be added here (like images?)
			
			callbackResult = RESULT_OK;
		} catch (CryptoHelperException e) {
			Log.e(TAG, e.toString());
		}
		return callbackResult;
	}

	/**
	 * Decrypt all supported fields in the intent and return the result in callbackIntent.
	 * 
	 * @param thisIntent
	 * @param callbackIntent
	 * @return callbackResult
	 */
	private int decryptIntent(final Intent thisIntent, Intent callbackIntent) {
    	int callbackResult = RESULT_CANCELED;
		try {

			if (thisIntent.hasExtra(CryptoIntents.EXTRA_TEXT)) {
				// get the body text out of the extras.
				String inputBody = thisIntent.getStringExtra (CryptoIntents.EXTRA_TEXT);
				String outputBody = "";
				outputBody = ch.decryptWithSessionKey (inputBody);
				// stash the encrypted text in the extra
				callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT, outputBody);
			}
			
			if (thisIntent.hasExtra(CryptoIntents.EXTRA_TEXT_ARRAY)) {
				String[] in = thisIntent.getStringArrayExtra(CryptoIntents.EXTRA_TEXT_ARRAY);
				String[] out = new String[in.length];
				for (int i = 0; i < in.length; i++) {
					if (in[i] != null) {
						out[i] = ch.decryptWithSessionKey(in[i]);
					}
				}
				callbackIntent.putExtra(CryptoIntents.EXTRA_TEXT_ARRAY, out);
			}
			
			// Support for binary fields could be added here (like images?)
			
			callbackResult = RESULT_OK;
		} catch (CryptoHelperException e) {
			Log.e(TAG, e.toString());
		}
		return callbackResult;
	}
	
	private Intent getSetPassword (Intent thisIntent, Intent callbackIntent) throws CryptoHelperException, Exception {
		String action = thisIntent.getAction();
        //TODO: Consider moving this elsewhere. Maybe DBHelper? Also move strings to resource.
        //DBHelper dbHelper = new DBHelper(this);
        if (debug) Log.d(TAG, "GET_or_SET_PASSWORD");
        String username = null;
        String password = null;

        String clearUniqueName = thisIntent.getStringExtra (CryptoIntents.EXTRA_UNIQUE_NAME);

        if (clearUniqueName == null) throw new Exception ("EXTRA_UNIQUE_NAME not set.");
        
		if (dbHelper == null) {
	        // Need to open DBHelper here, because
			// onResume() is called after onActivityResult()
			dbHelper = new DBHelper(this);
		}

        String uniqueName = ch.encrypt(clearUniqueName);
    	PassEntry row = dbHelper.fetchPassword(uniqueName);
    	boolean passExists = row.id > 1;

        String clearCallingPackage = getCallingPackage();
        String callingPackage = ch.encrypt (clearCallingPackage);
    	if (passExists) { // check for permission to access this password.
    		ArrayList<String> packageAccess = dbHelper.fetchPackageAccess(row.id);
    		if (! PassEntry.checkPackageAccess(packageAccess, callingPackage)) {
    			throw new Exception ("It is currently not permissible for this application to request this password.");
    		}
            /*TODO: check if this package is in the package_access table corresponding to this password:
             * "Application 'org.syntaxpolice.ServiceTest' wants to access the
    				password for 'opensocial'.
    				[ ] Grant access this time.
    				[ ] Always grant access.
    				[ ] Always grant access to all passwords in org.syntaxpolice.ServiceTest category?
    				[ ] Don't grant access"
             */
    	}
    	
        if (action.equals (CryptoIntents.ACTION_GET_PASSWORD)) {
        	if (passExists) {
        		username = ch.decrypt(row.username);
        		password = ch.decrypt(row.password);
        	} else throw new Exception ("Could not find password with the unique name: " + clearUniqueName);

        	// stashing the return values:
        	callbackIntent.putExtra(CryptoIntents.EXTRA_USERNAME, username);
        	callbackIntent.putExtra(CryptoIntents.EXTRA_PASSWORD, password);
        } else if (action.equals (CryptoIntents.ACTION_SET_PASSWORD)) {
            String clearUsername  = thisIntent.getStringExtra (CryptoIntents.EXTRA_USERNAME);
            String clearPassword = thisIntent.getStringExtra (CryptoIntents.EXTRA_PASSWORD);
            if (clearPassword == null) {
            		throw new Exception ("PASSWORD extra must be set.");
            }  
            row.username = ch.encrypt(clearUsername == null ? "" : clearUsername);
            row.password = ch.encrypt(clearPassword);
            // since this package is setting the password, it automatically gets access to it:
        	if (passExists) { //exists already 
        		if (clearUsername.equals("") && clearPassword.equals("")) {
        			dbHelper.deletePassword(row.id);
        		} else {
        			dbHelper.updatePassword(row.id, row);
        		}
        	} else {// add a new one
                row.uniqueName = uniqueName;
	            row.description=uniqueName; //for display purposes
                // TODO: Should we send these fields in extras also?  If so, probably not using 
                // the openintents namespace?  If another application were to implement a keystore
                // they might not want to use these.
	            row.website = ""; 
	            row.note = "";

	            String category = ch.encrypt("Application Data");
	            CategoryEntry c = new CategoryEntry();
	            c.name = category;
	            row.category = dbHelper.addCategory(c); //doesn't add category if it already exists
	            row.id = 0;	// entry is truly new
	            row.id = dbHelper.addPassword(row);
        	}  
    		ArrayList<String> packageAccess = dbHelper.fetchPackageAccess(row.id);
    		if (! PassEntry.checkPackageAccess(packageAccess, callingPackage)) {
            	dbHelper.addPackageAccess(row.id, callingPackage);
    		}
    
        }
        return (callbackIntent);
	}
	
	@Override
	protected void onPause() {
		super.onPause();

		if (debug)
			Log.d(TAG, "onPause()");

		releaseService();
		dbHelper.close();
		dbHelper = null;
	}

	@Override
	protected void onResume() {
		super.onPause();

		if (debug)
			Log.d(TAG, "onResume()");
		if (dbHelper == null) {
			dbHelper = new DBHelper(this);
		}

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		releaseService();
	}


	//--------------------------- service stuff ------------
	private void initService() {

        boolean isLocal = isIntentLocal();
		conn = new ServiceDispatchConnection(isLocal);
		Intent i = new Intent();
		i.setClass(this, ServiceDispatchImpl.class);
		startService(i);
		bindService( i, conn, Context.BIND_AUTO_CREATE);
	}

	/**
	 * @return
	 */
	private boolean isIntentLocal() {
		String action = getIntent().getAction();
        boolean isLocal = action == null || action.equals(Intent.ACTION_MAIN);
		return isLocal;
	}

	private void releaseService() {
		if (conn != null ) {
			unbindService( conn );
			conn = null;
		}
	}

	class ServiceDispatchConnection implements ServiceConnection
	{
		boolean askPassIsLocal = false;
		public ServiceDispatchConnection (Boolean isLocal) {
			askPassIsLocal = isLocal;
		}
		public void onServiceConnected(ComponentName className, 
				IBinder boundService )
		{
			service = ServiceDispatch.Stub.asInterface((IBinder)boundService);
			
			if (mServiceIntent != null) {
				setServiceParametersFromExtrasAndDispatchAction(mServiceIntent);
				mServiceIntent = null;
				return;
			}
			
			boolean promptforpassword = getIntent().getBooleanExtra(CryptoIntents.EXTRA_PROMPT, true);
			if (debug) Log.d(TAG, "Prompt for password: " + promptforpassword);
			try {
				if (service.getPassword() == null) {
					if (promptforpassword) {
						if (debug) Log.d(TAG, "ask for password");
						// the service isn't running
						Intent askPass = new Intent(getApplicationContext(),
								AskPassword.class);
	
						final Intent thisIntent = getIntent();
						String inputBody = thisIntent.getStringExtra (CryptoIntents.EXTRA_TEXT);
	
						askPass.putExtra (CryptoIntents.EXTRA_TEXT, inputBody);
						askPass.putExtra (AskPassword.EXTRA_IS_LOCAL, askPassIsLocal);
						//TODO: Is there a way to make sure all the extras are set?	
						startActivityForResult (askPass, REQUEST_CODE_ASK_PASSWORD);
					} else {
						if (debug) Log.d(TAG, "ask for password");
						// Don't prompt but cancel
						setResult(RESULT_CANCELED);
				        finish();
					}

				} else {
					if (debug) Log.d(TAG, "service already started");
					//service already started, so don't need to ask pw.

			        boolean externalAccess = mPreferences.getBoolean(Preferences.PREFERENCE_ALLOW_EXTERNAL_ACCESS, false);
			        
			        if (askPassIsLocal || externalAccess) {
			        	salt = service.getSalt();
						masterKey = service.getPassword();
						actionDispatch();
			        } else {
			        	if (debug) Log.d(TAG, "onServiceConnected: showDialogAllowExternalAccess()");
			        	showDialogAllowExternalAccess();
			        }
				}
			} catch (RemoteException e) {
				Log.d(TAG, e.toString());
			}
			if (debug) Log.d( TAG,"onServiceConnected" );
		}
		
		public void onServiceDisconnected(ComponentName className)
		{
			service = null;
			if (debug) Log.d( TAG,"onServiceDisconnected" );
		}
	};

}
