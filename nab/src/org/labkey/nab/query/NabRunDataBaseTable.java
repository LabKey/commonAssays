/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private NAbSpecimenTable _nabSpecimenTable;

    public NabRunDataBaseTable(final NabProtocolSchema schema, final ExpProtocol protocol)
    {
        super(new NAbSpecimenTable(schema), schema);
        _nabSpecimenTable = (NAbSpecimenTable)getRealTable();

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
        CutoffValueTable cutoffValueTable = new CutoffValueTable(schema);

        // add material lookup columns to the view first, so they appear at the left:
        String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(protocol, AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);
        if (sampleSet != null)
        {
            for (DomainProperty pd : sampleSet.getPropertiesForType())
            {
                visibleColumns.add(FieldKey.fromParts("Properties", getInputMaterialPropertyName(),
                        ExpMaterialTable.Column.Property.toString(), pd.getName()));
            }
        }
        // get all the properties from this plated-based protocol:
        PropertyDescriptor[] pds = getExistingDataProperties(protocol);

        // add object ID to this tableinfo and set it as a key field:
        ColumnInfo objectIdColumn = addWrapColumn(_rootTable.getColumn("ObjectId"));
        objectIdColumn.setKeyField(true);

        // add object ID again, this time as a lookup to a virtual property table that contains our selected NAB properties:
        ColumnInfo propertyLookupColumn = wrapColumn("Properties", _rootTable.getColumn("ObjectUri"));
        propertyLookupColumn.setKeyField(false);
        propertyLookupColumn.setIsUnselectable(true);

        QcAwarePropertyForeignKey fk = new QcAwarePropertyForeignKey(pds, this, schema)
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

        if (!NabManager.useNewNab)
        {
        propertyLookupColumn.setFk(fk);
        addColumn(propertyLookupColumn);
        }

        Set<String> hiddenCols = getHiddenColumns(protocol);
        for (PropertyDescriptor pd : fk.getDefaultHiddenProperties())
            hiddenCols.add(pd.getName());
        hiddenCols.add(getInputMaterialPropertyName());

        if (NabManager.useNewNab)
        {
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
                        return _userSchema.getTable(NabProtocolSchema.CUTOFF_VALUE_TABLE_NAME);
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
                if (columnName.contains("auc_") || columnName.equals("fiterror") || columnName.equals("wellgroupname"))
                    addWrapColumn(columnInfo);
            }

        }
        Map<Integer, ColumnInfo> cutoffValues = new HashMap<Integer, ColumnInfo>();

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
                        PropDescCategory pdCat = getPropDescCategory(lookupCol.getName());
                        if (null == pdCat._rangeOrNum && null != cutoffValueTable.getColumn(pdCat.getColumnName()))
                        {
                            Integer cutoffValue = pdCat._cutoffValue;
                            assert null != cutoffValue;

                            FieldKey key = ensurePropertyColumn(pdCat, cutoffValues, cutoffValueTable);
                            if (null != key)
                                visibleColumns.add(key);
                        }
                        else
                        {
                            // Calculated column
                            FieldKey key = ensureCalculatedColumn(pdCat, cutoffValues, cutoffValueTable);
                            if (null != key)
                                visibleColumns.add(key);
                        }
                    }
                }
            }
        }

    }

    private class PropDescCategory
    {
        public String _origName = null;
        public String _type = null;         // ic_4pl, ic_5pl, ic_poly, point, null
        public boolean _oor = false;
        public String _rangeOrNum = null;   // inrange, number, null
        public Integer _cutoffValue = null; // value, null

        public PropDescCategory(String name)
        {
            _origName = name;
        }

        public String getColumnName()
        {
            String colName = _type;
            if (_oor) colName += "OORIndicator";
            return colName;
        }
    }

    @Nullable
    private PropDescCategory getPropDescCategory(String name)
    {
        PropDescCategory pdCat = new PropDescCategory(name);
        if (name.contains("InRange"))
            pdCat._rangeOrNum = "inrange";
        else if (name.contains("Number"))
            pdCat._rangeOrNum = "number";

        if (name.startsWith("Point") && name.contains("IC"))
        {
            pdCat._type = "Point";
        }
        else if (name.startsWith("Curve") && name.contains("IC"))
        {
            if (name.contains("4pl"))
                pdCat._type = "IC_4pl";
            else if (name.contains("5pl"))
                pdCat._type = "IC_5pl";
            else if (name.contains("poly"))
                pdCat._type = "IC_Poly";
            else
                pdCat._type = "IC";
        }
        else if (name.equalsIgnoreCase("auc"))
            pdCat._rangeOrNum = "auc";
        else if (name.equalsIgnoreCase("positive auc"))
            pdCat._rangeOrNum = "positiveauc";

        pdCat._oor = name.contains("OORIndicator");
        pdCat._cutoffValue = cutoffValueFromName(name);
        return pdCat;
    }

    @Nullable
    private Integer cutoffValueFromName(String name)
    {
        int icIndex = name.indexOf("IC");
        if (icIndex >= 0 && name.length() <= icIndex + 4)
            return Integer.valueOf(name.substring(icIndex + 2, icIndex + 4));
        return null;
    }

    private FieldKey ensureCalculatedColumn(PropDescCategory pdCat, Map<Integer, ColumnInfo> cutoffValues, CutoffValueTable cutoffValueTable)
    {
        FieldKey fieldKey = null;
        if (null != pdCat._cutoffValue)
        {
            ColumnInfo cutoffColumn = ensureCutoffColumn(pdCat._cutoffValue, cutoffValues, cutoffValueTable);
            String columnName = null;

            // Must be InRange or Number
            if (null == pdCat._rangeOrNum)
            {
                columnName = pdCat.getColumnName();
            }
            else if (pdCat._rangeOrNum.equalsIgnoreCase("inrange"))
            {
                columnName = pdCat.getColumnName() + "InRange";
            }
            else if (pdCat._rangeOrNum.equalsIgnoreCase("number"))
            {
                columnName = pdCat.getColumnName() + "Number";
            }

            if (null != columnName)
            {
                fieldKey = FieldKey.fromParts(cutoffColumn.getAlias(), columnName);
            }
        }
        else
        {
            // Must be AUC or Positive AUC so no parent is needed in FieldKey
            if (null == pdCat._rangeOrNum)
            {

            }
            else if (pdCat._rangeOrNum.equalsIgnoreCase("auc"))
            {

            }
            else if(pdCat._rangeOrNum.equalsIgnoreCase("positiveauc"))
            {

            }
            else
            {
                assert false;       // should not happen
            }
        }

        return fieldKey;
    }

    private ColumnInfo ensureCutoffColumn(Integer cutoffValue, Map<Integer, ColumnInfo> cutoffValues, CutoffValueTable cutoffValueTable)
    {
        ColumnInfo cutoffColumn = getColumn("Cutoff" + cutoffValue);

        return cutoffColumn;
    }

    private FieldKey ensurePropertyColumn(PropDescCategory pdCat, Map<Integer, ColumnInfo> cutoffValues, CutoffValueTable cutoffValueTable)
    {
        assert null != pdCat._cutoffValue;
        ColumnInfo cutoffColumn = ensureCutoffColumn(pdCat._cutoffValue, cutoffValues, cutoffValueTable);
        return FieldKey.fromParts(cutoffColumn.getAlias(), pdCat.getColumnName());
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
}

