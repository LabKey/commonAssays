package org.labkey.microarray;

import org.labkey.api.view.JspView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.exp.ExperimentRunFilter;

/**
 * User: jeckels
 * Date: Feb 7, 2008
 */
public class MicroarrayStatisticsView extends JspView<MicroarrayStatisticsView.MicroarraySummaryBean>
{
    public static class MicroarraySummaryBean
    {
        private final int _runCount;

        public MicroarraySummaryBean(int runCount)
        {
            _runCount = runCount;
        }

        public int getRunCount()
        {
            return _runCount;
        }
    }

    public MicroarrayStatisticsView(ViewContext ctx)
    {
        super("/org/labkey/microarray/statistics.jsp");

        ExperimentRunFilter filter = MicroarrayRunFilter.INSTANCE;
        int runCount = filter.getRunCount(ctx.getUser(), ctx.getContainer());

        setModelBean(new MicroarraySummaryBean(runCount));
    }
}
