package org.labkey.ms1;

import org.labkey.api.data.DbSchema;

public class MS1Manager
{
    private static MS1Manager _instance;
    private static final String SCHEMA_NAME = "ms1";

    private MS1Manager()
    {
        // prevent external construction with a private default constructor
    }

    public static synchronized MS1Manager get()
    {
        if (_instance == null)
            _instance = new MS1Manager();
        return _instance;
    }

    public String getSchemaName()
    {
        return SCHEMA_NAME;
    }
    public DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME);
    }
}