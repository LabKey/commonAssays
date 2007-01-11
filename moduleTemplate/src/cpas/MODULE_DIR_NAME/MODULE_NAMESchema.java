package cpas.@@MODULE_LOWERCASE_NAME@@;

import org.fhcrc.cpas.data.DbSchema;
import org.fhcrc.cpas.data.SqlDialect;
import org.fhcrc.cpas.data.TableInfo;

public class @@MODULE_NAME@@Schema
{
    private static @@MODULE_NAME@@Schema _instance = null;

    public static @@MODULE_NAME@@Schema getInstance()
    {
        if (null == _instance)
            _instance = new @@MODULE_NAME@@Schema();

        return _instance;
    }

    private @@MODULE_NAME@@Schema()
    {
        // private contructor to prevent instantiation from
        // outside this class: this singleton should only be
        // accessed via cpas.@@MODULE_LOWERCASE_NAME@@.@@MODULE_NAME@@Schema.getInstance()
    }

    public DbSchema getSchema()
    {
        return DbSchema.get("@@MODULE_LOWERCASE_NAME@@");
    }

    public SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }
}
