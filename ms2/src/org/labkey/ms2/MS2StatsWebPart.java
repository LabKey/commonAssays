package org.labkey.ms2;

import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewURLHelper;

import java.sql.SQLException;
import java.util.Map;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class MS2StatsWebPart extends JspView<MS2StatsWebPart.StatsBean>
{
    public MS2StatsWebPart()
    {
        super("/org/labkey/ms2/stats.jsp", new StatsBean());
        setTitle("MS2 Statistics");

        if (!getViewContext().getUser().isGuest())
            setTitleHref(ViewURLHelper.toPathString("MS2", "exportHistory", ""));
    }


    public static class StatsBean
    {
        public String runs;
        public String peptides;

        private StatsBean()
        {
            try
            {
                Map<String, String> stats = MS2Manager.getBasicStats();
                runs = stats.get("Runs");
                peptides = stats.get("Peptides");
            }
            catch (SQLException e)
            {
                throw new RuntimeSQLException(e);
            }
        }
    }
}
