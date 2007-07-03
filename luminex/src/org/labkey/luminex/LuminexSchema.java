package org.labkey.luminex;

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SqlDialect;
import org.labkey.api.data.TableInfo;

public class LuminexSchema
{
    private static LuminexSchema _instance = null;

    public static LuminexSchema getInstance()
    {
        if (null == _instance)
            _instance = new LuminexSchema();

        return _instance;
    }

    private LuminexSchema()
    {
        // private contructor to prevent instantiation from
        // outside this class: this singleton should only be
        // accessed via cpas.luminex.LuminexSchema.getInstance()
    }

    public DbSchema getSchema()
    {
        return DbSchema.get("luminex");
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }
}
