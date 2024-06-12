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

package org.labkey.api.protein.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.AbstractForeignKey;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.ForeignKey;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.MultiValuedForeignKey;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.protein.CustomAnnotationSet;
import org.labkey.api.protein.CustomAnnotationSetManager;
import org.labkey.api.protein.CustomAnnotationType;
import org.labkey.api.protein.MatchCriteria;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.UserSchema;
import org.labkey.api.util.StringExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Consumer;

/**
 * NOTE: The SequencesTableInfo is attached to both the MS2Schema and the ProteinUserSchema.
 */
public class SequencesTableInfo<SchemaType extends UserSchema> extends FilteredTable<SchemaType>
{
    // A module (e.g., MS2) can set a table modifier to add module-specific columns and URLs to this table
    private static Consumer<SequencesTableInfo<?>> TABLE_MODIFIER = sequencesTableInfo -> {}; // No-op by default

    public static void setTableModifier(Consumer<SequencesTableInfo<?>> tableModifier)
    {
        TABLE_MODIFIER = tableModifier;
    }

    public SequencesTableInfo(String name, SchemaType schema, ContainerFilter cf)
    {
        this(schema, cf);
        setName(name);
    }

    public SequencesTableInfo(SchemaType schema, ContainerFilter cf)
    {
        super(ProteinSchema.getTableInfoSequences(), schema, cf);
        setPublicSchemaName(ProteinUserSchema.NAME);
        setTitleColumn("BestName");
        wrapAllColumns(true);

        getMutableColumn("OrgId").setFk( QueryForeignKey.from(schema, getContainerFilter()).schema(ProteinUserSchema.NAME).table(ProteinUserSchema.TableType.Organisms.name()) );

        addColumn(wrapColumn("Source", getRealTable().getColumn("SourceId")));
        getMutableColumn("Source").setFk( QueryForeignKey.from(schema, getContainerFilter()).to(ProteinUserSchema.TableType.InfoSources.name(), null, null) );
        removeColumn(getColumn("SourceId"));

        TABLE_MODIFIER.accept(this);

        var annotationColumn = wrapColumn("CustomAnnotations", _rootTable.getColumn("SeqId"));
        annotationColumn.setIsUnselectable(true);
        annotationColumn.setFk(new AbstractForeignKey(schema, cf)
        {
            @Override
            public StringExpression getURL(ColumnInfo parent)
            {
                return null;
            }

            @Override
            public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
            {
                if (displayField == null)
                    return null;

                for (final CustomAnnotationSet annotationSet : CustomAnnotationSetManager.getCustomAnnotationSets(_userSchema.getContainer(), true).values())
                {
                    if (displayField.equals(annotationSet.getName()))
                    {
                        SQLFragment sql = new SQLFragment();

                        sql.append("(SELECT MIN(CustomAnnotationId) FROM ");
                        sql.append(ProteinSchema.getTableInfoCustomAnnotation(), "ca");
                        CustomAnnotationType type = annotationSet.lookupCustomAnnotationType();
                        sql.append(" WHERE CustomAnnotationSetId = ? AND LookupString IN (");
                        sql.append(type.getLookupStringSelect(parent));
                        sql.append("))");
                        sql.add(annotationSet.getCustomAnnotationSetId());
                        ExprColumn ret = new ExprColumn(parent.getParentTable(), displayField,
                            sql, JdbcType.INTEGER, parent);
                        ret.setLabel(annotationSet.getName());
                        ForeignKey fk = new QueryForeignKey.Builder(schema, cf).
                                schema(CustomAnnotationSchema.SCHEMA_WITHOUT_SEQUENCES_NAME).
                                table(annotationSet.getName()).
                                key("CustomAnnotationId").
                                build();
                        ret.setFk(fk);
                        return ret;
                    }
                }

                return null;
            }

            @Override
            public TableInfo getLookupTableInfo()
            {
                return new CustomAnnotationSetsTable(SequencesTableInfo.this, new CustomAnnotationSchema(_userSchema.getUser(), _userSchema.getContainer(), false));
            }
        });
        addColumn(annotationColumn);

        var goMPColumn = wrapColumn("GOMetabolicProcesses", getRealTable().getColumn("SeqId"));
        goMPColumn.setLabel("GO Metabolic Processes");
        goMPColumn.setFk(new MultiValuedForeignKey(QueryForeignKey.from(schema, cf).schema(ProteinUserSchema.NAME).to("GOMetabolicProcess", "SeqId", "BestName"), "GOMPAnnotId"));
        addColumn(goMPColumn);

        var goCLColumn = wrapColumn("GOCellularLocations", getRealTable().getColumn("SeqId"));
        goCLColumn.setLabel("GO Cellular Locations");
        goCLColumn.setFk(new MultiValuedForeignKey(QueryForeignKey.from(schema, cf).schema(ProteinUserSchema.NAME).to("GOCellularLocation", "SeqId", "BestName"), "GOCLAnnotId"));
        addColumn(goCLColumn);

        var goMFColumn = wrapColumn("GOMolecularFunctions", getRealTable().getColumn("SeqId"));
        goMFColumn.setLabel("GO Molecular Functions");
        goMFColumn.setFk(new MultiValuedForeignKey(QueryForeignKey.from(schema, cf).schema(ProteinUserSchema.NAME).to("GOMolecularFunction", "SeqId", "BestName"), "GOMFAnnotId"));
        addColumn(goMFColumn);

        for (CustomAnnotationType type : CustomAnnotationType.values())
        {
            SQLFragment sql = new SQLFragment(type.getFirstSelectForSeqId());
            ExprColumn firstIdentColumn = new ExprColumn(this, "First" + type, sql, JdbcType.VARCHAR);
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

    public static List<String> getIdentifierParameters(String identifiers)
    {
        List<String> result = new ArrayList<>();
        if (identifiers == null || identifiers.trim().isEmpty())
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

    public void addProteinNameFilter(String identifier, @NotNull MatchCriteria matchCriteria)
    {
        List<String> params = getIdentifierParameters(identifier);
        SQLFragment sql = new SQLFragment();
        sql.append("SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinSchema.getTableInfoSequences(), "s");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "s.BestName"));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinSchema.getTableInfoAnnotations(), "a");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "a.AnnotVal"));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinSchema.getTableInfoFastaSequences(), "fs");
        sql.append(" WHERE ");
        sql.append(matchCriteria.getIdentifierClause(params, "fs.lookupstring"));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinSchema.getTableInfoIdentifiers(), "i");
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
