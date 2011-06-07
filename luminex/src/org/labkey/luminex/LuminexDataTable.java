/*
 * Copyright (c) 2009-2011 LabKey Corporation
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
package org.labkey.luminex;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.ContainerForeignKey;
import org.labkey.api.data.OORDisplayColumnFactory;
import org.labkey.api.data.Parameter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.UpdateableTableInfo;
import org.labkey.api.etl.DataIterator;
import org.labkey.api.etl.TableLoaderPump;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.FilteredTable;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.SpecimenForeignKey;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * User: jeckels
 * Date: May 22, 2009
 */
public class LuminexDataTable extends FilteredTable implements UpdateableTableInfo
{
    private final LuminexSchema _schema;
    private LuminexAssayProvider _provider;

    public LuminexDataTable(LuminexSchema schema)
    {
        super(LuminexSchema.getTableInfoDataRow(), schema.getContainer());

        ExpProtocol protocol = schema.getProtocol();
        _provider = (LuminexAssayProvider)AssayService.get().getProvider(protocol);

        setDescription("Contains all the Luminex data rows for the " + protocol.getName() + " assay definition");
        _schema = schema;

        addColumn(wrapColumn("Analyte", getRealTable().getColumn("AnalyteId")));
        ColumnInfo dataColumn = addColumn(wrapColumn("Data", getRealTable().getColumn("DataId")));
        dataColumn.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                ExpDataTable result = _schema.createDataTable();
                result.setContainerFilter(getContainerFilter());
                return result;
            }
        });
        ColumnInfo rowIdColumn = addColumn(wrapColumn(getRealTable().getColumn("RowId")));
        rowIdColumn.setHidden(true);
        rowIdColumn.setKeyField(true);
        addColumn(wrapColumn(getRealTable().getColumn("LSID"))).setHidden(true);
        ColumnInfo protocolColumn = addColumn(wrapColumn("Protocol", getRealTable().getColumn("ProtocolId")));
        protocolColumn.setFk(new ExpSchema(_schema.getUser(), _schema.getContainer()).getProtocolForeignKey("RowId"));
        protocolColumn.setHidden(true);
        addColumn(wrapColumn(getRealTable().getColumn("Type")));
        addColumn(wrapColumn(getRealTable().getColumn("Well")));
        addColumn(wrapColumn(getRealTable().getColumn("Outlier")));
        addColumn(wrapColumn(getRealTable().getColumn("Description")));
        ColumnInfo specimenColumn = wrapColumn(getRealTable().getColumn("SpecimenID"));
        specimenColumn.setFk(new SpecimenForeignKey(_schema, AssayService.get().getProvider(_schema.getProtocol()), _schema.getProtocol()));
        addColumn(specimenColumn);
        addColumn(wrapColumn(getRealTable().getColumn("ExtraSpecimenInfo")));
        addColumn(wrapColumn(getRealTable().getColumn("FIString"))).setLabel("FI String");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("FI"), getRealTable().getColumn("FIOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("FIBackgroundString"))).setLabel("FI-Bkgd String");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("FIBackground"), getRealTable().getColumn("FIBackgroundOORIndicator"), "FI-Bkgd");
        addColumn(wrapColumn(getRealTable().getColumn("StdDevString")));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("StdDev"), getRealTable().getColumn("StdDevOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("ObsConcString")));
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("ObsConc"), getRealTable().getColumn("ObsConcOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("ExpConc")));
        addColumn(wrapColumn(getRealTable().getColumn("ObsOverExp"))).setLabel("(Obs/Exp)*100");
        OORDisplayColumnFactory.addOORColumns(this, getRealTable().getColumn("ConcInRange"), getRealTable().getColumn("ConcInRangeOORIndicator"));
        addColumn(wrapColumn(getRealTable().getColumn("ConcInRangeString")));
        addColumn(wrapColumn(getRealTable().getColumn("Dilution")));
        addColumn(wrapColumn("Group", getRealTable().getColumn("DataRowGroup")));
        addColumn(wrapColumn(getRealTable().getColumn("Ratio")));
        addColumn(wrapColumn(getRealTable().getColumn("SamplingErrors")));
        addColumn(wrapColumn(getRealTable().getColumn("BeadCount")));
        ColumnInfo titrationColumn = addColumn(wrapColumn("Titration", getRealTable().getColumn("TitrationId")));
        titrationColumn.setFk(new LookupForeignKey("RowId")
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _schema.createTitrationTable(true);
            }
        });
        ColumnInfo containerColumn = addColumn(wrapColumn(getRealTable().getColumn("Container")));
        containerColumn.setHidden(true);
        containerColumn.setFk(new ContainerForeignKey());

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts("Analyte"));
        defaultCols.add(FieldKey.fromParts("Type"));
        defaultCols.add(FieldKey.fromParts("Well"));
        defaultCols.add(FieldKey.fromParts("Description"));
        defaultCols.add(FieldKey.fromParts("SpecimenID"));
        defaultCols.add(FieldKey.fromParts("ParticipantID"));
        defaultCols.add(FieldKey.fromParts("VisitID"));
        defaultCols.add(FieldKey.fromParts("FI"));
        defaultCols.add(FieldKey.fromParts("FIBackground"));
        defaultCols.add(FieldKey.fromParts("StdDev"));
        defaultCols.add(FieldKey.fromParts("ObsConc"));
        defaultCols.add(FieldKey.fromParts("ExpConc"));
        defaultCols.add(FieldKey.fromParts("ObsOverExp"));
        defaultCols.add(FieldKey.fromParts("ConcInRange"));
        defaultCols.add(FieldKey.fromParts("Dilution"));
        defaultCols.add(FieldKey.fromParts("BeadCount"));
        defaultCols.add(FieldKey.fromParts("Titration"));

        Domain domain = getDomain();
        for (ColumnInfo propertyCol : domain.getColumns(this, getColumn("LSID"), schema.getUser()))
        {
            addColumn(propertyCol);
            defaultCols.add(propertyCol.getFieldKey());
        }

        addColumn(wrapColumn("ParticipantID", getRealTable().getColumn("PTID")));
        addColumn(wrapColumn(getRealTable().getColumn("VisitID")));
        addColumn(wrapColumn(getRealTable().getColumn("Date")));


        for (DomainProperty prop : _provider.getRunDomain(protocol).getProperties())
        {
            defaultCols.add(new FieldKey(_provider.getTableMetadata().getRunFieldKeyFromResults(), prop.getName()));
        }
        for (DomainProperty prop : AbstractAssayProvider.getDomainByPrefix(protocol, LuminexAssayProvider.ASSAY_DOMAIN_EXCEL_RUN).getProperties())
        {
            defaultCols.add(new FieldKey(_provider.getTableMetadata().getRunFieldKeyFromResults(), prop.getName()));
        }
        for (DomainProperty prop : _provider.getBatchDomain(protocol).getProperties())
        {
            defaultCols.add(new FieldKey(new FieldKey(_provider.getTableMetadata().getRunFieldKeyFromResults(), "Batch"), prop.getName()));
        }

        setDefaultVisibleColumns(defaultCols);

        getColumn("Analyte").setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return _schema.createAnalyteTable(false);
            }
        });

        SQLFragment protocolIDFilter = new SQLFragment("ProtocolID = ?");
        protocolIDFilter.add(_schema.getProtocol().getRowId());
        addCondition(protocolIDFilter,"ProtocolID");

        SQLFragment containerFilter = new SQLFragment("Container = ?");
        containerFilter.add(_schema.getContainer().getId());
        addCondition(containerFilter, "Container");
    }

    @Override
    @NotNull
    public Domain getDomain()
    {
        return _provider.getResultsDomain(_schema.getProtocol());
    }

    @Override
    public boolean insertSupported()
    {
        return true;
    }

    @Override
    public boolean updateSupported()
    {
        return false;
    }

    @Override
    public boolean deleteSupported()
    {
        return false;
    }

    @Override
    public TableInfo getSchemaTableInfo()
    {
        return LuminexSchema.getTableInfoDataRow();
    }

    @Override
    public ObjectUriType getObjectUriType()
    {
        return ObjectUriType.schemaColumn;
    }

    @Override
    public String getObjectURIColumnName()
    {
        return "LSID";
    }

    @Override
    public String getObjectIdColumnName()
    {
        return null;
    }

    @Override
    public CaseInsensitiveHashMap<String> remapSchemaColumns()
    {
        CaseInsensitiveHashMap<String> result = new CaseInsensitiveHashMap<String>();
        result.put("PTID", "ParticipantID");
        result.put("DataId", "Data");
        result.put("AnalyteId", "Analyte");
        result.put("ProtocolId", "Protocol");
        return result;
    }

    @Override
    public CaseInsensitiveHashSet skipProperties()
    {
        return new CaseInsensitiveHashSet();
    }

    @Override
    public int persistRows(DataIterator data, BatchValidationException errors)
    {
        TableLoaderPump pump = new TableLoaderPump(data, this, errors);
        pump.run();
        return pump.getRowCount();
    }

    @Override
    public Parameter.ParameterMap insertStatement(Connection conn, User user) throws SQLException
    {
        return Table.insertStatement(conn, this, getContainer(), user, true);
    }

    @Override
    public Parameter.ParameterMap updateStatement(Connection conn, User user, Set<String> columns) throws SQLException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Parameter.ParameterMap deleteStatement(Connection conn) throws SQLException
    {
        throw new UnsupportedOperationException();
    }
}
