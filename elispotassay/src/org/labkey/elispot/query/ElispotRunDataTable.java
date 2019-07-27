/*
 * Copyright (c) 2008-2019 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.assay.plate.AbstractPlateBasedAssayProvider;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExpSampleSet;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpMaterialTable;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.assay.AbstractAssayProvider;
import org.labkey.api.assay.AssayProvider;
import org.labkey.api.assay.AssaySchema;
import org.labkey.api.assay.AssayService;
import org.labkey.api.assay.plate.PlateReader;
import org.labkey.elispot.ElispotAssayProvider;
import org.labkey.elispot.ElispotDataHandler;
import org.labkey.elispot.ElispotManager;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Karl Lum
 * Date: Jan 21, 2008
 */
public class ElispotRunDataTable extends PlateBasedAssayRunDataTable
{
    public ElispotRunDataTable(final AssaySchema schema, ContainerFilter cf, final ExpProtocol protocol)
    {
        this(schema, ElispotManager.getTableInfoElispotRunData(), cf, protocol);
    }

    public ElispotRunDataTable(final AssaySchema schema, TableInfo table, ContainerFilter cf, final ExpProtocol protocol)
    {
        super(schema, table, cf, protocol);

        setDescription("Contains one row per sample for the \"" + protocol.getName() + "\" ELISpot assay design.");

        // display column for spot counts
        var col = getMutableColumn(FieldKey.fromParts(ElispotDataHandler.SFU_PROPERTY_NAME));
        if (col != null)
        {
            col.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new SpotCountDisplayColumn(colInfo, schema);
                }
            });
        }

        // display column for spot size
        var spotSizeCol = getMutableColumn(FieldKey.fromParts(ElispotDataHandler.SPOT_SIZE_PROPERTY_NAME));
        if (spotSizeCol != null)
        {
            spotSizeCol.setDisplayColumnFactory(new DisplayColumnFactory()
            {
                @Override
                public DisplayColumn createRenderer(ColumnInfo colInfo)
                {
                    return new SpotCountDisplayColumn(colInfo, schema);
                }
            });
        }
    }

    protected void addPropertyColumns(final AssaySchema schema, final ExpProtocol protocol, final AssayProvider provider, List<FieldKey> visibleColumns)
    {
        // get all the properties from this plated-based protocol:
        for (ColumnInfo column : _rootTable.getColumns())
        {
            if ("RunId".equalsIgnoreCase(column.getName()))
            {
                continue;   // already added or added below
            }
            var wrapColumn = addWrapColumn(column);
            if ("ObjectUri".equalsIgnoreCase(column.getName()) || "RowId".equalsIgnoreCase(column.getName()))
                wrapColumn.setHidden(true);
        }

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

        var antigenLsidColumn = getMutableColumn("AntigenLsid");
        antigenLsidColumn.setLabel("Antigen");
        antigenLsidColumn.setFk(new LookupForeignKey( (String)null, "AntigenName")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                // TODO ContainerFilter
                return ElispotManager.getTableInfoElispotAntigen(_protocol);
            }
        });

    }

    @Override
    protected ColumnInfo resolveColumn(String name)
    {
        ColumnInfo result = super.resolveColumn(name);

        if ("Properties".equalsIgnoreCase(name))
        {
            // Hook up a column that joins back to this table so that the columns formerly under the Properties
            // node can still be queried there.
            var wrapped = wrapColumn("Properties", getRealTable().getColumn("ObjectId"));
            wrapped.setIsUnselectable(true);
            LookupForeignKey fk = new LookupForeignKey(getContainerFilter(), "ObjectId", null)
            {
                @Override
                public TableInfo getLookupTableInfo()
                {
                    return new ElispotRunDataTable(_userSchema, getLookupContainerFilter(), _protocol);
                }
            };
            fk.setPrefixColumnCaption(false);
            wrapped.setFk(fk);
            result = wrapped;
        }

        return result;
    }

    @Override
    public List<FieldKey> getDefaultVisibleColumns()
    {
        ElispotAssayProvider provider = (ElispotAssayProvider) AssayService.get().getProvider(ElispotAssayProvider.NAME);
        assert null != provider;
        ElispotAssayProvider.DetectionMethodType method = provider.getDetectionMethod(_protocol.getContainer(), _protocol);

        List<FieldKey> fieldKeys = new ArrayList<>();
        fieldKeys.add(FieldKey.fromString(ElispotDataHandler.WELLGROUP_PROPERTY_NAME));
        fieldKeys.add(FieldKey.fromString(ElispotDataHandler.ANTIGEN_WELLGROUP_PROPERTY_NAME));
        fieldKeys.add(FieldKey.fromParts("AntigenLsid", ElispotAssayProvider.ANTIGENNAME_PROPERTY_NAME));
        fieldKeys.add(FieldKey.fromParts("AntigenLsid", ElispotAssayProvider.CELLWELL_PROPERTY_NAME));
        fieldKeys.add(FieldKey.fromString(ElispotDataHandler.WELLGROUP_LOCATION_PROPERTY));
        fieldKeys.add(FieldKey.fromString(ElispotDataHandler.SFU_PROPERTY_NAME));
        fieldKeys.add(FieldKey.fromString(ElispotDataHandler.NORMALIZED_SFU_PROPERTY_NAME));
        if (ElispotAssayProvider.DetectionMethodType.FLUORESCENT == method)
        {
            fieldKeys.add(FieldKey.fromString(ElispotDataHandler.SPOT_SIZE_PROPERTY_NAME));
            fieldKeys.add(FieldKey.fromString(ElispotDataHandler.ANALYTE_PROPERTY_NAME));
            fieldKeys.add(FieldKey.fromString(ElispotDataHandler.CYTOKINE_PROPERTY_NAME));
            fieldKeys.add(FieldKey.fromString(ElispotDataHandler.ACTIVITY_PROPERTY_NAME));
            fieldKeys.add(FieldKey.fromString(ElispotDataHandler.INTENSITY_PROPERTY_NAME));
        }
        FieldKey specimenPropFieldKey = FieldKey.fromParts("SpecimenLsid", "Property");
        fieldKeys.add(FieldKey.fromParts(specimenPropFieldKey, FieldKey.fromString("SpecimenId")));
        fieldKeys.add(FieldKey.fromParts(specimenPropFieldKey, FieldKey.fromString("ParticipantId")));
        fieldKeys.add(FieldKey.fromParts(specimenPropFieldKey, FieldKey.fromString("VisitId")));
        fieldKeys.add(FieldKey.fromParts(specimenPropFieldKey, FieldKey.fromString("Date")));
        fieldKeys.add(FieldKey.fromParts(specimenPropFieldKey, FieldKey.fromString("SampleDescription")));
        FieldKey runPropFieldKey = FieldKey.fromParts("Run");
        fieldKeys.add(FieldKey.fromParts(runPropFieldKey, FieldKey.fromString("ProtocolName")));
        fieldKeys.add(FieldKey.fromParts(runPropFieldKey, FieldKey.fromString("PlateReader")));
        fieldKeys.add(FieldKey.fromParts(runPropFieldKey,  FieldKey.fromString("Batch"), FieldKey.fromString("TargetStudy")));
        return fieldKeys;
    }

    public static class SpotCountDisplayColumn extends DataColumn
    {
        private PlateReader _reader;
        private AssaySchema _schema;

        public SpotCountDisplayColumn(ColumnInfo col, AssaySchema schema)
        {
            super(col);
            _schema = schema;
        }

        @NotNull
        @Override
        public String getFormattedValue(RenderContext ctx)
        {
            String value = super.getFormattedValue(ctx);
            PlateReader reader = getReader(ctx);

            if (reader != null)
                return reader.getWellDisplayValue(value);
            else
                return value;
        }

        private PlateReader getReader(RenderContext ctx)
        {
            if (_reader == null)
            {
                ColumnInfo plateReaderColumn = ctx.getFieldMap().get(FieldKey.fromParts("Run", "PlateReader"));
                if (null != plateReaderColumn)
                {
                    String readerAlias = plateReaderColumn.getAlias();
                    Object readerName = ctx.getRow().get(readerAlias);

                    if (readerName != null)
                    {
                        ElispotAssayProvider provider = (ElispotAssayProvider) AssayService.get().getProvider(ElispotAssayProvider.NAME);
                        _reader = provider.getPlateReader(String.valueOf(readerName));
                    }
                }
            }
            return _reader;
        }
    }

    @Override
    protected boolean hasMaterialSpecimenPropertyColumnDecorator()
    {
        return true;
    }
}
