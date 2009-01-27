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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openintents.distribution.AboutDialog;
import org.openintents.intents.AboutMiniIntents;
import org.openintents.intents.CryptoIntents;
import org.openintents.safe.dialog.DialogHostingActivity;
import org.openintents.safe.service.ServiceDispatchImpl;
import org.openintents.util.IntentUtils;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

/**
 * CategoryList Activity
 * 
 * @author Randy McEoin
 * @author Steven Osborn - http://steven.bitsetters.com
 */
public class CategoryList extends ListActivity {

	private static boolean debug = false;
    private static final String TAG = "CategoryList";

    // Menu Item order
    public static final int LOCK_CATEGORY_INDEX = Menu.FIRST;
    public static final int OPEN_CATEGORY_INDEX = Menu.FIRST + 1;
    public static final int EDIT_CATEGORY_INDEX = Menu.FIRST + 2;
    public static final int ADD_CATEGORY_INDEX = Menu.FIRST + 3;
    public static final int DEL_CATEGORY_INDEX = Menu.FIRST + 4;
    public static final int HELP_INDEX = Menu.FIRST + 5;
    public static final int EXPORT_INDEX = Menu.FIRST + 6;
    public static final int IMPORT_INDEX = Menu.FIRST + 7;
    public static final int CHANGE_PASS_INDEX = Menu.FIRST + 8;
    public static final int BACKUP_INDEX = Menu.FIRST + 9;
    public static final int RESTORE_INDEX = Menu.FIRST + 10;
    public static final int PREFERENCES_INDEX = Menu.FIRST + 11;
    public static final int ABOUT_INDEX = Menu.FIRST + 12;
    
    public static final int REQUEST_ONCREATE = 0;
    public static final int REQUEST_EDIT_CATEGORY = 1;
    public static final int REQUEST_ADD_CATEGORY = 2;
    public static final int REQUEST_OPEN_CATEGORY = 3;
    public static final int REQUEST_RESTORE = 4;
    
    protected static final int MSG_IMPORT = 0x101; 
    protected static final int MSG_FILLDATA = MSG_IMPORT + 1; 
    protected static final int MSG_BACKUP = MSG_FILLDATA + 1; 
    
    private static final int IMPORT_PROGRESS_KEY = 0;
    private static final int BACKUP_PROGRESS_KEY = IMPORT_PROGRESS_KEY + 1;
    private static final int ABOUT_KEY = IMPORT_PROGRESS_KEY + 2;

    public static final int MAX_CATEGORIES = 256;

    private static final String EXPORT_FILENAME = "/sdcard/oisafe.csv";
    public static final String BACKUP_FILENAME = "/sdcard/oisafe.xml";
    
    public static final String KEY_ID = "id";  // Intent keys

    private CryptoHelper ch=null;
    private DBHelper dbHelper=null;
	
	private String importMessage="";
	private int importedEntries=0;
	private Thread importThread=null;
	private boolean importDeletedDatabase=false;
	private String importedFilename="";

	private Thread backupThread=null;

	private static String salt;
    private static String masterKey;			

    private List<CategoryEntry> rows;
    private Intent restartTimerIntent;
    
    BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            	 if (debug) Log.d(TAG,"caught ACTION_SCREEN_OFF");
            	 masterKey=null;
            } else if (intent.getAction().equals(CryptoIntents.ACTION_CRYPTO_LOGGED_OUT)) {
            	 if (debug) Log.d(TAG,"caught ACTION_CRYPTO_LOGGED_OUT");
            	 lockAndShutFrontDoor();
            }
        }
    };

    public Handler myViewUpdateHandler = new Handler(){
    	// @Override
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    			case CategoryList.MSG_BACKUP:
    				Bundle b=msg.getData();
    				String result=b.getString("msg");
         			Toast.makeText(CategoryList.this, result,
             				Toast.LENGTH_LONG).show();
    				break;
             	case CategoryList.MSG_IMPORT:
             		if (importMessage != "") {
             			Toast.makeText(CategoryList.this, importMessage,
             				Toast.LENGTH_LONG).show();
             		}
             		if (importedFilename != "") {
	             		String deleteMsg=getString(R.string.import_delete_csv) +
	             			" " + importedFilename + "?";
		         		Dialog about = new AlertDialog.Builder(CategoryList.this)
			    			.setIcon(R.drawable.passicon)
			    			.setTitle(R.string.import_complete)
			    			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			    				public void onClick(DialogInterface dialog, int whichButton) {
			    					File csvFile=new File(importedFilename);
			    					csvFile.delete();
			    					importedFilename="";
			    				}
			    			})
			    			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			    				public void onClick(DialogInterface dialog, int whichButton) {
			    				}
			    			}) 
			    			.setMessage(deleteMsg)
			    			.create();
		         		about.show();
             		}
	         		
             		if ((importedEntries!=0) || (importDeletedDatabase))
             		{
             			fillData();
             		}
             		break;
             	case CategoryList.MSG_FILLDATA:
             		fillData();
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

		restartTimerIntent = new Intent (CryptoIntents.ACTION_RESTART_TIMER);
		
		if (!isSignedIn()) {
			Intent frontdoor = new Intent(this, FrontDoor.class);
			startActivity(frontdoor);		
			finish();
    	}
		
		setContentView(R.layout.cat_list);
		String title = getResources().getString(R.string.app_name) + " - " +
			getResources().getString(R.string.categories);
		setTitle(title);

		if (dbHelper==null) {
			dbHelper = new DBHelper(this);
			if (dbHelper.getPrePopulate()==true)
			{
				prePopulate();
				dbHelper.clearPrePopulate();
			}
		}
		
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction (CryptoIntents.ACTION_CRYPTO_LOGGED_OUT);
        registerReceiver(mIntentReceiver, filter);

		fillData();

		final ListView list = getListView();
		list.setFocusable(true);
		list.setOnCreateContextMenuListener(this);
		registerForContextMenu(list);
    }

    @Override
    protected void onResume() {
		super.onResume();

		if (debug) Log.d(TAG,"onResume()");
		if (dbHelper == null) {
		    dbHelper = new DBHelper(this);
		}

		if (!isSignedIn()) {
			Intent frontdoor = new Intent(this, FrontDoor.class);
			startActivity(frontdoor);		
			finish();
    	}

        showFirstTimeWarningDialog();
    }

	/**
	 * 
	 */
	private void showFirstTimeWarningDialog() {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean firstTimeWarning = sp.getBoolean(Preferences.PREFERENCE_FIRST_TIME_WARNING, false);
        
		if (!firstTimeWarning) {
			Intent i = new Intent(this, DialogHostingActivity.class);
			i.putExtra(DialogHostingActivity.EXTRA_DIALOG_ID, DialogHostingActivity.DIALOG_ID_FIRST_TIME_WARNING);
			startActivity(i);
			
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean(Preferences.PREFERENCE_FIRST_TIME_WARNING, true);
            editor.commit();
		}
	}
    
    @Override
    protected void onPause() {
		super.onPause();

		if (debug) Log.d(TAG,"onPause()");
		
		if ((importThread != null) && (importThread.isAlive())) {
			if (debug) Log.d(TAG,"wait for thread");
			int maxWaitToDie=500000;
			try { importThread.join(maxWaitToDie); } 
			catch(InterruptedException e){} //  ignore 
		}
		if ((backupThread != null) && (backupThread.isAlive())) {
			if (debug) Log.d(TAG,"wait for backup thread");
			int maxWaitToDie=500000;
			try { backupThread.join(maxWaitToDie); } 
			catch(InterruptedException e){} //  ignore 
		}
		dbHelper.close();
		dbHelper = null;
    }

    @Override
    public void onStop() {
		super.onStop();

		if (debug) Log.d(TAG,"onStop()");
//		dbHelper.close();
    }
    
    @Override
    public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mIntentReceiver);
		if (debug) Log.d(TAG,"onDestroy()");
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view,
    		ContextMenuInfo menuInfo) {

		AdapterView.AdapterContextMenuInfo info;
		info = (AdapterView.AdapterContextMenuInfo) menuInfo;

		menu.setHeaderTitle(rows.get(info.position).plainName);
		menu.add(0,OPEN_CATEGORY_INDEX, 0, R.string.open)
			.setIcon(android.R.drawable.ic_menu_view)
			.setAlphabeticShortcut('o');
		menu.add(0,EDIT_CATEGORY_INDEX, 0, R.string.password_edit)
			.setIcon(android.R.drawable.ic_menu_edit)
			.setAlphabeticShortcut('e');
		menu.add(0, DEL_CATEGORY_INDEX, 0, R.string.password_delete)  
			.setIcon(android.R.drawable.ic_menu_delete)
			.setAlphabeticShortcut('d');
    }
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		onOptionsItemSelected(item);
		return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case IMPORT_PROGRESS_KEY: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage(getString(R.string.import_progress));
                dialog.setIndeterminate(false);
                dialog.setCancelable(false);
                return dialog;
            }
            case BACKUP_PROGRESS_KEY: {
                ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage(getString(R.string.backup_progress)+
                		" "+BACKUP_FILENAME);
                dialog.setIndeterminate(false);
                dialog.setCancelable(false);
                return dialog;
            }
            case ABOUT_KEY:
            	return new AboutDialog(this);
        }
        return null;
    }

    /**
     * Returns the current status of signedIn. 
     * 
     * @return	True if signed in
     */
    public static boolean isSignedIn() {
    	if (masterKey != null) {
    		return true;
    	}
    	return false;
    }
    
    
    /**
     * Sets signedIn status to false.
     * 
     * @see org.openintents.safe.CategoryList#isSignedIn
     */
    public static void setSignedOut() {
    	if (debug) Log.d(TAG,"setSignedOut()");
    	masterKey=null;
    }
    /**
     * Populates the category ListView
     */
    private void fillData() {
    	if (debug) Log.d(TAG,"fillData()");
		// initialize crypto so that we can display readable descriptions in
		// the list view
		ch = new CryptoHelper();
		if(masterKey == null) {
		    masterKey = "";
		}
		try {
			ch.init(CryptoHelper.EncryptionMedium,salt);
			ch.setPassword(masterKey);
		} catch (CryptoHelperException e1) {
			e1.printStackTrace();
			Toast.makeText(this,getString(R.string.crypto_error)
				+ e1.getMessage(), Toast.LENGTH_SHORT).show();
			return;
		}
	
		List<String> items = new ArrayList<String>();
		if (dbHelper==null) {
			return;
		}
		rows = dbHelper.fetchAllCategoryRows();

		for (CategoryEntry row : rows) {
		    String cryptDesc = row.name;
		    row.plainName = "";
		    try {
				row.plainName = ch.decrypt(cryptDesc);
		    } catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
		    }
		}
		Collections.sort(rows, new Comparator<CategoryEntry>() {
		    public int compare(CategoryEntry o1, CategoryEntry o2) {
		        return o1.plainName.compareToIgnoreCase(o2.plainName);
		    }});
		for (CategoryEntry row : rows) {
			items.add(row.plainName);
		}

		ArrayAdapter<String> entries = 
		    new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
		setListAdapter(entries);
		
    }

	@Override
	public boolean onMenuOpened(int featureId, Menu menu) {
		MenuItem miDelete = menu.findItem(DEL_CATEGORY_INDEX);
		MenuItem miEdit = menu.findItem(EDIT_CATEGORY_INDEX);
		if (getSelectedItemPosition() > -1) {
			miDelete.setEnabled(true);
			miEdit.setEnabled(true);
		} else {
			miDelete.setEnabled(false);
			miEdit.setEnabled(false);
		}
		return super.onMenuOpened(featureId, menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
	
		menu.add(0,LOCK_CATEGORY_INDEX, 0, R.string.password_lock)
			.setIcon(android.R.drawable.ic_lock_lock)
			.setShortcut('0', 'l');
		menu.add(0,EDIT_CATEGORY_INDEX, 0, R.string.password_edit)
			.setIcon(android.R.drawable.ic_menu_edit)
			.setShortcut('1', 'e');
		menu.add(0,ADD_CATEGORY_INDEX, 0, R.string.password_add)
			.setIcon(android.R.drawable.ic_menu_add)
			.setShortcut('2', 'a');

		menu.add(0, DEL_CATEGORY_INDEX, 0, R.string.password_delete)  
			.setIcon(android.R.drawable.ic_menu_delete)
			.setShortcut('3', 'd')
			.setEnabled(false);
		
		menu.add(0, HELP_INDEX, 0, R.string.help)
			.setIcon(android.R.drawable.ic_menu_help);

		menu.add(0, EXPORT_INDEX, 0, R.string.export_database)
			.setIcon(android.R.drawable.ic_menu_upload);
		menu.add(0, IMPORT_INDEX, 0, R.string.import_database)
			.setIcon(android.R.drawable.ic_input_get);

		menu.add(0, CHANGE_PASS_INDEX, 0, R.string.change_password)
			.setIcon(android.R.drawable.ic_menu_manage);

		menu.add(0, BACKUP_INDEX, 0, R.string.backup);
		menu.add(0, RESTORE_INDEX, 0, R.string.restore);

		menu.add(0, PREFERENCES_INDEX, 0, R.string.preferences);
		
		if (IntentUtils.isIntentAvailable(this, new Intent(AboutMiniIntents.ACTION_SHOW_ABOUT_DIALOG))) {
			// Only show "About" dialog, if OI About (or compatible) is installed.
			menu.add(0, ABOUT_INDEX, 0, R.string.about).setIcon(
	 				android.R.drawable.ic_menu_info_details);
		}
		
		return super.onCreateOptionsMenu(menu);
    }

    static void setSalt(String saltIn) {
		salt = saltIn;
    }

    static String getSalt() {
		return salt;
    }

    static void setMasterKey(String key) {
		masterKey = key;
    }

    static String getMasterKey() {
		return masterKey;
    }

    private void addCategory() {
		Intent i = new Intent(this, CategoryEdit.class);
		startActivityForResult(i,REQUEST_ADD_CATEGORY);
    }

    private void delCategory(long Id) {
    	if (dbHelper.countPasswords(Id)>0) {
            Toast.makeText(CategoryList.this, R.string.category_not_empty,
                    Toast.LENGTH_SHORT).show();
    		return;
    	}
		dbHelper.deleteCategory(Id);
		fillData();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
		sendBroadcast (restartTimerIntent);

		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		int position=-1;
		if (info==null) {
			position=getSelectedItemPosition();
		} else {
			// used when this is called from a ContextMenu
			position=info.position;
		}
		switch(item.getItemId()) {
		case LOCK_CATEGORY_INDEX:
			lockAndShutFrontDoor();
			break;
		case OPEN_CATEGORY_INDEX:
			launchPassList(rows.get(info.position).id);
			break;
		case EDIT_CATEGORY_INDEX:
			Intent i = new Intent(this, CategoryEdit.class);
			if (position > -1) {
				i.putExtra(KEY_ID, rows.get(position).id);
				startActivityForResult(i,REQUEST_EDIT_CATEGORY);
			}
		    break;
		case ADD_CATEGORY_INDEX:
		    addCategory();
		    break;
		case DEL_CATEGORY_INDEX:
		    try {
				if (position > -1) {
					delCategory(rows.get(position).id);
				}
		    } catch (IndexOutOfBoundsException e) {
				// This should only happen when there are no
				// entries to delete.
				Log.w(TAG,e.toString());
		    }
		    break;
		case HELP_INDEX:
		    Intent help = new Intent(this, Help.class);
		    startActivity(help);
			break;
		case EXPORT_INDEX:
			exportDatabase();
			break;
		case IMPORT_INDEX:
			importDatabase();
			break;
		case CHANGE_PASS_INDEX:
			Intent changePass = new Intent(this, ChangePass.class);
			startActivity(changePass);
			break;
		case BACKUP_INDEX:
			backupThreadStart();
			break;
		case RESTORE_INDEX:
			restoreDatabase();
			break;
		case PREFERENCES_INDEX:
			Intent preferences = new Intent(this, Preferences.class);
			startActivity(preferences);
			break;
		case ABOUT_INDEX:
			AboutDialog.showDialogOrStartActivity(this, ABOUT_KEY);
			break;
		default:
			Log.e(TAG,"Unknown itemId");
			break;
		}
		return super.onOptionsItemSelected(item);
    }

    private void launchPassList(long id) {
		Intent passList = new Intent(this, PassList.class);
		passList.putExtra(KEY_ID, id);
		startActivityForResult(passList,REQUEST_OPEN_CATEGORY);
    }
    
    private String backupDatabase() {
    	Backup backup=new Backup(this);
    	
    	backup.write(BACKUP_FILENAME);
    	return backup.getResult();
    }

    
    private void lockAndShutFrontDoor () {
    	Intent serviceIntent = new Intent();
		serviceIntent.setClass(this, ServiceDispatchImpl.class );
	    stopService(serviceIntent);
		masterKey=null;
	    Intent frontdoor = new Intent(this, FrontDoor.class);
	    frontdoor.setAction(Intent.ACTION_MAIN);
	    startActivity(frontdoor);
	    finish();
    }
    
	/**
	 * Start a separate thread to backup the database.   By running
	 * the backup in a thread it allows the main UI thread to return
	 * and permit the updating of the progress dialog.
	 */
	private void backupThreadStart(){
		showDialog(BACKUP_PROGRESS_KEY);
		backupThread = new Thread(new Runnable() {
			public void run() {
				String result=backupDatabase();
				dismissDialog(BACKUP_PROGRESS_KEY);

				Message m = new Message();
				m.what = CategoryList.MSG_BACKUP;
				Bundle b = new Bundle();
				b.putString("msg", result);
				m.setData(b);
				CategoryList.this.myViewUpdateHandler.sendMessage(m); 
				
				if (debug) Log.d(TAG,"thread end");
				}
			});
		backupThread.start();
	}

    private void restoreDatabase() {
//    	Restore restore=new Restore(myViewUpdateHandler, this);
    	
//    	restore.read(BACKUP_FILENAME, masterKey);
		Intent i = new Intent(this, Restore.class);
	    startActivityForResult(i,REQUEST_RESTORE);
    }
    
    protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		sendBroadcast (restartTimerIntent);
		launchPassList(rows.get(position).id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent i) {
    	super.onActivityResult(requestCode, resultCode, i);

    	if (dbHelper == null) {
		    dbHelper = new DBHelper(this);
		}

    	if (resultCode == RESULT_OK) {
    		fillData();
    	}
    }

    private void prePopulate() {
		addCategory(getString(R.string.category_business));
		addCategory(getString(R.string.category_personal));
    }
    
    private long addCategory(String name) {
    	if (debug) Log.d(TAG,"addCategory("+name+")");
    	if ((name==null) || (name=="")) return -1;
		CategoryEntry entry =  new CategoryEntry();

		sendBroadcast (restartTimerIntent);
		String namePlain = name;

		try {
			ch = new CryptoHelper();
			if(masterKey == null) {
			    masterKey = "";
			}
			ch.init(CryptoHelper.EncryptionMedium,salt);
			ch.setPassword(masterKey);

		    entry.name = ch.encrypt(namePlain);
		} catch(CryptoHelperException e) {
		    Log.e(TAG,e.toString());
			Toast.makeText(this,getString(R.string.crypto_error)
				+ e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	    return dbHelper.addCategory(entry);
    }
    
	public boolean exportDatabase(){
		String filename=EXPORT_FILENAME;
		try {
			CSVWriter writer = new CSVWriter(new FileWriter(filename), ',');

			String[] header = { getString(R.string.category),
					getString(R.string.description), 
					getString(R.string.website),
					getString(R.string.username),
					getString(R.string.password),
					getString(R.string.notes)
			};
			writer.writeNext(header);
			
			ch = new CryptoHelper();
			if(masterKey == null) {
			    masterKey = "";
			}
			try {
				ch.init(CryptoHelper.EncryptionMedium,salt);
				ch.setPassword(masterKey);
			} catch (CryptoHelperException e1) {
				e1.printStackTrace();
				Toast.makeText(this,getString(R.string.crypto_error)
					+ e1.getMessage(), Toast.LENGTH_SHORT).show();
				return false;
			}
		
			HashMap<Long, String> categories = new HashMap<Long, String>();
			
			List<CategoryEntry> crows;
			crows = dbHelper.fetchAllCategoryRows();
		
			for (CategoryEntry row : crows) {
			    String cryptDesc = row.name;
			    row.plainName = "";
			    try {
					row.plainName = ch.decrypt(cryptDesc);
					categories.put(row.id, row.plainName);
			    } catch (CryptoHelperException e) {
					Log.e(TAG,e.toString());
		            Toast.makeText(CategoryList.this, R.string.cannot_decrypt_category,
		                    Toast.LENGTH_SHORT).show();
		            return false;
			    }
			}
		
			List<PassEntry> rows;
			rows = dbHelper.fetchAllRows(new Long(0));
		
			for (PassEntry row : rows) {
			    String cryptDesc = row.description;
			    String cryptWebsite = row.website;
			    String cryptUsername = row.username;
			    String cryptPassword = row.password;
			    String cryptNote = row.note;
			    row.plainDescription = "";
			    row.plainWebsite = "";
			    row.plainUsername = "";
			    row.plainPassword = "";
			    row.plainNote = "";
			    try {
					row.plainDescription = ch.decrypt(cryptDesc);
					row.plainWebsite     = ch.decrypt(cryptWebsite);
					row.plainUsername    = ch.decrypt(cryptUsername);
					row.plainPassword    = ch.decrypt(cryptPassword);
					row.plainNote        = ch.decrypt(cryptNote);
			    } catch (CryptoHelperException e) {
					Log.e(TAG,e.toString());
		            Toast.makeText(CategoryList.this, R.string.cannot_decrypt_password,
		                    Toast.LENGTH_SHORT).show();
		            return false;
			    }
			    String[] rowEntries = { categories.get(row.category),
			    		row.plainDescription,
			    		row.plainWebsite,
			    		row.plainUsername,
			    		row.plainPassword,
			    		row.plainNote
			    };
			    writer.writeNext(rowEntries);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
	        Toast.makeText(CategoryList.this, R.string.export_file_error,
	                Toast.LENGTH_SHORT).show();
			return false;
		}
		String msg=getString(R.string.export_success)+filename+
			getString(R.string.export_success2);
        Toast.makeText(CategoryList.this, msg,
                Toast.LENGTH_LONG).show();
		return true;
	}

	private void deleteDatabaseNow(){
		dbHelper.deleteDatabase();
	}

	public void deleteDatabase4Import(final String filename){
//		Log.i(TAG,"deleteDatabase4Import");
		Dialog about = new AlertDialog.Builder(this)
			.setIcon(R.drawable.passicon)
			.setTitle(R.string.dialog_delete_database_title)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					deleteDatabaseNow();
					importDeletedDatabase=true;
					importDatabaseThreadStart(filename);
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
			}) 
			.setMessage(R.string.dialog_delete_database_msg)
			.create();
		about.show();
	}
		
	public void importDatabase(){
		final String filename=EXPORT_FILENAME;
		File csvFile=new File(filename);
		if (!csvFile.exists()) {
			String msg=getString(R.string.import_file_missing) + " " +
				filename;
	        Toast.makeText(CategoryList.this, msg,
	                Toast.LENGTH_LONG).show();
			return;
		}
		Dialog about = new AlertDialog.Builder(this)
			.setIcon(R.drawable.passicon)
			.setTitle(R.string.dialog_import_title)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					deleteDatabase4Import(filename);
				}
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					importDeletedDatabase=false;
					importDatabaseThreadStart(filename);
				}
			}) 
			.setMessage(R.string.dialog_import_msg)
			.create();
		about.show();
	}

	/**
	 * Start a separate thread to import the database.   By running
	 * the import in a thread it allows the main UI thread to return
	 * and permit the updating of the progress dialog.
	 */
	private void importDatabaseThreadStart(final String filename){
		showDialog(IMPORT_PROGRESS_KEY);
		importThread = new Thread(new Runnable() {
			public void run() {
				importDatabaseFromCSV(filename);
				dismissDialog(IMPORT_PROGRESS_KEY);
				
				Message m = new Message();
				m.what = CategoryList.MSG_IMPORT;
				CategoryList.this.myViewUpdateHandler.sendMessage(m); 

				if (debug) Log.d(TAG,"thread end");
				}
			});
		importThread.start();
	}
	
	/**
	 * While running inside a thread, read from a CSV and import
	 * into the database.
	 */
	private void importDatabaseFromCSV(String filename){
		try {
			importMessage="";
			importedEntries=0;
			
			final int recordLength=6;
			CSVReader reader= new CSVReader(new FileReader(filename));
		    String [] nextLine;
		    nextLine = reader.readNext();
		    if (nextLine==null) {
		    	importMessage=getString(R.string.import_error_first_line);
		        return;
		    }
		    if (nextLine.length != recordLength){
		    	importMessage=getString(R.string.import_error_first_line);
		        return;
		    }
		    if ((nextLine[0].compareToIgnoreCase(getString(R.string.category)) != 0) ||
			    (nextLine[1].compareToIgnoreCase(getString(R.string.description)) != 0) ||
			    (nextLine[2].compareToIgnoreCase(getString(R.string.website)) != 0) ||
			    (nextLine[3].compareToIgnoreCase(getString(R.string.username)) != 0) ||
			    (nextLine[4].compareToIgnoreCase(getString(R.string.password)) != 0) ||
			    (nextLine[5].compareToIgnoreCase(getString(R.string.notes)) != 0))
		    {
		    	importMessage=getString(R.string.import_error_first_line);
		        return;
		    }
//		    Log.i(TAG,"first line is valid");
		    
		    HashMap<String, Long> categoryToId=getCategoryToId(dbHelper);
		    //
		    // take a pass through the CSV and collect any new Categories
		    //
			HashMap<String,Long> categoriesFound = new HashMap<String,Long>();
		    int categoryCount=0;
		    int line=0;
		    while ((nextLine = reader.readNext()) != null) {
		    	line++;
		    	if (importThread.isInterrupted()) {
		    		return;
		    	}
		        // nextLine[] is an array of values from the line
		        if ((nextLine==null) || (nextLine[0]=="")){
		        	continue;	// skip blank categories
		        }
		        if (categoryToId.containsKey(nextLine[0])){
		        	continue;	// don't recreate existing categories
		        }
//		        if (debug) Log.d(TAG,"line["+line+"] found category ("+nextLine[0]+")");
	        	Long passwordsInCategory= new Long(1);
		        if (categoriesFound.containsKey(nextLine[0])) {
		        	// we've seen this category before, bump its count
		        	passwordsInCategory+=categoriesFound.get(nextLine[0]);
		        } else {
		        	// newly discovered category
			        categoryCount++;
		        }
	        	categoriesFound.put(nextLine[0], passwordsInCategory);
		        if (categoryCount>MAX_CATEGORIES){
		        	importMessage=getString(R.string.import_too_many_categories);
			        return;
		        }
		    }
		    if (debug) Log.d(TAG,"found "+categoryCount+" categories");
		    if (categoryCount!=0)
		    {
			    Set<String> categorySet = categoriesFound.keySet();
			    Iterator<String> i=categorySet.iterator();
			    while (i.hasNext()){
		    		addCategory(i.next());
			    }
		    }
		    reader.close();

		    categoryToId=getCategoryToId(dbHelper);	// re-read the categories to get id's of new categories
		    //
		    // read the whole file again to import the actual fields
		    //
			reader = new CSVReader(new FileReader(filename));
		    nextLine = reader.readNext();
		    int newEntries=0;
		    int lineNumber=0;
		    String lineErrors="";
		    int lineErrorsCount=0;
		    final int maxLineErrors=10;
		    while ((nextLine = reader.readNext()) != null) {
		    	lineNumber++;
//		    	Log.d(TAG,"lineNumber="+lineNumber);
		    	
		    	if (importThread.isInterrupted()) {
		    		return;
		    	}
		    	
		        // nextLine[] is an array of values from the line
			    if (nextLine.length < 2){
			    	if (lineErrorsCount < maxLineErrors) {
				    	lineErrors += "line "+lineNumber+": "+
				    		getString(R.string.import_not_enough_fields)+"\n";
			    		lineErrorsCount++;
			    	}
			    	continue;	// skip if not enough fields
			    }
			    if (nextLine.length < recordLength){
			    	// if the fields after category and description are missing, 
			    	// just fill them in
			    	String [] replacement=new String[recordLength];
			    	for (int i=0;i<nextLine.length; i++) {
			    		// copy over the fields we did get
			    		replacement[i]=nextLine[i];
			    	}
			    	for (int i=nextLine.length; i<recordLength; i++) {
			    		// flesh out the rest of the fields
			    		replacement[i]="";
			    	}
			    	nextLine=replacement;
			    }
		        if ((nextLine==null) || (nextLine[0]=="")){
			    	if (lineErrorsCount < maxLineErrors) {
				    	lineErrors += "line "+lineNumber+": "+
				    		getString(R.string.import_blank_category)+"\n";
			    		lineErrorsCount++;
			    	}
		        	continue;	// skip blank categories
		        }
		        String description=nextLine[1];
		        if ((description==null) || (description=="")){
			    	if (lineErrorsCount < maxLineErrors) {
				    	lineErrors += "line "+lineNumber+": "+
				    		getString(R.string.import_blank_description)+"\n";
			    		lineErrorsCount++;
			    	}
		        	continue;
		        }
		        
		        PassEntry entry=new PassEntry();
				try {
					entry.category = categoryToId.get(nextLine[0]);
				    entry.description = ch.encrypt(description);
				    entry.website = ch.encrypt(nextLine[2]);
				    entry.username = ch.encrypt(nextLine[3]);
				    entry.password = ch.encrypt(nextLine[4]);
				    entry.note = ch.encrypt(nextLine[5]);
				} catch(CryptoHelperException e) {
				    Log.e(TAG,e.toString());
				    continue;
				}
				entry.id=0;
			    dbHelper.addPassword(entry);
		        newEntries++;
		    }
			reader.close();
			if (lineErrors != "") {
				if (debug) Log.d(TAG,lineErrors);
			}

			importedEntries=newEntries;
			if (newEntries==0)
		    {
		        importMessage=getString(R.string.import_no_entries);
		        return;
		    }else{
		    	importMessage=getString(R.string.added)+ " "+ newEntries +
		    		" "+ getString(R.string.entries);
		    	importedFilename=filename;
		    }
		} catch (IOException e) {
			e.printStackTrace();
			importMessage=getString(R.string.import_file_error);
		}
	}

	public static HashMap<String, Long> getCategoryToId(DBHelper dbHelper)
	{
		CryptoHelper ch = new CryptoHelper();
		if(masterKey == null) {
		    masterKey = "";
		}
		try {
			ch.init(CryptoHelper.EncryptionMedium,salt);
			ch.setPassword(masterKey);
		} catch (CryptoHelperException e1) {
			e1.printStackTrace();
			return null;
		}
	
		HashMap<String,Long> categories = new HashMap<String,Long>();
		List<CategoryEntry> rows;
		rows = dbHelper.fetchAllCategoryRows();

		for (CategoryEntry row : rows) {
		    String cryptDesc = row.name;
		    row.plainName = "";
		    try {
				row.plainName = ch.decrypt(cryptDesc);
				categories.put(row.plainName, row.id);
		    } catch (CryptoHelperException e) {
				Log.e(TAG,e.toString());
		    }
		}
		return categories;
	}
}