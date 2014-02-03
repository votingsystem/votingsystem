package org.votingsystem.android.ui;

import android.webkit.JavascriptInterface;

import org.votingsystem.android.fragment.EditorFragment;
import org.votingsystem.model.OperationVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class JavaScriptInterface {
	
	public static final String TAG = JavaScriptInterface.class.getSimpleName();

    private EditorFragment editorFragment;

    public JavaScriptInterface(EditorFragment editorFragment) {
    	this.editorFragment = editorFragment;
    }
    
    @JavascriptInterface public void setVotingWebAppMessage (String appMessage) {
    	try {
			OperationVS operationVS = OperationVS.parse(appMessage);
            editorFragment.processOperation(operationVS);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

}