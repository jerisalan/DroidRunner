package com.jeris.android.droidrunner;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.jeris.android.droidrunner.RunJumpView.DroidRunJumpThread;


public class GameActivity extends Activity {
	
	public static final String PREFS_NAME = "DRJPrefsFile";
	
	RunJumpView drjView;
	DroidRunJumpThread drjThread;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.screen);        
        drjView = (RunJumpView) findViewById(R.id.droidrunjump);                       
    }
    
    @Override
    protected void onPause() {
    	super.onPause();

    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
       	SharedPreferences.Editor editor = settings.edit();
       	
       	drjThread = drjView.getThread();
       	       	    	
       	// if player wants to quit then reset the game
    	if (isFinishing()) {
    		drjThread.resetGame();
    	}
    	else {	    	
    		drjThread.pause();
    	}
    	
       	drjThread.saveGame(editor);
    }
    
    @Override
    protected void onResume() {
    	super.onResume();    
    	// restore game
    	drjThread = drjView.getThread();    
	    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	   	drjThread.restoreGame(settings);  
    }   
}