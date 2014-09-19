package org.votingsystem.android.contentprovider;

import android.database.Cursor;
import android.util.Log;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.model.TagVSData;

import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.Vicket;
import org.votingsystem.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static final String TAG = Utils.class.getSimpleName();

    public static TagVSData getCurrencyData(AppContextVS contextVS, String currencyCode) {
        String selection = VicketContentProvider.WEEK_LAPSE_COL + " =? AND " +
                VicketContentProvider.STATE_COL + " =? AND " +
                VicketContentProvider.CURRENCY_COL + "= ? ";
        String weekLapseId = contextVS.getCurrentWeekLapseId();
        Cursor cursor = contextVS.getContentResolver().query(VicketContentProvider.CONTENT_URI,null, selection,
                new String[]{weekLapseId, Vicket.State.OK.toString(), currencyCode}, null);
        Log.d(TAG + ".getCurrencyData(...)", "VicketContentProvider - cursor.getCount(): " + cursor.getCount());
        List<Vicket> vicketList = new ArrayList<Vicket>();
        while(cursor.moveToNext()) {
            Vicket vicket = (Vicket) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(VicketContentProvider.SERIALIZED_OBJECT_COL)));
            Long vicketId = cursor.getLong(cursor.getColumnIndex(VicketContentProvider.ID_COL));
            vicket.setLocalId(vicketId);
            vicketList.add(vicket);
        }
        selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? AND " +
                TransactionVSContentProvider.CURRENCY_COL + "= ? ";
        cursor = contextVS.getContentResolver().query(TransactionVSContentProvider.CONTENT_URI,null,
                selection, new String[]{weekLapseId, currencyCode}, null);
        List<TransactionVS> transactionList = new ArrayList<TransactionVS>();
        while(cursor.moveToNext()) {
            TransactionVS transactionVS = (TransactionVS) ObjectUtils.deSerializeObject(cursor.getBlob(
                    cursor.getColumnIndex(TransactionVSContentProvider.SERIALIZED_OBJECT_COL)));
            transactionList.add(transactionVS);
        }
        TagVSData currencyData = null;
        try {
            currencyData = new TagVSData(transactionList);
            currencyData.setVicketList(vicketList);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return currencyData;
    }

}
