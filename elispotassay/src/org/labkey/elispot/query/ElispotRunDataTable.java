/*
 * Copyright (c) 2008-2013 LabKey Corporation
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

package org.labkey.elispot.query;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.PropertyColumn;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.PropertyForeignKey;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AbstractPlateBasedAssayProvider;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.SpecimenPropertyColumnDecorator;
import org.labkey.api.study.query.PlateBasedAssayRunDataTable;
import org.labkey.elispot.ElispotDataHandler;
import org.labkey.elispot.ElispotProtocolSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Karl Lum
 * Date: Jan 21, 2008
 */
public class ElispotRunDataTable extends PlateBasedAssayRunDataTable
{
    protected ExpProtocol _protocol;

    public ElispotRunDataTable(final AssaySchema schema, final ExpProtocol protocol)
    {
        super(schema, protocol);
        _protocol = protocol;

        setDescription("Contains one row per sample for the \"" + protocol.getName() + "\" ELISpot assay design.");
    }

    public PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol)
    {
        return ElispotProtocolSchema.getExistingDataProperties(protocol, ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX);
    }

    public String getInputMaterialPropertyName()
    {
        return ElispotDataHandler.ELISPOT_INPUT_MATERIAL_DATA_PROPERTY;
    }

    public String getDataRowLsidPrefix()
    {
        return ElispotDataHandler.ELISPOT_DATA_ROW_LSID_PREFIX;
    }

    protected void addPropertyColumns(final AssaySchema schema, final ExpProtocol protocol, final AssayProvider provider, List<FieldKey> visibleColumns)
    {
        // get all the properties from this plated-based protocol:
        List<PropertyDescriptor> properties = new ArrayList<PropertyDescriptor>();
        PropertyDescriptor materialProperty = null;
        Set<String> hiddenCols = getHiddenColumns(protocol);

        for (PropertyDescriptor pd : getExistingDataProperties(protocol))
        {
            if (getInputMaterialPropertyName().equals(pd.getName()))
                materialProperty = pd;
            else
                properties.add(pd);
        }

        // add object ID to this tableinfo and set it as a key field:
        ColumnInfo objectIdColumn = addWrapColumn(_rootTable.getColumn("ObjectId"));
        objectIdColumn.setKeyField(true);

        ColumnInfo objectUriColumn = addWrapColumn(_rootTable.getColumn("ObjectUri"));
        objectUriColumn.setHidden(true);

        if (materialProperty != null)
        {
            // add material lookup columns to the view first, so they appear at the left:
            String sampleDomainURI = AbstractAssayProvider.getDomainURIForPrefix(protocol, AbstractPlateBasedAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
            final ExpSampleSet sampleSet = ExperimentService.get().getSampleSet(sampleDomainURI);
            if (sampleSet != null)
            {
                for (DomainProperty pd : sampleSet.getPropertiesForType())
                {
                    visibleColumns.add(FieldKey.fromParts(getInputMaterialPropertyName(), ExpMaterialTable.Column.Property.toString(), pd.getName()));
                }
            }

            ColumnInfo materialColumn = new PropertyColumn(materialProperty, objectUriColumn, getContainer(), schema.getUser(), true);
            materialColumn.setLabel("Specimen");
            materialColumn.setFk(new LookupForeignKey("LSID")
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
            addColumn(materialColumn);
            hiddenCols.add(getInputMaterialPropertyName());
        }

        // run through the property columns, setting all to be visible by default:
        for (PropertyDescriptor pd : properties)
        {
            ColumnInfo propColumn = new PropertyColumn(pd, objectUriColumn, getContainer(), schema.getUser(), true);
            if (getColumn(propColumn.getName()) == null)
                addColumn(propColumn);

            if (!hiddenCols.contains(pd.getName()))
            {
                if (!ElispotDataHandler.NORMALIZED_SFU_PROPERTY_NAME.equals(pd.getName()))
                    visibleColumns.add(FieldKey.fromParts(pd.getName()));
            }
        }
    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        ColumnInfo result = super.resolveColumn(name);

        if ("Properties".equalsIgnoreCase(name))
        {
            // Hook up a column that joins back to this table so that the columns formerly under the Properties
            // node can still be queried there.
            result = wrapColumn("Properties", getRealTable().getColumn("ObjectId"));
            result.setIsUnselectable(true);
            LookupForeignKey fk = new LookupForeignKey("ObjectId")
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new ElispotRunDataTable(_userSchema, _protocol);
                }
            };
            fk.setPrefixColumnCaption(false);
            result.setFk(fk);
        }

        return result;
    }
}
