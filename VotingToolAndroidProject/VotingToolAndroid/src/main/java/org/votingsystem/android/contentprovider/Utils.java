package org.votingsystem.android.contentprovider;

import android.database.Cursor;
import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.model.CurrencyData;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.TicketVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    public static CurrencyData getCurrencyData(AppContextVS contextVS, CurrencyVS currency) {
        String selection = TicketContentProvider.WEEK_LAPSE_COL + " =? AND " +
                TicketContentProvider.STATE_COL + " =? AND " +
                TicketContentProvider.CURRENCY_COL + "= ? ";
        String weekLapseId = contextVS.getCurrentWeekLapseId();
        Cursor cursor = contextVS.getContentResolver().query(TicketContentProvider.CONTENT_URI,null, selection,
                new String[]{weekLapseId, TicketVS.State.OK.toString(), currency.toString()}, null);
        Log.d(TAG + ".getCurrencyData(...)", "TicketContentProvider - cursor.getCount(): " + cursor.getCount());
        List<TicketVS> ticketList = new ArrayList<TicketVS>();
        while(cursor.moveToNext()) {
            TicketVS ticketVS = (TicketVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(TicketContentProvider.SERIALIZED_OBJECT_COL)));
            Long ticketId = cursor.getLong(cursor.getColumnIndex(TicketContentProvider.ID_COL));
            ticketVS.setLocalId(ticketId);
            ticketList.add(ticketVS);
        }
        selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? AND " +
                TransactionVSContentProvider.CURRENCY_COL + "= ? ";
        cursor = contextVS.getContentResolver().query(TransactionVSContentProvider.CONTENT_URI,null,
                selection, new String[]{weekLapseId, currency.toString()}, null);
        List<TransactionVS> transactionList = new ArrayList<TransactionVS>();
        while(cursor.moveToNext()) {
            TransactionVS transactionVS = (TransactionVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(TransactionVSContentProvider.SERIALIZED_OBJECT_COL)));
            transactionList.add(transactionVS);
        }
        CurrencyData currencyData = null;
        try {
            currencyData = new CurrencyData(transactionList);
            currencyData.setTicketList(ticketList);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return currencyData;
    }

}
