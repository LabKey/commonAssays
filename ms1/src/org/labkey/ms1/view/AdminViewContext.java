package org.labkey.ms1.view;

import org.labkey.api.view.ViewURLHelper;

/**
 * Context object for AdminView.jsp
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Nov 1, 2007
 * Time: 11:10:05 AM
 */
public class AdminViewContext
{
    private int _numDeleted = 0;
    private ViewURLHelper _purgeNowUrl;
    private boolean _purgeRunning = false;

    public AdminViewContext(int numDeleted)
    {
        _numDeleted = numDeleted;
        _purgeNowUrl = new ViewURLHelper("ms1", "showAdmin.view", "");
        _purgeNowUrl.addParameter("purgeNow", "true");
    }

    public int getNumDeleted()
    {
        return _numDeleted;
    }

    public String getPurgeNowUrl()
    {
        return _purgeNowUrl.getLocalURIString();
    }

    public boolean isPurgeRunning()
    {
        return _purgeRunning;
    }

    public void setPurgeRunning(boolean purgeRunning)
    {
        _purgeRunning = purgeRunning;
    }
}
