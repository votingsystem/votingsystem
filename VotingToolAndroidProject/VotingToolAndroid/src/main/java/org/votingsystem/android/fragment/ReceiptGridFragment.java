package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.ReceiptPagerActivity;
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import java.util.Date;

import static org.votingsystem.util.LogUtils.LOGD;

public class ReceiptGridFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener{

    public static final String TAG = ReceiptGridFragment.class.getSimpleName();

    private View rootView;
    private ReceiptGridAdapter adapter = null;
    private VoteVS vote = null;
    private int menuItemSelected = R.id.all_receipts;
    private GridView gridView;
    private static final int loaderId = 0;

    CharSequence[] gridItemMenuOptions;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                   Bundle savedInstanceState) {
        LOGD(TAG +  ".onCreateView", "savedInstanceState: " + savedInstanceState);
        rootView = inflater.inflate(R.layout.receipt_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        gridItemMenuOptions = new CharSequence[] {getString(R.string.delete_lbl)};
        adapter = new ReceiptGridAdapter(getActivity(), null,false);
        gridView.setAdapter(adapter);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        getLoaderManager().initLoader(loaderId, null, this);
        return rootView;
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        LOGD(TAG +  ".onListItemClick", "Clicked item - position:" + position + " -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity(),ReceiptPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(gridItemMenuOptions, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int position) {
                //gridItemMenuOptions[position]
                Cursor cursor = adapter.getCursor();
                cursor.moveToPosition(position);
                Long receiptId = cursor.getLong(cursor.getColumnIndex(ReceiptContentProvider.ID_COL));
                LOGD(TAG + ".onLongListItemClick", "position: " + position + " - receiptId: " +
                        receiptId);
                getActivity().getContentResolver().delete(ReceiptContentProvider.
                        getReceiptURI(receiptId), null, null);
            }
        }).show();
        return true;
    }

    private void filterReceiptList(TypeVS receiptType) {
        String selection = null;
        String[] selectionArgs = null;
        if(receiptType != null) {
            selection = ReceiptContentProvider.TYPE_COL + "=? ";
            selectionArgs = new String[]{receiptType.toString()};
        }
        Cursor cursor = getActivity().getContentResolver().query(
                ReceiptContentProvider.CONTENT_URI, null, selection, selectionArgs, null);
        getLoaderManager().getLoader(loaderId).deliverResult(cursor);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), ReceiptContentProvider.CONTENT_URI, null, null, null, null);
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        LOGD(TAG + ".onLoadFinished", " - cursor.getCount(): " + cursor.getCount());
        setProgressDialogVisible(false);
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        LOGD(TAG + ".onLoaderReset", "");
        ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
           int visibleItemCount, int totalItemCount) { }

    private void setProgressDialogVisible(boolean isVisible) {
        if(isVisible){
            ProgressDialogFragment.showDialog(getString(R.string.loading_data_msg),
                    getString(R.string.loading_info_msg), getFragmentManager());
        } else ProgressDialogFragment.hide(getFragmentManager());
    }

    public class ReceiptGridAdapter extends CursorAdapter {

        private LayoutInflater inflater = null;

        public ReceiptGridAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.receipt_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                        ReceiptContentProvider.SERIALIZED_OBJECT_COL));
                ReceiptContainer receiptContainer = (ReceiptContainer) ObjectUtils.
                        deSerializeObject(serializedReceiptContainer);
                if(receiptContainer.getTypeVS() == null) {
                    LOGD(TAG + ".bindView", "receiptContainer id: " + receiptContainer.getLocalId() +
                            " has null TypeVS");
                    return;
                }
                String stateStr = cursor.getString(cursor.getColumnIndex(
                        ReceiptContentProvider.STATE_COL));
                Long createdMillis = cursor.getLong(cursor.getColumnIndex(
                        ReceiptContentProvider.TIMESTAMP_CREATED_COL));
                String dateInfoStr = DateUtils.getDayWeekDateStr(new Date(createdMillis));
                ReceiptContainer.State state =  ReceiptContainer.State.valueOf(stateStr);
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView dateInfo = (TextView) view.findViewById(R.id.receipt_date_info);
                TextView receiptState = (TextView) view.findViewById(R.id.receipt_state);
                ((TextView) view.findViewById(R.id.receipt_subject)).setText(
                        receiptContainer.getCardSubject(context));
                ((ImageView) view.findViewById(R.id.receipt_icon)).setImageResource(
                        receiptContainer.getLogoId());
                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                else dateInfo.setVisibility(View.GONE);
                if(state == ReceiptContainer.State.CANCELLED) {
                    receiptState.setText(getString(R.string.vote_canceled_receipt_lbl));
                    receiptState.setVisibility(View.VISIBLE);
                } else receiptState.setVisibility(View.GONE);
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        outState.putInt(ContextVS.ITEM_ID_KEY, menuItemSelected);
    }

}