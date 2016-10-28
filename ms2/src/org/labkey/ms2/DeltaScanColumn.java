/*
 * Copyright (c) 2004-2016 Fred Hutchinson Cancer Research Center
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

import org.apache.log4j.Logger;
import org.labkey.api.arrays.DoubleArray;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.Selector;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.util.MatrixUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: arauch
 * Date: Sep 18, 2004
 * Time: 7:20:25 AM
 */
public class DeltaScanColumn extends AbstractPeptideDisplayColumn
{
    private static final Logger _log = Logger.getLogger(DeltaScanColumn.class);

    private ColumnInfo _fractionColInfo;
    private ColumnInfo _scanColInfo;
    private ColumnInfo _peptideColInfo;

    public DeltaScanColumn(ColumnInfo colInfo)
    {
        super();
        if (colInfo != null)
        {
            _fractionColInfo = colInfo;
            List<FieldKey> keys = new ArrayList<>();
            FieldKey scanKey = FieldKey.fromParts("Scan");
            keys.add(scanKey);
            FieldKey peptideKey = FieldKey.fromParts("Peptide");
            keys.add(peptideKey);
            Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(_fractionColInfo.getParentTable(), keys);
            _scanColInfo = cols.get(scanKey);
            _peptideColInfo = cols.get(peptideKey);
        }

        setCaption("dScan");
        setFormatString("0.00");
        setWidth("45");
        setTextAlign("right");
        setName("dscan");
    }

    @Override
    public ColumnInfo getColumnInfo()
    {
        return _fractionColInfo;
    }

    public Object getValue(RenderContext ctx)
    {
        Integer fractionId = (Integer)getColumnValue(ctx, _fractionColInfo, "Fraction", "Fraction$Fraction");

        if (null == fractionId)
            return 0;

        MS2Fraction fraction = MS2Manager.getFraction(fractionId);

        if (null == fraction.getHydroR2())
            fraction = MS2Manager.writeHydro(fraction, runRegression(fraction));

        if (0 == fraction.getHydroR2())
        {
            return 0;
        }
        else
        {
            Integer scan = (Integer) getColumnValue(ctx, _scanColInfo, "Scan");
            String peptide = (String) getColumnValue(ctx, _peptideColInfo, "Peptide");

            if (null != scan && null != peptide)
            {
                double h = MS2Peptide.hydrophobicity(peptide);
                return (scan - (fraction.getHydroB0() + fraction.getHydroB1() * h)) / fraction.getHydroSigma();
            }

            return 0;
        }
    }


    public Class getValueClass()
    {
        return Double.class;
    }


    private Map runRegression(MS2Fraction fraction)
    {
        final DoubleArray xArray = new DoubleArray();
        final DoubleArray yArray = new DoubleArray();

        float ppLimit = 0.99f;

        new SqlSelector(MS2Manager.getSchema(), "SELECT MIN(Scan) AS Scan, Peptide FROM " + MS2Manager.getTableInfoPeptides() + " WHERE (Fraction = ?) AND (PeptideProphet > .6) GROUP BY Peptide HAVING (MAX(PeptideProphet) > ?)", fraction.getFraction(), ppLimit).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                xArray.add(MS2Peptide.hydrophobicity(rs.getString("Peptide")));
                yArray.add(rs.getInt("Scan"));
            }
        });

        Map<String, Float> map = new HashMap<>();

        // Need at least 10 points to run the regression
        if (xArray.size() >= 10)
        {
            double[] x = new double[xArray.size()];
            double[] y = new double[yArray.size()];

            // UNDONE: use xArray.toArray() once it's tested and working
            for (int i = 0; i < x.length; i++)
            {
                x[i] = xArray.get(i);
                y[i] = yArray.get(i);
            }

            double[] b = MatrixUtil.linearRegression(x, y);
            Float b0 = new Float(b[0]);
            Float b1 = new Float(b[1]);
            Float r2 = new Float(MatrixUtil.r2(x, y));
            Float sigma = new Float(MatrixUtil.sigma(x, y, b));

            if (allLegalValues(b0, b1, r2, sigma))
            {
                _log.debug("b0=" + b0 + " b1=" + b1 + " r2=" + r2 + " sigma=" + sigma);

                map.put("HydroB0", b0);
                map.put("HydroB1", b1);
                map.put("HydroR2", r2);
                map.put("HydroSigma", sigma);

                return map;
            }
        }

        map.put("HydroB0", 0.0f);
        map.put("HydroB1", 0.0f);
        map.put("HydroR2", 0.0f);
        map.put("HydroSigma", 0.0f);

        return map;
    }


    private static boolean allLegalValues(Float... floats)
    {
        for (Float f : floats)
            if (null == f || f.isNaN() || f.isInfinite())
                return false;

        return true;
    }


    public void addQueryColumns(Set<ColumnInfo> set)
    {
        set.add(_fractionColInfo);
        set.add(_scanColInfo);
        set.add(_peptideColInfo);
    }
}
