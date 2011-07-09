package org.labkey.luminex;

import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FilteredTable;

import java.util.Collection;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public abstract class AbstractLuminexTable extends FilteredTable
{
    protected final LuminexSchema _schema;
    private final boolean _needsFilter;

    private static final String CONTAINER_FAKE_COLUMN_NAME = "Container";

    public AbstractLuminexTable(TableInfo table, LuminexSchema schema, boolean filter)
    {
        super(table);
        _schema = schema;
        _needsFilter = filter;

        applyContainerFilter(getContainerFilter());
    }

    @Override
    protected final void applyContainerFilter(ContainerFilter filter)
    {
        if (_needsFilter)
        {
            clearConditions(CONTAINER_FAKE_COLUMN_NAME);
            Collection<String> ids = filter.getIds(_schema.getContainer());
            if (ids != null)
            {
                addCondition(createContainerFilterSQL(ids), CONTAINER_FAKE_COLUMN_NAME);
            }
        }
    }

    protected abstract SQLFragment createContainerFilterSQL(Collection<String> ids);
}
