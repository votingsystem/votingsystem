/*
 * Copyright 2013 - Jose. J. García Zornoza
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.votingsystem.android.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.SubSystemVS;
import org.votingsystem.model.TypeVS;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class NavigatorDrawerOptionsAdapter extends BaseExpandableListAdapter {

    public static final String TAG = "NavigatorDrawerOptionsAdapter";
    public static final String GROUP_POSITION_KEY = "groupPosition";


    public enum GroupPosition {
        VOTING(0, SubSystemVS.VOTES, EnumSet.of(
            ChildPosition.OPEN, ChildPosition.PENDING, ChildPosition.CLOSED)),
        MANIFESTS(1, SubSystemVS.MANIFESTS, EnumSet.of(
                ChildPosition.OPEN, ChildPosition.PENDING, ChildPosition.CLOSED)),
        CLAIMS(2, SubSystemVS.CLAIMS, EnumSet.of(
                ChildPosition.OPEN, ChildPosition.PENDING, ChildPosition.CLOSED)),
        REPRESENTATIVES(3, SubSystemVS.REPRESENTATIVES,  EnumSet.of(
                ChildPosition.REPRESENTATIVE_LIST, ChildPosition.REPRESENTATIVE_OPERATION));
        int position;
        SubSystemVS subsystem;
        Set<ChildPosition> childSet;

        private GroupPosition(int position, SubSystemVS subsystem, Set<ChildPosition> childSet) {
            this.position = position;
            this.subsystem = subsystem;
            this.childSet = childSet;
        }
        public static GroupPosition valueOf(int position)  {
            switch (position) {
                case 0: return VOTING;
                case 1: return MANIFESTS;
                case 2: return CLAIMS;
                case 3: return REPRESENTATIVES;
                default: return null;
            }
        }

        public Set<ChildPosition> getChildSet() {
            return childSet;
        }

        public String getURLPart() {
            switch(this) {
                case CLAIMS: return "/eventVSClaim";
                case MANIFESTS: return "/eventVSManifest";
                case VOTING: return "/eventVSElection";
            }
            return "/eventVS";
        }

        public int getPosition() {
            return position;
        }

        public SubSystemVS getSubsystem() {
            return subsystem;
        }

        public String getDescription(Context context) {
            switch(this) {
                case VOTING:
                    return context.getString(R.string.voting_drop_down_lbl);
                case MANIFESTS:
                    return context.getString(R.string.manifiests_drop_down_lbl);
                case CLAIMS:
                    return context.getString(R.string.claims_drop_down_lbl);
                case REPRESENTATIVES:
                    return ContextVS.getMessage("representativeListOptionLbl");
                default:
                    return context.getString(R.string.unknown_drop_down_lbl);
            }
        }

    }

    public enum ChildPosition{ OPEN(0), PENDING(1), CLOSED(2), REPRESENTATIVE_LIST(0),
        REPRESENTATIVE_OPERATION(1);

        private int position;

        private ChildPosition(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }

        public static ChildPosition getEventPosition(int position) {
            switch (position) {
                case 0: return OPEN;
                case 1: return PENDING;
                case 2: return CLOSED;
                default: return null;
            }
        }

        public static ChildPosition getRepresentativePosition(int position) {
            switch (position) {
                case 0: return REPRESENTATIVE_LIST;
                case 1: return REPRESENTATIVE_OPERATION;
                default: return null;
            }
        }
    }

	private Context context;
	private List<String> listDataHeader; // header titles
	// child data in format of header title, child title
	private HashMap<String, List<String>> listDataChild;

	public NavigatorDrawerOptionsAdapter(Context context) {
		this.context = context;
        initListData();
	}

	@Override public Object getChild(int groupPosition, int childPosititon) {
		return this.listDataChild.get(this.listDataHeader.get(groupPosition))
				.get(childPosititon);
	}

	@Override public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override public View getChildView(int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {

		final String childText = (String) getChild(groupPosition, childPosition);

		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) this.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.drawer_list_item, null);
		}

		TextView txtListChild = (TextView) convertView
				.findViewById(R.id.lblListItem);

		txtListChild.setText(childText);
		return convertView;
	}

	@Override public int getChildrenCount(int groupPosition) {
		return this.listDataChild.get(this.listDataHeader.get(groupPosition)).size();
	}

	@Override public Object getGroup(int groupPosition) {
		return this.listDataHeader.get(groupPosition);
	}

	@Override public int getGroupCount() {
		return this.listDataHeader.size();
	}

	@Override public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
        Log.d(TAG + ".getGroupView(...)", "isExpanded: " + isExpanded);
		String headerTitle = (String) getGroup(groupPosition);
		if (convertView == null) {
			LayoutInflater layoutInflater = (LayoutInflater) this.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = layoutInflater.inflate(R.layout.drawer_list_group, null);
		}

		TextView lblListHeader = (TextView) convertView
				.findViewById(R.id.lblListHeader);
		lblListHeader.setTypeface(null, Typeface.BOLD);
		lblListHeader.setText(headerTitle);
        /*ImageView imageView = (ImageView)convertView.findViewById(R.id.headerIcon);
        if(isExpanded) {
            imageView.setImageResource(R.drawable.bullet_toggle_minus);
        } else imageView.setImageResource(R.drawable.bullet_toggle_plus);*/
		return convertView;
	}

	@Override public boolean hasStableIds() {
		return false;
	}

	@Override public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

    private void initListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        listDataHeader.add(GroupPosition.VOTING.getPosition(),
                context.getString(R.string.voting_drop_down_lbl));
        listDataHeader.add(GroupPosition.MANIFESTS.getPosition(),
                context.getString(R.string.manifiests_drop_down_lbl));
        listDataHeader.add(GroupPosition.CLAIMS.getPosition(),
                context.getString(R.string.claims_drop_down_lbl));
        listDataHeader.add(GroupPosition.REPRESENTATIVES.getPosition(),
                ContextVS.getMessage("representativeNavDrawerLbl"));

        List<String> voting = new ArrayList<String>();
        voting.add(ChildPosition.OPEN.getPosition(),
                context.getString(R.string.open_voting_lbl));
        voting.add(ChildPosition.PENDING.getPosition(),
                context.getString(R.string.pending_voting_lbl));
        voting.add(ChildPosition.CLOSED.getPosition(),
                context.getString(R.string.closed_voting_lbl));

        List<String> manifests = new ArrayList<String>();
        manifests.add(ChildPosition.OPEN.getPosition(),
                context.getString(R.string.open_manifest_lbl));
        manifests.add(ChildPosition.PENDING.getPosition(),
                context.getString(R.string.pending_manifest_lbl));
        manifests.add(ChildPosition.CLOSED.getPosition(),
                context.getString(R.string.closed_manifest_lbl));

        List<String> claims = new ArrayList<String>();
        claims.add(ChildPosition.OPEN.getPosition(),
                context.getString(R.string.open_claim_lbl));
        claims.add(ChildPosition.PENDING.getPosition(),
                context.getString(R.string.pending_claim_lbl));
        claims.add(ChildPosition.CLOSED.getPosition(),
                context.getString(R.string.closed_claim_lbl));

        List<String> representatives = new ArrayList<String>();
        representatives.add(ChildPosition.REPRESENTATIVE_LIST.getPosition(),
                ContextVS.getMessage("representativeListOptionLbl"));
        representatives.add(ChildPosition.REPRESENTATIVE_OPERATION.getPosition(),
                ContextVS.getMessage("representativeOperationsOptionLbl"));

        listDataChild.put(listDataHeader.get(GroupPosition.VOTING.getPosition()), voting);
        listDataChild.put(listDataHeader.get(GroupPosition.MANIFESTS.getPosition()), manifests);
        listDataChild.put(listDataHeader.get(GroupPosition.CLAIMS.getPosition()), claims);
        listDataChild.put(listDataHeader.get(GroupPosition.REPRESENTATIVES.getPosition()), representatives);
    }

}