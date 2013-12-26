package org.votingsystem.android.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public interface PagerAdapterVS {

    public void updateChildPosition(int position);
    public void selectItem(int groupPosition, int childPosition);
    public int getSelectedGroupPosition();
    public int getSelectedChildPosition();
    public String getSelectedGroupDescription(Context context);
    public String getSelectedChildDescription(Context context);
    public Drawable getLogo(Context context);

}
