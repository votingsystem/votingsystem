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
import android.view.View;

import org.votingsystem.android.fragment.EventListFragment;
import org.votingsystem.android.R;
import org.votingsystem.model.EventVS;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.ChildPosition;
import org.votingsystem.android.ui.NavigatorDrawerOptionsAdapter.GroupPosition;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class EventNavigationPagerAdapter extends FragmentStatePagerAdapter implements PagerAdapterVS {

    public static final String TAG = "EventNavigationPagerAdapter";

    private ChildPosition selectedChild = ChildPosition.OPEN;
    private GroupPosition selectedSubsystem = GroupPosition.VOTING;

    private Fragment openEventsFragment;
    private Fragment pendingEventsFragment;
    private Fragment closedEventsFragment;

    private String searchQuery = null;

    private Fragment representativeListFragment;
    private FragmentManager fragmentManager;
    private ViewPager viewPager;
    //private Fragment operationsFragment;

    public EventNavigationPagerAdapter(FragmentManager fragmentManager, ViewPager viewPager) {
        super(fragmentManager);
        this.fragmentManager = fragmentManager;
        this.viewPager = viewPager;
    }

    @Override public Fragment getItem(int position) {
        EventVS.State eventState = null;
        Fragment selectedFragment = null;
        ChildPosition childPosition = ChildPosition.getEventPosition(position);
        switch(childPosition) {
            case OPEN:
                eventState = EventVS.State.ACTIVE;
                //selectedFragment = fragmentManager.findFragmentByTag(ChildPosition.OPEN.toString());
                if(selectedFragment == null) {
                    selectedFragment = new EventListFragment();
                    //fragmentManager.beginTransaction().add(selectedFragment, ChildPosition.OPEN.toString()).commit();
                    Log.d(TAG + ".getItem(...) ", "created new OPEN EventListFragment - id: " + selectedFragment.getId());
                }
                /*if(openEventsFragment != null) selectedFragment = openEventsFragment;
                else {
                    Log.d(TAG + ".getItem(...) ", "created new OPEN EventListFragment");
                    openEventsFragment = new EventListFragment();
                    selectedFragment = openEventsFragment;
                }*/
                break;
            case PENDING:
                eventState = EventVS.State.AWAITING;
                //selectedFragment = fragmentManager.findFragmentByTag(ChildPosition.PENDING.toString());
                if(selectedFragment == null) {

                    selectedFragment = new EventListFragment();
                    Log.d(TAG + ".getItem(...) ", "created new PENDING EventListFragment - id: " + selectedFragment.getId());
                    //fragmentManager.beginTransaction().add(selectedFragment, ChildPosition.PENDING.toString()).commit();
                }
                /*if(pendingEventsFragment != null) selectedFragment = pendingEventsFragment;
                else {
                    Log.d(TAG + ".getItem(...) ", "created new AWAITING EventListFragment");
                    pendingEventsFragment = new EventListFragment();
                    selectedFragment = pendingEventsFragment;
                }*/
                break;
            case CLOSED:
                eventState = EventVS.State.TERMINATED;
                //selectedFragment = fragmentManager.findFragmentByTag(ChildPosition.CLOSED.toString());
                if(selectedFragment == null) {

                    selectedFragment = new EventListFragment();
                    //  fragmentManager.beginTransaction().add(selectedFragment, ChildPosition.CLOSED.toString()).commit();
                    Log.d(TAG + ".getItem(...) ", "created new CLOSED EventListFragment - id: " + selectedFragment.getId());
                }
                /*if(closedEventsFragment != null) selectedFragment = closedEventsFragment;
                else {
                    Log.d(TAG + ".getItem(...) ", "created new TERMINATED EventListFragment");
                    closedEventsFragment = new EventListFragment();
                    selectedFragment = closedEventsFragment;
                }*/
                break;
        }
        Bundle args = new Bundle();
        args.putString(EventListFragment.EVENT_TYPE_KEY, selectedSubsystem.toString());
        args.putString(EventListFragment.EVENT_STATE_KEY, eventState.toString());
        args.putString(SearchManager.QUERY, searchQuery);
        selectedFragment.setArguments(args);
        Log.d(TAG + ".getItem(...) ", "childPosition: " +  childPosition +
                " - selectedSubsystem:" + selectedSubsystem + " - args: " + args);
        return selectedFragment;
    }

    public String getSelectedChildDescription(Context context) {
        switch(selectedSubsystem) {
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
        return selectedSubsystem.getDescription(context);
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public void selectItem(int groupPosition, int childPosition) {
        GroupPosition newSelectedSubsystem = GroupPosition.valueOf(groupPosition);
        if(newSelectedSubsystem != selectedSubsystem) {
            Log.d(TAG + ".selectItem(...) ", "from: " + selectedSubsystem + " - to: " +
                    newSelectedSubsystem);
            selectedSubsystem = newSelectedSubsystem;
            viewPager.setAdapter(this);
        }
        selectedChild = ChildPosition.getEventPosition(childPosition);
    }

    public void updateChildPosition(int childPosition) {
        selectedChild = ChildPosition.getEventPosition(childPosition);
    }

    public int getSelectedChildPosition() {
        return selectedChild.getPosition();
    }

    public int getSelectedGroupPosition() {
        return selectedSubsystem.getPosition();
    }

    public Drawable getLogo(Context context) {
        switch (selectedSubsystem) {
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
        switch(selectedSubsystem) {
            case CLAIMS: return GroupPosition.CLAIMS.getChildSet().size();
            case MANIFESTS: return GroupPosition.MANIFESTS.getChildSet().size();
            case VOTING:return GroupPosition.VOTING.getChildSet().size();
            default:
                Log.d(TAG + ".getCount(...)", " system without pages: " + selectedSubsystem);
                return 0;
        }
    }

}