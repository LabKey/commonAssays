package org.labkey.ms1;

import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ActionButton;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;

/**
 * Implements a query view over the features data, allows filtering for features from a specific run
 *
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Oct 3, 2007
 * Time: 11:27:01 AM
 */
public class FeaturesView extends QueryView
{
    //Localizable strings
    private static final String CAPTION_EXPORT_ALL_EXCEL = "Export All to Excel";
    private static final String CAPTION_EXPORT_ALL_TSV = "Export All to Text";
    private static final String CAPTION_PRINT_ALL = "Print";

    /**
     * Constructs a new FeaturesView given a ViewContext and the experiment run id. The view
     * will automatically filter the list of Features to just those belonging to the specified run.
     * @param ctx       The view context
     * @param schema    The MS1Schema to use
     * @param runId     The id of the experiment run
     * @param peaksAvailable Pass true if peak data is available
     */
    public FeaturesView(ViewContext ctx, MS1Schema schema, int runId, boolean peaksAvailable)
    {
        super(schema);
        _ms1Schema = schema;
        _runId = runId;
        _peaksAvaialble = peaksAvailable;

        //NOTE: The use of QueryView.DATAREGIONNAME_DEFAULT is essential here!
        //the export/print buttons that I will add later use the query controller's
        //actions, and those expect that the sorts/filters use the default data region name.
        QuerySettings settings = new QuerySettings(ctx.getViewURLHelper(), ctx.getRequest(), QueryView.DATAREGIONNAME_DEFAULT);
        settings.setQueryName(MS1Schema.TABLE_FEATURES);
        settings.setAllowChooseQuery(false);
        setSettings(settings);

        ExpRun run = ExperimentService.get().getExpRun(runId);
        setTitle("MS1 Features from " + (null != run ? run.getName() : "(Invalid Run)"));
        setShowCustomizeViewLinkInButtonBar(true);
        setShowRecordSelectors(false);
        setShowExportButtons(false);
    }

    /**
     * Overridden to add the run id filter condition to the features TableInfo.
     * @return A features TableInfo filtered to the current run id
     */
    protected TableInfo createTable()
    {
        assert null != _ms1Schema : "MS1 Schema was not set in FeaturesView class!";

        FeaturesTableInfo tinfo = _ms1Schema.getFeaturesTableInfo();
        if(_runId >= 0)
            tinfo.addRunIdCondition(_runId, getContainer(), _peaksAvaialble);
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
     * substitute our runId filter paramter with the equivalent query filter, as well as add
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
        url.addParameter("query.queryName", MS1Schema.TABLE_FEATURES);
        url.addParameter("schemaName", MS1Schema.SCHEMA_NAME);

        //replace our runId paramter with a query filter on the run table's RowId column
        url.deleteParameter("runId");
        url.addParameter("query.FileId/ExpDataFileId/Run/RowId~eq", _runId);

        ActionButton btn = new ActionButton(url.getEncodedLocalURIString(), caption, DataRegion.MODE_ALL, ActionButton.Action.LINK);
        bar.add(btn);
    } //addQueryActionButton()

    //Data members
    private MS1Schema _ms1Schema;
    private int _runId = -1;
    private boolean _peaksAvaialble = false;
} //class FeaturesView
