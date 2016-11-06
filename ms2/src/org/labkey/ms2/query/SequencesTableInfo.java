/*
 * Copyright (c) 2007-2016 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.AbstractForeignKey;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.util.StringExpression;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.CustomAnnotationSet;
import org.labkey.ms2.protein.CustomAnnotationType;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.query.CustomAnnotationSchema;
import org.labkey.ms2.protein.query.CustomAnnotationSetsTable;
import org.labkey.ms2.protein.query.CustomAnnotationTable;
import org.labkey.ms2.protein.query.ProteinUserSchema;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * User: jeckels
 * Date: Feb 9, 2007
 *
 * NOTE: The SequencesTableInfo is attached to both the MS2Schema and the ProteinUserSchema.
 */
public class SequencesTableInfo<SchemaType extends UserSchema> extends FilteredTable<SchemaType>
{
    protected SequencesTableInfo(String name, SchemaType schema)
    {
        this(schema);
        setName(name);
    }

    public SequencesTableInfo(SchemaType schema)
    {
        super(ProteinManager.getTableInfoSequences(), schema);
        setPublicSchemaName(ProteinUserSchema.NAME);
        setTitleColumn("BestName");
        wrapAllColumns(true);

        getColumn("OrgId").setFk(new QueryForeignKey(ProteinUserSchema.NAME, schema.getContainer(), null, schema.getUser(), ProteinUserSchema.TableType.Organisms.name(), null, null));

        addColumn(wrapColumn("Source", getRealTable().getColumn("SourceId")));
        getColumn("Source").setFk(new QueryForeignKey(schema, null, ProteinUserSchema.TableType.InfoSources.name(), null, null));
        removeColumn(getColumn("SourceId"));

        ActionURL url = new ActionURL(MS2Controller.ShowProteinAction.class, _userSchema.getContainer());
        url.addParameter("seqId", "${SeqId}");
        ColumnInfo bnColumn = getColumn("BestName");
        bnColumn.setURL(StringExpressionFactory.createURL(url));
        bnColumn.setURLTargetWindow("prot");

        setDetailsURL(new DetailsURL(url));

        ColumnInfo annotationColumn = wrapColumn("CustomAnnotations", _rootTable.getColumn("SeqId"));
        annotationColumn.setIsUnselectable(true);
        annotationColumn.setFk(new AbstractForeignKey()
        {
            public StringExpression getURL(ColumnInfo parent)
            {
                return null;
            }

            public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
            {
                if (displayField == null)
                    return null;

                for (final CustomAnnotationSet annotationSet : ProteinManager.getCustomAnnotationSets(_userSchema.getContainer(), true).values())
                {
                    if (displayField.equals(annotationSet.getName()))
                    {
                        SQLFragment sql = new SQLFragment();

                        sql.append("(SELECT MIN(CustomAnnotationId) FROM ");
                        sql.append(ProteinManager.getTableInfoCustomAnnotation());
                        CustomAnnotationType type = annotationSet.lookupCustomAnnotationType();
                        sql.append(" WHERE CustomAnnotationSetId = ? AND LookupString IN (");
                        sql.append(type.getLookupStringSelect(parent));
                        sql.append("))");
                        sql.add(annotationSet.getCustomAnnotationSetId());
                        ExprColumn ret = new ExprColumn(parent.getParentTable(), displayField,
                            sql, JdbcType.INTEGER, parent);
                        ret.setLabel(annotationSet.getName());
                        ret.setFk(new LookupForeignKey("CustomAnnotationId")
                        {
                            public TableInfo getLookupTableInfo()
                            {
                                return new CustomAnnotationTable(annotationSet, new CustomAnnotationSchema(_userSchema.getUser(), _userSchema.getContainer(), false));
                            }
                        });
                        return ret;
                    }
                }

                return null;
            }

            public TableInfo getLookupTableInfo()
            {
                return new CustomAnnotationSetsTable(SequencesTableInfo.this, new CustomAnnotationSchema(_userSchema.getUser(), _userSchema.getContainer(), false));
            }
        });
        addColumn(annotationColumn);

        ColumnInfo goMPColumn = wrapColumn("GOMetabolicProcesses", getRealTable().getColumn("SeqId"));
        goMPColumn.setLabel("GO Metabolic Processes");
        goMPColumn.setFk(new MultiValuedForeignKey(new QueryForeignKey(ProteinUserSchema.NAME, getUserSchema().getContainer(), null, getUserSchema().getUser(), "GOMetabolicProcess", "SeqId", "BestName"), "GOMPAnnotId"));
        addColumn(goMPColumn);

        ColumnInfo goCLColumn = wrapColumn("GOCellularLocations", getRealTable().getColumn("SeqId"));
        goCLColumn.setLabel("GO Cellular Locations");
        goCLColumn.setFk(new MultiValuedForeignKey(new QueryForeignKey(ProteinUserSchema.NAME, getUserSchema().getContainer(), null, getUserSchema().getUser(), "GOCellularLocation", "SeqId", "BestName"), "GOCLAnnotId"));
        addColumn(goCLColumn);

        ColumnInfo goMFColumn = wrapColumn("GOMolecularFunctions", getRealTable().getColumn("SeqId"));
        goMFColumn.setLabel("GO Molecular Functions");
        goMFColumn.setFk(new MultiValuedForeignKey(new QueryForeignKey(ProteinUserSchema.NAME, getUserSchema().getContainer(), null, getUserSchema().getUser(), "GOMolecularFunction", "SeqId", "BestName"), "GOMFAnnotId"));
        addColumn(goMFColumn);

        for (CustomAnnotationType type : CustomAnnotationType.values())
        {
            SQLFragment sql = new SQLFragment(type.getFirstSelectForSeqId());
            ExprColumn firstIdentColumn = new ExprColumn(this, "First" + type.toString(), sql, JdbcType.VARCHAR);
            firstIdentColumn.setLabel("First " + type.getDescription());
            addColumn(firstIdentColumn);
        }

        List<FieldKey> cols = new ArrayList<>();
        cols.add(FieldKey.fromParts("BestName"));
        cols.add(FieldKey.fromParts("Description"));
        cols.add(FieldKey.fromParts("BestGeneName"));
        cols.add(FieldKey.fromParts("Length"));
        cols.add(FieldKey.fromParts("Mass"));
        cols.add(FieldKey.fromParts("OrgId"));
        setDefaultVisibleColumns(cols);

    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        ColumnInfo col = super.resolveColumn(name);
        if (col != null)
            return col;

        if (name.equalsIgnoreCase("SourceId"))
            return super.getColumn("Source");

        return null;
    }

