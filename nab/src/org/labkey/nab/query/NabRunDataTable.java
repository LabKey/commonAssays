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

package org.labkey.nab.query;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.assay.dilution.DilutionManager;
import org.labkey.api.assay.dilution.query.DilutionProviderSchema;
import org.labkey.api.assay.nab.query.CutoffValueTable;
import org.labkey.api.assay.nab.query.NAbSpecimenTable;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.query.QcAwarePropertyForeignKey;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.SpecimenPropertyColumnDecorator;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabManager;
import org.labkey.nab.SinglePlateNabDataHandler;

import java.util.*;

/**
 * User: brittp
 * Date: Jul 6, 2007
 * Time: 5:35:15 PM
 */
public class NabRunDataTable extends NabBaseTable
{
    protected final NAbSpecimenTable _nabSpecimenTable;

    public NabRunDataTable(final NabProtocolSchema schema, final ExpProtocol protocol)
    {
        super(schema, new NAbSpecimenTable(schema), protocol);
        _nabSpecimenTable = (NAbSpecimenTable) getRealTable();

        final AssayProvider provider = AssayService.get().getProvider(protocol);
        List<FieldKey> visibleColumns = new ArrayList<>();

        // add any property columns
        addPropertyColumns(schema, protocol, provider, visibleColumns);

        // TODO - we should have a more reliable (and speedier) way of identifying just the data rows here
        SQLFragment dataRowClause = new SQLFragment("ObjectURI LIKE '%" + getDataRowLsidPrefix() + "%'");
        addCondition(dataRowClause, FieldKey.fromParts("ObjectURI"));

        addRunColumn();

        ExprColumn runIdColumn = new ExprColumn(this, DilutionProviderSchema.RUN_ID_COLUMN_NAME, new SQLFragment(ExprColumn.STR_TABLE_ALIAS + ".RunID"), JdbcType.INTEGER);
        ColumnInfo addedRunIdColumn = addColumn(runIdColumn);
        addedRunIdColumn.setHidden(true);

        Set<String> hiddenProperties = new HashSet<>();
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
        setDescription("Contains one row per data for the \"" + protocol.getName() + "\" Neutralizing Antibodies assay design.");
    }

    public Collection<PropertyDescriptor> getExistingDataProperties(ExpProtocol protocol)
    {
        List<PropertyDescriptor> pds = NabProviderSchema.getExistingDataProperties(protocol, _schema.getCutoffValues());

        pds.sort(Comparator.comparing(PropertyDescriptor::getName));
        return pds;
    }

    public String getDataRowLsidPrefix()
    {
        return SinglePlateNabDataHandler.NAB_DATA_ROW_LSID_PREFIX;
    }

    protected Set<String> getHiddenColumns(ExpProtocol protocol)
    {
        Set<String> hiddenCols = new HashSet<>();

        // hide the fit method specific values for curve IC and AUC
        for (PropertyDescriptor prop : getExistingDataProperties(protocol))
        {
            String propName = prop.getName();
            if (propName.startsWith(SinglePlateNabDataHandler.CURVE_IC_PREFIX) ||
                    propName.startsWith(SinglePlateNabDataHandler.AUC_PREFIX) ||
                    propName.startsWith(SinglePlateNabDataHandler.pAUC_PREFIX))
            {
                if (propName.indexOf('_') != -1)
                    hiddenCols.add(propName);
            }
        }
        return hiddenCols;
    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
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
        // Be backwards compatible with queries that expect there to an "ObjectId" column. It's a different value from
        // the pre-migration value, but it's enough to make the query run and should be sufficient as long as we
        // continue to generate the same number for a given row.
        else if ("ObjectId".equalsIgnoreCase(name))
        {
            result = getColumn("RowId");
        }
        else if ("Wellgroup Name".equalsIgnoreCase(name))
        {
            result = getColumn("WellGroupName");
        }
        else if (StringUtils.startsWithIgnoreCase(name, "curve") || StringUtils.startsWithIgnoreCase(name, "point"))
        {
            DilutionManager.PropDescCategory pdCat = NabManager.getPropDescCategory(name);
            FieldKey fieldKey = DilutionManager.getCalculatedColumn(pdCat);
            if (null != fieldKey)
            {
                ColumnInfo cutoffColumn = getColumn(pdCat.getCutoffValueColumnName());
                if (cutoffColumn != null)
                {
                    String columnName = pdCat.getCalculatedColumnName();
                    TableInfo tableInfo = cutoffColumn.getFkTableInfo();
                    if (null != tableInfo && null != columnName)
                        result = tableInfo.getLookupColumn(cutoffColumn, columnName);
                }
            }
        }

        return result;
    }

