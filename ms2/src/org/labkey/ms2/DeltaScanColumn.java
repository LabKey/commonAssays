/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.Table;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.FieldKey;
import org.labkey.common.tools.DoubleArray;
import org.labkey.common.tools.MatrixUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * User: arauch
 * Date: Sep 18, 2004
 * Time: 7:20:25 AM
 */
public class DeltaScanColumn extends SimpleDisplayColumn
{
    private static Logger _log = Logger.getLogger(DeltaScanColumn.class);
    private static Set<ColumnInfo> _queryColumns = new HashSet<ColumnInfo>(MS2Manager.getTableInfoPeptides().getColumns("Fraction,Scan,Peptide"));

    private ColumnInfo _fractionColInfo;
    private ColumnInfo _scanColInfo;
    private ColumnInfo _peptideColInfo;

    public DeltaScanColumn()
    {
        this(null);
    }

    public DeltaScanColumn(ColumnInfo colInfo)
    {
        super();
        if (colInfo != null)
        {
            _fractionColInfo = colInfo;
            List<FieldKey> keys = new ArrayList<FieldKey>();
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

    public Object getValue(RenderContext ctx)
    {
        Integer fractionId = (Integer) ctx.get(_fractionColInfo == null ? "Fraction" : _fractionColInfo.getAlias());

        if (null == fractionId)
            return 0;

        MS2Fraction fraction = MS2Manager.getFraction(fractionId.intValue());

        if (null == fraction.getHydroR2())
            fraction = MS2Manager.writeHydro(fraction, runRegression(fraction));

        if (0 == fraction.getHydroR2().floatValue())
        {
            return 0;
        }
        else
        {
            Integer scan = (Integer) ctx.get(_scanColInfo == null ? "Scan" : _scanColInfo.getAlias());
            String peptide = (String) ctx.get(_peptideColInfo == null ? "Peptide" : _peptideColInfo.getAlias());

            if (null != scan && null != peptide)
            {
                double h = MS2Peptide.hydrophobicity(peptide);
                return (scan.intValue() - (fraction.getHydroB0() + fraction.getHydroB1() * h)) / fraction.getHydroSigma();
            }

            return 0;
        }
    }


    public Class getValueClass()
    {
        return Double.class;
    }


    private final float ppLimit = 0.99f;

    private Map runRegression(MS2Fraction fraction)
    {
        Map<String, Float> map = new HashMap<String, Float>();
        ResultSet rs = null;

        try
        {
//                ResultSet rs = Table.executeQuery("ms2", "SELECT DERIVEDTBL.Scan, DERIVEDTBL.Peptide FROM (SELECT MIN(Scan) AS Scan, Peptide FROM MS2Peptides WHERE (Fraction = ?) GROUP BY Peptide) DERIVEDTBL INNER JOIN MS2Peptides ON DERIVEDTBL.Scan = MS2Peptides.Scan AND DERIVEDTBL.Peptide = MS2Peptides.Peptide WHERE (MS2Peptides.PeptideProphet > ?)", new Object[] {fractionId, new Float(ppLimit)});
            rs = Table.executeQuery(MS2Manager.getSchema(), "SELECT MIN(Scan) AS Scan, Peptide FROM " + MS2Manager.getTableInfoPeptides() + " WHERE (Fraction = ?) AND (PeptideProphet > .6) GROUP BY Peptide HAVING (MAX(PeptideProphet) > ?)", new Object[]{fraction.getFraction(), new Float(ppLimit)});

            DoubleArray xArray = new DoubleArray();
            DoubleArray yArray = new DoubleArray();

            while (rs.next())
            {
                xArray.add(MS2Peptide.hydrophobicity(rs.getString("Peptide")));
                yArray.add(rs.getInt("Scan"));
            }

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
                double r2 = MatrixUtil.r2(x, y);
                double sigma = MatrixUtil.sigma(x, y, b);

                _log.debug("b0=" + b[0] + " b1=" + b[1] + " r2=" + r2 + " sigma=" + sigma);

                map.put("HydroB0", new Float(b[0]));
                map.put("HydroB1", new Float(b[1]));
                map.put("HydroR2", new Float(r2));
                map.put("HydroSigma", new Float(sigma));
                return map;
            }
        }
        catch (SQLException e)
        {
            _log.error("runRegression", e);
        }
        finally
        {
            if (rs != null)
                try {rs.close();} catch(SQLException e) {_log.error(e);}
        }

        map.put("HydroB0", 0.0f);
        map.put("HydroB1", 0.0f);
        map.put("HydroR2", 0.0f);
        map.put("HydroSigma", 0.0f);
        return map;
    }


    public void addQueryColumns(Set<ColumnInfo> set)
    {
        if (_fractionColInfo != null)
        {
            set.add(_fractionColInfo);
            set.add(_scanColInfo);
            set.add(_peptideColInfo);
        }
        else
        {
            set.addAll(_queryColumns);
            super.addQueryColumns(set);
        }
    }
}
