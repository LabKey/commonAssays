/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

package org.labkey.ms2.compare;

import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.util.Pair;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.DisplayColumn;

import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * User: adam
 * Date: Oct 3, 2006
 * Time: 10:56:03 AM
 */
public class PeptideCompareQuery extends CompareQuery
{
    public static final String COMPARISON_DESCRIPTION = "Compare Peptides";

    public PeptideCompareQuery(ActionURL currentUrl, List<MS2Run> runs, User user)
    {
        super(currentUrl, "Peptide", runs, user);
        
        StringBuilder header = new StringBuilder(HEADER_PREFIX);
        header.append("number of times each peptide appears in each run.");
        setHeader(header.toString());
        addGridColumn("Total", "Peptide", "COUNT");
        if ("1".equals(currentUrl.getParameter("maxPeptideProphet")))
        {
            addGridColumn("MaxPepProphet", "peptideprophet", "MAX", "0.0000");
        }
        if ("1".equals(currentUrl.getParameter("avgPeptideProphet")))
        {
            addGridColumn("AvgPepProphet", "CASE WHEN peptideprophet >= 0 THEN peptideprophet ELSE NULL END", "AVG", "0.0000");
        }
        if ("1".equals(currentUrl.getParameter("minPeptideProphetErrorRate")))
        {
            addGridColumn("MinErrorRate", "peptidepropheterrorrate", "MIN", "0.00");
        }
        if ("1".equals(currentUrl.getParameter("avgPeptideProphetErrorRate")))
        {
            addGridColumn("AvgErrorRate", "peptidepropheterrorrate", "AVG", "0.00");
        }
        if ("1".equals(currentUrl.getParameter("sumLightArea-Peptide")))
        {
            addGridColumn("SumLightArea", "lightarea", "SUM", "0.##");
        }
        if ("1".equals(currentUrl.getParameter("sumHeavyArea-Peptide")))
        {
            addGridColumn("SumHeavyArea", "heavyarea", "SUM", "0.##");
        }
        if ("1".equals(currentUrl.getParameter("avgDecimalRatio-Peptide")))
        {
            addGridColumn("AvgDecimalRatio", "DecimalRatio", "AVG", "0.##");
        }
        if ("1".equals(currentUrl.getParameter("maxDecimalRatio-Peptide")))
        {
            addGridColumn("MaxDecimalRatio", "DecimalRatio", "MAX", "0.##");
        }
        if ("1".equals(currentUrl.getParameter("minDecimalRatio-Peptide")))
        {
            addGridColumn("MinDecimalRatio", "DecimalRatio", "MIN", "0.##");
        }
    }

    public String getComparisonDescription()
    {
        return COMPARISON_DESCRIPTION;
    }

    protected void addWhereClauses(SimpleFilter filter)
    {
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(_currentUrl, _runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, _user);
        peptideFilter = ProteinManager.reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoPeptides());
        filter.addAllClauses(peptideFilter);
    }

    protected String setupComparisonColumnLink(ActionURL linkURL, String columnName, String runPrefix)
    {
        linkURL.setAction(MS2Controller.ShowRunAction.class);
        linkURL.deleteParameter("view");  // Always link to Peptide view (the default)
        return MS2Manager.getDataRegionNamePeptides() + ".Peptide~eq=${Peptide}";
    }

    protected ColumnInfo getComparisonCommonColumn(TableInfo ti)
    {
        return ti.getColumn("Peptide");
    }

    public List<Pair<String, String>> getSQLSummaries(User user)
    {
        List<Pair<String, String>> result = new ArrayList<>();
        SimpleFilter peptideFilter = new SimpleFilter();
        addWhereClauses(peptideFilter);
        String filterString = peptideFilter.getFilterText();
        result.add(new Pair<>("Peptide Filter", filterString));
        return result;
    }

    protected DisplayColumn createColumn(ActionURL linkURL, RunColumn column, String runPrefix, String columnName, TableInfo ti, ResultSetMetaData md, CompareDataRegion rgn)
        throws SQLException
    {
        DisplayColumn result = super.createColumn(linkURL, column, runPrefix, columnName, ti, md, rgn);
        if (column.getLabel().equals("AvgPepProphet"))
        {
            result.setFormatString("0.####");
        }
        else if (column.getLabel().equals("AvgErrorRate"))
        {
            result.setFormatString("0.######");
        }
        return result;
    }

    protected String getComparisonColumnLinkTarget()
    {
        return null;
    }
}
