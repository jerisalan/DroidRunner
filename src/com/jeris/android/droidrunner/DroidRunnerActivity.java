package com.jeris.android.droidrunner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class DroidRunnerActivity extends Activity {
    /** Called when the activity is first created. */
	Button start,about; 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        start=(Button)findViewById(R.id.startbutton);
        about=(Button)findViewById(R.id.aboutbutton);
        
        start.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i=new Intent(DroidRunnerActivity.this,GameActivity.class);
				startActivity(i);			
			}
		});
        
        about.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Context c=DroidRunnerActivity.this;
				new AlertDialog.Builder(c)
				.setTitle("About Droid-Runner").
				setMessage("This is a simple game in which you have to avoid the potholes" +
						" in the road and eat the food by jumping on a " +
						"simple tap to gain more points.")
				.setNegativeButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {						
					}
				}).show();
				
			}
		});
    }
}