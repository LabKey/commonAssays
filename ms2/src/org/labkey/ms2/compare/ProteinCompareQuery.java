/*
 * Copyright (c) 2006-2014 LabKey Corporation
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

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.util.Pair;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.protein.ProteinManager;
import org.springframework.validation.BindException;

import java.util.ArrayList;
import java.util.List;

/**
 * User: adam
 * Date: Oct 3, 2006
 * Time: 10:56:03 AM
 */
public class ProteinCompareQuery extends CompareQuery
{
    public static final String COMPARISON_DESCRIPTION = "Compare Search Engine Proteins";

    public ProteinCompareQuery(ActionURL currentUrl, List<MS2Run> runs, User user)
    {
        super(currentUrl, "SeqId", runs, user);

        boolean total = "1".equals(currentUrl.getParameter("total"));
        boolean unique = "1".equals(currentUrl.getParameter("unique"));

        StringBuilder header = new StringBuilder(HEADER_PREFIX);
        if (total)
        {
            addGridColumn("Total", "Peptide", "COUNT");
            header.append("total peptides ");
        }
        if (unique)
        {
            addGridColumn("Unique", "DISTINCT Peptide", "COUNT");
            if (total)
                header.append("and ");
            header.append("unique peptides ");
        }
        if ("1".equals(currentUrl.getParameter("sumLightArea-Protein")))
        {
            addGridColumn("SumLightArea", "lightarea", "SUM", "0.##");
        }
        if ("1".equals(currentUrl.getParameter("sumHeavyArea-Protein")))
        {
            addGridColumn("SumHeavyArea", "heavyarea", "SUM", "0.##");
        }
        if ("1".equals(currentUrl.getParameter("avgDecimalRatio-Protein")))
        {
            addGridColumn("AvgDecimalRatio", "DecimalRatio", "AVG", "0.##");
        }
        if ("1".equals(currentUrl.getParameter("maxDecimalRatio-Protein")))
        {
            addGridColumn("MaxDecimalRatio", "DecimalRatio", "MAX", "0.##");
        }
        if ("1".equals(currentUrl.getParameter("minDecimalRatio-Protein")))
        {
            addGridColumn("MinDecimalRatio", "DecimalRatio", "MIN", "0.##");
        }
        header.append("mapping to each protein in each run.");

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

    protected String getFromClause()
    {
        return MS2Manager.getTableInfoPeptides() + " p LEFT OUTER JOIN " +
            "(SELECT Mass AS SequenceMass, BestName, BestGeneName, SeqId AS SeqSeqId FROM " + ProteinManager.getTableInfoSequences() + ") s ON " +
            "p.SeqId = s.SeqSeqId ";
    }

    protected void selectRows(BindException errors)
    {
        super.selectRows(errors);

        SimpleFilter proteinFilter = new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteins());
        // Add to GROUP BY
        for (FieldKey fieldKey : proteinFilter.getAllFieldKeys())
        {
            if (!fieldKey.equals(FieldKey.fromParts("Peptides")) && !fieldKey.equals(FieldKey.fromParts("UniquePeptides")))
            {
                append(", ");
                append(fieldKey.toString());
            }
        }

        appendNewLine();


        // TODO: Make Nick happy by using a sub-SELECT instead of HAVING
        String proteinHaving = proteinFilter.getWhereSQL(MS2Manager.getTableInfoProteins()).replaceFirst("WHERE", "HAVING");
        // Can't use SELECT aliases in HAVING clause, so replace names with aggregate functions
        proteinHaving = proteinHaving.replaceAll("UniquePeptides", "COUNT(DISTINCT Peptide)");
        proteinHaving = proteinHaving.replaceAll("Peptides", "COUNT(Peptide)");
        addAll(proteinFilter.getWhereParams(MS2Manager.getTableInfoProteins()));
        append(proteinHaving);
    }

    protected void addWhereClauses(SimpleFilter filter)
    {
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(_currentUrl, _runs, ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, _user);
        peptideFilter = ProteinManager.reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoPeptides());
        filter.addAllClauses(peptideFilter);
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
        linkURL.setAction(MS2Controller.ShowProteinAction.class);   // Could target the "prot" window instead of using the main window
        return "protein=${Protein}&seqId=${SeqId}";
    }

    protected String getComparisonColumnLinkTarget()
    {
        return "prot";
    }

    protected ColumnInfo getComparisonCommonColumn(TableInfo ti)
    {
        return ti.getColumn("Protein");
    }

    public List<Pair<String, String>> getSQLSummaries(User user)
    {
        List<Pair<String, String>> result = new ArrayList<>();
        SimpleFilter peptideFilter = new SimpleFilter();
        addWhereClauses(peptideFilter);
        result.add(new Pair<>("Peptide Filter", peptideFilter.getFilterText()));
        result.add(new Pair<>("Protein Filter", new SimpleFilter(_currentUrl, MS2Manager.getDataRegionNameProteins()).getFilterText()));
        return result;
    }
}
