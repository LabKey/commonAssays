package org.labkey.microarray;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SqlDialect;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.security.User;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.exp.api.ExpRunTable;
import org.labkey.api.exp.api.ExpSchema;
import org.labkey.microarray.assay.MicroarrayAssayProvider;

import java.util.Set;

public class MicroarraySchema extends UserSchema
{
    public static final String SCHEMA_NAME = "Microarray";
    public static final String TABLE_RUNS = "MicroarrayRuns";

    private ExpSchema _expSchema;
    
    public MicroarraySchema(User user, Container container)
    {
        super(SCHEMA_NAME, user, container, getSchema());
        _expSchema = new ExpSchema(user, container);
    }


    static public void register()
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new MicroarraySchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public Set<String> getTableNames()
    {
        return PageFlowUtil.set(TABLE_RUNS);
    }

    public TableInfo getTable(String name, String alias)
    {
        if (TABLE_RUNS.equalsIgnoreCase(name))
        {
            return createRunsTable(alias);
        }
        return super.getTable(name, alias);
    }

    private TableInfo createRunsTable(String alias)
    {
        ExpRunTable result = _expSchema.createRunsTable(alias);
        result.setProtocolPatterns("urn:lsid:%:" + MicroarrayAssayProvider.PROTOCOL_PREFIX + ".%");
        return result;
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get("microarray");
    }

    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }
}
