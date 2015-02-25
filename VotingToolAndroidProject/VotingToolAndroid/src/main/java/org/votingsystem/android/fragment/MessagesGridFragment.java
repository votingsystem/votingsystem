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
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.MessagesPagerActivity;
import org.votingsystem.android.contentprovider.MessageContentProvider;
import org.votingsystem.android.util.CooinBundle;
import org.votingsystem.android.util.WebSocketMessage;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DateUtils;

import java.util.Date;

import static org.votingsystem.util.LogUtils.LOGD;

public class MessagesGridFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener{

    public static final String TAG = MessagesGridFragment.class.getSimpleName();

    private View rootView;
    private MessageGridAdapter adapter = null;
    private int menuItemSelected;
    private GridView gridView;
    private Cursor cursor;
    private static final int loaderId = 0;

    CharSequence[] gridItemMenuOptions;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                   Bundle savedInstanceState) {
        LOGD(TAG +  ".onCreateView", "savedInstanceState: " + savedInstanceState);
        rootView = inflater.inflate(R.layout.messages_grid, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        gridItemMenuOptions = new CharSequence[] {getString(R.string.delete_lbl)};
        adapter = new MessageGridAdapter(getActivity(), null,false);
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
        cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity(),MessagesPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        LOGD(TAG + ".onLongListItemClick", "id: " + id);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(gridItemMenuOptions, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int position) {
                //gridItemMenuOptions[position]
                cursor = adapter.getCursor();
                cursor.moveToPosition(position);
                Long messageId = cursor.getLong(cursor.getColumnIndex(MessageContentProvider.ID_COL));
                LOGD(TAG + ".onLongListItemClick", "position: " + position + " - messageId: " +
                        messageId);
                getActivity().getContentResolver().delete(MessageContentProvider.
                        getMessageURI(messageId), null, null);
            }
        }).show();
        return true;
    }

    private void filterMessageList(TypeVS messageType) {
        String selection = null;
        String[] selectionArgs = null;
        if(messageType != null) {
            selection = MessageContentProvider.TYPE_COL + "=? ";
            selectionArgs = new String[]{messageType.toString()};
        }
        cursor = getActivity().getContentResolver().query(
                MessageContentProvider.CONTENT_URI, null, selection, selectionArgs, null);
        getLoaderManager().getLoader(loaderId).deliverResult(cursor);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), MessageContentProvider.CONTENT_URI, null, null, null, null);
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

    public class MessageGridAdapter extends CursorAdapter {

        private LayoutInflater inflater = null;

        public MessageGridAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.message_card, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                try {
                    JSONObject decryptedJSON = new JSONObject(cursor.getString(
                            cursor.getColumnIndex(MessageContentProvider.JSON_COL)));
                    WebSocketMessage socketMessage = new WebSocketMessage(decryptedJSON);
                    TypeVS typeVS =  TypeVS.valueOf(cursor.getString(cursor.getColumnIndex(
                            MessageContentProvider.TYPE_COL)));
                    Integer logoId = null;
                    String messageSubject = null;
                    switch(typeVS) {
                        case MESSAGEVS:
                            logoId = R.drawable.fa_comment_32;
                            messageSubject = socketMessage.getFrom();
                            break;
                        case COOIN_WALLET_CHANGE:
                            CooinBundle cooinBundle = CooinBundle.load(socketMessage.getCooinList());
                            logoId = R.drawable.fa_money_24;
                            messageSubject = getString(R.string.wallet_change_lbl) + " - " +
                                    cooinBundle.getAmount().toPlainString() + " " +
                                    cooinBundle.getCurrencyCode();
                            break;
                    }
                    ((ImageView) view.findViewById(R.id.message_icon)).setImageResource(logoId);
                    MessageContentProvider.State state =  MessageContentProvider.State.valueOf(cursor.getString(
                            cursor.getColumnIndex(MessageContentProvider.STATE_COL)));
                    Integer colorFilter = null;
                    switch(state) {
                        case NOT_READED:
                            colorFilter = Color.parseColor("#ba0011");
                            break;
                        case READED:
                            colorFilter = Color.parseColor("#888888");
                            break;
                    }
                    ((ImageView) view.findViewById(R.id.message_icon)).setColorFilter(colorFilter);
                    Long createdMillis = cursor.getLong(cursor.getColumnIndex(
                            MessageContentProvider.TIMESTAMP_CREATED_COL));
                    String dateInfoStr = DateUtils.getDayWeekDateStr(new Date(createdMillis));
                    TextView dateInfo = (TextView) view.findViewById(R.id.message_date_info);
                    ((TextView) view.findViewById(R.id.message_subject)).setText(messageSubject);
                    if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                    else dateInfo.setVisibility(View.GONE);
                } catch(Exception ex) { ex.printStackTrace(); }
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        outState.putInt(ContextVS.ITEM_ID_KEY, menuItemSelected);
    }

    @Override public void onPause() {
        super.onPause();
    }

}