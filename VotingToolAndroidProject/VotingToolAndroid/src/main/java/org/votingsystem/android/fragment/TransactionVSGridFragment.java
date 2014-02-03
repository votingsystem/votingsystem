package org.votingsystem.android.fragment;

import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.android.activity.TransactionVSPagerActivity;
import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.service.TicketService;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import java.text.Collator;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TransactionVSGridFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, AbsListView.OnScrollListener {

    public static final String TAG = TransactionVSGridFragment.class.getSimpleName();

    private View rootView;
    private GridView gridView;
    private AtomicBoolean progressVisible = new AtomicBoolean(false);
    private TransactionVSListAdapter adapter = null;
    private String queryStr = null;
    private AppContextVS contextVS = null;
    private Long offset = new Long(0);
    private Integer firstVisiblePosition = null;
    private View progressContainer;
    private FrameLayout gridContainer;
    private String broadCastId = TransactionVSGridFragment.class.getName();
    private int loaderId = -1;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
        Log.d(TAG + ".broadcastReceiver.onReceive(...)", "extras(): " + intent.getExtras());
        String pin = intent.getStringExtra(ContextVS.PIN_KEY);
            ResponseVS responseVS = intent.getParcelableExtra(ContextVS.RESPONSEVS_KEY);
        if(pin != null) {
            switch(responseVS.getTypeVS()) {
                case TICKET_USER_INFO:
                    launchUpdateUserInfoService(pin);
                    break;
            }
        } else {
            switch(responseVS.getTypeVS()) {
                case TICKET_USER_INFO:
                    break;
            }
            showProgress(false, false);
        }
        }
    };


    private void launchUpdateUserInfoService(String pin) {
        Log.d(TAG + ".launchUpdateUserInfoService(...) ", "");
        try {
            Intent startIntent = new Intent(getActivity().getApplicationContext(),
                    TicketService.class);
            startIntent.putExtra(ContextVS.PIN_KEY, pin);
            startIntent.putExtra(ContextVS.TYPEVS_KEY, TypeVS.TICKET_USER_INFO);
            startIntent.putExtra(ContextVS.CALLER_KEY, broadCastId);
            showProgress(true, true);
            getActivity().startService(startIntent);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Perform alphabetical comparison of application entry objects.
     */
    public static final Comparator<UserVS> ALPHA_COMPARATOR = new Comparator<UserVS>() {
        private final Collator sCollator = Collator.getInstance();
        @Override public int compare(UserVS object1, UserVS object2) {
            return sCollator.compare(object1.getName(), object2.getName());
        }
    };

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        loaderId = NavigatorDrawerOptionsAdapter.GroupPosition.TICKETS.getLoaderId(0);
        queryStr = getArguments().getString(SearchManager.QUERY);
        Log.d(TAG +  ".onCreate(...)", "args: " + getArguments() + " - loaderId: " + loaderId);
        setHasOptionsMenu(true);
    };

    private void showMessage(Integer statusCode, String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "statusCode: " + statusCode + " - caption: " + caption +
                " - message: " + message);
        MessageDialogFragment newFragment = MessageDialogFragment.newInstance(statusCode, caption,
                message);
        newFragment.show(getFragmentManager(), MessageDialogFragment.TAG);
    }

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
           Bundle savedInstanceState) {
        Log.d(TAG +  ".onCreateView(..)", "savedInstanceState: " + savedInstanceState);
        rootView = inflater.inflate(R.layout.generic_grid_fragment, container, false);
        gridView = (GridView) rootView.findViewById(R.id.gridview);
        adapter = new TransactionVSListAdapter(getActivity().getApplicationContext(), null,false);
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
        progressContainer = rootView.findViewById(R.id.progressContainer);
        gridContainer =  (FrameLayout)rootView.findViewById(R.id.gridContainer);
        gridContainer.getForeground().setAlpha(0);
        return rootView;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG +  ".onActivityCreated(...)", "savedInstanceState: " + savedInstanceState);
        super.onActivityCreated(savedInstanceState);
        //Prepare the loader. Either re-connect with an existing one or start a new one.
        getLoaderManager().initLoader(loaderId, null, this);
        if(savedInstanceState != null) {
            Parcelable gridState = savedInstanceState.getParcelable(ContextVS.LIST_STATE_KEY);
            gridView.onRestoreInstanceState(gridState);
            offset = savedInstanceState.getLong(ContextVS.OFFSET_KEY);
            if(savedInstanceState.getBoolean(ContextVS.LOADING_KEY, false)) showProgress(true, true);
        }
    }

    protected boolean onLongListItemClick(View v, int pos, long id) {
        Log.d(TAG + ".onLongListItemClick(...)", "id: " + id);
        return true;
    }

    @Override public void onScrollStateChanged(AbsListView absListView, int i) { }

    @Override public void onScroll(AbsListView view, int firstVisibleItem,
               int visibleItemCount, int totalItemCount) { }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.ticket_user_info, menu);

        menu.setGroupVisible(R.id.general_items, false);
        menu.removeItem(R.id.search_item);
        List<String> transactionWeekList =TransactionVSContentProvider.getTransactionWeekList (
                (AppContextVS)getActivity().getApplicationContext());
        for(final String weekLbl: transactionWeekList) {
            MenuItem item = menu.add (weekLbl);
            item.setOnMenuItemClickListener (new MenuItem.OnMenuItemClickListener(){
                @Override public boolean onMenuItemClick (MenuItem item){
                    Log.d(TAG +  ".onMenuItemClick(..)", "click on weekLbl: " + weekLbl);
                    return true;
                }
            });
        }

    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG +  ".onOptionsItemSelected(..)", "Title: " + item.getTitle() +
                " - ItemId: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.reload:
                return true;
            case R.id.update_signers_info:
                PinDialogFragment.showPinScreen(getFragmentManager(), broadCastId,
                        getString(R.string.update_user_info_pin_msg), false, TypeVS.TICKET_USER_INFO);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onListItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG +  ".onListItemClick(...)", "Clicked item - position:" + position +" -id: " + id);
        Cursor cursor = ((Cursor) gridView.getAdapter().getItem(position));
        Intent intent = new Intent(getActivity().getApplicationContext(),
                TransactionVSPagerActivity.class);
        intent.putExtra(ContextVS.CURSOR_POSITION_KEY, position);
        startActivity(intent);
    }

    @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        Log.d(TAG + ".onCreateLoader(...)", "");
        String weekLapse = DateUtils.getDirPath(DateUtils.getMonday(Calendar.getInstance()).getTime());
        String selection = TransactionVSContentProvider.WEEK_LAPSE_COL + " =? ";
        CursorLoader loader = new CursorLoader(this.getActivity(),
                TransactionVSContentProvider.CONTENT_URI, null, selection,
                new String[]{weekLapse}, null);
        return loader;
    }

    @Override public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Log.d(TAG + ".onLoadFinished(...)", " - cursor.getCount(): " + cursor.getCount() +
                " - firstVisiblePosition: " + firstVisiblePosition);
        showProgress(false, true);
        if(firstVisiblePosition != null) cursor.moveToPosition(firstVisiblePosition);
        firstVisiblePosition = null;
        ((CursorAdapter)gridView.getAdapter()).swapCursor(cursor);
        if(cursor.getCount() == 0) {
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        } else rootView.findViewById(android.R.id.empty).setVisibility(View.GONE);
    }

    @Override public void onLoaderReset(Loader<Cursor> cursorLoader) {
        Log.d(TAG + ".onLoaderReset(...)", "");
        ((CursorAdapter)gridView.getAdapter()).swapCursor(null);
    }

    @Override public void onAttach(Activity activity) {
        super.onAttach(activity);
        Intent intent = activity.getIntent();
        if(intent != null) {
            String query = null;
            if (Intent.ACTION_SEARCH.equals(intent)) {
                query = intent.getStringExtra(SearchManager.QUERY);
            }
            Log.d(TAG + ".onAttach()", "activity: " + activity.getClass().getName() +
                    " - query: " + query + " - activity: ");
        }
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

    public class TransactionVSListAdapter  extends CursorAdapter {

        private LayoutInflater inflater = null;

        public TransactionVSListAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
            inflater = LayoutInflater.from(context);
        }

        @Override public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            return inflater.inflate(R.layout.row_transactionvs, viewGroup, false);
        }

        @Override public void bindView(View view, Context context, Cursor cursor) {
            if(cursor != null) {
                byte[] serializedTransactionVS = cursor.getBlob(cursor.getColumnIndex(
                        TransactionVSContentProvider.SERIALIZED_OBJECT_COL));
                TransactionVS transactionVS = (TransactionVS) ObjectUtils.
                        deSerializeObject(serializedTransactionVS);
                String weekLapseStr = cursor.getString(cursor.getColumnIndex(
                        TransactionVSContentProvider.WEEK_LAPSE_COL));
                Date weekLapse = DateUtils.getDateFromDirPath(weekLapseStr);
                Calendar weekLapseCalendar = Calendar.getInstance();
                weekLapseCalendar.setTime(weekLapse);
                LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.row);
                linearLayout.setBackgroundColor(Color.WHITE);
                TextView transaction_type = (TextView) view.findViewById(R.id.transaction_type);
                transaction_type.setText(transactionVS.getDescription(getActivity().getApplicationContext()));
                TextView week_lapse = (TextView) view.findViewById(R.id.week_lapse);
                week_lapse.setText(DateUtils.getLongDate_Es(transactionVS.getDateCreated()));

                TextView amount = (TextView) view.findViewById(R.id.amount);
                amount.setText(transactionVS.getAmount().toPlainString());
                TextView currency = (TextView) view.findViewById(R.id.currency);
                currency.setText(transactionVS.getCurrencyVS().toString());

                ((ImageView)view.findViewById(R.id.transaction_icon)).setImageResource(
                        transactionVS.getIconId(getActivity().getApplicationContext()));
            }
        }
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ContextVS.OFFSET_KEY, offset);
        Parcelable gridState = gridView.onSaveInstanceState();
        outState.putParcelable(ContextVS.LIST_STATE_KEY, gridState);
        outState.putBoolean(ContextVS.LOADING_KEY, progressVisible.get());
        Log.d(TAG +  ".onSaveInstanceState(...)", "outState: " + outState);
    }

    @Override public void onResume() {
        Log.d(TAG + ".onResume() ", "");
        super.onResume();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).registerReceiver(
                broadcastReceiver, new IntentFilter(broadCastId));
    }

    @Override public void onPause() {
        Log.d(TAG + ".onPause(...)", "");
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity().getApplicationContext()).
                unregisterReceiver(broadcastReceiver);
    }

}