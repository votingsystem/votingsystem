package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.android.contentprovider.EventVSContentProvider;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventVSDBViewerFragment extends Fragment {

    public static final String TAG = "EventVSDBViewerFragment";

    private EventVS eventVS;
    private ContextVS contextVS;

    public static EventVSFragment newInstance(String eventJSONStr) {
        EventVSFragment fragment = new EventVSFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.EVENTVS_KEY, eventJSONStr);
        fragment.setArguments(args);
        return fragment;
    }


    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
               Bundle savedInstanceState) {
        Log.d(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        contextVS = ContextVS.getInstance(getActivity().getApplicationContext());
        try {
            if(getArguments().getString(ContextVS.EVENTVS_KEY) != null) {
                eventVS = EventVS.parse(new JSONObject(getArguments().getString(
                        ContextVS.EVENTVS_KEY)));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        View rootView = inflater.inflate(R.layout.eventvs_db_fragment, container, false);
        TextView subjectTextView = (TextView) rootView.findViewById(R.id.event_id_and_subject);
        TextView typeAndStateTextView = (TextView) rootView.findViewById(R.id.event_stated_and_type);
        subjectTextView.setText(eventVS.getId() + " - " + eventVS.getSubject());
        typeAndStateTextView.setText(eventVS.getTypeVS().toString() + " - " +
                eventVS.getState().toString());
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.event, menu);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG + ".onOptionsItemSelected(...) ", " - item: " + item.getTitle());
        return false;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.d(TAG + ".onDestroy()", " - onDestroy");
    };

    @Override public void onStop() {
        super.onStop();
        Log.d(TAG + ".onStop()", " - onStop");
    }

    private void showMessage(String caption, String message) {
        Log.d(TAG + ".showMessage(...) ", "caption: " + caption + " - showMessage: " + message);
        AlertDialog.Builder builder= new AlertDialog.Builder(getActivity());
        builder.setTitle(caption).setMessage(message).show();
    }

}