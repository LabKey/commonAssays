package org.labkey.ms2;

import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.view.GroovyView;
import org.labkey.api.view.ViewURLHelper;

import java.sql.SQLException;
import java.util.Map;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class MS2StatsWebPart extends GroovyView
{
    public MS2StatsWebPart()
    {
        super("/org/labkey/ms2/stats.gm");
        setTitle("MS2 Statistics");
        Map stats;
        try
        {
            stats = MS2Manager.getBasicStats();
        } catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        addObject("stats", stats);
    }


    @Override
    protected void prepareWebPart(Object model)
    {
        if (!getViewContext().getUser().isGuest())
            setTitleHref(ViewURLHelper.toPathString("MS2", "exportHistory", ""));
    }
}
