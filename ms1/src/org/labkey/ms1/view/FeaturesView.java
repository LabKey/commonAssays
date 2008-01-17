package org.labkey.ms1.view;

import org.labkey.api.data.*;
import org.labkey.api.query.QueryView;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.ViewContext;
import org.labkey.ms1.query.*;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    public static final String DATAREGION_NAME = "features";

    //Localizable strings
    private static final String CAPTION_EXPORT = "Export";
    private static final String CAPTION_EXPORT_ALL_EXCEL = "Export All to Excel (.xls)";
    private static final String CAPTION_EXPORT_ALL_TSV = "Export All to Text (.txt)";
    private static final String CAPTION_PRINT_ALL = "Print";

    private List<FeaturesFilter> _baseFilters = null;
    private MS1Schema _ms1Schema = null;
    private boolean _forExport = false;

    public FeaturesView(MS1Schema schema)
    {
        this(schema, new ArrayList<FeaturesFilter>());
    }

    public FeaturesView(MS1Schema schema, Container container)
    {
        this(schema);
        _baseFilters.add(new ContainerFilter(container));
    }

    public FeaturesView(MS1Schema schema, List<FeaturesFilter> baseFilters)
    {
        super(schema);
        _ms1Schema = schema;
        _baseFilters = baseFilters;

        QuerySettings settings = new QuerySettings(getViewContext().getActionURL(), DATAREGION_NAME);
        settings.setSchemaName(schema.getSchemaName());
        settings.setQueryName(MS1Schema.TABLE_FEATURES);
        settings.setAllowChooseQuery(false);
        setSettings(settings);

        setShowCustomizeViewLinkInButtonBar(true);
        setShowRecordSelectors(false);
        setShowExportButtons(false);
    }

    public List<FeaturesFilter> getBaseFilters()
    {
        return _baseFilters;
    }

    public void setBaseFilters(List<FeaturesFilter> baseFilters)
    {
        _baseFilters = baseFilters;
    }

    public boolean isForExport()
    {
        return _forExport;
    }

    public void setForExport(boolean forExport)
    {
        _forExport = forExport;
    }

    public int[] getPrevNextFeature(int featureIdCur) throws SQLException, IOException
    {
        ResultSet rs = null;
        int prevFeatureId = -1;
        int nextFeatureId = -1;
        try
        {
            RenderContext ctx = new RenderContext(getViewContext());
            ctx.setBaseSort(getBaseSort());
            rs = createDataRegion().getResultSet(ctx);
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

        //apply base filters
        if(null != _baseFilters)
        {
            for(FeaturesFilter filter : _baseFilters)
            {
                filter.setFilters(tinfo);
            }
        }

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
            region.removeColumnsFromDisplayColumnList(PeaksAvailableColumnInfo.COLUMN_NAME);
        return region;
    }

    /**
     * Overridden to create a customized data view.
     * @return A customized DataView
     */
    protected DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setBaseSort(getBaseSort());
        DataRegion region = view.getDataRegion();

        //Since this code calls getDataRegion() on the newly created view, you'd *think* that
        //this could all be done in the overidden createDataRegion() method, but it can't for some reason.
        //the button bar returned from DataRegion.getButtonBar() during createDataRegion()
        //is unmodifiable. It only becomes modifiable after the call to QueryView.createDataView().
        if(region.getButtonBarPosition() != DataRegion.ButtonBarPosition.NONE)
        {
            ButtonBar bar = region.getButtonBar(DataRegion.MODE_GRID);
            assert null != bar : "Coun't get the button bar during FeaturesView.createDataView()!";

            MenuButton exportButton = new MenuButton(CAPTION_EXPORT);
            exportButton.addMenuItem(CAPTION_EXPORT_ALL_EXCEL, getExportUrl("excel"));
            exportButton.addMenuItem(CAPTION_EXPORT_ALL_TSV, getExportUrl("tsv"));
            bar.add(exportButton);

            bar.add(new ActionButton(getExportUrl("print"), CAPTION_PRINT_ALL, DataRegion.MODE_ALL, ActionButton.Action.LINK));
        }

        return view;
    } //createDataView()

    protected String getExportUrl(String format)
    {
        ActionURL url = getViewContext().getActionURL().clone();
        url.replaceParameter("export", format);
        return url.getLocalURIString();
    }

    protected Sort getBaseSort()
    {
        return new Sort("Scan,MZ");
    }
} //class FeaturesView
