package org.openintents.safe.dialog;

import org.openintents.distribution.GetFromMarketDialog;
import org.openintents.distribution.RD;
import org.openintents.intents.FileManagerIntents;
import org.openintents.util.IntentUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

public class DialogHostingActivity extends Activity {

	private static final String TAG = "DialogHostingActivity";
	private static final boolean debug = false;

	public static final int DIALOG_ID_SAVE = 1;
	public static final int DIALOG_ID_OPEN = 2;
	public static final int DIALOG_ID_NO_FILE_MANAGER_AVAILABLE = 3;
	public static final int DIALOG_ID_ALLOW_EXTERNAL_ACCESS = 4;
	public static final int DIALOG_ID_FIRST_TIME_WARNING = 5;
	
	public static final String EXTRA_DIALOG_ID = "org.openintents.notepad.extra.dialog_id";

	/**
	 * Whether dialog is simply pausing while hidden by another activity
	 * or when configuration changes.
	 * If this is false, then we can safely finish this activity if a dialog
	 * gets dismissed.
	 */
	private boolean mIsPausing = false;
	
    EditText mEditText;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (debug) Log.d(TAG, "onCreate");
		
		Intent i = getIntent();
		if (i != null && savedInstanceState == null) {
			if (debug) Log.d(TAG, "new dialog");
			int dialogId = i.getIntExtra(EXTRA_DIALOG_ID, 0);
			switch (dialogId) {
			case DIALOG_ID_SAVE:
				Log.i(TAG, "Show Save dialog");
				saveFile();
				break;
			case DIALOG_ID_OPEN:
				Log.i(TAG, "Show Save dialog");
				openFile();
				break;
			case DIALOG_ID_NO_FILE_MANAGER_AVAILABLE:
				Log.i(TAG, "Show no file manager dialog");
				showDialog(DIALOG_ID_NO_FILE_MANAGER_AVAILABLE);
			case DIALOG_ID_ALLOW_EXTERNAL_ACCESS:
				Log.i(TAG, "Show allow access dialog");
				showDialog(DIALOG_ID_ALLOW_EXTERNAL_ACCESS);
				break;
			case DIALOG_ID_FIRST_TIME_WARNING:
				Log.i(TAG, "Show first time warning dialog");
				showDialog(DIALOG_ID_FIRST_TIME_WARNING);
				break;
			}
		}
	}


	/**
	 * 
	 */
	private void saveFile() {
		
		// Check whether intent exists
		Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
		intent.setData(getIntent().getData());
		if (IntentUtils.isIntentAvailable(this, intent)) {
			/*
			intent.putExtra(NotePadIntents.EXTRA_URI, getIntent().getStringExtra(NotePadIntents.EXTRA_URI));
			intent.putExtra(FileManagerIntents.EXTRA_TITLE, getText(R.string.menu_save_to_sdcard));
			intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getText(R.string.save));
			*/
			intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			startActivity(intent);
			finish();
		} else {
			showDialog(DIALOG_ID_SAVE);
		}
	}
	

	private void openFile() {
		
		// Check whether intent exists
		Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
		intent.setData(getIntent().getData());
		if (IntentUtils.isIntentAvailable(this, intent)) {
			/*
			intent.putExtra(NotePadIntents.EXTRA_URI, getIntent().getStringExtra(NotePadIntents.EXTRA_URI));
			intent.putExtra(FileManagerIntents.EXTRA_TITLE, getText(R.string.menu_open_from_sdcard));
			intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getText(R.string.open));
			intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
			*/
			startActivity(intent);
			finish();
		} else {
			showDialog(DIALOG_ID_OPEN);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		if (debug) Log.d(TAG, "onCreateDialog");

		Dialog dialog = null;
		
		switch (id) {
		case DIALOG_ID_SAVE:
			dialog = new FilenameDialog(this);
			break;
		case DIALOG_ID_OPEN:
			dialog = new FilenameDialog(this);
			break;
		case DIALOG_ID_NO_FILE_MANAGER_AVAILABLE:
			Log.i(TAG, "fmd - create");
			dialog = new GetFromMarketDialog(this, 
					RD.string.filemanager_not_available,
					RD.string.filemanager_get_oi_filemanager,
					RD.string.filemanager_market_uri);
			break;
		case DIALOG_ID_ALLOW_EXTERNAL_ACCESS:
			dialog = new AllowExternalAccessDialog(this);
			break;
		case DIALOG_ID_FIRST_TIME_WARNING:
			dialog = new FirstTimeWarningDialog(this);
			break;
		}
		if (dialog == null) {
			dialog = super.onCreateDialog(id);
		}
		if (dialog != null) {
			dialog.setOnDismissListener(mDismissListener);
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
		
		if (debug) Log.d(TAG, "onPrepareDialog");
		
		//dialog.setOnDismissListener(mDismissListener);
		
		switch (id) {
		case DIALOG_ID_SAVE:
			break;
		case DIALOG_ID_OPEN:
			break;
		case DIALOG_ID_NO_FILE_MANAGER_AVAILABLE:
			break;
		}
	}
	
	OnDismissListener mDismissListener = new OnDismissListener() {
		
		public void onDismiss(DialogInterface dialoginterface) {
			if (debug) Log.d(TAG, "Dialog dismissed. Pausing: " + mIsPausing);
			if (!mIsPausing) {
				if (debug) Log.d(TAG, "finish");
				// Dialog has been dismissed by user.
				DialogHostingActivity.this.finish();
			} else {
				// Probably just a screen orientation change. Don't finish yet.
				// Dialog has been dismissed by system.
			}
		}
		
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (debug) Log.d(TAG, "onSaveInstanceState");
		mIsPausing = true;
		if (debug) Log.d(TAG, "onSaveInstanceState. Pausing: " + mIsPausing);
	}
	
	
	
}
