package org.votingsystem.android.ui;

import org.votingsystem.model.VoteVS;

public interface ReceiptOperationsListener {

	void cancelVote(VoteVS receipt);
	void removeReceipt(VoteVS receipt);

}
