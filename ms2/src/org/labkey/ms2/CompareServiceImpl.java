package org.labkey.ms2;

import org.labkey.ms2.client.CompareService;
import org.labkey.ms2.client.CompareResult;
import org.labkey.ms2.query.CompareProteinsView;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ActionURL;
import org.apache.log4j.Logger;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public class CompareServiceImpl extends BaseRemoteService implements CompareService
{
    private static Logger _log = Logger.getLogger(CompareServiceImpl.class);

    public CompareServiceImpl(ViewContext context)
    {
        super(context);
    }

    public CompareResult getProteinProphetComparison(String originalURL) throws Exception
    {
        try
        {
            ActionURL url = new ActionURL(originalURL);
            int runList = Integer.parseInt(url.getParameter("runList"));
            String viewName = url.getParameter(MS2Controller.PEPTIDES_FILTER_VIEW_NAME);
            ViewContext queryContext = new ViewContext(_context);
            queryContext.setActionURL(url);

            CompareProteinsView view = new CompareProteinsView(queryContext, runList, false, viewName);
            return view.createCompareResult();
        }
        catch (Exception e)
        {
            _log.error("Problem generating comparison", e);
            throw e;
        }
    }

    public CompareResult getPeptideComparison(String originalURL) throws Exception
    {
/*        ActionURL url = new ActionURL(originalURL);
        int runList = Integer.parseInt(url.getParameter("runList"));
        String viewName = url.getParameter(MS2Controller..COMPARE_PEPTIDES_PEPTIDES_FILTER + "." + QueryParam.viewName.toString());
        ViewContext queryContext = new ViewContext(_context);
        queryContext.setActionURL(url);

        ComparePeptidesView view = new ComparePeptidesView(queryContext, _controller, runList, false, viewName);
        return view.createCompareResult();
        */
        throw new UnsupportedOperationException();
    }
}