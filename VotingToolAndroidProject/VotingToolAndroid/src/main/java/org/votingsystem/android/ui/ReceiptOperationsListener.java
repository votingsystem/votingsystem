package org.votingsystem.android.ui;

import org.votingsystem.android.model.VoteReceipt;

public interface ReceiptOperationsListener {

	void cancelVote(VoteReceipt receipt);
	void removeReceipt(VoteReceipt receipt);

}
