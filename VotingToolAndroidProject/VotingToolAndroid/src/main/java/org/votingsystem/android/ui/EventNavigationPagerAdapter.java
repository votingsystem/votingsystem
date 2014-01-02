package org.votingsystem.android.ui;

import android.app.SearchManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EventVSGridFragment;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.ChildPosition;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventNavigationPagerAdapter extends FragmentStatePagerAdapter
        implements PagerAdapterVS {

    public static final String TAG = "EventNavigationPagerAdapter";

    private GroupPosition selectedGroup = GroupPosition.VOTING;
    private ChildPosition selectedChild = GroupPosition.VOTING.getChildList().get(0);

    private String searchQuery = null;
    private ViewPager viewPager;

    public EventNavigationPagerAdapter(FragmentManager fragmentManager, ViewPager viewPager) {
        super(fragmentManager);
        this.viewPager = viewPager;
    }

    @Override public Fragment getItem(int position) {
        EventVS.State eventState = null;
        Fragment selectedFragment = new EventVSGridFragment();
        ChildPosition childPosition = selectedGroup.getChildList().get(position);
        switch(childPosition) {
            case OPEN:
                eventState = EventVS.State.ACTIVE;
                break;
            case PENDING:
                eventState = EventVS.State.AWAITING;
                break;
            case CLOSED:
                eventState = EventVS.State.TERMINATED;
                break;
        }
        Bundle args = new Bundle();
        args.putString(ContextVS.EVENT_TYPE_KEY, selectedGroup.toString());
        args.putString(ContextVS.EVENT_STATE_KEY, eventState.toString());
        args.putString(SearchManager.QUERY, searchQuery);
        selectedFragment.setArguments(args);
        Log.d(TAG + ".getItem(...) ", "childPosition: " +  childPosition +
                " - selectedGroup:" + selectedGroup + " - args: " + args);
        return selectedFragment;
    }

    /*@Override public int getItemPosition(Object item) {
        Log.d(TAG + ".getItemPosition(...) ", "");
        EventVSGridFragment eventListFragment = (EventVSGridFragment)item;
        int position = POSITION_NONE;
        switch(eventListFragment.getState()) {
            case ACTIVE: position = 0;
                break;
            case AWAITING: position = 1;
                break;
            case TERMINATED: position = 2;
                break;
        }
        Log.d(TAG + ".getItemPosition(...) ", "groupPosition: " + eventListFragment.
                getGroupPosition() + " - State:" + eventListFragment.getState() +
                " - position: " + position);
        return position;
    }*/

    public String getSelectedChildDescription(Context context) {
        switch(selectedGroup) {
            case CLAIMS:
                switch(selectedChild) {
                    case OPEN:
                        return context.getString(R.string.open_claim_lbl);
                    case PENDING:
                        return context.getString(R.string.pending_claim_lbl);
                    case CLOSED:
                        return context.getString(R.string.closed_claim_lbl);
                    default:
                        return context.getString(R.string.unknown_event_state_lbl);
                }
            case MANIFESTS:
                switch(selectedChild) {
                    case OPEN:
                        return context.getString(R.string.open_manifest_lbl);
                    case PENDING:
                        return context.getString(R.string.pending_manifest_lbl);
                    case CLOSED:
                        return context.getString(R.string.closed_manifest_lbl);
                    default:
                        return context.getString(R.string.unknown_event_state_lbl);
                }
            case VOTING:
                switch(selectedChild) {
                    case OPEN:
                        return context.getString(R.string.open_voting_lbl);
                    case PENDING:
                        return context.getString(R.string.pending_voting_lbl);
                    case CLOSED:
                        return context.getString(R.string.closed_voting_lbl);
                    default:
                        return context.getString(R.string.unknown_event_state_lbl);
                }
            default:
                return context.getString(R.string.unknown_drop_down_lbl);
        }
    }

    public String getSelectedGroupDescription(Context context) {
        return selectedGroup.getDescription(context);
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void selectItem(int groupPosition, int childPosition) {
        GroupPosition newSelectedSubsystem = GroupPosition.valueOf(groupPosition);
        if(newSelectedSubsystem != selectedGroup) {
            Log.d(TAG + ".selectItem(...) ", "from: " + selectedGroup + " - to: " +
                    newSelectedSubsystem);
            selectedGroup = newSelectedSubsystem;
            viewPager.setAdapter(this);
        }
        selectedChild = selectedGroup.getChildList().get(childPosition);
    }

    public void updateChildPosition(int childPosition) {
        selectedChild = selectedGroup.getChildList().get(childPosition);
    }

    public int getSelectedChildPosition() {
        return selectedGroup.getChildList().indexOf(selectedChild);
    }

    public int getSelectedGroupPosition() {
        return selectedGroup.getPosition();
    }

    public Drawable getLogo(Context context) {
        switch (selectedGroup) {
            case CLAIMS: return context.getResources().getDrawable(R.drawable.filenew_22);
            case MANIFESTS: return context.getResources().getDrawable(R.drawable.manifest_22);
            case VOTING: return context.getResources().getDrawable(R.drawable.poll_22);
            case REPRESENTATIVES:
                return context.getResources().getDrawable(R.drawable.system_users_22);
            default:
                return context.getResources().getDrawable(R.drawable.mail_mark_unread_22);
        }
    }

    @Override public int getCount() {
        switch(selectedGroup) {
            case CLAIMS: return GroupPosition.CLAIMS.getChildList().size();
            case MANIFESTS: return GroupPosition.MANIFESTS.getChildList().size();
            case VOTING:return GroupPosition.VOTING.getChildList().size();
            default:
                Log.d(TAG + ".getCount(...)", " system without pages: " + selectedGroup);
                return 0;
        }
    }

}