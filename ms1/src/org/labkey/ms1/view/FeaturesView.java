package org.labkey.ms1.view;

import org.labkey.api.data.*;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.ms1.query.FeaturesTableInfo;
import org.labkey.ms1.query.MS1Schema;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

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
     * @param forExport Pass true if this is being created for export to excel/tsv/print
     */
    public FeaturesView(ViewContext ctx, MS1Schema schema, int runId, boolean peaksAvailable, boolean forExport)
    {
        super(schema);
        _ms1Schema = schema;
        _runId = runId;
        _peaksAvailable = peaksAvailable;
        _forExport = forExport;

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

    public int[] getPrevNextFeature(int featureIdCur) throws SQLException, IOException
    {
        ResultSet rs = null;
        int prevFeatureId = -1;
        int nextFeatureId = -1;
        try
        {
            rs = createDataRegion().getResultSet(new RenderContext(getViewContext()));
            while(rs.next())
            {
                if(rs.getInt("FeatureId") == featureIdCur)
                {
                    if(rs.next())
                        nextFeatureId = rs.getInt("FeatureId");
                    break;
                }

                prevFeatureId = rs.getInt("FeatureId");
            }

            return new int[] {prevFeatureId, nextFeatureId};
        }
        finally
        {
            ResultSetUtil.close(rs);
        }
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
            tinfo.addRunIdCondition(_runId, getContainer(), getViewContext().getViewURLHelper(), _peaksAvailable, _forExport);
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

        //if this is for export, remove the details and peaks links
        if(_forExport)
            region.removeColumnsFromDisplayColumnList(FeaturesTableInfo.COLUMN_DETAILS_LINK, FeaturesTableInfo.COLUMN_PEAKS_LINK);
        return region;
    }

    /**
     * Overridden to create a customized data view.
     * @return A customized DataView
     */
    protected DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setBaseSort(new Sort("Scan"));
        DataRegion region = view.getDataRegion();

        //Since this code calls getDataRegion() on the newly created view, you'd *think* that
        //this could all be done in the overidden createDataRegion() method, but it can't for some reason.
        //the button bar returned from DataRegion.getButtonBar() during createDataRegion()
        //is unmodifiable. It only becomes modifiable after the call to QueryView.createDataView().
        if(region.getButtonBarPosition() != DataRegion.ButtonBarPosition.NONE)
        {
            ButtonBar bar = region.getButtonBar(DataRegion.MODE_GRID);
            assert null != bar : "Coun't get the button bar during FeaturesView.createDataView()!";

            addExportButton(bar, "excel", CAPTION_EXPORT_ALL_EXCEL);
            addExportButton(bar, "tsv", CAPTION_EXPORT_ALL_TSV);
            addExportButton(bar, "print", CAPTION_PRINT_ALL);
        }

        return view;
    } //createDataView()

    protected void addExportButton(ButtonBar bar, String format, String caption)
    {
        ViewURLHelper url = getViewContext().getViewURLHelper().clone();
        url.replaceParameter("export", format);
        bar.add(new ActionButton(url.getEncodedLocalURIString(), caption, DataRegion.MODE_ALL, ActionButton.Action.LINK));
    }


    //Data members
    private MS1Schema _ms1Schema;
    private int _runId = -1;
    private boolean _peaksAvailable = false;
    private boolean _forExport = false;
} //class FeaturesView
