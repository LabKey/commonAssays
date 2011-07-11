/*
 * Copyright (c) 2011 LabKey Corporation
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