    public void addPeptideAggregationColumns()
    {
        ColumnInfo aaColumn = wrapColumn("AACoverage", getRealTable().getColumn("ProtSequence"));
        aaColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ColumnInfo peptideColumn = colInfo.getParentTable().getColumn("Peptide");
                ColumnInfo seqIdColumn = colInfo.getParentTable().getColumn("SeqId");
                return new QueryAACoverageColumn(colInfo, seqIdColumn, peptideColumn);
            }
        });
        addColumn(aaColumn);

        ColumnInfo totalCount = wrapColumn("Peptides", getRealTable().getColumn("SeqId"));
        totalCount.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ColumnInfo peptideColumn = colInfo.getParentTable().getColumn("Peptide");
                return new PeptideCountCoverageColumn(colInfo, peptideColumn, "Peptides");
            }
        });
        addColumn(totalCount);

        ColumnInfo uniqueCount = wrapColumn("UniquePeptides", getRealTable().getColumn("SeqId"));
        uniqueCount.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                ColumnInfo peptideColumn = colInfo.getParentTable().getColumn("Peptide");
                return new UniquePeptideCountCoverageColumn(colInfo, peptideColumn, "Unique");
            }
        });
        addColumn(uniqueCount);
    }

    /*package*/ static List<String> getIdentifierParameters(String identifiers)
    {
        List<String> result = new ArrayList<>();
        if (identifiers == null || identifiers.trim().equals(""))
        {
            return result;
        }

        StringTokenizer st = new StringTokenizer(identifiers, " \t\n\r,");
        while (st.hasMoreTokens())
        {
            result.add(st.nextToken());
        }
        return result;
    }

    public void addContainerCondition(Container c, User u, boolean includeSubfolders)
    {
        List<Container> containers = ContainerManager.getAllChildren(c, u);
        SQLFragment sql = new SQLFragment();
        sql.append("SeqId IN (SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences(), "fs");
        sql.append(", ");
        sql.append(MS2Manager.getTableInfoRuns(), "r");
        sql.append(", ");
        sql.append(MS2Manager.getTableInfoFastaRunMapping(), "frm");
        sql.append(" WHERE fs.FastaId = frm.FastaId AND frm.Run = r.Run AND r.Deleted = ? AND r.Container IN ");
        sql.add(Boolean.FALSE);
        if (includeSubfolders)
        {
            sql.append(ContainerManager.getIdsAsCsvList(new HashSet<>(containers)));
        }
        else
        {
            sql.append("('");
            sql.append(c.getId());
            sql.append("')");
        }
        sql.append(")");
        addCondition(sql);
    }

    public void addProteinNameFilter(String identifier, @NotNull MS2Controller.MatchCriteria matchCriteria)
    {
        List<String> params = getIdentifierParameters(identifier);
        SQLFragment sql = new SQLFragment();
        sql.append("SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoSequences(), "s");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "s.BestName"));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations(), "a");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "a.AnnotVal"));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences(), "fs");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "fs.lookupstring"));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers(), "i");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "i.Identifier"));
        sql.append("\n");
        sql.append(")");
        addCondition(sql);
    }

    public void addSeqIdFilter(int[] seqIds)
    {
        SQLFragment sql = new SQLFragment("SeqId IN (");
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
                sql.append(Long.toString(seqId));
            }
        }
        sql.append(")");
        addCondition(sql, FieldKey.fromParts("SeqId"));
    }
}
