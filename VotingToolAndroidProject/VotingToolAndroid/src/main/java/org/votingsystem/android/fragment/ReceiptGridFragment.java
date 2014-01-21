package org.votingsystem.android.fragment;

import android.content.Context;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
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

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReceiptGridFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener{

    public static final String TAG = "ReceiptGridFragment";

    private View rootView;
    private ReceiptGridAdapter adapter = null;
    private VoteVS vote = null;
    private ContextVS contextVS;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private int menuItemSelected = R.id.all_receipts;
    private GridView gridView;
    private FrameLayout gridContainer;
    private Menu menu;

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                       Bundle savedInstanceState) {
        Log.d(TAG +  ".onCreateView(..)", "savedInstanceState: " + savedInstanceState);
        rootView = inflater.inflate(R.layout.receipt_grid_fragment, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        adapter = new ReceiptGridAdapter(getActivity().getApplicationContext(), null,false);
        gridView.setAdapter(adapter);
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> av, View v, int pos, long id) {
                return onLongListItemClick(v, pos, id);
            }
        });
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                onListItemClick(parent, v, position, id);
            }
        });
        gridView.setOnScrollListener(this);
        progressContainer = rootView.findViewById(R.id.progressContainer);
        gridContainer =  (FrameLayout)rootView.findViewById(R.id.gridContainer);
        gridContainer.getForeground().setAlpha(0);
        getLoaderManager().initLoader(ContextVS.RECEIPT_LOADER_ID, null, this);
        return rootView;
    }


    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Clicked item - position:" + position +
                " -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity(),ReceiptPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d(TAG + ".onLongListItemClick(...)", "id: " + id);
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
        getLoaderManager().getLoader(ContextVS.RECEIPT_LOADER_ID).
                deliverResult(cursor);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(getActivity(), ReceiptContentProvider.CONTENT_URI, null, null, null, null);
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG + ".onLoadFinished(...)", " - cursor.getCount(): " + cursor.getCount());
        showProgress(false, true);
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(TAG + ".onLoaderReset(...)", "");
        ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
           int visibleItemCount, int totalItemCount) { }


    public class ReceiptGridAdapter extends CursorAdapter {

        private LayoutInflater inflater = null;

        public ReceiptGridAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.row_receipt, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                        ReceiptContentProvider.SERIALIZED_OBJECT_COL));
                ReceiptContainer receiptContainer = (ReceiptContainer) ObjectUtils.
                        deSerializeObject(serializedReceiptContainer);
                String stateStr = cursor.getString(cursor.getColumnIndex(
                        ReceiptContentProvider.STATE_COL));

                Long lastUpdatedMillis = cursor.getLong(cursor.getColumnIndex(
                        ReceiptContentProvider.TIMESTAMP_UPDATED_COL));
                Date lastUpdated = new Date(lastUpdatedMillis);
                String dateInfoStr = context.getString(R.string.last_updated_msg,
                        DateUtils.getLongDate_Es(lastUpdated));

                ReceiptContainer.State state =  ReceiptContainer.State.valueOf(stateStr);

                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView subject = (TextView) view.findViewById(R.id.receipt_subject);
                TextView dateInfo = (TextView) view.findViewById(R.id.receipt_date_info);
                TextView typeTextView = (TextView) view.findViewById(R.id.receipt_type);
                TextView receiptState = (TextView) view.findViewById(R.id.receipt_state);

                subject.setText(receiptContainer.getSubject());
                typeTextView.setText(receiptContainer.getTypeDescription(context));

                ((ImageView) view.findViewById(R.id.receipt_icon)).setImageResource(
                        receiptContainer.getLogoId());

                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                else dateInfo.setVisibility(View.GONE);
                if(state == ReceiptContainer.State.CANCELLED) {
                    receiptState.setText(getString(R.string.vote_canceled_receipt_lbl));
                    receiptState.setVisibility(View.VISIBLE);
                } else {
                    receiptState.setVisibility(View.GONE);
                }
            }
        }
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }


    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        outState.putInt(ContextVS.ITEM_ID_KEY, menuItemSelected);
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState: " + outState);
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getActivity().getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_in));
            progressContainer.setVisibility(View.VISIBLE);
            gridContainer.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) progressContainer.startAnimation(AnimationUtils.loadAnimation(
                    getActivity().getApplicationContext(), android.R.anim.fade_out));
            progressContainer.setVisibility(View.GONE);
            gridContainer.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

}