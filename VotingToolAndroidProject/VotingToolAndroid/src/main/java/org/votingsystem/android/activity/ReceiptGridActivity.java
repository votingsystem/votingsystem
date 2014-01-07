package org.votingsystem.android.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import org.votingsystem.android.contentprovider.ReceiptContentProvider;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReceiptGridActivity extends ActionBarActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener{

    public static final String TAG = "ReceiptGridActivity";

    private ReceiptGridAdapter adapter = null;
    private VoteVS vote = null;
    private ContextVS contextVS;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String queryStr;
    private int menuItemSelected = R.id.all_receipts;
    private GridView gridView;
    private FrameLayout gridContainer;
    private Menu menu;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.receipt_grid_activity);
        contextVS = ContextVS.getInstance(getBaseContext());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        queryStr = getIntent().getStringExtra(SearchManager.QUERY);
        Log.d(TAG +  ".onCreate(...)", "args: " + getIntent().getExtras());
        gridView = (GridView) findViewById(R.id.gridview);
        adapter = new ReceiptGridAdapter(getApplicationContext(), null,false);
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
        progressContainer = findViewById(R.id.progressContainer);
        gridContainer =  (FrameLayout)findViewById(R.id.gridContainer);
        gridContainer.getForeground().setAlpha(0);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
        }
        if(savedInstanceState != null)
        menuItemSelected = savedInstanceState.getInt(ContextVS.ITEM_ID_KEY, R.id.all_receipts);
        getSupportLoaderManager().initLoader(ContextVS.RECEIPT_LOADER_ID, null, this);
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Clicked item - position:" + position +
                " -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(this,ReceiptPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d(TAG + ".onLongListItemClick(...)", "id: " + id);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.all_receipts:
            case R.id.vote_receipts:
            case R.id.cancel_vote_receipts:
                if(menuItemSelected != item.getItemId()) {
                    String selection = null;
                    String[] selectionArgs = null;
                    if(item.getItemId() != R.id.all_receipts) {
                        selection = ReceiptContentProvider.TYPE_COL + "=? ";
                        String typeStr = null;
                        if(item.getItemId() == R.id.vote_receipts)
                            typeStr = TypeVS.VOTEVS.toString();
                        else if (item.getItemId() == R.id.cancel_vote_receipts)
                            typeStr = TypeVS.CANCEL_VOTE.toString();
                        selectionArgs = new String[]{typeStr};
                        Log.d(TAG + ".onOptionsItemSelected(...)", "filtering by " + typeStr);
                    } Log.d(TAG + ".onOptionsItemSelected(...)", "showing all receipts");
                    Cursor cursor = getContentResolver().query(ReceiptContentProvider.CONTENT_URI,
                            null, selection, selectionArgs, null);
                    getSupportLoaderManager().getLoader(ContextVS.RECEIPT_LOADER_ID).
                            deliverResult(cursor);
                }
                onOptionsItemSelected(item.getItemId());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean onOptionsItemSelected(int itemId) {
        menuItemSelected = itemId;
        menu.removeItem(R.id.all_receipts);
        menu.removeItem(R.id.vote_receipts);
        menu.removeItem(R.id.cancel_vote_receipts);
        switch (itemId) {
            case R.id.all_receipts:
                getMenuInflater().inflate(R.menu.receipt_grid_activity, menu);
                menu.removeItem(R.id.all_receipts);
                break;
            case R.id.vote_receipts:
                getMenuInflater().inflate(R.menu.receipt_grid_activity, menu);
                menu.removeItem(R.id.vote_receipts);
                break;
            case R.id.cancel_vote_receipts:
                getMenuInflater().inflate(R.menu.receipt_grid_activity, menu);
                menu.removeItem(R.id.cancel_vote_receipts);
                break;
        }
        return true;
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG + ".onCreateOptionsMenu(...)", "onCreateOptionsMenu");
        this.menu = menu;
        onOptionsItemSelected(menuItemSelected);
        return true;
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, ReceiptContentProvider.CONTENT_URI, null, null, null, null);
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG + ".onLoadFinished(...)", " - cursor.getCount(): " + cursor.getCount());
        showProgress(false, true);
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else findViewById(android.R.id.empty).setVisibility(View.GONE);
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
                ReceiptContainer.State state =  ReceiptContainer.State.valueOf(stateStr);

                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView subject = (TextView) view.findViewById(R.id.event_subject);
                TextView dateInfo = (TextView) view.findViewById(R.id.event_date_info);
                TextView author = (TextView) view.findViewById(R.id.event_author);
                TextView receiptState = (TextView) view.findViewById(R.id.receipt_state);

                subject.setText(receiptContainer.getSubject());
                String dateInfoStr = null;
                ImageView imgView = (ImageView)view.findViewById(R.id.event_state_icon);
                if(DateUtils.getTodayDate().after(receiptContainer.getValidTo())) {
                    imgView.setImageResource(R.drawable.closed);
                    dateInfoStr = "<b>" + getString(R.string.closed_upper_lbl) + "</b> - " +
                            "<b>" + getString(R.string.inicio_lbl) + "</b>: " +
                            DateUtils.getSpanishStringFromDate(
                                    receiptContainer.getValidFrom()) + " - " +
                            "<b>" + getString(R.string.fin_lbl) + "</b>: " +
                            DateUtils.getSpanishStringFromDate(receiptContainer.getValidTo());
                } else {
                    imgView.setImageResource(R.drawable.open);
                    dateInfoStr = "<b>" + getString(R.string.remain_lbl, DateUtils.
                            getElpasedTimeHoursFromNow(receiptContainer.getValidTo()))  +"</b>";
                }
                if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));
                else dateInfo.setVisibility(View.GONE);
                if(state == ReceiptContainer.State.CANCELLED) {
                    receiptState.setText(getString(R.string.vote_canceled_receipt_lbl));
                    receiptState.setVisibility(View.VISIBLE);
                } else {
                    receiptState.setVisibility(View.GONE);
                }
                if(true) {
                    author.setText(Html.fromHtml(receiptContainer.getType().toString()));
                } else author.setVisibility(View.GONE);
            }
        }
    }

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getSupportFragmentManager(), MessageDialogFragment.TAG);
        showProgress(false, true);
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume()", "");
        super.onResume();
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        outState.putInt(ContextVS.ITEM_ID_KEY, menuItemSelected);
        Log.d(TAG + ".onSaveInstanceState(...) ", "outState: " + outState);
    }

    @Override protected void onDestroy() {
        Log.d(TAG + ".onDestroy()", "");
        super.onDestroy();
    };

    @Override public void onStop() {
        Log.d(TAG + ".onStop()", "onStop");
        super.onStop();
    }

    public void showProgress(boolean showProgress, boolean animate) {
        if (progressVisible.get() == showProgress)  return;
        progressVisible.set(showProgress);
        if (progressVisible.get() && progressContainer != null) {
            getWindow().getDecorView().findViewById(android.R.id.content).invalidate();
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getApplicationContext(), android.R.anim.fade_in));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_out));
            }
            progressContainer.setVisibility(View.VISIBLE);
            //eventContainer.setVisibility(View.INVISIBLE);
            mainLayout.getForeground().setAlpha(150); // dim
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to disable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        } else {
            if (animate) {
                progressContainer.startAnimation(AnimationUtils.loadAnimation(
                        getApplicationContext(), android.R.anim.fade_out));
                //eventContainer.startAnimation(AnimationUtils.loadAnimation(
                //        this, android.R.anim.fade_in));
            }
            progressContainer.setVisibility(View.GONE);
            //eventContainer.setVisibility(View.VISIBLE);
            mainLayout.getForeground().setAlpha(0); // restore
            progressContainer.setOnTouchListener(new View.OnTouchListener() {
                //to enable touch events on background view
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }
    }

}