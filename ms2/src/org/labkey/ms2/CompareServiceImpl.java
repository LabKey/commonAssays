package org.labkey.ms2;

import org.labkey.ms2.client.CompareService;
import org.labkey.ms2.client.CompareResult;
import org.labkey.ms2.query.ComparePeptidesView;
import org.labkey.ms2.query.CompareProteinsView;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.apache.log4j.Logger;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public class CompareServiceImpl extends BaseRemoteService implements CompareService
{
    private static Logger _log = Logger.getLogger(CompareServiceImpl.class);
    private final OldMS2Controller _controller;

    public CompareServiceImpl(ViewContext context, OldMS2Controller controller)
    {
        super(context);
        _controller = controller;
    }

    public CompareResult getProteinProphetComparison(String originalURL) throws Exception
    {
        try
        {
            ViewURLHelper url = new ViewURLHelper(originalURL);
            int runList = Integer.parseInt(url.getParameter("runList"));
            ViewContext queryContext = new ViewContext(_context);
            queryContext.setViewURLHelper(url);

            CompareProteinsView view = new CompareProteinsView(queryContext, _controller, runList, false);
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
        ViewURLHelper url = new ViewURLHelper(originalURL);
        int runList = Integer.parseInt(url.getParameter("runList"));
        ViewContext queryContext = new ViewContext(_context);
        queryContext.setViewURLHelper(url);

        ComparePeptidesView view = new ComparePeptidesView(queryContext, _controller, runList, false);
        return view.createCompareResult();
    }
}