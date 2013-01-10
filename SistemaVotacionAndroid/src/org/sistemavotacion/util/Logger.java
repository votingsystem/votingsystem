package org.sistemavotacion.util;

import android.util.Log;

public class Logger {
	
	public static final String CLASSTAG = "Logger";
	
	private static boolean isDebugEnabled = true;
	
	public static void setDebugEnabled (boolean enabled) {
		Log.d(CLASSTAG, " - setDebugEnabled: " + enabled);
		isDebugEnabled = enabled;
	}
	
	public static void d (String tag, String msg) {
		if (!isDebugEnabled) return;
		Log.d(tag, msg != null? msg : ""); 
	}
	
	public static void e (String tag, String msg, Throwable tr) {
		Log.e(tag, msg, tr);
	}

}
