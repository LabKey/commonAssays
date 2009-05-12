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

package org.labkey.luminex;

import org.labkey.api.data.*;
import org.labkey.api.query.*;
import org.labkey.api.security.User;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.query.ExpDataTable;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.SpecimenForeignKey;

import java.util.*;
import java.sql.Types;

public class LuminexSchema extends UserSchema
{
    private static final String ANALYTE_TABLE_NAME = "Analyte";
    private static final String DATA_ROW_TABLE_NAME = "DataRow";
    private final ExpProtocol _protocol;

    public LuminexSchema(User user, Container container)
    {
        this(user, container, null);
    }

    public LuminexSchema(User user, Container container, ExpProtocol protocol)
    {
        super("Luminex", user, container, getSchema());
        _protocol = protocol;
    }
    
    public Set<String> getTableNames()
    {
        return new HashSet<String>(Arrays.asList(ANALYTE_TABLE_NAME, DATA_ROW_TABLE_NAME));
    }

    public TableInfo createTable(String name)
    {
        if (ANALYTE_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createAnalyteTable();
        }
        else if (DATA_ROW_TABLE_NAME.equalsIgnoreCase(name))
        {
            return createDataRowTable();
        }
        return null;
    }

    private TableInfo createAnalyteTable()
    {
        FilteredTable result = new FilteredTable(getTableInfoAnalytes());
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Name")));
        result.addColumn(result.wrapColumn("Data", result.getRealTable().getColumn("DataId"))).setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return createDataTable();
            }
        });
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RowId"))).setIsHidden(true);
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("FitProb")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ResVar")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RegressionType")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("StdCurve")));
        ColumnInfo lsidColumn = result.addColumn(result.wrapColumn(result.getRealTable().getColumn("LSID")));
        lsidColumn.setIsHidden(true);

        String sqlObjectId = "(SELECT objectid FROM " + OntologyManager.getTinfoObject() + " o WHERE o.objecturi = " +
                ExprColumn.STR_TABLE_ALIAS + ".lsid)";

        ColumnInfo colProperty = new ExprColumn(result, "Properties", new SQLFragment(sqlObjectId), Types.INTEGER);
        Domain analyteDomain = AbstractAssayProvider.getDomainByPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        Map<String, PropertyDescriptor> map = new TreeMap<String, PropertyDescriptor>();
        for(DomainProperty pd : analyteDomain.getProperties())
        {
            map.put(pd.getName(), pd.getPropertyDescriptor());
        }
        colProperty.setFk(new PropertyForeignKey(map, this));
        colProperty.setIsUnselectable(true);
        result.addColumn(colProperty);

        addDataFilter(result);
        result.setTitleColumn("Name");
        return result;
    }

    public ExpDataTable createDataTable()
    {
        ExpDataTable ret = ExperimentService.get().createDataTable(ExpSchema.TableType.Datas.toString(), this);
        ret.addColumn(ExpDataTable.Column.RowId);
        ret.addColumn(ExpDataTable.Column.Name);
        ret.addColumn(ExpDataTable.Column.Flag);
        ret.addColumn(ExpDataTable.Column.Created);
        ret.setTitleColumn("Name");
        ColumnInfo protocol = ret.addColumn(ExpDataTable.Column.Protocol);
        protocol.setIsHidden(true);

        ColumnInfo runCol = ret.addColumn(ExpDataTable.Column.Run);
        if (_protocol != null)
        {
            runCol.setFk(new LookupForeignKey("RowId")
            {
                public TableInfo getLookupTableInfo()
                {
                    return AssayService.get().createRunTable(_protocol, AssayService.get().getProvider(_protocol), _user, _container);
                }
            });
        }

        return ret;
    }

    public FilteredTable createDataRowTable()
    {
        final FilteredTable result = new FilteredTable(getTableInfoDataRow());
        result.addColumn(result.wrapColumn("Analyte", result.getRealTable().getColumn("AnalyteId")));
        ColumnInfo dataColumn = result.addColumn(result.wrapColumn("Data", result.getRealTable().getColumn("DataId")));
        dataColumn.setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return createDataTable();
            }
        });
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("RowId"))).setIsHidden(true);
        result.getColumn("RowId").setKeyField(true);
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Type")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Well")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Outlier")));
        ColumnInfo specimenColumn = result.wrapColumn(result.getRealTable().getColumn("Description"));
        specimenColumn.setFk(new SpecimenForeignKey(this, AssayService.get().getProvider(_protocol), _protocol));
        result.addColumn(specimenColumn);
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ExtraSpecimenInfo")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("FIString"))).setCaption("FI String");
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("FI"), result.getRealTable().getColumn("FIOORIndicator"));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("FIBackgroundString"))).setCaption("FI-Bkgd String");
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("FIBackground"), result.getRealTable().getColumn("FIBackgroundOORIndicator"), "FI-Bkgd");
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("StdDevString")));
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("StdDev"), result.getRealTable().getColumn("StdDevOORIndicator"));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ObsConcString")));
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("ObsConc"), result.getRealTable().getColumn("ObsConcOORIndicator"));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ExpConc")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ObsOverExp"))).setCaption("(Obs/Exp)*100");
        OORDisplayColumnFactory.addOORColumns(result, result.getRealTable().getColumn("ConcInRange"), result.getRealTable().getColumn("ConcInRangeOORIndicator"));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("ConcInRangeString")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Dilution")));
        result.addColumn(result.wrapColumn("Group", result.getRealTable().getColumn("DataRowGroup")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Ratio")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("SamplingErrors")));

        result.addColumn(result.wrapColumn("ParticipantID", result.getRealTable().getColumn("PTID")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("VisitID")));
        result.addColumn(result.wrapColumn(result.getRealTable().getColumn("Date")));

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();
        defaultCols.add(FieldKey.fromParts("Analyte"));
        defaultCols.add(FieldKey.fromParts("Type"));
        defaultCols.add(FieldKey.fromParts("Well"));
        defaultCols.add(FieldKey.fromParts("Description"));
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
        result.setDefaultVisibleColumns(defaultCols);

        result.getColumn("Analyte").setFk(new LookupForeignKey("RowId")
        {
            public TableInfo getLookupTableInfo()
            {
                return createAnalyteTable();
            }
        });
        addDataFilter(result);
        return result;
    }

    private void addDataFilter(FilteredTable result)
    {
        SQLFragment filter = new SQLFragment("DataId IN (SELECT d.RowId FROM " + ExperimentService.get().getTinfoData() + " d, " + ExperimentService.get().getTinfoExperimentRun() + " r WHERE d.RunId = r.RowId AND d.Container = ?");
        filter.add(getContainer().getId());
        if (_protocol != null)
        {
            filter.append(" AND r.ProtocolLSID = ?");
            filter.add(_protocol.getLSID());
        }
        filter.append(")");
        result.addCondition(filter);
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get("luminex");
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }

    public static TableInfo getTableInfoAnalytes()
    {
        return getSchema().getTable(ANALYTE_TABLE_NAME);
    }

    public static TableInfo getTableInfoDataRow()
    {
        return getSchema().getTable(DATA_ROW_TABLE_NAME);
    }
}
