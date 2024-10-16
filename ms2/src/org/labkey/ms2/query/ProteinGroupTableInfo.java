/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.ms2.query;

import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.protein.MatchCriteria;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.protein.query.SequencesTableInfo;
import org.labkey.api.protein.search.ProbabilityProteinSearchForm;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.security.User;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.ViewContext;
import org.labkey.ms2.GroupNumberDisplayColumn;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.ProteinListDisplayColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProteinGroupTableInfo extends FilteredTable<MS2Schema>
{
    private static final Set<String> HIDDEN_PROTEIN_GROUP_COLUMN_NAMES = Collections.unmodifiableSet(new CaseInsensitiveHashSet(Arrays.asList("RowId", "GroupNumber", "IndistinguishableCollectionId", "Deleted", "HasPeptideProphet")));
    private static final Set<String> HIDDEN_PROTEIN_GROUP_MEMBERSHIPS_COLUMN_NAMES = Collections.unmodifiableSet(new CaseInsensitiveHashSet("ProteinGroupId", "SeqId"));
    private List<MS2Run> _runs;

    public ProteinGroupTableInfo(MS2Schema schema, ContainerFilter cf)
    {
        this(schema, cf, true);
    }

    public ProteinGroupTableInfo(MS2Schema schema, ContainerFilter cf, boolean includeFirstProteinColumn)
    {
        super(MS2Manager.getTableInfoProteinGroups(), schema, cf);

        var groupNumberColumn = wrapColumn("Group", getRealTable().getColumn("GroupNumber"));
        groupNumberColumn.setDisplayColumnFactory(colInfo -> new GroupNumberDisplayColumn(colInfo, _userSchema.getContainer()));

        addColumn(groupNumberColumn);

        wrapAllColumns(true);

        setTitleColumn("Group");
        addColumn(wrapColumn("ProteinProphet", getRealTable().getColumn("ProteinProphetFileId")));
        getMutableColumn("ProteinProphetFileId").setHidden(true);

        var quantitation = wrapColumn("Quantitation", getRealTable().getColumn("RowId"));
        quantitation.setIsUnselectable(true);
        quantitation.setFk(new LookupForeignKey("ProteinGroupId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return new ProteinQuantitationTable(_userSchema, cf);
            }
        });
        quantitation.setKeyField(false);
        addColumn(quantitation);

        var iTraqQuantitation = wrapColumn("iTRAQQuantitation", getRealTable().getColumn("RowId"));
        iTraqQuantitation.setLabel("iTRAQ Quantitation");
        iTraqQuantitation.setIsUnselectable(true);
        iTraqQuantitation.setFk(new LookupForeignKey("ProteinGroupId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return new ITraqProteinQuantitationTable(_userSchema, cf);
            }
        });
        iTraqQuantitation.setKeyField(false);
        addColumn(iTraqQuantitation);

        for (var col : getMutableColumns())
        {
            if (HIDDEN_PROTEIN_GROUP_COLUMN_NAMES.contains(col.getName()))
            {
                col.setHidden(true);
            }
        }

        LookupForeignKey foreignKey = new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return new ProteinProphetFileTableInfo(_userSchema, cf);
            }
        };
        foreignKey.setPrefixColumnCaption(false);
        getMutableColumn("ProteinProphetFileId").setFk(foreignKey);
        getMutableColumn("ProteinProphet").setFk(foreignKey);

        if (includeFirstProteinColumn)
        {
            SQLFragment firstProteinSQL = new SQLFragment();
            firstProteinSQL.append("SELECT s.SeqId FROM ");
            firstProteinSQL.append(MS2Manager.getTableInfoProteinGroupMemberships(), "pgm");
            firstProteinSQL.append(", ");
            firstProteinSQL.append(ProteinSchema.getTableInfoSequences(), "s");
            firstProteinSQL.append(" WHERE s.SeqId = pgm.SeqId AND pgm.ProteinGroupId = ");
            firstProteinSQL.append(ExprColumn.STR_TABLE_ALIAS);
            firstProteinSQL.append(".RowId ORDER BY s.Length, s.BestName");
            ProteinSchema.getSqlDialect().limitRows(firstProteinSQL, 1);
            firstProteinSQL.insert(0, "(");
            firstProteinSQL.append(")");

            ExprColumn firstProteinColumn = new ExprColumn(this, "FirstProtein", firstProteinSQL, JdbcType.INTEGER);
            firstProteinColumn.setFk(new LookupForeignKey(cf, "SeqId", null)
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new SequencesTableInfo<>(null, _userSchema, getLookupContainerFilter());
                }
            });
            addColumn(firstProteinColumn);
        }

        SQLFragment proteinCountSQL = new SQLFragment();
        proteinCountSQL.append("(SELECT COUNT(SeqId) FROM ");
        proteinCountSQL.append(MS2Manager.getTableInfoProteinGroupMemberships(), "pm");
        proteinCountSQL.append(" WHERE ProteinGroupId = ");
        proteinCountSQL.append(ExprColumn.STR_TABLE_ALIAS);
        proteinCountSQL.append(".RowId)");
        ExprColumn proteinCountColumn = new ExprColumn(this, "ProteinCount", proteinCountSQL, JdbcType.INTEGER);
        addColumn(proteinCountColumn);

        List<FieldKey> defaultColumns = new ArrayList<>();
        defaultColumns.add(FieldKey.fromParts("Group"));
        defaultColumns.add(FieldKey.fromParts("GroupProbability"));
        defaultColumns.add(FieldKey.fromParts("ErrorRate"));
        defaultColumns.add(FieldKey.fromParts("UniquePeptidesCount"));
        defaultColumns.add(FieldKey.fromParts("TotalNumberPeptides"));

        setDefaultVisibleColumns(defaultColumns);
    }

    public void addPeptideFilter(ProbabilityProteinSearchForm form, ViewContext context)
    {
        if (form.isNoPeptideFilter())
        {
            return;
        }

        SQLFragment peptidesSQL;

        Set<FieldKey> peptideFieldKeys = Collections.singleton(FieldKey.fromParts("RowId"));
        if (form.isCustomViewPeptideFilter())
        {
            peptidesSQL = _userSchema.getPeptideSelectSQL(context.getRequest(), form.getCustomViewName(context), peptideFieldKeys, null);
        }
        else
        {
            SimpleFilter filter = new SimpleFilter();
            if (form.isPeptideProphetFilter() && form.getPeptideProphetProbability() != null)
            {
                filter.addClause(new CompareType.CompareClause(FieldKey.fromParts("PeptideProphet"), CompareType.GTE, form.getPeptideProphetProbability()));
            }
            peptidesSQL = _userSchema.getPeptideSelectSQL(filter, peptideFieldKeys);
        }
        SQLFragment condition = new SQLFragment();
        condition.append("RowId IN (SELECT ProteinGroupId FROM ").append(String.valueOf(MS2Manager.getTableInfoPeptideMemberships())).append(" WHERE PeptideId IN (");
        condition.append(peptidesSQL);
        condition.append("))");
        addCondition(condition, FieldKey.fromParts("RowId"));
    }

    public void addProteinsColumn(ContainerFilter cf)
    {
        var proteinGroup = wrapColumn("Proteins", getRealTable().getColumn("RowId"));
        LookupForeignKey fk = new LookupForeignKey(getContainerFilter(), "ProteinGroupId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                TableInfo info = MS2Manager.getTableInfoProteinGroupMemberships();
                FilteredTable<?> result = new FilteredTable<>(info, getUserSchema(), cf);
                for (ColumnInfo col : info.getColumns())
                {
                    var newColumn = result.addWrapColumn(col);
                    if (HIDDEN_PROTEIN_GROUP_MEMBERSHIPS_COLUMN_NAMES.contains(newColumn.getName()))
                    {
                        newColumn.setHidden(true);
                    }
                }

                var proteinColumn = result.wrapColumn("Protein", info.getColumn("SeqId"));
                proteinColumn.setDisplayColumnFactory(colInfo -> {
                    DataColumn result1 = new DataColumn(colInfo);
                    result1.setLinkTarget("prot");

                    ActionURL url = new ActionURL(MS2Controller.ShowProteinAction.class, _userSchema.getContainer());
                    if (_runs != null && _runs.size() == 1)
                    {
                        url.addParameter("run", Integer.toString(_runs.get(0).getRun()));
                    }
                    result1.setURL(url.addParameter("proteinGroupId", "${RowId}").addParameter("seqId", "${" + colInfo.getName() + "}"));
                    return result1;
                });
                result.addColumn(proteinColumn);

                proteinColumn.setFk(new LookupForeignKey(getContainerFilter(), "SeqId", "DatabaseSequenceName")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        SequencesTableInfo<?> result = new SequencesTableInfo<>(_userSchema, getLookupContainerFilter());
                        ExprColumn col = new ExprColumn(result, "DatabaseSequenceName", new SQLFragment("#PLACEHOLDER#"), JdbcType.VARCHAR)
                        {
                            @Override
                            public SQLFragment getValueSql(String tableAlias)
                            {
                                SQLFragment sql = new SQLFragment();
                                sql.append("SELECT LookupString FROM ");
                                sql.append(ProteinSchema.getTableInfoFastaSequences(), "fs");
                                sql.append(", ");
                                sql.append(MS2Manager.getTableInfoRuns(), "r");
                                sql.append(", ");
                                sql.append(MS2Manager.getTableInfoFastaRunMapping(), "frm");
                                sql.append(", ");
                                sql.append(MS2Manager.getTableInfoProteinProphetFiles(), "ppf");
                                sql.append("\nWHERE fs.SeqId = ");
                                sql.append(tableAlias);
                                sql.append(".SeqId AND fs.FastaId = frm.FastaId AND r.Run = frm.Run AND r.Run = ppf.Run AND ppf.RowId = ");
                                SQLFragment fileid = ProteinGroupTableInfo.this.getColumn("ProteinProphetFileId").getValueSql(ExprColumn.STR_TABLE_ALIAS);
                                // HACK: This is egregious.  We can't just pluck this column out of thin air!
                                SQLFragment fixupFileId = new SQLFragment(fileid.getSQL().replace(ExprColumn.STR_TABLE_ALIAS + ".", ""), fileid.getParams());
                                // HACK
                                sql.append(fixupFileId);
                                sql.append(" ORDER BY LookupString");
                                getSchema().getSqlDialect().limitRows(sql, 1);
                                // Wrap the whole thing in parenthesis after the LIMIT/TOP has been applied
                                sql.insert(0, "(");
                                sql.append(")");
                                return sql;
                            }
                        };

                        result.addColumn(col);
                        return result;
                    }
                });
                return result;
            }
        };
        fk.setPrefixColumnCaption(false);
        proteinGroup.setFk(fk);
        proteinGroup.setKeyField(false);
        addColumn(proteinGroup);
    }

    public void addProteinDetailColumns()
    {
        ColumnInfo rowIdColumn = _rootTable.getColumn("RowId");

        DisplayColumnFactory factory = colInfo -> {
            _userSchema.getProteinGroupProteins().setRuns(_userSchema.getRuns());
            ProteinListDisplayColumn result = new ProteinListDisplayColumn(colInfo.getColumnName(), _userSchema.getProteinGroupProteins());
            result.setColumnInfo(colInfo);
            return result;
        };

        var proteinNameColumn = wrapColumn("Protein", rowIdColumn);
        proteinNameColumn.setDisplayColumnFactory(factory);
        addColumn(proteinNameColumn);

        var bestNameColumn = wrapColumn("BestName", rowIdColumn);
        bestNameColumn.setDisplayColumnFactory(factory);
        addColumn(bestNameColumn);

        var bestGeneNameColumn = wrapColumn("BestGeneName", rowIdColumn);
        bestGeneNameColumn.setDisplayColumnFactory(factory);
        addColumn(bestGeneNameColumn);

        var massColumn = wrapColumn("SequenceMass", rowIdColumn);
        massColumn.setDisplayColumnFactory(factory);
        addColumn(massColumn);

        var descriptionColumn = wrapColumn("Description", rowIdColumn);
        descriptionColumn.setDisplayColumnFactory(factory);
        addColumn(descriptionColumn);


        var totalCount = wrapColumn("TotalFilteredPeptides", rowIdColumn);
        totalCount.setDisplayColumnFactory(colInfo -> {
            ColumnInfo peptideColumn = colInfo.getParentTable().getColumn("Peptide");
            return new PeptideCountCoverageColumn(colInfo, peptideColumn, "TotalFilteredPeptides");
        });
        addColumn(totalCount);

        var uniqueCount = wrapColumn("UniqueFilteredPeptides", rowIdColumn);
        uniqueCount.setDisplayColumnFactory(colInfo -> {
            ColumnInfo peptideColumn = colInfo.getParentTable().getColumn("Peptide");
            return new UniquePeptideCountCoverageColumn(colInfo, peptideColumn, "UniqueFilteredPeptides");
        });
        addColumn(uniqueCount);
    }

    public void addContainerCondition(Container c, User u, boolean includeSubfolders)
    {
        SqlDialect d = MS2Manager.getSqlDialect();
        SQLFragment sql = new SQLFragment();
        sql.append("ProteinProphetFileId IN (SELECT ppf.RowId FROM ");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles(), "ppf");
        sql.append(", ");
        sql.append(MS2Manager.getTableInfoRuns(), "r");
        sql.append(" WHERE ppf.run = r.run AND r.Deleted = ? AND r.Container IN ");
        sql.add(Boolean.FALSE);
        if (includeSubfolders)
        {
            List<Container> containers = ContainerManager.getAllChildren(c, u);
            sql.append(ContainerManager.getIdsAsCsvList(new HashSet<>(containers),d));
        }
        else
        {
            sql.append("(");
            sql.appendValue(c,d);
            sql.append(")");
        }
        sql.append(")");
        addCondition(sql);
    }

    public void addSeqIdFilter(int[] seqIds)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("RowId IN (\n");
        sql.append("SELECT ProteinGroupId FROM ");
        sql.append(MS2Manager.getTableInfoProteinGroupMemberships(), "pgm");
        sql.append(" WHERE pgm.SeqId IN (\n");
        if (seqIds.length == 0)
        {
            sql.append("NULL");
        }
        else
        {
            String separator = "";
            for (long seqId : seqIds)
            {
                sql.append(separator);
                separator = ", ";
                sql.appendValue(seqId);
            }
        }
        sql.append("))");

        addCondition(sql);
    }
    
    public void addProteinNameFilter(String identifier, MatchCriteria matchCriteria)
    {
        List<String> params = SequencesTableInfo.getIdentifierParameters(identifier);
        SQLFragment sql = new SQLFragment();
        sql.append("RowId IN (\n");
        sql.append("SELECT ProteinGroupId FROM ");
        sql.append(MS2Manager.getTableInfoProteinGroupMemberships(), "pgm");
        sql.append(" WHERE pgm.SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinSchema.getTableInfoAnnotations(), "a");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "a.AnnotVal"));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinSchema.getTableInfoFastaSequences(), "fs");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "fs.LookupString"));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinSchema.getTableInfoIdentifiers(), "i");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "i.Identifier"));
        sql.append("\n");
        sql.append("))");
        addCondition(sql);
    }

    public void setRunFilter(List<MS2Run> runs)
    {
        _runs = runs;
        SQLFragment sql = new SQLFragment();
        sql.append("ProteinProphetFileId IN (SELECT RowId FROM ");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles(), "ppf");
        sql.append(" WHERE Run IN (SELECT Run FROM ");
        sql.append(MS2Manager.getTableInfoRuns(), "r");
        sql.append(" WHERE ");
        sql.append(getContainerFilter().getSQLFragment(getSchema(), new SQLFragment("Container")));
        sql.append(" AND Deleted = ?");
        sql.add(Boolean.FALSE);
        if (runs != null)
        {
            assert !runs.isEmpty() : "Doesn't make sense to filter to no runs";
            sql.append(" AND Run IN (");
            String separator = "";
            for (MS2Run run : runs)
            {
                sql.append(separator);
                sql.appendValue(run.getRun());
                separator = ", ";
            }
            sql.append(")");
        }
        sql.append("))");
        addCondition(sql);
    }
}
