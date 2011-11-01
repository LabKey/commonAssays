/*
 * Copyright (c) 2011 LabKey Corporation
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
package org.labkey.luminex;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.RenderContext;
import org.labkey.api.query.FieldKey;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * User: cnathe
 * Date: Oct 27, 2011
 * Time: 9:43:49 AM
 */
public class GuideSetOutOfRangeDisplayColumn extends DataColumn
{
    private final FieldKey _averageFieldKey;
    private final FieldKey _stdDevFieldKey;
    private final String _colName;
    private final String _colTitle;

    public GuideSetOutOfRangeDisplayColumn(ColumnInfo colInfo, String colTitle, String colName, String curveType)
    {
        super(colInfo);
        _colName = colName;
        _colTitle = colTitle;
        FieldKey parentFK = colInfo.getFieldKey().getParent();

        if (null != curveType)
        {
            // field key for avg and stdDev of EC50 and AUC
            _averageFieldKey = new FieldKey(new FieldKey(new FieldKey(new FieldKey(parentFK, "AnalyteTitration"), "GuideSet"), curveType + "CurveFit"), _colName + "Average");
            _stdDevFieldKey = new FieldKey(new FieldKey(new FieldKey(new FieldKey(parentFK, "AnalyteTitration"), "GuideSet"), curveType + "CurveFit"), _colName + "StdDev");
        }
        else
        {
            // field key for avg and stdDev of MaxFI
            _averageFieldKey = new FieldKey(new FieldKey(parentFK, "GuideSet"), _colName + "Average");
            _stdDevFieldKey = new FieldKey(new FieldKey(parentFK, "GuideSet"), _colName + "StdDev");
        }
    }

    @Override
    public void addQueryFieldKeys(Set<FieldKey> keys)
    {
        super.addQueryFieldKeys(keys);
        keys.add(_averageFieldKey);
        keys.add(_stdDevFieldKey);
    }

    @Override
    public void renderTitle(RenderContext ctx, Writer out) throws IOException
    {
        out.write(_colTitle);
    }

    @Override
    public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
    {
        Double value = null;
        if (null != getValue(ctx))
            value = ((Float)getValue(ctx)).doubleValue();
        Double avg = (Double)ctx.get(_averageFieldKey);
        Double stdDev = (Double)ctx.get(_stdDevFieldKey);

        // display values that are outside of guide set range in red
        boolean outOfRange = isOutOfRange(value, avg, stdDev);
        if (outOfRange)
            out.write("<span style='color:red;'>");

        super.renderGridCellContents(ctx, out);

        if (outOfRange)
            out.write("</span>");
    }

    private boolean isOutOfRange(Double value, Double avg, Double stdDev)
    {
        if (null != value && null != avg)
        {
            // set the stdDev to zero if there is none
            if (null == stdDev)
                stdDev = 0.0;

            value = (double)Math.round(value * 100) / 100;
            double top = (double)Math.round((avg + 3 * stdDev) * 100) / 100;
            double bottom = (double)Math.round((avg - 3 * stdDev) * 100) / 100;
            if (value > top || value < bottom)
                return true;
        }

        return false;
    }
}
