package org.labkey.ms2;

import org.labkey.api.view.WebPartView;
import org.labkey.api.view.ViewURLHelper;
import org.labkey.api.view.GridView;
import org.labkey.api.data.*;

import java.io.PrintWriter;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class MS2WebPart extends WebPartView
{
    public MS2WebPart()
    {
    }


    @Override
    public void renderView(Object model, PrintWriter out) throws Exception
    {
        Container c = hasAccess(getViewContext(), "MS2 Runs");
        if (c == null)
        {
            return;
        }

        DataRegion rgn = getGridRegionWebPart(c);
        rgn.getDisplayColumn(0).setURL(ViewURLHelper.toPathString("MS2", "showRun", c.getPath()) + "?run=${Run}");

        GridView gridView = new GridView(rgn);
        gridView.setCustomizeLinks(getCustomizeLinks());
        gridView.setTitle("MS2 Runs");
        gridView.setTitleHref(ViewURLHelper.toPathString("MS2", "showList", c.getPath()));
        gridView.setFilter(new SimpleFilter("Deleted", Boolean.FALSE));
        gridView.setSort(MS2Manager.getRunsBaseSort());

        include(gridView);
    }


    private DataRegion getGridRegionWebPart(Container c)
    {
        DataRegion rgn = new DataRegion();
        rgn.setName(MS2Manager.getDataRegionNameExperimentRuns());
        TableInfo ti = MS2Manager.getTableInfoExperimentRuns();
        ColumnInfo[] cols = ti.getColumns("Description", "Path", "Created", "Run", "ExperimentRunLSID", "ProtocolName", "ExperimentRunRowId");
        rgn.setColumns(cols);
        rgn.getDisplayColumn(3).setVisible(false);
        rgn.getDisplayColumn(4).setVisible(false);
        rgn.getDisplayColumn(5).setVisible(false);
        rgn.getDisplayColumn(6).setVisible(false);

        rgn.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY, DataRegion.MODE_GRID);
        return rgn;
    }
}
