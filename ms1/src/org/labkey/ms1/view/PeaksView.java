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
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.query.QueryView;
import org.labkey.api.reports.ReportService;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.DisplayElement;
import org.labkey.api.view.ViewContext;
import org.labkey.ms1.model.Feature;
import org.labkey.ms1.query.MS1Schema;
import org.labkey.ms1.query.PeaksTableInfo;
import org.labkey.ms1.report.PeaksRReport;

import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;

/**
 * Implements a simple, flat QueryView over the Scan/Peaks data
 *
 * User: Dave
 * Date: Oct 5, 2007
 * Time: 11:04:01 AM
 */
public class PeaksView extends QueryView
{
    public PeaksView(ViewContext ctx, MS1Schema schema, ExpRun run, Feature feature) throws SQLException
    {
        this(ctx, schema, run, feature, feature.getScanFirst().intValue(), feature.getScanLast().intValue());
    }

    public PeaksView(ViewContext ctx, MS1Schema schema, ExpRun run, Feature feature, int scanFirst, int scanLast) throws SQLException
    {
        super(schema);
        assert null != schema : "Null schema passed to PeaksView!";
        assert null != feature : "Null Feature passed to PeaksView!";
        _schema = schema;
        _feature = feature;
        _expRun = run;
        _scanFirst = scanFirst;
        _scanLast = scanLast;

        QuerySettings settings = schema.getSettings(ctx, QueryView.DATAREGIONNAME_DEFAULT, MS1Schema.TABLE_PEAKS);
        setSettings(settings);

        if(null != run)
            setTitle("Peaks from Scans " +  _scanFirst + " through " + _scanLast + " from " + PageFlowUtil.filter(run.getName()));
        else
            setTitle("Peaks from Scans " + _scanFirst + " through " + _scanLast);
        

        setShowRecordSelectors(false);
        setViewItemFilter(new ReportService.ItemFilter() {
            public boolean accept(String type, String label)
            {
                return (PeaksRReport.TYPE.equals(type));
            }
        });
    }

    protected TableInfo createTable()
    {
        PeaksTableInfo tinfo = _schema.getPeaksTableInfo();
        tinfo.addScanRangeCondition(_expRun.getRowId(), _scanFirst, _scanLast, getContainer());
        return tinfo;
    }

    /**
     * Overridden to create a customized data view.
     * @return A customized DataView
     */
    public DataView createDataView()
    {
        DataView view = super.createDataView();
        view.getRenderContext().setBaseSort(new Sort("ScanId,PeakFamilies/PeakFamilyId/MZMono"));
        DataRegion region = view.getDataRegion();

        //Since this code calls getDataRegion() on the newly created view, you'd *think* that
        //this could all be done in the overidden createDataRegion() method, but it can't for some reason.
        //the button bar returned from DataRegion.getButtonBar() during createDataRegion()
        //is unmodifiable. It only becomes modifiable after the call to QueryView.createDataView().
        if(region.getButtonBarPosition() != DataRegion.ButtonBarPosition.NONE)
        {
            ButtonBar bar = region.getButtonBar(DataRegion.MODE_GRID);
            assert null != bar : "Coun't get the button bar during FeaturesView.createDataView()!";

            bar.add(0, new ScanFilter(_feature, getViewContext().getActionURL()));
        }

        return view;
    } //createDataView()

    public static class ScanFilter extends DisplayElement
    {
        private static final String SCAN_FILTER = "query.ScanId/Scan~eq";

        public ScanFilter(Feature feature, ActionURL url)
        {
            _url = url;
            _feature = feature;
        }
        public void render(RenderContext ctx, Writer out) throws IOException
        {
            if(null == _feature)
                return;

            ActionURL url = _url.clone();
            url.deleteParameter(SCAN_FILTER);

            out.write("<select onchange=\"document.location.href=this.options[this.selectedIndex].value\">");
            out.write("<option selected value=\"#\">Show...</option>");
            out.write("<option value=\"" + url.getLocalURIString() + "\">All Scans</option>");

            if(_feature.getScan() != null)
            {
                url.addParameter(SCAN_FILTER, _feature.getScan().intValue());
                out.write("<option value=\"" + url.getLocalURIString() + "\">Feature Apex Scan</option>");
            }
            out.write("</select>");
        }

        private ActionURL _url = null;
        private Feature _feature = null;
    }

    private MS1Schema _schema;
    private Feature _feature = null;
    private ExpRun _expRun = null;
    private int _scanFirst = 0;
    private int _scanLast = 0;
} //class PeaksView

