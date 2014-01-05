package org.votingsystem.android.activity;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
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
import org.votingsystem.android.contentprovider.RepresentativeContentProvider;
import org.votingsystem.android.fragment.MessageDialogFragment;
import org.votingsystem.android.fragment.PinDialogFragment;
import org.votingsystem.android.service.VoteService;
import org.votingsystem.android.ui.CertNotFoundDialog;
import org.votingsystem.android.ui.ReceiptOptionsDialog;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ReceiptContainer;
import org.votingsystem.model.VoteVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class ReceiptGridActivity extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener{

    public static final String TAG = "ReceiptGridActivity";

    private ReceiptGridAdapter adapter = null;
    private VoteVS vote = null;
    private ContextVS contextVS;
    private View progressContainer;
    private FrameLayout mainLayout;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private String queryStr;
    private int loaderId = 1;
    private GridView gridView;
    private FrameLayout gridContainer;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)",
                "intent.getExtras(): " + intent.getExtras());
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
        ReceiptOptionsDialog.Operation operation = (ReceiptOptionsDialog.Operation) intent.
                getSerializableExtra(ContextVS.OPERATION_KEY);
        if(pin != null) launchVoteCancellationService(pin);
        else {
            if(operation == ReceiptOptionsDialog.Operation.CANCEL_VOTE) {
                cancelVote(vote);
            } else if(operation == ReceiptOptionsDialog.Operation.REMOVE_RECEIPT) {
                removeReceipt(vote);
            }
        }
        }
    };

    private void launchVoteCancellationService(String pin) {
        Log.d(TAG + ".launchVoteCancellationService(...)", "");
        try {
            Intent startIntent = new Intent(getApplicationContext(), VoteService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.OPERATION_KEY, VoteService.Operation.CANCEL_VOTE);
            startIntent.putExtra(ContextVS.CALLER_KEY, this.getClass().getName());
            startIntent.putExtra(ContextVS.VOTE_KEY, vote);
            showProgress(true, true);
            startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG + ".onCreate(...) ", "savedInstanceState: " + savedInstanceState);
        setContentView(R.layout.receipt_grid_activity);
        contextVS = ContextVS.getInstance(getBaseContext());
        queryStr = getIntent().getStringExtra(SearchManager.QUERY);
        Log.d(TAG +  ".onCreate(...)", "args: " + getIntent().getExtras() + " - loaderId: " +
                loaderId);
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
        getSupportLoaderManager().initLoader(loaderId, null, this);
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Clicked item - position:" + position +
                " -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        byte[] serializedReceiptContainer = cursor.getBlob(cursor.getColumnIndex(
                ReceiptContentProvider.SERIALIZED_OBJECT_COL));
        ReceiptContainer receiptContainer = null;
        try {
            receiptContainer = (ReceiptContainer) ObjectUtils.
                    deSerializeObject(serializedReceiptContainer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        launchOptionsDialog(receiptContainer);
    }

    private void launchOptionsDialog(ReceiptContainer receiptContainer) {
        String caption = receiptContainer.getSubject();
        String msg = getString(R.string.receipt_options_dialog_msg);
        ReceiptOptionsDialog optionsDialog = ReceiptOptionsDialog.newInstance(
                caption, msg, vote, this.getClass().getName());
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(ReceiptOptionsDialog.TAG);
        if (prev != null) ft.remove(prev);
        ft.addToBackStack(null);
        optionsDialog.show(ft, ReceiptOptionsDialog.TAG);
    }

    private void showPinScreen(String message) {
        PinDialogFragment pinDialog = PinDialogFragment.newInstance(
                message, false, this.getClass().getName());
        pinDialog.show(getSupportFragmentManager(), PinDialogFragment.TAG);
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d(TAG + ".onLongListItemClick(...)", "id: " + id);
        return true;
    }


    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", "item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, NavigationDrawer.class);
                startActivity(intent);
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.vote_receipt_list, menu);
        return true;
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        CursorLoader loader = new CursorLoader(this, ReceiptContentProvider.CONTENT_URI, null, null, null, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG + ".f(...)", " - cursor.getCount(): " + cursor.getCount());
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
                ReceiptContainer receiptContainer = null;
                try {
                    receiptContainer = (ReceiptContainer) ObjectUtils.
                            deSerializeObject(serializedReceiptContainer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                Log.d(TAG + ".ReceiptListAdapter.getView(...)", " - receiptId: " +
                        receiptContainer.getId() + " - type: " + receiptContainer.getType());
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
                            DateUtils.getShortSpanishStringFromDate(
                                    receiptContainer.getValidFrom()) + " - " +
                            "<b>" + getString(R.string.fin_lbl) + "</b>: " +
                            DateUtils.getShortSpanishStringFromDate(receiptContainer.getValidTo());
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
                    String authorStr =  "<b> ===========</b>: " + "==========";
                    author.setText(Html.fromHtml(authorStr));
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

    private void cancelVote(VoteVS receipt) {
        Log.d(TAG + ".cancelVote(...)", " - cancelVote");
        vote = receipt;
        if (!ContextVS.State.WITH_CERTIFICATE.equals(contextVS.getState())) {
            Log.d(TAG + "- firmarEnviarButton -", " mostrando dialogo certificado no encontrado");
            showCertNotFoundDialog();
        } else {
            showPinScreen(getString(R.string.cancel_vote_msg));
        }
    }

    private void dismissOptionsDialog() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        DialogFragment prev = (DialogFragment) getSupportFragmentManager().findFragmentByTag(
                ReceiptOptionsDialog.TAG);
        if(prev != null) {
            prev.getDialog().dismiss();
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);
        }
    }

    private void showCertNotFoundDialog() {
        CertNotFoundDialog certDialog = new CertNotFoundDialog();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(ContextVS.CERT_NOT_FOUND_DIALOG_ID);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        certDialog.show(ft, ContextVS.CERT_NOT_FOUND_DIALOG_ID);
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume()", "");
        super.onResume();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(this.getClass().getName()));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

    private void removeReceipt(ReceiptContainer receipt) {
        Log.d(TAG + ".removeReceipt()", "receipt: " + receipt.getId());
        String selection = ReceiptContentProvider.ID_COL + "=? ";
        getContentResolver().delete(ReceiptContentProvider.CONTENT_URI, selection,
                new String[]{receipt.getId().toString()});
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
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