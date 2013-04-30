/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.di.filters;

import org.apache.commons.lang3.StringUtils;
import org.labkey.api.data.Aggregate;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.etl.CopyConfig;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.di.pipeline.TransformJobContext;
import org.labkey.di.VariableMap;
import org.labkey.di.pipeline.TransformManager;
import org.labkey.di.steps.StepMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 4/22/13
 * Time: 12:26 PM
 */
public class ModifiedSinceFilterStrategy implements FilterStrategy
{
    final TransformJobContext _context;
    final CopyConfig _config;

    TableInfo _table;
    ColumnInfo _tsCol;

    public ModifiedSinceFilterStrategy(TransformJobContext context, StepMeta stepMeta)
    {
        if (!(stepMeta instanceof CopyConfig))
            throw new IllegalArgumentException(this.getClass().getName() + " is not compatible with " + stepMeta.getClass().getName());
        _context = context;
        _config = (CopyConfig)stepMeta;
    }


    private void init()
    {
        if (null != _tsCol)
            return;

        QuerySchema schema = DefaultSchema.get(_context.getUser(), _context.getContainer(), _config.getSourceSchema());
        if (null == schema)
            throw new IllegalArgumentException("Schema not found: " + _config.getSourceSchema());

        _table = schema.getTable(_config.getSourceQuery());
        if (null == _table)
            throw new IllegalArgumentException("Table not found: " + _config.getSourceQuery());

        String timestampColumnName = StringUtils.defaultString(_config.getSourceTimestampColumnName(), "modified");
        _tsCol = _table.getColumn(timestampColumnName);
        if (null == _tsCol)
            throw new IllegalArgumentException("Column not found: " + _config.getSourceQuery() + "." + timestampColumnName);
    }


    @Override
    public boolean hasWork()
    {
        init();

        SimpleFilter f = null;
        Date lastSuccessfulEndDate = getLastSuccessfulIncrementalEndTimestamp();

        if (null != lastSuccessfulEndDate)
            f = new SimpleFilter(_tsCol.getFieldKey(), lastSuccessfulEndDate, CompareType.GT);

        TableSelector ts = new TableSelector(_table, Collections.singleton(_tsCol), f, null);
        long count = ts.getRowCount();
        return count > 0;
    }



    @Override
    public SimpleFilter getFilter(VariableMap variables)
    {
        init();

        SimpleFilter f = new SimpleFilter();
        Date incrementalStartTimestamp = getLastSuccessfulIncrementalEndTimestamp();
        if (null != incrementalStartTimestamp)
            f.addCondition(_tsCol.getFieldKey(), incrementalStartTimestamp, CompareType.GT);

        Aggregate max = new Aggregate(_tsCol, Aggregate.Type.MAX);

        TableSelector ts = new TableSelector(_table, Collections.singleton(_tsCol), f, null);
        Map<String, List<Aggregate.Result>> results = ts.getAggregates(Arrays.asList(max));
        List<Aggregate.Result> list = results.get(_tsCol.getName());
        Aggregate.Result maxResult = list.get(0);
        Date incrementalEndDate = ((Date)maxResult.getValue());
        if (null == incrementalEndDate)
            incrementalEndDate = incrementalStartTimestamp;
        f.addCondition(_tsCol.getFieldKey(), incrementalEndDate, CompareType.LTE);

        variables.put(TransformJobContext.Variable.IncrementalStartTimestamp, incrementalStartTimestamp);
        variables.put(TransformJobContext.Variable.IncrementalEndTimestamp, incrementalEndDate);
        return f;
    }


    Date getLastSuccessfulIncrementalEndTimestamp()
    {
        // get the experiment run for the last successfully run transform for this configuration
        Integer expRunId = TransformManager.get().getLastSuccessfulTransformExpRun(_context.getTransformId(), _context.getTransformVersion());
        if (null != expRunId)
        {
            VariableMap map = TransformManager.get().getVariableMapForTransformStep(expRunId, _config.getId());
            if (null != map)
            {
                for (String key : map.keySet())
                {
                    if (StringUtils.equals(key, TransformJobContext.Variable.IncrementalEndTimestamp.getName()))
                        return (Date) map.get(key);
                }
            }
        }
        return null;
    }
}
