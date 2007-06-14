package org.labkey.ms2;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.labkey.ms2.client.CompareService;
import org.labkey.ms2.client.CompareResult;
import org.labkey.ms2.query.MS2Schema;
import org.labkey.ms2.query.CompareProteinsView;
import org.labkey.api.gwt.server.BaseRemoteService;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.FieldKey;
import org.labkey.api.data.TSVGridWriter;

import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

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

    public CompareResult getComparison(String originalURL) throws Exception
    {
        ViewURLHelper url = new ViewURLHelper(originalURL);
        QuerySettings settings = new QuerySettings(url, _context.getRequest(), "Compare");
        settings.setQueryName(MS2Schema.COMPARE_PROTEIN_PROPHET_TABLE_NAME);
        settings.setAllowChooseQuery(false);
        List<String> errors = new ArrayList<String>();
        int runList = Integer.parseInt(url.getParameter("runList"));
        List<MS2Run> runs = _controller.getCachedRuns(runList, errors);
        ViewContext queryContext = new ViewContext(_context);
        queryContext.setViewURLHelper(url);
        CompareProteinsView view = new CompareProteinsView(queryContext, new MS2Schema(getUser(), getContainer()), settings, runs, runList);
        view.setViewContext((ViewContext)view.getModel());
        List<FieldKey> cols = new ArrayList<FieldKey>();
        cols.add(FieldKey.fromParts("SeqId"));
        cols.add(FieldKey.fromParts("Run", "RowId"));
        view.setColumns(cols);

        StringBuilder sb = new StringBuilder();
        TSVGridWriter tsvWriter = view.getTsvWriter();
        tsvWriter.setCaptionRowVisible(false);
        tsvWriter.write(sb);

        StringTokenizer lines = new StringTokenizer(sb.toString(), "\n");
        int proteinCount = lines.countTokens();
        String[] proteins = new String[proteinCount];
        String[] runNames = new String[runs.size()];
        boolean[][] hits = new boolean[proteinCount][];

        int runIndex = 0;
        for (MS2Run run : runs)
        {
            runNames[runIndex++] = run.getDescription();
        }

        int index = 0;
        while (lines.hasMoreTokens())
        {
            String line = lines.nextToken();
            String[] values = line.split("\\t");
            proteins[index] = values[0];
            hits[index] = new boolean[runs.size()];
            for (int i = 0; i < runs.size(); i++)
            {
                hits[index][i] = !"".equals(values[i + 1].trim());
            }
            index++;
        }
        return new CompareResult(proteins, runNames, hits);
    }
}