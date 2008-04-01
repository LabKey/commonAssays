package org.labkey.ms1;

import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.gwt.client.model.GWTComparisonResult;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.ms1.client.CompareService;
import org.labkey.ms1.view.CompareRunsView;
import org.labkey.ms1.query.MS1Schema;
import org.apache.log4j.Logger;

/**
 * User: jeckels
 * Date: Mar 24, 2008
 */
public class CompareServiceImpl extends BaseRemoteService implements CompareService
{
    private static Logger _log = Logger.getLogger(CompareServiceImpl.class);

    public CompareServiceImpl(ViewContext context)
    {
        super(context);
    }
    
    public GWTComparisonResult getFeaturesByPeptideComparison(String originalURL) throws Exception
    {
        try
        {
            ActionURL url = new ActionURL(originalURL);

            int[] runIds = PageFlowUtil.toInts(url.getParameter("runIds").split(","));

            return new CompareRunsView(new MS1Schema(getUser(), _context.getContainer()), runIds).createComparisonResult();
        }
        catch (Exception e)
        {
            _log.error("Problem generating comparison", e);
            throw e;
        }
    }
    
}