    protected void addPropertyColumns(final NabProtocolSchema schema, final ExpProtocol protocol, final AssayProvider provider, List<FieldKey> visibleColumns)
    {
        // add material lookup columns to the view first, so they appear at the left:
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(protocol, AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);
        if (sampleSet != null)
        {
            for (DomainProperty pd : sampleSet.getType().getProperties())
            {
                visibleColumns.add(FieldKey.fromParts(getInputMaterialPropertyName(), ExpMaterialTable.Column.Property.toString(), pd.getName()));
            }
        }
        // get all the properties from this plated-based protocol:
        Collection<PropertyDescriptor> pds = getExistingDataProperties(protocol);

        ColumnInfo objectUriColumn = addWrapColumn(_rootTable.getColumn("ObjectUri"));
        objectUriColumn.setIsUnselectable(true);
        objectUriColumn.setHidden(true);
        ColumnInfo rowIdColumn = addWrapColumn(_rootTable.getColumn("RowId"));
        rowIdColumn.setKeyField(true);
        rowIdColumn.setHidden(true);
        rowIdColumn.setIsUnselectable(true);

        // add object ID again, this time as a lookup to a virtual property table that contains our selected NAB properties:

        QcAwarePropertyForeignKey fk = new QcAwarePropertyForeignKey(pds, this, schema)             // Needed by NewNab only to get defaultHiddenProperties
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
        Set<String> hiddenCols = getHiddenColumns(protocol);
        for (PropertyDescriptor pd : fk.getDefaultHiddenProperties())
            hiddenCols.add(pd.getName());
        hiddenCols.add(getInputMaterialPropertyName());

        addSpecimenColumn();

        Set<Double> cutoffValuess = _schema.getCutoffValues();
        for (Double value : cutoffValuess)
        {
            final Integer intCutoff = (int)Math.floor(value);
            final CutoffValueTable cutoffValueTable = new CutoffValueTable(schema);
            cutoffValueTable.removeContainerAndProtocolFilters();
            cutoffValueTable.addCondition(new SimpleFilter(FieldKey.fromString("Cutoff"), intCutoff));
            ColumnInfo nabSpecimenColumn = cutoffValueTable.getColumn("NabSpecimenId");
            nabSpecimenColumn.setIsUnselectable(true);
            nabSpecimenColumn.setHidden(true);

            // Update column labels like IC_4pl to Curve ICxx 4pl
            for (ColumnInfo column : cutoffValueTable.getColumns())
                updateLabelWithCutoff(column, intCutoff);

            ColumnInfo cutoffColumn = wrapColumn("Cutoff" + intCutoff, _rootTable.getColumn("RowId"));
            cutoffColumn.setLabel("Cutoff " + intCutoff);
            cutoffColumn.setKeyField(false);
            cutoffColumn.setIsUnselectable(true);
            LookupForeignKey lfk = new LookupForeignKey("NabSpecimenId")
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
                    return result;
                }
            };
            cutoffColumn.setFk(lfk);
            addColumn(cutoffColumn);
        }

        for (ColumnInfo columnInfo : _rootTable.getColumns())
        {
            String columnName = columnInfo.getColumnName().toLowerCase();
            if (columnName.contains("auc_") || columnName.equals("fiterror"))
            {
                addWrapColumn(columnInfo);
            }
            else if (columnName.equals("wellgroupname"))
            {
                ColumnInfo wellgroupColumn = wrapColumn(columnInfo);
                wellgroupColumn.setLabel("Wellgroup Name");
                addColumn(wellgroupColumn);
            }
        }

        // run through the property columns, setting all to be visible by default:
        FieldKey dataKeyProp = FieldKey.fromParts(propertyLookupColumn.getName());
        for (PropertyDescriptor lookupCol : pds)
        {
            if (!hiddenCols.contains(lookupCol.getName()))
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
                    DilutionManager.PropDescCategory pdCat = DilutionManager.getPropDescCategory(lookupCol.getName());
                    FieldKey key = DilutionManager.getCalculatedColumn(pdCat);
                    if (null != key)
                        visibleColumns.add(key);
                }
            }
        }

        if (provider instanceof NabAssayProvider && ((NabAssayProvider)provider).supportsMultiVirusPlate())
        {
            Domain domain = ((NabAssayProvider)provider).getVirusWellGroupDomain(protocol);
            if (null != domain)
            {
                ColumnInfo virusColumn = wrapColumn(_rootTable.getColumn("VirusLsid"));
                virusColumn.setLabel("Virus");
                virusColumn.setIsUnselectable(true);
                virusColumn.setKeyField(false);
                LookupForeignKey fkVirus = new LookupForeignKey("VirusLsid")
                {
                    @Override
                    public TableInfo getLookupTableInfo()
                    {
                        AssayProtocolSchema protocolSchema = provider.createProtocolSchema(schema.getUser(), getContainer(), protocol, null);
                        return protocolSchema.createTable(DilutionManager.VIRUS_TABLE_NAME);
                    }
                };
                virusColumn.setFk(fkVirus);
                addColumn(virusColumn);
                visibleColumns.add(new FieldKey(virusColumn.getFieldKey(), "VirusName"));
                visibleColumns.add(new FieldKey(virusColumn.getFieldKey(), "VirusID"));
            }
        }
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


}
