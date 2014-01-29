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
package org.labkey.di.steps;

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.etl.AsyncDataIterator;
import org.labkey.api.etl.CopyConfig;
import org.labkey.api.etl.DataIteratorBuilder;
import org.labkey.api.etl.DataIteratorContext;
import org.labkey.api.etl.QueryDataIteratorBuilder;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.pipeline.PipelineJobException;
import org.labkey.api.pipeline.RecordedAction;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QueryParseException;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.di.TransformDataIteratorBuilder;
import org.labkey.di.data.TransformProperty;
import org.labkey.di.filters.FilterStrategy;
import org.labkey.di.pipeline.TransformJobContext;
import org.labkey.di.pipeline.TransformTask;
import org.labkey.di.pipeline.TransformTaskFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: matthewb
 * Date: 2013-04-16
 * Time: 12:27 PM
 */
public class SimpleQueryTransformStep extends TransformTask
{
    final SimpleQueryTransformStepMeta _meta;

    boolean _useAsynchrousQuery = false;

    public SimpleQueryTransformStep(TransformTaskFactory f, PipelineJob job, SimpleQueryTransformStepMeta meta, TransformJobContext context)
    {
        super(f, job, meta, context);
        _meta = meta;
    }

    public void doWork(RecordedAction action) throws PipelineJobException
    {
        try
        {
            getJob().getLogger().debug("SimpleQueryTransformStep.doWork called");
            if (!executeCopy(_meta, _context.getContainer(), _context.getUser(), getJob().getLogger()))
                throw new PipelineJobException("Error running executeCopy");
            recordWork(action);
        }
        catch (Exception x)
        {
            throw new PipelineJobException(x);
        }
    }

    protected void recordWork(RecordedAction action)
    {
        if (-1 != _recordsInserted)
            action.addProperty(TransformProperty.RecordsInserted.getPropertyDescriptor(), _recordsInserted);
        if (-1 != _recordsDeleted)
            action.addProperty(TransformProperty.RecordsDeleted.getPropertyDescriptor(), _recordsDeleted);

        try
        {
            // input is source table
            // output is dest table
            // todo: this is a fake URI, figure out the real story for the Data Input/Ouput for a transform step
            action.addInput(new URI(_meta.getSourceSchema() + "." + _meta.getSourceQuery()), TransformTask.INPUT_ROLE);
            action.addOutput(new URI(_meta.getTargetSchema() + "." + _meta.getTargetQuery()), TransformTask.OUTPUT_ROLE, false);
        }
        catch (URISyntaxException ignore)
        {
        }
    }

    public boolean validate(CopyConfig meta, Container c, User u, Logger log)
    {
        QuerySchema sourceSchema = DefaultSchema.get(u, c, meta.getSourceSchema());
        if (null == sourceSchema || null == sourceSchema.getDbSchema())
        {
            log.error("ERROR: Source schema not found: " + meta.getSourceSchema());
            return false;
        }

        QuerySchema targetSchema = DefaultSchema.get(u, c, meta.getTargetSchema());
        if (null == targetSchema || null == targetSchema.getDbSchema())
        {
            log.error("ERROR: Target schema not found: " + meta.getTargetSchema());
            return false;
        }

        return true;
    }

    // allows RemoteQueryTransformStep to override this method and selectively alter executeCopy
    protected DbScope getSourceScope(QuerySchema sourceSchema, DbScope targetScope)
    {
        DbScope sourceScope = sourceSchema.getDbSchema().getScope();
        if (sourceScope.equals(targetScope))
            return null;
        return sourceScope;
    }

