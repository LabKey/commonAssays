/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

package org.labkey.ms1.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.*;
import org.labkey.api.ms1.MS1Service;
import org.labkey.api.ms2.MS2Service;
import org.labkey.api.query.AliasedColumn;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.ms1.MS1Controller;
import org.labkey.ms1.MS1Manager;

import java.util.*;

/**
 * Provides a filtered table implementation for the Features table, allowing clients
 * to add more conditions (e.g., filtering for features from a specific run)
 *
 * User: Dave
 * Date: Oct 3, 2007
 * Time: 11:00:43 AM
 */
public class FeaturesTableInfo extends VirtualTable<MS1Schema>
{
    public static final String COLUMN_PEPTIDE_INFO = "RelatedPeptide";
    public static final String COLUMN_FIND_SIMILAR_LINK = "FindSimilarLink";

    //Data Members
    private TableInfo _sourceTable;
    private boolean _includePepFk = true;
    private List<FeaturesFilter> _filters = null;
    private boolean _includeDeleted = false;

    public FeaturesTableInfo(MS1Schema schema, boolean includePepFk)
    {
        this(schema, includePepFk, null);
    }

    public FeaturesTableInfo(MS1Schema schema, boolean includePepFk, Boolean peaksAvailable)
    {
        super(schema.getDbSchema(), "Features", schema);
        setDescription("Contains all features from all MS1 experiment runs loaded into this folder.");

        _sourceTable = MS1Manager.get().getTable(MS1Service.Tables.Features.name());
        _includePepFk = includePepFk;

        //wrap all the columns
        wrapAllColumns(true);

        //tell query that FileId is an FK to the Files user table info
        getColumn("FileId").setFk(new LookupForeignKey("FileId")
        {
            public TableInfo getLookupTableInfo()
            {
                return getUserSchema().getFilesTableInfo();
            }
        });

        //URL and display factory for the scan and peptide columns
        String urlPep = new ActionURL(MS1Controller.ShowMS2PeptideAction.class, schema.getContainer()).getLocalURIString()
                + "featureId=${FeatureId}";

        DisplayColumnFactory dcfPep = new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                DataColumn dataColumn = new DataColumn(colInfo);
                dataColumn.setLinkTarget("peptide");
                return dataColumn;
            }
        };

        if(includePepFk)
        {
            ColumnInfo ciPepId = addColumn(new ExprColumn(this, COLUMN_PEPTIDE_INFO,
                    new SQLFragment(COLUMN_PEPTIDE_INFO), JdbcType.INTEGER));

            //tell query that this new column is an FK to the peptides data table
            ciPepId.setFk(new LookupForeignKey("RowId", "Peptide")
            {
                public TableInfo getLookupTableInfo()
                {
                    return MS2Service.get().createPeptidesTableInfo(getUserSchema().getUser(), getUserSchema().getContainer(),
                            false, getUserSchema().isRestrictContainer() ? ContainerFilter.CURRENT : ContainerFilter.EVERYTHING, null, null);
                }
            });

            ciPepId.setURL(StringExpressionFactory.createURL(urlPep));
            ciPepId.setDisplayColumnFactory(dcfPep);
        } //if(includePepFk)

        //make the ms2 scan a hyperlink to showPeptide view
        ColumnInfo ciMS2Scan = getColumn("MS2Scan");
        ciMS2Scan.setURL(StringExpressionFactory.createURL(urlPep));
        ciMS2Scan.setDisplayColumnFactory(dcfPep);

        //add new columns for the peaks and details links
        if(null != peaksAvailable)
            addColumn(new PeaksAvailableColumnInfo(this, peaksAvailable.booleanValue()));
        else
            addColumn(new PeaksAvailableColumnInfo(this));

        //add a column for the find similar link
        ColumnInfo similarLinkCol = addColumn(wrapColumn(COLUMN_FIND_SIMILAR_LINK, getSourceTable().getColumn("FeatureId")));
        similarLinkCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new SimilarLinkDisplayColumn(colInfo);
            }
        });

        //only display a subset of the columns by by default
        ArrayList<FieldKey> visibleColumns = new ArrayList<>(getDefaultVisibleColumns());
        visibleColumns.remove(FieldKey.fromParts("FeatureId"));
        visibleColumns.remove(FieldKey.fromParts("FileId"));
        visibleColumns.remove(FieldKey.fromParts("Description"));
        visibleColumns.remove(FieldKey.fromParts("Background"));
        visibleColumns.remove(FieldKey.fromParts("Median"));
        visibleColumns.remove(FieldKey.fromParts("KL"));
        visibleColumns.remove(FieldKey.fromParts("ScanCount"));
        visibleColumns.remove(FieldKey.fromParts("ChargeStates"));

        //move peak and detail links column to first position
        visibleColumns.remove(FieldKey.fromParts(PeaksAvailableColumnInfo.COLUMN_NAME));
        visibleColumns.add(0, FieldKey.fromParts(PeaksAvailableColumnInfo.COLUMN_NAME));

        //move find similar link column to second position
        visibleColumns.remove(FieldKey.fromParts(COLUMN_FIND_SIMILAR_LINK));
        visibleColumns.add(1, FieldKey.fromParts(COLUMN_FIND_SIMILAR_LINK));

        visibleColumns.add(FieldKey.fromParts(COLUMN_PEPTIDE_INFO, "Fraction", "Run", "Description"));

        setDefaultVisibleColumns(visibleColumns);
    } //c-tor

    public List<FeaturesFilter> getBaseFilters()
    {
        return _filters;
    }

    public void setBaseFilters(List<FeaturesFilter> filters)
    {
        _filters = filters;
    }

    public boolean includeDeleted()
    {
        return _includeDeleted;
    }

    public void setIncludeDeleted(boolean includeDeleted)
    {
        _includeDeleted = includeDeleted;
    }

    public boolean includePepFk()
    {
        return _includePepFk;
    }

    public void setIncludePepFk(boolean includePepFk)
    {
        _includePepFk = includePepFk;
    }


    @Override @NotNull
    public SQLFragment getFromSQL()
    {
        assert null != getSourceTable();

        SQLFragment sql = new SQLFragment("SELECT\n");
        String sep = "";

        //all base-table columns
        for(ColumnInfo col : getSourceTable().getColumns())
        {
            sql.append(sep);
            sql.append("fe.");
            sql.append(col.getName());
            sql.append(" AS ");
            sql.append(col.getAlias());
            sep = ",\n";
        }

        //peptide row id
        if(includePepFk())
        {
            sql.append(sep);
            sql.append("pep.RowId AS ");
            sql.append(COLUMN_PEPTIDE_INFO);
        }

        //from clause
        sql.append("\nFROM ");
        sql.append(MS1Service.Tables.Features.getFullName());
        sql.append(" AS fe");

        //always join to files and exp.Data so that we can filter for features within the schema's container list
        sql.append("\nINNER JOIN ");
        sql.append(MS1Service.Tables.Files.getFullName());
        sql.append(" AS fi ON (fe.FileId=fi.FileId)");
        sql.append("\nINNER JOIN exp.Data AS d ON (fi.ExpDataFileId=d.RowId)");

        if(includePepFk() || null != getBaseFilters())
        {
            sql.append("\nLEFT OUTER JOIN (SELECT pd.*, fr.MzXmlUrl");
            sql.append("\nFROM ms2.Fractions AS fr");
            sql.append("\nINNER JOIN ms2.PeptidesData AS pd ON (pd.Fraction=fr.Fraction)");
            sql.append("\nINNER JOIN ms2.Runs AS r ON (fr.Run=r.Run");
            sql.append("\nAND r.Container IN (");
            sql.append(getUserSchema().getContainerInList());
            sql.append(")\nAND r.Deleted='0')");
            sql.append(") AS pep ON (fi.MzXmlUrl=pep.MzXmlUrl AND fe.MS2Scan=pep.Scan AND fe.MS2Charge=pep.Charge)");
        }

        //add a base filter that includes only features in the schema's container list
        sql.append("\nWHERE d.Container IN (");
        sql.append(getUserSchema().getContainerInList());
        sql.append(")");

        //set a base filter condition to exclude deleted and unimported runs
        //unless the includeDeleted() flag is on
        if(!includeDeleted())
        {
            sql.append("\nAND ");
            sql.append("fi.Imported='1' AND fi.Deleted='0'");
        }

        //if there are other filters, apply them as well
        if(null != getBaseFilters())
        {
            Map<String,String> aliasMap = getAliasMap();
            for(FeaturesFilter filter : getBaseFilters())
            {
                sql.append("\nAND ");
                sql.append("(");
                sql.append(filter.getWhereClause(aliasMap, getSqlDialect()));
                sql.append(")");
            }
        }

        return sql;
    }


    public Map<String,String> getAliasMap()
    {
        HashMap<String,String> aliasMap = new HashMap<>();
        aliasMap.put(MS1Service.Tables.Features.getFullName(), "fe");
        aliasMap.put(MS1Service.Tables.Files.getFullName(), "fi");
        aliasMap.put("exp.Data", "d");
        aliasMap.put("ms2.Fractions", "pep");
        aliasMap.put("ms2.PeptidesData", "pep");
        aliasMap.put("ms2.Runs", "pep");
        return aliasMap;
    }

    protected void wrapAllColumns(boolean preserveHidden)
    {
        for (ColumnInfo col : getSourceTable().getColumns())
        {
            ColumnInfo newCol = new AliasedColumn(this, col.getName(), col);
            addColumn(newCol);
            if (preserveHidden && col.isHidden())
                newCol.setHidden(col.isHidden());
        }
    }

    public ColumnInfo wrapColumn(String alias, ColumnInfo underlyingColumn)
    {
        assert underlyingColumn.getParentTable() == getSourceTable();
        ExprColumn ret = new ExprColumn(this, alias, underlyingColumn.getValueSql(ExprColumn.STR_TABLE_ALIAS), underlyingColumn.getJdbcType());
        ret.copyAttributesFrom(underlyingColumn);
        ret.copyURLFrom(underlyingColumn, null, Collections.singletonMap(underlyingColumn.getFieldKey(), ret.getFieldKey()));
        ret.setLabel(ColumnInfo.labelFromName(alias));
        return ret;
    }

    protected TableInfo getSourceTable()
    {
        return _sourceTable;
    }

    @Override
    public boolean isPublic()
    {
        return true;
    }
    
} //class FeaturesTableInfo
