/*
 * Copyright (c) 2007-2010 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.ms2;

import org.labkey.api.view.GridView;
import org.labkey.api.view.ViewContext;
import org.labkey.api.data.*;
import org.springframework.validation.BindException;

import java.util.List;

/**
 * User: jeckels
* Date: Feb 6, 2007
*/
public class MS2WebPart extends GridView
{
    public MS2WebPart(ViewContext viewContext)
    {
        super(getGridRegionWebPart(), (BindException)null);

        DataRegion rgn = getDataRegion();
        rgn.getDisplayColumn(0).setURL(MS2Controller.getShowRunSubstitutionURL(viewContext.getContainer()));

        setTitle("MS2 Runs");
        setTitleHref(MS2Controller.getShowListURL(viewContext.getContainer()));
        setFilter(new SimpleFilter("Deleted", Boolean.FALSE));
        setSort(MS2Manager.getRunsBaseSort());
    }

    private static DataRegion getGridRegionWebPart()
    {
        DataRegion rgn = new DataRegion();
        rgn.setName(MS2Manager.getDataRegionNameExperimentRuns());
        TableInfo ti = MS2Manager.getTableInfoExperimentRuns();
        List<ColumnInfo> cols = ti.getColumns("Description", "Path", "Created", "Run", "ExperimentRunLSID", "ProtocolName", "ExperimentRunRowId", "PeptideCount", "NegativeHitCount");
        rgn.setColumns(cols);
        for (int i = 3; i <= 8; i++)
        {
            rgn.getDisplayColumn(i).setVisible(false);
        }

        rgn.setButtonBar(ButtonBar.BUTTON_BAR_EMPTY, DataRegion.MODE_GRID);
        return rgn;
    }
}
