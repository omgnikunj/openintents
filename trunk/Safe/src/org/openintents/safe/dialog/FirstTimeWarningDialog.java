package org.openintents.safe.dialog;

import org.openintents.safe.Preferences;
import org.openintents.safe.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

public class FirstTimeWarningDialog extends AlertDialog implements OnClickListener {
	private static final String TAG = "FilenameDialog";

    private static final String BUNDLE_TAGS = "tags";
    
	protected static final int DIALOG_ID_NO_FILE_MANAGER_AVAILABLE = 2;
    
    Context mContext;
    
    CheckBox mCheckBox;
    
    public FirstTimeWarningDialog(Context context) {
        super(context);
        mContext = context;
        
        setTitle(context.getText(R.string.dialog_title_first_time_warning));
        setButton(context.getText(android.R.string.ok), (OnClickListener) null);
        //setButton2(context.getText(android.R.string.cancel), (OnClickListener) null);
        setIcon(android.R.drawable.ic_dialog_alert);
        setMessage(context.getText(R.string.dialog_summary_first_time_warning));

    }
    
          
	public void onClick(DialogInterface dialog, int which) {
    	if (which == BUTTON1) {
    		// User pressed OK
    	}
		
	}
}
