package org.votingsystem.android.ui;

import android.graphics.drawable.Drawable;

import org.votingsystem.android.AppContextVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface PagerAdapterVS {

    public void updateChildPosition(int position);
    public void selectItem(Integer groupPosition, Integer childPosition);
    public int getSelectedGroupPosition();
    public int getSelectedChildPosition();
    public String getSelectedGroupDescription(AppContextVS context);
    public String getSelectedChildDescription(AppContextVS context);
    public Drawable getLogo(AppContextVS context);

}
