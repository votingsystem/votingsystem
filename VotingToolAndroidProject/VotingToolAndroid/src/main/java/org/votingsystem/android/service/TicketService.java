package org.votingsystem.android.service;

import android.app.IntentService;
import android.content.Intent;

import org.votingsystem.model.ContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketService extends IntentService {

    public static final String TAG = "TicketService";

    public TicketService() { super(TAG); }

    private ContextVS contextVS;

    @Override protected void onHandleIntent(Intent intent) {

    }

}
