package org.labkey.ms2;

import org.labkey.ms2.client.CompareService;
import org.labkey.ms2.client.CompareResult;
import org.labkey.ms2.query.ComparePeptidesView;
import org.labkey.ms2.query.CompareProteinsView;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.TSVGridWriter;
import org.labkey.api.data.ContainerManager;

import javax.servlet.ServletException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.io.IOException;

/**
 * User: jeckels
 * Date: Jun 8, 2007
 */
public class CompareServiceImpl extends BaseRemoteService implements CompareService
{
    private final MS2Controller _controller;

    public CompareServiceImpl(ViewContext context, MS2Controller controller)
    {
        super(context);
        _controller = controller;
    }

    public CompareResult getProteinProphetComparison(String originalURL) throws Exception
    {
        ViewURLHelper url = new ViewURLHelper(originalURL);
        int runList = Integer.parseInt(url.getParameter("runList"));
        ViewContext queryContext = new ViewContext(_context);
        queryContext.setViewURLHelper(url);

        CompareProteinsView view = new CompareProteinsView(queryContext, _controller, runList, false);
        return view.createCompareResult();
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