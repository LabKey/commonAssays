package Issues.query;

import org.fhcrc.cpas.query.api.UserSchema;
import org.fhcrc.cpas.query.api.DefaultSchema;
import org.fhcrc.cpas.query.api.QuerySchema;
import org.fhcrc.cpas.security.User;
import org.fhcrc.cpas.data.Container;
import org.fhcrc.cpas.data.TableInfo;
import org.fhcrc.cpas.issues.IssuesSchema;

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collections;

public class IssuesQuerySchema extends UserSchema 
{
    public static final String SCHEMA_NAME = "issues";

    public enum TableType
    {
        Issues,
    }
    static private Set<String> tableNames = new LinkedHashSet<String>();
    static
    {
        for (TableType type : TableType.values())
        {
            tableNames.add(type.toString());
        }
        tableNames = Collections.unmodifiableSet(tableNames);
    }

    static public void register()
    {
        DefaultSchema.registerProvider(SCHEMA_NAME, new DefaultSchema.SchemaProvider() {
            public QuerySchema getSchema(DefaultSchema schema)
            {
                return new IssuesQuerySchema(schema.getUser(), schema.getContainer());
            }
        });
    }

    public IssuesQuerySchema(User user, Container container)
    {
        super(SCHEMA_NAME, user, container, IssuesSchema.getInstance().getSchema());
    }

    public Set<String> getTableNames()
    {
        return tableNames;
    }

    public TableInfo getTable(String name, String alias)
    {
        try
        {
            switch(TableType.valueOf(name))
            {
                case Issues:
                    return createIssuesTable(alias);
            }
        }
        catch (IllegalArgumentException e){}
        return super.getTable(name, alias);
    }

    public TableInfo createIssuesTable(String alias)
    {
        return new IssuesTable(this);
    }
}
