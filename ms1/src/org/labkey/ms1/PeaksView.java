package org.labkey.ms1;

import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.DataView;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ActionButton;

import java.sql.SQLException;

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

    public PeaksView(ViewContext ctx, MS1Schema schema, int runId, int scan, int scanId) throws SQLException
    {
        super(schema);
        _schema = schema;
        _scanId = scanId;

        //NOTE: The use of QueryView.DATAREGIONNAME_DEFAULT is essential here!
        //the export/print buttons that I will add later use the query controller's
        //actions, and those expect that the sorts/filters use the default data region name.
        QuerySettings settings = new QuerySettings(ctx.getViewURLHelper(), ctx.getRequest(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setQueryName(MS1Schema.TABLE_PEAKS);
        settings.setAllowChooseQuery(false);
        setSettings(settings);

        ExpRun run = ExperimentService.get().getExpRun(runId);
        if(null != run)
            setTitle("Peaks from Scan " + scan + " from " + run.getName());
        else
            setTitle("Peaks from Scan " + scan);
        

        setShowCustomizeViewLinkInButtonBar(true);
        setShowRecordSelectors(false);
        setShowExportButtons(false);
    }

    protected TableInfo createTable()
    {
        assert null != _schema : "MS1 Schema was not set in PeaksView class!";

        PeaksTableInfo tinfo = _schema.getPeaksTableInfo();
        if(null != _scanId)
            tinfo.addScanCondition(_scanId.intValue());
        else
            tinfo.addScanCondition(-1);
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

    /**
     * Overridden to create a customized data view.
     * @return A customized DataView
     */
    protected DataView createDataView()
    {
        DataView view = super.createDataView();
        DataRegion region = view.getDataRegion();

        //Since this code calls getDataRegion() on the newly created view, you'd *think* that
        //this could all be done in the overidden createDataRegion() method, but it can't for some reason.
        //the button bar returned from DataRegion.getButtonBar() during createDataRegion()
        //is unmodifiable. It only becomes modifiable after the call to QueryView.createDataView().
        if(region.getButtonBarPosition() != DataRegion.ButtonBarPosition.NONE)
        {
            ButtonBar bar = region.getButtonBar(DataRegion.MODE_GRID);
            assert null != bar : "Coun't get the button bar during FeaturesView.createDataView()!";

            addQueryActionButton(bar, "exportRowsExcel", CAPTION_EXPORT_ALL_EXCEL);
            addQueryActionButton(bar, "exportRowsTsv", CAPTION_EXPORT_ALL_TSV);
            addQueryActionButton(bar, "printRows", CAPTION_PRINT_ALL);
        }

        return view;
    } //createDataView()

    /**
     * Adds a button to the bar that will invoke a query controller action. This will automatically
     * substitute our scanId filter paramter with the equivalent query filter, as well as add
     * other parameters needed by query (e.g., schema and query name).
     * @param bar       The button bar
     * @param action    The query controller action name
     * @param caption   The caption of the new button
     */
    protected void addQueryActionButton(ButtonBar bar, String action, String caption)
    {
        ViewURLHelper url = getViewContext().getViewURLHelper().clone();
        url.setPageFlow("query");
        url.setAction(action);

        //add the parameters the query action will need
        url.addParameter("query.queryName", MS1Schema.TABLE_PEAKS);
        url.addParameter("schemaName", MS1Schema.SCHEMA_NAME);

        //replace our runId paramter with a query filter on the run table's RowId column
        url.deleteParameter("runId");
        url.deleteParameter("scan");
        if(null != _scanId)
            url.addParameter("query.ScanId~eq", _scanId.intValue());

        ActionButton btn = new ActionButton(url.getEncodedLocalURIString(), caption, DataRegion.MODE_ALL, ActionButton.Action.LINK);
        bar.add(btn);
    } //addQueryActionButton()

    private MS1Schema _schema;
    private Integer _scanId = null;
} //class PeaksView

