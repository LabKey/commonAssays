/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.nab.query;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.query.QcAwarePropertyForeignKey;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.SpecimenPropertyColumnDecorator;
import org.labkey.nab.NabManager;
import org.labkey.nab.NabManager.PropDescCategory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: davebradlee
 * Date: 2/20/13
 * Time: 6:36 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class NabRunDataBaseTable extends FilteredTable<AssaySchema>
{
    public static final String RUN_ID_COLUMN_NAME = "RunId";

    public abstract PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol);
    public abstract String getInputMaterialPropertyName();
    public abstract String getDataRowLsidPrefix();

    private NabProtocolSchema _schema;
    private NAbSpecimenTable _nabSpecimenTable;
    private ExpProtocol _protocol;

    public NabRunDataBaseTable(final NabProtocolSchema schema, final ExpProtocol protocol)
    {
        super(new NAbSpecimenTable(schema), schema);
        _nabSpecimenTable = (NAbSpecimenTable)getRealTable();
        _schema = schema;
        _protocol = protocol;

        final AssayProvider provider = AssayService.get().getProvider(protocol);
        List<FieldKey> visibleColumns = new ArrayList<FieldKey>();

        // add any property columns
        addPropertyColumns(schema, protocol, provider, visibleColumns);

        // TODO - we should have a more reliable (and speedier) way of identifying just the data rows here
        SQLFragment dataRowClause = new SQLFragment("ObjectURI LIKE '%" + getDataRowLsidPrefix() + "%'");
        addCondition(dataRowClause, FieldKey.fromParts("ObjectURI"));

        ExprColumn runColumn = new ExprColumn(this, "Run", new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".RunID"), JdbcType.INTEGER);
        runColumn.setFk(new LookupForeignKey("RowID")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpRunTable expRunTable = AssayService.get().createRunTable(protocol, provider, schema.getUser(), schema.getContainer());
                expRunTable.setContainerFilter(getContainerFilter());
                return expRunTable;
            }
        });
        addColumn(runColumn);

        ExprColumn runIdColumn = new ExprColumn(this, RUN_ID_COLUMN_NAME, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".RunID"), JdbcType.INTEGER);
        ColumnInfo addedRunIdColumn = addColumn(runIdColumn);
        addedRunIdColumn.setHidden(true);

        Set<String> hiddenProperties = new HashSet<String>();
        hiddenProperties.add(AbstractAssayProvider.PARTICIPANTID_PROPERTY_NAME);
        hiddenProperties.add(AbstractAssayProvider.PARTICIPANT_VISIT_RESOLVER_PROPERTY_NAME);
        Domain runDomain = provider.getRunDomain(protocol);
        for (DomainProperty prop : runDomain.getProperties())
        {
            if (!hiddenProperties.contains(prop.getName()))
                visibleColumns.add(FieldKey.fromParts("Run", prop.getName()));
        }
        Domain uploadSetDomain = provider.getBatchDomain(protocol);
        for (DomainProperty prop : uploadSetDomain.getProperties())
        {
            if (!hiddenProperties.contains(prop.getName()))
                visibleColumns.add(FieldKey.fromParts("Run", AssayService.BATCH_COLUMN_NAME, prop.getName()));
        }
        setDefaultVisibleColumns(visibleColumns);
    }

    protected void addPropertyColumns(final NabProtocolSchema schema, final ExpProtocol protocol, final AssayProvider provider, List<FieldKey> visibleColumns)
    {
        final CutoffValueTable cutoffValueTable = new CutoffValueTable(schema);
        cutoffValueTable.removeContainerFilter();

        // add material lookup columns to the view first, so they appear at the left:
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(protocol, AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);
        if (sampleSet != null)
        {
            for (DomainProperty pd : sampleSet.getPropertiesForType())
            {
                if (!NabManager.useNewNab)
                    visibleColumns.add(FieldKey.fromParts("Properties", getInputMaterialPropertyName(), ExpMaterialTable.Column.Property.toString(), pd.getName()));
                else
                    visibleColumns.add(FieldKey.fromParts(getInputMaterialPropertyName(), ExpMaterialTable.Column.Property.toString(), pd.getName()));
            }
        }
        // get all the properties from this plated-based protocol:
        PropertyDescriptor[] pds = getExistingDataProperties(protocol);

        // add object ID to this tableinfo and set it as a key field:
        ColumnInfo objectIdColumn = addWrapColumn(_rootTable.getColumn("ObjectId"));

        if (NabManager.useNewNab)
        {
            ColumnInfo objectUriColumn = addWrapColumn(_rootTable.getColumn("ObjectUri"));
            objectUriColumn.setIsUnselectable(true);
            objectUriColumn.setHidden(true);
            ColumnInfo rowIdColumn = addWrapColumn(_rootTable.getColumn("RowId"));
            rowIdColumn.setKeyField(true);
            rowIdColumn.setHidden(true);
            rowIdColumn.setIsUnselectable(true);
        }
        else
        {
            objectIdColumn.setKeyField(true);
        }

        // add object ID again, this time as a lookup to a virtual property table that contains our selected NAB properties:

        QcAwarePropertyForeignKey fk = new QcAwarePropertyForeignKey(pds, this, schema)             // Needed by NewNab only to ger defaultHiddenProperties
        {
            @Override
            protected ColumnInfo constructColumnInfo(ColumnInfo parent, FieldKey name, PropertyDescriptor pd)
            {
                ColumnInfo result = super.constructColumnInfo(parent, name, pd);
                if (getInputMaterialPropertyName().equals(pd.getName()))
                {
                    result.setLabel("Specimen");
                    result.setFk(new LookupForeignKey("LSID")
                    {
                        public TableInfo getLookupTableInfo()
                        {
                            ExpMaterialTable materials = ExperimentService.get().createMaterialTable(ExpSchema.TableType.Materials.toString(), schema);
                            // Make sure we are filtering to the same set of containers
                            materials.setContainerFilter(getContainerFilter());
                            if (sampleSet != null)
                            {
                                materials.setSampleSet(sampleSet, true);
                            }
                            ColumnInfo propertyCol = materials.addColumn(ExpMaterialTable.Column.Property);
                            if (propertyCol.getFk() instanceof PropertyForeignKey)
                            {
                                ((PropertyForeignKey)propertyCol.getFk()).addDecorator(new SpecimenPropertyColumnDecorator(provider, protocol, schema));
                            }
                            propertyCol.setHidden(false);
                            materials.addColumn(ExpMaterialTable.Column.LSID).setHidden(true);
                            return materials;
                        }
                    });
                }
                return result;
            }
        };

        ColumnInfo propertyLookupColumn = wrapColumn("Properties", _rootTable.getColumn("ObjectUri"));      // TODO: Will go away with NewNab?
        if (!NabManager.useNewNab)
        {
            propertyLookupColumn.setKeyField(false);
            propertyLookupColumn.setIsUnselectable(true);
            propertyLookupColumn.setFk(fk);
            addColumn(propertyLookupColumn);
        }
        else
        {
        }

        Set<String> hiddenCols = getHiddenColumns(protocol);
        for (PropertyDescriptor pd : fk.getDefaultHiddenProperties())
            hiddenCols.add(pd.getName());
        hiddenCols.add(getInputMaterialPropertyName());

        if (NabManager.useNewNab)
        {
            ColumnInfo specimenColumn = wrapColumn(getInputMaterialPropertyName(), _rootTable.getColumn("SpecimenLsid"));
            specimenColumn.setKeyField(false);
            specimenColumn.setIsUnselectable(true);
            LookupForeignKey lfkSpecimen = new LookupForeignKey("LSID")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    ExpMaterialTable materials = ExperimentService.get().createMaterialTable(ExpSchema.TableType.Materials.toString(), schema);
                    // Make sure we are filtering to the same set of containers
                    materials.setContainerFilter(getContainerFilter());
                    if (sampleSet != null)
                    {
                        materials.setSampleSet(sampleSet, true);
                    }
                    ColumnInfo propertyCol = materials.addColumn(ExpMaterialTable.Column.Property);
                    if (propertyCol.getFk() instanceof PropertyForeignKey)
                    {
                        ((PropertyForeignKey)propertyCol.getFk()).addDecorator(new SpecimenPropertyColumnDecorator(provider, protocol, schema));
                    }
                    propertyCol.setHidden(false);
                    materials.addColumn(ExpMaterialTable.Column.LSID).setHidden(true);
                    return materials;
                }
            };
            specimenColumn.setFk(lfkSpecimen);
            addColumn(specimenColumn);

            Set<Double> cutoffValuess = _nabSpecimenTable.getCutoffValues();
            for (Double value : cutoffValuess)
            {
                final Integer intCutoff = (int)Math.floor(value);
                ColumnInfo cutoffColumn = makeCutoffValueColumn(intCutoff, cutoffValueTable);

                cutoffColumn.setKeyField(false);
                cutoffColumn.setIsUnselectable(true);
                LookupForeignKey lfk = new LookupForeignKey()
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        return cutoffValueTable;  // _userSchema.getTable(NabProtocolSchema.CUTOFF_VALUE_TABLE_NAME);
                    }
                    @Override
                    public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
                    {
                        ColumnInfo result = super.createLookupColumn(parent, displayField);
                        updateLabelWithCutoff(result, intCutoff);
                        return result;
                    }
                };
                cutoffColumn.setFk(lfk);
                addColumn(cutoffColumn);
            }

            for (ColumnInfo columnInfo : _rootTable.getColumns())
            {
                String columnName = columnInfo.getColumnName().toLowerCase();
                if (columnName.contains("auc_") || columnName.equals("fiterror") || columnName.equals("wellgroupname"))
                    addWrapColumn(columnInfo);
            }

        }

        // run through the property columns, setting all to be visible by default:
        FieldKey dataKeyProp = FieldKey.fromParts(propertyLookupColumn.getName());
        for (PropertyDescriptor lookupCol : pds)
        {
            if (!hiddenCols.contains(lookupCol.getName()))
            {
                if (!NabManager.useNewNab)
                {
                    FieldKey key = new FieldKey(dataKeyProp, lookupCol.getName());
                    visibleColumns.add(key);
                }
                else
                {
                    String legalName = ColumnInfo.legalNameFromName(lookupCol.getName());
                    if (null != _rootTable.getColumn(legalName))
                    {
                        // Column is in NabSpecimen
                        FieldKey key = FieldKey.fromString(legalName);
                        visibleColumns.add(key);
                        if (null == getColumn(key))
                            addWrapColumn(_rootTable.getColumn(key));
                    }
                    else
                    {
                        // Cutoff table column or calculated column
                        PropDescCategory pdCat = NabManager.getPropDescCategory(lookupCol.getName());
                        FieldKey key = getCalculatedColumn(pdCat);
                        if (null != key)
                            visibleColumns.add(key);
                    }
                }
            }
        }

    }

    private FieldKey getCalculatedColumn(PropDescCategory pdCat)
    {
        FieldKey fieldKey = null;
        if (null != pdCat.getCutoffValue())
        {
            String cutoffColumnName = pdCat.getCutoffValueColumnName();
            String columnName = pdCat.getCalculatedColumnName();
            if (null != columnName)         // cutoffColumn could be null when we're deleting folders
            {
                fieldKey = FieldKey.fromParts(cutoffColumnName, columnName);
            }
        }

        return fieldKey;
    }

    private ColumnInfo makeCutoffValueColumn(Integer cutoffValue, CutoffValueTable cutoffValueTable)
    {
        SQLFragment sql = new SQLFragment("(SELECT co.RowId FROM ");
        sql.append(cutoffValueTable, "co");
        sql.append(" WHERE " + ExprColumn.STR_TABLE_ALIAS + ".RowId = co.NAbSpecimenId");
        sql.append("   AND co.Cutoff = " + cutoffValue + ")");
        ExprColumn column = new ExprColumn(this, "Cutoff" + cutoffValue, sql, JdbcType.INTEGER);
        return column;
    }

    protected Set<String> getHiddenColumns(ExpProtocol protocol)
    {
        return new HashSet<String>();
    }

    private static void updateLabelWithCutoff(ColumnInfo column, Integer intCutoff)
    {
        if (null != intCutoff)
        {
            String label = column.getLabel();
            if (label.startsWith("IC"))
            {
                column.setLabel("Curve IC" + intCutoff + label.substring(2));
            }
            else if (label.startsWith("Point"))
            {
                column.setLabel("Point IC" + intCutoff + label.substring(5));
            }
        }
    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        if (!NabManager.useNewNab)
        {
            return super.resolveColumn(name);
        }

        ColumnInfo result = null;
        if ("Properties".equalsIgnoreCase(name))
        {
            // Hook up a column that joins back to this table so that the columns formerly under the Properties
            // node when this was OntologyManager-backed can still be queried there
            result = wrapColumn("Properties", getRealTable().getColumn("RowId"));
            result.setIsUnselectable(true);
            LookupForeignKey fk = new LookupForeignKey("RowId")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new NabRunDataTable(_schema, _protocol);
                }

                @Override
                public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
                {
                    ColumnInfo result = super.createLookupColumn(parent, displayField);
                    if (null != result && (displayField.startsWith("Curve") || displayField.startsWith("Point")))
                    {
                        String columnName = NabManager.getPropDescCategory(displayField).getCalculatedColumnName();
                        TableInfo tableInfo = result.getFkTableInfo();
                        if (null != tableInfo && null != columnName)
                            result = tableInfo.getLookupColumn(result, columnName);
                    }
                    return result;
                }
            };
            fk.setPrefixColumnCaption(false);
            result.setFk(fk);
        }
        else if ("Fit Error".equalsIgnoreCase(name))
        {
            result = getColumn("FitError");
        }
        else if ("Wellgroup Name".equals(name))
        {
            result = getColumn("WellGroupName");
        }
        else if (name.startsWith("Curve") || name.startsWith("Point"))
        {
            PropDescCategory pdCat = NabManager.getPropDescCategory(name);
            FieldKey fieldKey = getCalculatedColumn(pdCat);
            if (null != fieldKey)
                result = getColumn(pdCat.getCutoffValueColumnName());
        }

        return result;
    }
}

