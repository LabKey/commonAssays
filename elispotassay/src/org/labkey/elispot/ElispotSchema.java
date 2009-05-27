/*
 * Copyright (c) 2008-2009 LabKey Corporation
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

package org.labkey.elispot;

import org.labkey.api.data.*;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.api.study.assay.AssaySchema;
import org.labkey.api.study.assay.AssayService;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ElispotSchema extends AssaySchema
{
    private static final String DATA_ROW_TABLE_NAME = "DataRow";
    private final ExpProtocol _protocol;

    public ElispotSchema(User user, Container container)
    {
        this(user, container, null);
    }

    public ElispotSchema(User user, Container container, ExpProtocol protocol)
    {
        super("Elispot", user, container, ExperimentService.get().getSchema());
        _protocol = protocol;
    }

    public Set<String> getTableNames()
    {
        return new HashSet<String>(Arrays.asList(DATA_ROW_TABLE_NAME));
    }

    public TableInfo createTable(String name)
    {
        for (ExpProtocol protocol : AssayService.get().getAssayProtocols(getContainer()))
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider != null && provider instanceof ElispotAssayProvider)
            {
                if (DATA_ROW_TABLE_NAME.equalsIgnoreCase(name))
                {
                    return provider.createDataTable(AssayService.get().createSchema(getUser(), getContainer()), protocol);
                }
            }
        }
        return null;
    }

/*
    public FilteredTable createDataRowTable(String alias, QuerySchema schema)
    {
        final FilteredTable result = new FilteredTable(getTableInfoDataRow());


        result.addColumn(result.wrapColumn("Antigen", result.getRealTable().getColumn("Antigen")));
        result.addColumn(result.wrapColumn("SFU", result.getRealTable().getColumn("SFU")));
        //result.addColumn(result.wrapColumn(result.getRealTable().getColumn("CellWell")));

        List<FieldKey> defaultCols = new ArrayList<FieldKey>();

        ColumnInfo lsidColumn = result.addColumn(result.wrapColumn(result.getRealTable().getColumn("LSID")));
        lsidColumn.setIsHidden(true);

        String sqlObjectId = "(SELECT objectid FROM " + OntologyManager.getTinfoObject() + " o WHERE o.objecturi = " +
                ExprColumn.STR_TABLE_ALIAS + ".lsid)";

        ColumnInfo colProperty = new ExprColumn(result, "Properties", new SQLFragment(sqlObjectId), Types.INTEGER);
        PropertyDescriptor[] pds = AbstractAssayProvider.getDomainByPrefix(_protocol, ElispotAssayProvider.ASSAY_DOMAIN_SAMPLE_WELLGROUP);
        Map<String, PropertyDescriptor> map = new TreeMap<String, PropertyDescriptor>();
        FieldKey keyProp = new FieldKey(null, "Property");
        for(PropertyDescriptor pd : pds)
        {
            map.put(pd.getName(), pd);
            defaultCols.add(new FieldKey(keyProp, pd.getName()));
        }
        colProperty.setFk(new PropertyForeignKey(map, this));
        colProperty.setIsUnselectable(true);
        result.addColumn(colProperty);

        defaultCols.add(FieldKey.fromParts("Antigen"));
        defaultCols.add(FieldKey.fromParts("SFU"));
        //defaultCols.add(FieldKey.fromParts("CellWell"));

        result.setDefaultVisibleColumns(defaultCols);
        result.setAlias(alias);

        return result;
    }

    public ExpDataTable createDataTable(String alias)
    {
        ExpDataTable ret = ExperimentService.get().createDataTable(alias);
        ret.setContainer(getContainer());
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
                    return AssayService.get().createRunTable(null, _protocol, AssayService.get().getProvider(_protocol), _user, _container);
                }
            });
        }

        return ret;
    }
*/

    public static DbSchema getSchema()
    {
        return DbSchema.get("elispot");
    }

    public static PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol) throws SQLException
    {
        String propPrefix = new Lsid(ElispotDataHandler.ELISPOT_PROPERTY_LSID_PREFIX, protocol.getName(), "").toString();
        SimpleFilter propertyFilter = new SimpleFilter();
        propertyFilter.addCondition("PropertyURI", propPrefix, CompareType.STARTS_WITH);

        return Table.select(OntologyManager.getTinfoPropertyDescriptor(), Table.ALL_COLUMNS,
                propertyFilter, null, PropertyDescriptor.class);
    }

}
