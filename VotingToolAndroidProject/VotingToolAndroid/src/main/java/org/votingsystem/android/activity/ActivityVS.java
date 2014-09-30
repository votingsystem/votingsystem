package org.votingsystem.android.activity;

public interface ActivityVS {

    public void refreshingStateChanged(boolean refreshing);
    public void showMessage(int statusCode, String caption, String notificationMessage);
    public void showRefreshMessage(String message);
    public boolean isRefreshing();

}