    public boolean executeCopy(CopyConfig meta, Container c, User u, Logger log) throws IOException, SQLException
    {
        boolean validationResult = validate(meta, c, u, log);
        if (validationResult == false)
            return false;

        QuerySchema sourceSchema = DefaultSchema.get(u, c, meta.getSourceSchema());
        QuerySchema targetSchema = DefaultSchema.get(u, c, meta.getTargetSchema());

        DbScope targetScope = targetSchema.getDbSchema().getScope();
        DbScope sourceScope = getSourceScope(sourceSchema, targetScope);

        ResultSet rs = null;
        DataIteratorContext context = new DataIteratorContext();
        context.setInsertOption(QueryUpdateService.InsertOption.MERGE);
        context.setFailFast(true);
        try
        {
            long start = System.currentTimeMillis();

            try (
                    DbScope.Transaction txTarget = targetScope.ensureTransaction(Connection.TRANSACTION_SERIALIZABLE);
                    DbScope.Transaction txSource = (null==sourceScope)?null:sourceScope.ensureTransaction(Connection.TRANSACTION_REPEATABLE_READ)
            )
            {
                log.info("Copying data from " + meta.getSourceSchema() + "." + meta.getSourceQuery() + " to " +
                        meta.getTargetSchema() + "." + meta.getTargetQuery());

                DataIteratorBuilder source = selectFromSource(meta, c, u, context, log);
                if (null == source)
                    return false;
                int transformRunId = getTransformJob().getTransformRunId();
                DataIteratorBuilder transformSource = new TransformDataIteratorBuilder(transformRunId, source, log);

                _recordsInserted = appendToTarget(meta, c, u, context, transformSource, log);

                txTarget.commit();
                if (null != txSource)
                    txSource.commit();
            }
            long finish = System.currentTimeMillis();
            log.info("Copied " + getNumRowsString(_recordsInserted) + " in " + DateUtil.formatDuration(finish - start));
        }
        catch (Exception x)
        {
            // TODO: more verbose logging
            log.error("Failed to run transform from source.", x);
            return false;
        }
        finally
        {
            ResultSetUtil.close(rs);
            targetScope.closeConnection();
            if (null != sourceScope)
                sourceScope.closeConnection();
        }
        if (context.getErrors().hasErrors())
        {
            for (ValidationException v : context.getErrors().getRowErrors())
            {
                log.error(v.getMessage());
            }
            return false;
        }
        return true;
    }


    DataIteratorBuilder selectFromSource(CopyConfig meta, Container c, User u, DataIteratorContext context, Logger log) throws SQLException
    {
        try
        {
            QuerySchema sourceSchema = DefaultSchema.get(u, c, meta.getSourceSchema());
            sourceSchema.getTable(meta.getSourceQuery());   // validate source query
            FilterStrategy filterStrategy = getFilterStrategy();
            SimpleFilter f = filterStrategy.getFilter(getVariableMap());

            try
            {
                log.info(filterStrategy.getClass().getSimpleName() + ": " + (null == f ? "no filter"  : f.toSQLString(sourceSchema.getDbSchema().getSqlDialect())));
            }
            catch (UnsupportedOperationException|IllegalArgumentException x)
            {
                /* oh well */
            }

            DataIteratorBuilder source = new QueryDataIteratorBuilder(sourceSchema, meta.getSourceQuery(), null, f);
            if (_useAsynchrousQuery)
                source = new AsyncDataIterator.Builder(source);
            return source;
        }
        catch (QueryParseException x)
        {
            log.error(x.getMessage());
            return null;
        }
    }

    static int appendToTarget(CopyConfig meta, Container c, User u, DataIteratorContext context, DataIteratorBuilder source, Logger log)
    {
        QuerySchema querySchema =  DefaultSchema.get(u, c, meta.getTargetSchema());
        if (null == querySchema || null == querySchema.getDbSchema())
        {
            context.getErrors().addRowError(new ValidationException("Could not find schema: " + meta.getTargetSchema()));
            return -1;
        }
        TableInfo targetTableInfo = querySchema.getTable(meta.getTargetQuery());
        if (null == targetTableInfo)
        {
            context.getErrors().addRowError(new ValidationException("Could not find table: " +  meta.getTargetSchema() + "." + meta.getTargetQuery()));
            return -1;
        }
        try
        {
            QueryUpdateService qus = targetTableInfo.getUpdateService();
            if (null == qus)
            {
                context.getErrors().addRowError(new ValidationException("Can't import into table: " + meta.getTargetSchema() + "." + meta.getTargetQuery()));
                return -1;
            }

            log.info("Target option: " + meta.getTargetOptions());
            if (CopyConfig.TargetOptions.merge == meta.getTargetOptions())
            {
                return qus.mergeRows(u, c, source, context.getErrors(), null);
            }
            else
            if (CopyConfig.TargetOptions.truncate == meta.getTargetOptions())
            {
                int rows = qus.truncateRows(u, c, null /*extra script context */);
                log.info("Deleted " + getNumRowsString(rows) + " from " + meta.getTargetSchema() + "."  + meta.getTargetQuery());
                return qus.importRows(u, c, source, context.getErrors(), null);
            }
            else
            {
                Map<Enum,Object> options = new HashMap<>();
                options.put(QueryUpdateService.ConfigParameters.Logger,log);
                return qus.importRows(u, c, source, context.getErrors(), options, null);
            }
        }
        catch (BatchValidationException|QueryUpdateServiceException ex)
        {
            throw new RuntimeException(ex);
        }
        catch (SQLException sqlx)
        {
            throw new RuntimeSQLException(sqlx);
        }
    }
}
