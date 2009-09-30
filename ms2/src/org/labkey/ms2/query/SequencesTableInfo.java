/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.security.ACL;
import org.labkey.api.security.User;
import org.labkey.api.util.StringExpression;
import org.labkey.api.util.StringExpressionFactory;
import org.labkey.api.view.ActionURL;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.protein.CustomAnnotationSet;
import org.labkey.ms2.protein.CustomAnnotationType;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.query.CustomAnnotationSetsTable;
import org.labkey.ms2.protein.query.CustomAnnotationTable;

import java.sql.Types;
import java.util.*;

/**
 * User: jeckels
 * Date: Feb 9, 2007
 */
public class SequencesTableInfo extends FilteredTable
{
    private final Container _container;
    private final QuerySchema _schema;

    protected SequencesTableInfo(String name, QuerySchema schema)
    {
        this(schema);
        setName(name);
    }

    public SequencesTableInfo(QuerySchema schema)
    {
        super(ProteinManager.getTableInfoSequences());
        _schema = schema;
        _container = schema.getContainer();
        setTitleColumn("BestName");
        wrapAllColumns(true);

        getColumn("OrgId").setFk(new LookupForeignKey("OrgId", "Description")
        {
            public TableInfo getLookupTableInfo()
            {
                return new OrganismTableInfo();
            }
        });

        addColumn(wrapColumn("Source", getRealTable().getColumn("SourceId")));
        getColumn("SourceId").setHidden(true);

        ActionURL url = new ActionURL(MS2Controller.ShowProteinAction.class, _container);
        url.addParameter("seqId", "${SeqId}");
        ColumnInfo bnColumn = getColumn("BestName");
        bnColumn.setURL(StringExpressionFactory.createURL(url));
        bnColumn.setDisplayColumnFactory(new DisplayColumnFactory() {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                DataColumn dc = new DataColumn(colInfo);
                dc.setLinkTarget("prot");
                return dc;
            }
        });

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

                for (final CustomAnnotationSet annotationSet : ProteinManager.getCustomAnnotationSets(_container, true).values())
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
                            sql, Types.INTEGER, parent);
                        ret.setLabel(annotationSet.getName());
                        ret.setFk(new LookupForeignKey("CustomAnnotationId")
                        {
                            public TableInfo getLookupTableInfo()
                            {
                                return new CustomAnnotationTable(annotationSet, _schema);
                            }
                        });
                        return ret;
                    }
                }

                return null;
            }

            public TableInfo getLookupTableInfo()
            {
                return new CustomAnnotationSetsTable(SequencesTableInfo.this, _schema, _container);
            }
        });
        addColumn(annotationColumn);

        for (CustomAnnotationType type : CustomAnnotationType.values())
        {
            SQLFragment sql = new SQLFragment(type.getFirstSelectForSeqId());
            ExprColumn firstIdentColumn = new ExprColumn(this, "First" + type.toString(), sql, Types.VARCHAR);
            firstIdentColumn.setLabel("First " + type.getDescription());
            addColumn(firstIdentColumn);
        }

        List<FieldKey> cols = new ArrayList<FieldKey>();
        cols.add(FieldKey.fromParts("BestName"));
        cols.add(FieldKey.fromParts("Description"));
        cols.add(FieldKey.fromParts("BestGeneName"));
        cols.add(FieldKey.fromParts("Length"));
        cols.add(FieldKey.fromParts("Mass"));
        cols.add(FieldKey.fromParts("OrgId"));
        setDefaultVisibleColumns(cols);
    }

    @Override
    public String getPublicSchemaName()
    {
        return MS2Schema.SCHEMA_NAME;
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
        List<String> result = new ArrayList<String>();
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

    /*package*/ static String getIdentifierClause(List<String> params, String columnName, boolean exactMatch)
    {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        sb.append("(");
        if (params.isEmpty())
        {
            sb.append("1 = 2");
        }
        for (String param : params)
        {
            sb.append(separator);
            sb.append(columnName);
            if (exactMatch)
            {
                sb.append(" = '");
                sb.append(param.replaceAll("'", "''"));
                sb.append("'");
            }
            else
            {
                sb.append(" LIKE '");
                sb.append(param.replaceAll("'", "''"));
                sb.append("%'");
            }
            separator = " OR ";
        }
        sb.append(")");
        return sb.toString();
    }

    public void addContainerCondition(Container c, User u, boolean includeSubfolders)
    {
        Set<Container> containers = ContainerManager.getAllChildren(c, u, ACL.PERM_READ);
        SQLFragment sql = new SQLFragment();
        sql.append("SeqId IN (SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences(), "fs");
        sql.append(", ");
        sql.append(MS2Manager.getTableInfoRuns(), "r");
        sql.append(" WHERE fs.FastaId = r.FastaId AND r.Deleted = ? AND r.Container IN ");
        sql.add(Boolean.FALSE);
        if (includeSubfolders)
        {
            sql.append(ContainerManager.getIdsAsCsvList(new HashSet<Container>(containers)));
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

    public void addProteinNameFilter(String identifier, boolean exactMatch)
    {
        List<String> params = getIdentifierParameters(identifier);
        SQLFragment sql = new SQLFragment();
        sql.append("SeqId IN (\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoAnnotations(), "a");
        sql.append(" WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "a.AnnotVal", exactMatch));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoFastaSequences(), "fs");
        sql.append(" WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "fs.lookupstring", exactMatch));
        sql.append("\n");
        sql.append("UNION\n");
        sql.append("SELECT SeqId FROM ");
        sql.append(ProteinManager.getTableInfoIdentifiers(), "i");
        sql.append(" WHERE ");
        sql.append(SequencesTableInfo.getIdentifierClause(params, "i.Identifier", exactMatch));
        sql.append("\n");
        sql.append(")");
        addCondition(sql);
    }

    public void addSeqIdFilter(Integer[] seqIds)
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
        addCondition(sql, "SeqId");
    }
}
