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

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.labkey.api.data.TableResultSet;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

/**
 * This class can create a feature chart of the requested type for the requested feature Id
 * User: Dave
 * Date: Oct 12, 2007
 * Time: 12:00:16 PM
 */
public abstract class FeatureChart
{
    public void render(OutputStream out) throws SQLException, IOException
    {
        render(out, 425, 300);
    }

    public void render(OutputStream out, int width, int height) throws SQLException, IOException
    {
        //get the scan/peak data for this feature
        try (TableResultSet rs = getChartData())
        {
            //construct the chart
            JFreeChart chart = null;
            if(null != rs)
                chart = makeChart(rs);

            //render
            if(null != chart)
                ChartUtilities.writeChartAsPNG(out, chart, width, height);
        }
    }

    /**
     * Derived classes may override this to fetch a different dataset
     * @return The data to use for the chart
     */
    protected abstract TableResultSet getChartData();

    /**
     * Derived classes should override this to actually construct the chart. The calling
     * method will then write it out to the browser.
     *
     * @param rs The data for the chart
     * @return A constructed and populated JFreeChart
     * @throws SQLException Thrown if database error
     */
    protected abstract JFreeChart makeChart(TableResultSet rs) throws SQLException;
}
