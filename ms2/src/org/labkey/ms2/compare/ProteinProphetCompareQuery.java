/*
 * Copyright (c) 2006-2017 LabKey Corporation
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
import org.labkey.ms2.GroupNumberDisplayColumn;
import org.labkey.api.data.*;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.api.util.Pair;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.api.action.LabKeyError;
import org.springframework.validation.BindException;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: jeckels
 * Date: Oct 6, 2006
 */
public class ProteinProphetCompareQuery extends CompareQuery
{
    public static final String COMPARISON_DESCRIPTION = "Compare ProteinProphet (Legacy) Proteins";

    protected ProteinProphetCompareQuery(ActionURL currentUrl, List<MS2Run> runs, User user)
    {
        super(currentUrl, "SeqId", runs, user);

        boolean groupProbability = "1".equals(currentUrl.getParameter("groupProbability"));
        boolean light2HeavyRatioMean = "1".equals(currentUrl.getParameter("light2HeavyRatioMean"));
        boolean heavy2LightRatioMean = "1".equals(currentUrl.getParameter("heavy2LightRatioMean"));
        boolean totalPeptides = "1".equals(currentUrl.getParameter("totalPeptides"));
        boolean uniquePeptides = "1".equals(currentUrl.getParameter("uniquePeptides"));

        StringBuilder header = new StringBuilder(HEADER_PREFIX);

        List<String> descriptions = new ArrayList<>();

        addGridColumn("GroupNumber", "GroupNumber", "MAX");
        addGridColumn("CollectionId", "IndistinguishableCollectionId", "MAX");
        descriptions.add("protein group number");

        if (groupProbability)
        {
            addGridColumn("GroupProbability", "GroupProbability", "MAX");
            descriptions.add("protein group probability");
        }
        if (heavy2LightRatioMean)
        {
            addGridColumn("Heavy2LightRatioMean", "Heavy2LightRatioMean", "MAX");
            descriptions.add("heavy to light ratio mean");
        }
        if (light2HeavyRatioMean)
        {
            addGridColumn("ratiomean", "ratiomean", "MAX");
            descriptions.add("light to heavy ratio mean");
        }
        if (totalPeptides)
        {
            addGridColumn("TotalNumberPeptides", "TotalNumberPeptides", "MAX");
            descriptions.add("total peptides");
        }
        if (uniquePeptides)
        {
            addGridColumn("UniquePeptidesCount", "UniquePeptidesCount", "MAX");
            descriptions.add("unique peptides");
        }

        for (int i = 0; i < descriptions.size(); i++)
        {
            if (i > 0)
            {
                header.append(", ");
            }
            if (i == descriptions.size() - 1)
            {
                header.append(" and ");
            }
            header.append(descriptions.get(i));
        }
        header.append(" mapping to each protein in each run.");

        setHeader(header.toString());
    }

    public String getComparisonDescription()
    {
        return COMPARISON_DESCRIPTION;
    }

    protected String getLabelColumn()
    {
        return "BestName";
    }

    protected void selectColumns(BindException errors)
    {
        // Use subselect to make it easier to join seqid to prot.sequences for bestname
        append("SELECT ");
        append(getLabelColumn());
        append(" AS Protein, grouped.* FROM");
        appendNewLine();
        append("(");
        indent();
        appendNewLine();

        super.selectColumns(errors);
    }

    protected void groupByCompareColumn(BindException errors)
    {
        super.groupByCompareColumn(errors);

        outdent();
        appendNewLine();
        append(") grouped INNER JOIN ");
        append(ProteinManager.getTableInfoSequences(), "seq");
        append(" ON grouped.SeqId = seq.SeqId");
    }

    protected String setupComparisonColumnLink(ActionURL linkURL, String columnName, String runPrefix)
    {
        linkURL.setAction(MS2Controller.ShowRunAction.class);
        linkURL.replaceParameter("expanded", "1");
        linkURL.replaceParameter("grouping", "proteinprophet");
        String paramName = MS2Manager.getDataRegionNameProteinGroups() + ".GroupNumber";
        linkURL.deleteParameter(paramName);
        return paramName + "~eq=${" + runPrefix + "GroupNumber}";
    }

    protected DisplayColumn createColumn(ActionURL linkURL, RunColumn column, String runPrefix, String columnName, TableInfo ti, ResultSetMetaData md, CompareDataRegion rgn)
        throws SQLException
    {
        if (column.getLabel().equals("CollectionId"))
        {
            return null;
        }
        else if (column.getLabel().equals("GroupNumber"))
        {
            ColumnInfo ci = new ColumnInfo(columnName);
            ci.setParentTable(ti);
            ci.setSqlTypeName(md.getColumnTypeName(rgn.getResultSet().findColumn(columnName)));
            ci.setLabel(column.getLabel());
            return new GroupNumberDisplayColumn(ci, linkURL, runPrefix + "GroupNumber", runPrefix + "CollectionId");
        }
        else
        {
            DisplayColumn result = super.createColumn(linkURL, column, runPrefix, columnName, ti, md, rgn);
            result.setURL(null);
            return result;
        }


    }

    protected String getComparisonColumnLinkTarget()
    {
        return "prot";
    }

    protected ColumnInfo getComparisonCommonColumn(TableInfo ti)
    {
        ColumnInfo result = ti.getColumn("Protein");
        ActionURL linkURL = _currentUrl.clone();
        linkURL.setAction(MS2Controller.ShowProteinAction.class);
        linkURL.replaceParameter("seqId", "${SeqId}");
        result.setURL(StringExpressionFactory.createURL(linkURL));
        return result;
    }

    public List<Pair<String, String>> getSQLSummaries(User user)
    {
        List<Pair<String, String>> result = new ArrayList<>();
        result.add(new Pair<>("Protein Group Filter", new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteinGroups()).getFilterText()));
        return result;
    }

    protected String getFromClause()
    {
        SimpleFilter proteinGroupFilter = new SimpleFilter();
        proteinGroupFilter.addUrlFilters(_currentUrl, MS2Manager.getDataRegionNameProteinGroups());
        addAll(proteinGroupFilter.getWhereParams(MS2Manager.getTableInfoProteinGroupsWithQuantitation()));

        return MS2Manager.getTableInfoProteinProphetFiles() + " ppf, " +
            " ( SELECT * FROM " + MS2Manager.getTableInfoProteinGroupsWithQuantitation() +
            " " + proteinGroupFilter.getWhereSQL(MS2Manager.getTableInfoProteinGroupsWithQuantitation()) + " ) pg, " +
            MS2Manager.getTableInfoProteinGroupMemberships() + " pgm";
    }


    protected void addWhereClauses(SimpleFilter filter)
    {
        filter.addWhereClause("ppf.rowid = pg.proteinprophetfileid", new Object[0]);
        filter.addWhereClause("pg.rowId = pgm.proteingroupid", new Object[0]);
    }

    public void checkForErrors(BindException errors) throws SQLException
    {
        super.checkForErrors(errors);
        for (MS2Run run : _runs)
        {
            if (run.getProteinProphetFile() == null)
            {
                errors.addError(new LabKeyError("Run " + run.getDescription() + " does not have ProteinProphet results."));
            }
        }
    }
}
