package org.sistemavotacion.android.ui;

import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.util.SubSystem;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class HomeDropdownListAdapter extends BaseAdapter {
	
	public static final String TAG = "HomeDropdownListAdapter";
	
    public static final int ACTION_VOTING   = 0;
    public static final int ACTION_MANIFEST = 1;
    public static final int ACTION_CLAIM    = 2;

    private final Context context;
    
    public HomeDropdownListAdapter(final Context context) {
        this.context = context;
    }
	
	@Override
	public int getCount() {
		// minus de SubSystem.UNKNOW state
		return SubSystem.values().length - 1;
	}

	@Override
	public Object getItem(int position) {
        //Log.d(TAG + ".getItem()", " - position: " + position);
		//return Aplicacion.INSTANCIA.setSelectedSubsystem(SubSystem.valueOf(position));
		return SubSystem.valueOf(position).toString();
	}

	@Override
	public long getItemId(int position) {
        //Log.d(TAG + ".getItemId()", " - position: " + position);
		return getItem(position).hashCode();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        //Log.d(TAG + ".getView()", " - position: " + position);
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.dropdown_item, null);
        }
        TextView itemSubject = (TextView) v.findViewById(R.id.system_subject);
        ImageView imgView = (ImageView)v.findViewById(R.id.system_icon);
        if(Aplicacion.INSTANCIA != null) {
            switch(position) {
	        	case ACTION_VOTING:
	        		itemSubject.setText(Aplicacion.INSTANCIA.
	        				getSubsystemDesc(SubSystem.VOTING));
	        		imgView.setImageResource(R.drawable.poll_22);
	        		break;
	        	case ACTION_MANIFEST:
	        		itemSubject.setText(Aplicacion.INSTANCIA.
	        				getSubsystemDesc(SubSystem.MANIFESTS));
	        		imgView.setImageResource(R.drawable.manifest_22);
	        		break;
	        	case ACTION_CLAIM:
	        		itemSubject.setText(Aplicacion.INSTANCIA.
	        				getSubsystemDesc(SubSystem.CLAIMS));
	        		imgView.setImageResource(R.drawable.filenew_22);
	        		break;
	        	default:
	        		imgView.setImageResource(R.drawable.machine);
	        }	
        }
        return v;
	}

}
