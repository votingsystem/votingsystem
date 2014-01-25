package org.votingsystem.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.android.activity.ReceiptPagerActivity;
import org.votingsystem.android.contentprovider.TicketContentProvider;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CurrencyVS;
import org.votingsystem.model.TicketAccount;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TicketGridFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<TransactionVS>>, AbsListView.OnScrollListener{

    public static final String TAG = "TicketGridFragment";

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
        adapter = new ReceiptGridAdapter(getActivity().getApplicationContext());
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

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.ticket_user_info, menu);
        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
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
            selection = TicketContentProvider.TYPE_COL + "=? ";
            selectionArgs = new String[]{receiptType.toString()};
        }
        Cursor cursor = getActivity().getContentResolver().query(
                TicketContentProvider.CONTENT_URI, null, selection, selectionArgs, null);
        getLoaderManager().getLoader(ContextVS.RECEIPT_LOADER_ID).
                deliverResult(cursor);
    }

    @Override public Loader<List<TransactionVS>> onCreateLoader(int i, Bundle bundle) {
        return new TransactionVSLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<TransactionVS>> listLoader, List<TransactionVS> data) {
        Log.d(TAG + "onLoadFinished(...)", "");
        adapter.setData(data);
    }

    @Override public void onLoaderReset(Loader<List<TransactionVS>> cursorLoader) {
        Log.d(TAG + ".onLoaderReset(...)", "");
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
           int visibleItemCount, int totalItemCount) { }


    public class ReceiptGridAdapter extends ArrayAdapter<TransactionVS> {

        private LayoutInflater inflater = null;

        public ReceiptGridAdapter(Context ctx) {
            super(ctx, android.R.layout.simple_list_item_2);
            inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = inflater.inflate(R.layout.row_receipt, parent, false);
            } else {
                view = convertView;
            }

            TransactionVS transaction = getItem(position);


            LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
            linearLayout.setBackgroundColor(Color.WHITE);
            TextView subject = (TextView) view.findViewById(R.id.receipt_subject);
            TextView dateInfo = (TextView) view.findViewById(R.id.receipt_date_info);
            TextView typeTextView = (TextView) view.findViewById(R.id.receipt_type);
            TextView receiptState = (TextView) view.findViewById(R.id.receipt_state);

            subject.setText(transaction.getFromUserVS().getFullName());
            String dateInfoStr = null;
            ImageView imgView = (ImageView)view.findViewById(R.id.receipt_icon);

            if(dateInfoStr != null) dateInfo.setText(Html.fromHtml(dateInfoStr));

            return view;
        }

        public void setData(List<TransactionVS> data) {
            clear();
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    add(data.get(i));
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


    public static class TransactionVSLoader extends AsyncTaskLoader<List<TransactionVS>> {

        List<TransactionVS> transactionList;

        File ticketUserInfoDataFile;

        public TransactionVSLoader(Context context) {
            super(context);
            ticketUserInfoDataFile = new File(context.getFilesDir(),
                    ContextVS.TICKET_USER_INFO_DATA_FILE_NAME);
        }

        /**
         * This is where the bulk of our work is done.  This function is
         * called in a background thread and should generate a new set of
         * data to be published by the loader.
         */
        @Override public List<TransactionVS> loadInBackground() {
            // Retrieve all known applications.
            List<TransactionVS> transactions = null;
            try {
                if(ticketUserInfoDataFile.exists()) {
                    byte[] serializedTicketUserInfo = FileUtils.getBytesFromFile(ticketUserInfoDataFile);
                    TicketAccount ticketUserInfo = (TicketAccount) ObjectUtils.deSerializeObject(
                            serializedTicketUserInfo);
                    transactions = ticketUserInfo.getCurrencyMap().get(CurrencyVS.Euro).
                            getTransactionList();
                }
            }catch(Exception ex) {
                ex.printStackTrace();
            }
            return transactions;
        }


        /**
         * Handles a request to start the Loader.
         */
        @Override protected void onStartLoading() {
            forceLoad();
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override protected void onStopLoading() {
            // Attempt to cancel the current load task if possible.
            cancelLoad();
        }

        /**
         * Handles a request to cancel a load.
         */
        @Override public void onCanceled(List<TransactionVS> transactions) {
            super.onCanceled(transactions);
        }

        /**
         * Handles a request to completely reset the Loader.
         */
        @Override protected void onReset() {
            super.onReset();
            onStopLoading();
        }
    }

}