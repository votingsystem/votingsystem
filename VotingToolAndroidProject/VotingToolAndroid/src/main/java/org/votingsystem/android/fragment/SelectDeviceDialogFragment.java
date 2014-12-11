package org.votingsystem.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.DeviceUtils;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.JsonUtils;
import org.votingsystem.util.ResponseVS;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SelectDeviceDialogFragment extends DialogFragment {

    public static final String TAG = SelectDeviceDialogFragment.class.getSimpleName();

    private AppContextVS contextVS;
    private SimpleAdapter simpleAdapter;
    private String dialogCaller;
    private String deviceConnectedResponseStr;
    private TextView msg_text;
    private List<String> tagList = new ArrayList<String>();

    public static void showDialog(String dialogCaller, FragmentManager manager, String tag) {
        SelectDeviceDialogFragment dialogFragment = new SelectDeviceDialogFragment();
        Bundle args = new Bundle();
        args.putString(ContextVS.CALLER_KEY, dialogCaller);
        dialogFragment.setArguments(args);
        dialogFragment.show(manager, tag);
    }

    @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
        contextVS = (AppContextVS) getActivity().getApplicationContext();
        dialogCaller = getArguments().getString(ContextVS.CALLER_KEY);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.select_device_dialog, null);
        ListView tag_list_view = (ListView) view.findViewById(R.id.tag_list_view);
        msg_text = (TextView) view.findViewById(R.id.msg_text);
        simpleAdapter = new SimpleAdapter(new ArrayList<String>(), getActivity());
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setView(view);
        tag_list_view.setAdapter(simpleAdapter);
        tag_list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parentAdapter, View view, int position,long id) {
                Intent intent = new Intent(dialogCaller);
                ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK, TypeVS.DEVICE_SELECT);
                responseVS.setMessageJSON(JsonUtils.getDeviceFromConnectedResponse(
                        tagList.get(position), deviceConnectedResponseStr));
                intent.putExtra(ContextVS.RESPONSEVS_KEY, responseVS);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                dialog.dismiss();
            }
        });
        return dialog;
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ContextVS.CALLER_KEY, dialogCaller);
        outState.putSerializable(ContextVS.MESSAGE_KEY, deviceConnectedResponseStr);
        outState.putSerializable(ContextVS.FORM_DATA_KEY, (Serializable) tagList);
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey(ContextVS.FORM_DATA_KEY)) {
            dialogCaller = savedInstanceState.getString(ContextVS.CALLER_KEY);
            deviceConnectedResponseStr = savedInstanceState.getString(ContextVS.MESSAGE_KEY);
            tagList = (List<String>) savedInstanceState.getSerializable(ContextVS.FORM_DATA_KEY);
            if(tagList.size() > 0) {
                msg_text.setText(getString(R.string.select_connected_device_msg));
            }
            simpleAdapter.setItemList(tagList);
            simpleAdapter.notifyDataSetChanged();
        } else {
            DeviceLoader deviceLoader = new DeviceLoader();
            String targetURL = ((AppContextVS)getActivity().getApplicationContext()).getCooinServer().
                    getDeviceVSConnectedServiceURL(contextVS.getUserVS().getNif());
            deviceLoader.execute(targetURL);
        }
    }

    private class DeviceLoader extends AsyncTask<String, Void, List<String>> {

        private final ProgressDialog dialog = new ProgressDialog(getActivity());

        @Override protected void onPostExecute(List<String> result) {
            super.onPostExecute(result);
            if(tagList.size() > 0) {
                msg_text.setText(getString(R.string.select_connected_device_msg));
            }
            dialog.dismiss();
            simpleAdapter.setItemList(result);
            simpleAdapter.notifyDataSetChanged();
        }

        @Override protected void onPreExecute() {
            super.onPreExecute();
            dialog.setMessage(getString(R.string.searching_devices_lbl));
            dialog.show();
        }

        @Override protected List<String> doInBackground(String... params) {
            tagList.clear();
            try {
                ResponseVS responseVS  = HttpHelper.getData(params[0], ContentTypeVS.JSON);
                if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    deviceConnectedResponseStr = responseVS.getMessageJSON().toString();
                    JSONArray tagRecords = responseVS.getMessageJSON().getJSONArray("deviceList");
                    for(int i = 0 ; i < tagRecords.length(); i++) {
                        String deviceName = tagRecords.getJSONObject(i).getString("deviceName");
                        if(!DeviceUtils.getDeviceName().equals(deviceName)) tagList.add(deviceName);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return tagList;
        }
    }

    public class SimpleAdapter extends ArrayAdapter<String> {

        private List<String> itemList;
        private Context context;

        public SimpleAdapter(List<String> itemList, Context ctx) {
            super(ctx, R.layout.adapter_list_item, itemList);
            this.itemList = itemList;
            this.context = ctx;
        }

        public int getCount() {
            if (itemList != null) return itemList.size();
            return 0;
        }

        public String getItem(int position) {
            if (itemList != null) return itemList.get(position);
            return null;
        }

        public long getItemId(int position) {
            if (itemList != null) return itemList.get(position).hashCode();
            return 0;
        }

        @Override public View getView(int position, View itemView, ViewGroup parent) {
            String tag = itemList.get(position);
            if (itemView == null) {
                LayoutInflater inflater =
                        (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                itemView = inflater.inflate(R.layout.adapter_list_item, null);
            }
            TextView text = (TextView) itemView.findViewById(R.id.list_item);
            text.setText(tag);
            return itemView;
        }

        public List<String> getItemList() {
            return itemList;
        }

        public void setItemList(List<String> itemList) {
            this.itemList = itemList;
        }

    }

}
