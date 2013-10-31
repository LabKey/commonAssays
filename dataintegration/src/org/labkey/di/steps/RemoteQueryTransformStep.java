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
import org.junit.Assert;
import org.junit.Test;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.PropertyManager;
import org.labkey.api.etl.CopyConfig;
import org.labkey.api.etl.DataIterator;
import org.labkey.api.etl.DataIteratorBuilder;
import org.labkey.api.etl.DataIteratorContext;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.security.User;
import org.labkey.api.settings.AppProps;
import org.labkey.di.pipeline.TransformJobContext;
import org.labkey.di.pipeline.TransformTaskFactory;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.RemoteConnections;
import org.labkey.remoteapi.SelectRowsStreamHack;
import org.labkey.remoteapi.query.SelectRowsCommand;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

/**
 * User: gktaylor
 * Date: 2013-10-08
 */
public class RemoteQueryTransformStep extends SimpleQueryTransformStep
{

    public RemoteQueryTransformStep(TransformTaskFactory f, PipelineJob job, SimpleQueryTransformStepMeta meta, TransformJobContext context)
    {
        super(f, job, meta, context);
    }

    @Override
    public boolean hasWork()
    {
        return true;
    }

    @Override
    public boolean validate(CopyConfig meta, Container c, User u, Logger log)
    {
        // sourceSchema is remote and is not used

        QuerySchema targetSchema = DefaultSchema.get(u, c, meta.getTargetSchema());
        if (null == targetSchema || null == targetSchema.getDbSchema())
        {
            log.error("ERROR: Target schema not found: " + meta.getTargetSchema());
            return false;
        }

        return true;
    }

    // allows RemoteQueryTransformStep to override this method and selectively alter executeCopy
    @Override
    protected DbScope getSourceScope(QuerySchema sourceSchema, DbScope targetScope)
    {
        // return null, there is no source scope for a remote query
        return null;
    }

    @Override
    DataIteratorBuilder selectFromSource(CopyConfig meta, Container c, User u, DataIteratorContext context, Logger log) throws SQLException
    {
        // find the category to look up in the property manager, provided by the .xml
        if (! (meta instanceof RemoteQueryTransformStepMeta) )
            throw new UnsupportedOperationException("This xml parser was expected an instance of RemoteQueryTransformStepMeta");
        String name = ((RemoteQueryTransformStepMeta)meta).getRemoteSource();
        if (name == null)
        {
            log.error("The remoteSource option provided in the xml must refer to a Remote Connection.");
            return null;
        }

        // Check that an entry for the remote connection name exists
        Map<String, String> connectionMap = PropertyManager.getEncryptedStore().getProperties(c, RemoteConnections.REMOTE_CONNECTIONS_CATEGORY);
        if (connectionMap.get(RemoteConnections.REMOTE_CONNECTIONS_CATEGORY + ":" + name) == null)
        {
            log.error("The remote connection " + name + " has not yet been setup in the remote connection manager.  You may configure a new remote connection through the schema browser.");
            return null;
        }

        // Extract the username, password, and container from the secure property store
        Map<String, String> singleConnectionMap = PropertyManager.getEncryptedStore().getProperties(c, RemoteConnections.REMOTE_CONNECTIONS_CATEGORY + ":" + name);
        String url = singleConnectionMap.get(RemoteConnections.FIELD_URL);
        String user = singleConnectionMap.get(RemoteConnections.FIELD_USER);
        String password = singleConnectionMap.get(RemoteConnections.FIELD_PASSWORD);
        String container = singleConnectionMap.get(RemoteConnections.FIELD_CONTAINER);
        if (url == null || user == null || password == null || container == null)
        {
            log.error("Invalid login credentials in the secure user store");
            return null;
        }

        try
        {
            return selectFromSource(meta.getSourceSchema().toString(), meta.getSourceQuery(), url, user, password, container);
        }
        catch (IOException | CommandException exception)
        {
            log.error(exception.getMessage());
            return null;
        }
    }

    static DataIteratorBuilder selectFromSource(String schemaName, String queryName, String url, String user, String password, String container)
            throws IOException, CommandException
    {
        // connect to the remote server and retrieve an input stream
        Connection cn = new Connection(url, user, password);
        final SelectRowsCommand cmd = new SelectRowsCommand(schemaName, queryName);

        DataIteratorBuilder source = SelectRowsStreamHack.go(cn, container, cmd);
        return source;
    }

    public static class TestCase extends Assert
    {
        @Test
        public void selectRows() throws Exception
        {
            // Execute a 'remote' query against the currently running server.
            // We use the home container since we won't need to authenticate the user.
            String url = AppProps.getInstance().getBaseServerUrl() + AppProps.getInstance().getContextPath();
            Container home = ContainerManager.getHomeContainer();
            DataIteratorBuilder b = selectFromSource("core", "Containers", url, null, null, home.getPath());

            DataIteratorContext context = new DataIteratorContext();
            try (DataIterator iter = b.getDataIterator(context))
            {
                int idxEntityId = -1;
                int idxID = -1;
                int idxName = -1;
                int idxCreated = -1;

                for (int i = 1; i <= iter.getColumnCount(); i++)
                {
                    ColumnInfo col = iter.getColumnInfo(i);
                    switch (col.getName())
                    {
                        case "EntityId": idxEntityId = i; break;
                        case "ID":       idxID = i;       break;
                        case "Name":     idxName = i;     break;
                        case "Created":  idxCreated = i;  break;
                    }
                }

                assertTrue("Expected to find EntityId column: " + idxEntityId, idxEntityId > 0);
                assertTrue("Expected to find ID column: " + idxID, idxID > 0);
                assertTrue("Expected to find Name column: " + idxName, idxName > 0);
                assertTrue("Expected to find Created column: " + idxCreated, idxCreated > 0);

                assertTrue("Expected to select a single row for the Home container.", iter.next());

                // Check the select rows returns the Home container details
                assertEquals(home.getId(), iter.get(idxEntityId));
                assertTrue(iter.get(idxEntityId) instanceof String);

                assertEquals(home.getRowId(), iter.get(idxID));
                assertTrue(iter.get(idxID) instanceof Integer);

                assertEquals(home.getName(), iter.get(idxName));
                assertTrue(iter.get(idxName) instanceof String);

                assertTrue(iter.get(idxCreated) instanceof Date);
                // The remoteapi Date doesn't have milliseconds so the Dates won't be equal -- just compare day instead.
                assertEquals(home.getCreated().getDay(), ((Date)iter.get(idxCreated)).getDay());

                // We expect only one row
                assertFalse(iter.next());
            }

        }
    }

}
