package org.labkey.nab.query;

import org.labkey.api.query.*;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.security.User;
import org.labkey.api.data.*;
import org.labkey.api.study.assay.AssayService;
import org.labkey.api.study.assay.AssayProvider;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabDataHandler;

import java.util.*;
import java.sql.SQLException;

/**
 * User: brittp
 * Date: Oct 3, 2007
 * Time: 4:22:26 PM
 */
public class NabSchema extends UserSchema
{
    private static final String DATA_ROW_TABLE_NAME = "Data";

    public NabSchema(User user, Container container)
    {
        super("Nab", user, container, ExperimentService.get().getSchema());
    }

    public Set<String> getTableNames()
    {
        Set<String> names = new TreeSet<String>(new Comparator<String>()
        {
            public int compare(String o1, String o2)
            {
                return o1.compareToIgnoreCase(o2);
            }
        });

        for (ExpProtocol protocol : AssayService.get().getAssayProtocols(getContainer()))
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider != null && provider instanceof NabAssayProvider)
                names.add(getTableName(protocol));
        }
        return names;
    }

    private static String getTableName(ExpProtocol protocol)
    {
        return protocol.getName() + " " + DATA_ROW_TABLE_NAME;
    }

    public TableInfo getTable(String name, String alias)
    {
        for (ExpProtocol protocol : AssayService.get().getAssayProtocols(getContainer()))
        {
            AssayProvider provider = AssayService.get().getProvider(protocol);
            if (provider != null && provider instanceof NabAssayProvider)
            {
                if (getTableName(protocol).equalsIgnoreCase(name))
                {
                    return new NabRunDataTable(this, alias, protocol);
                }
            }
        }
        return super.getTable(name, alias);
    }

    public static TableInfo getDataRowTable(User user, ExpProtocol protocol, String alias)
    {
        return new NabSchema(user, protocol.getContainer()).getTable(getTableName(protocol), alias);        
    }

    public static PropertyDescriptor[] getExistingDataProperties(ExpProtocol protocol) throws SQLException
    {
        String propPrefix = new Lsid(NabDataHandler.NAB_PROPERTY_LSID_PREFIX, protocol.getName(), "").toString();
        SimpleFilter propertyFilter = new SimpleFilter();
        propertyFilter.addCondition("PropertyURI", propPrefix, CompareType.STARTS_WITH);
        return Table.select(OntologyManager.getTinfoPropertyDescriptor(), Table.ALL_COLUMNS,
                propertyFilter, null, PropertyDescriptor.class);
    }
}
