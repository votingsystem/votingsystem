package org.votingsystem.android.ui.debug.actions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.fragment.QRGeneratorFragment;
import org.votingsystem.android.ui.debug.DebugAction;
import org.votingsystem.model.ContextVS;

public class QRGeneratorAction implements DebugAction {

    private Activity activity;
    private static final String qrMessage = "operation=COOIN_TICKET_REQUEST;id=1;URL=https://cooins:8086/Cooins;amount=100_eur_WILDTAG;";

    public QRGeneratorAction(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run(final Context context, final Callback callback) {
        Intent intent = new Intent(activity, FragmentContainerActivity.class);
        intent.putExtra(ContextVS.MESSAGE_KEY, qrMessage);
        intent.putExtra(ContextVS.FRAGMENT_KEY, QRGeneratorFragment.class.getName());
        activity.startActivity(intent);
    }

    @Override
    public String getLabel() {
        return "launch QR encoder";
    }
}
