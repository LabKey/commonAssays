package org.labkey.ms1;

import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ViewContext;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.DataRegion;

/**
 * Implements a simple, flat QueryView over the Scan/Peaks data
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 5, 2007
 * Time: 11:04:01 AM
 */
public class PeaksView extends QueryView
{
    //Localizable strings
    private static final String CAPTION_EXPORT_ALL_EXCEL = "Export All to Excel";
    private static final String CAPTION_EXPORT_ALL_TSV = "Export All to Text";
    private static final String CAPTION_PRINT_ALL = "Print";

    public PeaksView(ViewContext ctx, MS1Schema schema, int runId, int scan)
    {
        super(schema);
        _schema = schema;
        _runId = runId;
        _scan = scan;

        //NOTE: The use of QueryView.DATAREGIONNAME_DEFAULT is essential here!
        //the export/print buttons that I will add later use the query controller's
        //actions, and those expect that the sorts/filters use the default data region name.
        QuerySettings settings = new QuerySettings(ctx.getViewURLHelper(), ctx.getRequest(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setQueryName(MS1Schema.TABLE_PEAKS);
        settings.setAllowChooseQuery(false);
        setSettings(settings);

        ExpRun run = ExperimentService.get().getExpRun(runId);
        setTitle("Peaks from Scan " + _scan + " from " + run.getName());
        setShowCustomizeViewLinkInButtonBar(true);
        setShowRecordSelectors(false);
        setShowExportButtons(false);
    }

    protected TableInfo createTable()
    {
        assert null != _schema : "MS1 Schema was not set in PeaksView class!";

        PeaksTableInfo tinfo = _schema.getPeaksTableInfo();
        if(_runId >= 0 && _scan >= 0)
            tinfo.addScanCondition(_runId, _scan);
        return tinfo;
    }

    /**
     * Overridden to customize the data region.
     * @return A customized DataRegion
     */
    protected DataRegion createDataRegion()
    {
        DataRegion region = super.createDataRegion();
        region.setShadeAlternatingRows(true);
        return region;
    }

    private MS1Schema _schema;
    private int _runId = -1;
    private int _scan = -1;
} //class PeaksView

