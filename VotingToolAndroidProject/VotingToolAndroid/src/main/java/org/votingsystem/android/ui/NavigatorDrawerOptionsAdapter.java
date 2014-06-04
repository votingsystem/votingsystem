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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.votingsystem.android.R;
import org.votingsystem.model.SubSystemVS;
import org.votingsystem.model.TypeVS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class NavigatorDrawerOptionsAdapter extends BaseExpandableListAdapter {

    public static final String TAG = NavigatorDrawerOptionsAdapter.class.getSimpleName();

    public static final String GROUP_POSITION_KEY = "groupPosition";

    public static final int VOTING_GROUP_POSITION          = 0;
    public static final int MANIFESTS_GROUP_POSITION       = -1;
    public static final int CLAIMS_GROUP_POSITION          = -1;
    public static final int REPRESENTATIVES_GROUP_POSITION = 1;
    public static final int RECEIPTS_GROUP_POSITION        = 2;
    public static final int VICKETS_GROUP_POSITION         = 3;

    public static final int OPEN_CHILD_POSITION            = 0;
    public static final int PENDING_CHILD_POSITION         = 1;
    public static final int CLOSED_CHILD_POSITION          = 2;

    public static final int VICKET_USER_INFO_POSITION      = 0;
    public static final int VICKET_LIST_POSITION           = 1;

    public enum GroupPosition {
        VOTING(VOTING_GROUP_POSITION, R.drawable.poll_32, SubSystemVS.VOTES, TypeVS.VOTING_EVENT,
                Arrays.asList(ChildPosition.OPEN, ChildPosition.PENDING, ChildPosition.CLOSED)),
        MANIFESTS(MANIFESTS_GROUP_POSITION, R.drawable.manifest_32, SubSystemVS.MANIFESTS,
                TypeVS.MANIFEST_EVENT, Arrays.asList(ChildPosition.OPEN, ChildPosition.PENDING,
                ChildPosition.CLOSED)),
        CLAIMS(CLAIMS_GROUP_POSITION, R.drawable.fa_exclamation_triangle_32, SubSystemVS.CLAIMS, TypeVS.CLAIM_EVENT,
                Arrays.asList(ChildPosition.OPEN, ChildPosition.PENDING, ChildPosition.CLOSED)),
        REPRESENTATIVES(REPRESENTATIVES_GROUP_POSITION, R.drawable.system_users_32,
                SubSystemVS.REPRESENTATIVES, TypeVS.REPRESENTATIVE, new ArrayList<ChildPosition>()),
        RECEIPTS(RECEIPTS_GROUP_POSITION, R.drawable.receipt_32, SubSystemVS.RECEIPTS,
                TypeVS.RECEIPT, new ArrayList<ChildPosition>()),
        VICKETS(VICKETS_GROUP_POSITION, R.drawable.fa_money_32, SubSystemVS.VICKETS,
                TypeVS.VICKET, Arrays.asList(ChildPosition.VICKET_USER_INFO,
                ChildPosition.VICKET_LIST));

        int position;
        int logo;
        SubSystemVS subsystem;
        List<ChildPosition> childList;
        TypeVS typeVS;

        private GroupPosition(int position, int logo, SubSystemVS subsystem, TypeVS typeVS,
              List<ChildPosition> childList) {
            this.position = position;
            this.logo = logo;
            this.subsystem = subsystem;
            this.typeVS = typeVS;
            this.childList = childList;
        }
        public static GroupPosition valueOf(int position)  {
            switch (position) {
                case VOTING_GROUP_POSITION: return VOTING;
                //case MANIFESTS_GROUP_POSITION: return MANIFESTS;
                //case CLAIMS_GROUP_POSITION: return CLAIMS;
                case REPRESENTATIVES_GROUP_POSITION: return REPRESENTATIVES;
                case RECEIPTS_GROUP_POSITION: return RECEIPTS;
                case VICKETS_GROUP_POSITION: return VICKETS;
                default: return null;
            }
        }

        public List<ChildPosition> getChildList() {
            return childList;
        }

        public boolean isEmpty() {
            return childList.isEmpty();
        }

        public String getURLPart() {
            switch(this) {
                case CLAIMS: return "/eventVSClaim";
                case MANIFESTS: return "/eventVSManifest";
                case VOTING: return "/eventVSElection";
                case REPRESENTATIVES: return "/representative";
            }
            return "/eventVS";
        }

        public int getPosition() {
            return position;
        }

        public int getLogo() {
            return logo;
        }

        public SubSystemVS getSubsystem() {
            return subsystem;
        }

        public String getDescription(Context context) {
            switch(this) {
                case VOTING: return context.getString(R.string.voting_drop_down_lbl);
                case MANIFESTS: return context.getString(R.string.manifiests_drop_down_lbl);
                case CLAIMS: return context.getString(R.string.claims_drop_down_lbl);
                case REPRESENTATIVES: return context.getString(R.string.representatives_drop_down_lbl);
                case RECEIPTS: return context.getString(R.string.receipts_drop_down_lbl);
                case VICKETS: return context.getString(R.string.vickets_drop_down_lbl);
                default: return context.getString(R.string.unknown_drop_down_lbl);
            }
        }

        public TypeVS getTypeVS() {
            return typeVS;
        }

        public int getLoaderId(int childPosition){
            //enough to avoid loader id collisions while child list count < 10
            return (10 * position) + childPosition;
        }
    }

    public enum ChildPosition{OPEN(OPEN_CHILD_POSITION), PENDING(PENDING_CHILD_POSITION),
        CLOSED(CLOSED_CHILD_POSITION), VICKET_USER_INFO(VICKET_USER_INFO_POSITION),
        VICKET_LIST(VICKET_LIST_POSITION);

        //public static final int VICKET_USER_INFO_POSITION
        //public static final int VICKET_LIST_POSITION

        int position;

        private ChildPosition(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
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

	@Override public Object getChild(int groupPosition, int childPosition) {
		return this.listDataChild.get(this.listDataHeader.get(groupPosition))
				.get(childPosition);
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
		TextView txtListChild = (TextView) convertView.findViewById(R.id.lblListItem);
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
        //Log.d(TAG + ".getGroupView(...)", "isExpanded: " + isExpanded);
        LayoutInflater layoutInflater = (LayoutInflater) this.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		String headerTitle = (String) getGroup(groupPosition);
        GroupPosition gPosition = GroupPosition.valueOf(groupPosition);
        ((ExpandableListView)parent).setGroupIndicator(null);
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.drawer_list_group, null);
        }
        TextView lblListHeader = (TextView) convertView.findViewById(R.id.lblListHeader);
        lblListHeader.setText(headerTitle);
        ImageView imageView = (ImageView) convertView.findViewById(R.id.header_image);
        imageView.setImageResource(gPosition.getLogo());
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
        /*listDataHeader.add(GroupPosition.MANIFESTS.getPosition(),
                context.getString(R.string.manifiests_drop_down_lbl));
        listDataHeader.add(GroupPosition.CLAIMS.getPosition(),
                context.getString(R.string.claims_drop_down_lbl));*/
        listDataHeader.add(GroupPosition.REPRESENTATIVES.getPosition(),
                context.getString(R.string.representatives_drop_down_lbl));
        listDataHeader.add(GroupPosition.RECEIPTS.getPosition(),
                context.getString(R.string.receipts_drop_down_lbl));
        listDataHeader.add(GroupPosition.VICKETS.getPosition(),
                context.getString(R.string.vickets_drop_down_lbl));

        List<String> voting = new ArrayList<String>();
        voting.add(context.getString(R.string.open_voting_lbl));
        voting.add(context.getString(R.string.pending_voting_lbl));
        voting.add(context.getString(R.string.closed_voting_lbl));

        List<String> manifests = new ArrayList<String>();
        manifests.add(context.getString(R.string.open_manifest_lbl));
        manifests.add(context.getString(R.string.pending_manifest_lbl));
        manifests.add(context.getString(R.string.closed_manifest_lbl));

        List<String> claims = new ArrayList<String>();
        claims.add(context.getString(R.string.open_claim_lbl));
        claims.add(context.getString(R.string.pending_claim_lbl));
        claims.add(context.getString(R.string.closed_claim_lbl));

        List<String> vickets = new ArrayList<String>();
        vickets.add(context.getString(R.string.vicket_user_info_lbl));
        vickets.add(context.getString(R.string.vickets_list_lbl));

        listDataChild.put(listDataHeader.get(GroupPosition.VOTING.getPosition()), voting);
        //listDataChild.put(listDataHeader.get(GroupPosition.MANIFESTS.getPosition()), manifests);
        //listDataChild.put(listDataHeader.get(GroupPosition.CLAIMS.getPosition()), claims);
        listDataChild.put(listDataHeader.get(GroupPosition.REPRESENTATIVES.getPosition()),
                new ArrayList<String>());
        listDataChild.put(listDataHeader.get(GroupPosition.RECEIPTS.getPosition()),
                new ArrayList<String>());
        listDataChild.put(listDataHeader.get(GroupPosition.VICKETS.getPosition()), vickets);
    }

}