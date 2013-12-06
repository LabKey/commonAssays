/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.ms1.view;

import org.labkey.api.data.*;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.view.DataView;
import org.labkey.api.reports.ReportService;
import org.labkey.ms1.MS1Manager;
import org.labkey.ms1.report.FeaturesRReport;
import org.labkey.ms1.query.*;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements a query view over the features data, allows filtering for features from a specific run
 *
 * User: Dave
 * Date: Oct 3, 2007
 * Time: 11:27:01 AM
 */
public class FeaturesView extends QueryView
{
    public static final String DATAREGION_NAME = "fv";

    private List<FeaturesFilter> _baseFilters = null;
    private MS1Schema _ms1Schema = null;
    private boolean _forExport = false;
    private Boolean _peaksAvailable = null;
    private boolean _forSearch = false;

    public FeaturesView(MS1Schema schema)
    {
        this(schema, new ArrayList<FeaturesFilter>(), false);
    }

    public FeaturesView(MS1Schema schema, Container container)
    {
        this(schema, new ArrayList<FeaturesFilter>(), false);
        _baseFilters.add(new ContainerFeaturesFilter(container));
    }

    public FeaturesView(MS1Schema schema, int runId)
    {
        this(schema, new ArrayList<FeaturesFilter>(), false);
        _baseFilters.add(new RunFilter(runId));
        _peaksAvailable = Boolean.valueOf(MS1Manager.get().isPeakDataAvailable(runId) == MS1Manager.PeakAvailability.Available);
    }

    public FeaturesView(MS1Schema schema, List<FeaturesFilter> baseFilters, boolean forSearch)
    {
        super(schema);
        _ms1Schema = schema;
        _baseFilters = baseFilters;
        _forSearch = forSearch;

        QuerySettings settings = schema.getSettings(getViewContext(), DATAREGION_NAME, _forSearch ? MS1Schema.TABLE_FEATURES_SEARCH : MS1Schema.TABLE_FEATURES);
        setSettings(settings);
        setShowDetailsColumn(false);

        setShowRecordSelectors(false);
        setViewItemFilter(new ReportService.ItemFilter() {
            public boolean accept(String type, String label)
            {
                return (FeaturesRReport.TYPE.equals(type));
            }
        });

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
        int id;
        try
        {
            rs = getResultSet();
            while(rs.next())
            {
                id = rs.getInt("FeatureId");
                assert !rs.wasNull() : "Got a null FeatureId back from rs.getInt()!";

                if(id == featureIdCur)
                {
                    if(rs.next())
                        nextFeatureId = rs.getInt("FeatureId");
                    break;
                }

                prevFeatureId = id;
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

        FeaturesTableInfo tinfo = _forSearch ? _ms1Schema.getFeaturesTableInfoSearch() : _ms1Schema.getFeaturesTableInfo(true, _peaksAvailable);
        tinfo.setBaseFilters(_baseFilters);
        return tinfo;
    }

    /**
     * Overridden to customize the data region.
     * @return A customized DataRegion
     */
    protected DataRegion createDataRegion()
    {
        DataRegion region = super.createDataRegion();

        //if this is for export, remove the details and peaks links
        if(_forExport)
        {
            region.removeColumns(PeaksAvailableColumnInfo.COLUMN_NAME,
                    FeaturesTableInfo.COLUMN_FIND_SIMILAR_LINK);
        }
        return region;
    }

    /**
     * Overridden to create a customized data view.
     * @return A customized DataView
     */
    public DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setBaseSort(getBaseSort());
        return view;
    } //createDataView()

    protected Sort getBaseSort()
    {
        return new Sort("FileId/ExpDataFileId/Run,Scan,MZ");
    }
} //class FeaturesView
