/*
 * Copyright (c) 2005-2014 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein;

import org.labkey.api.data.DbScope;
import org.labkey.api.pipeline.PipeRoot;
import org.labkey.api.view.ViewBackgroundInfo;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;

/**
 * Based loosely on XERCES'
 * sample SAX2 counter.
 */

public class XMLProteinLoader extends DefaultAnnotationLoader
{
    private final boolean _clearExisting;

    @Override
    public String getDescription()
    {
        return "Uniprot XML import - " + _file;
    }

    public XMLProteinLoader(File file, ViewBackgroundInfo info, PipeRoot root, boolean clearExisting) throws IOException
    {
        super(file, info, root);
        _clearExisting = clearExisting;
    }

    public boolean isClearExisting()
    {
        return _clearExisting;
    }

    @Override
    public void run()
    {
        boolean success = false;
        info("Starting annotation load for " + _file);
        setStatus(TaskStatus.running);
        try (DbScope.Transaction transaction = ProteinManager.getSchema().getScope().ensureTransaction())
        {
            validate();
            Connection conn = transaction.getConnection();
            XMLProteinHandler handler = new XMLProteinHandler(conn, this);
            handler.parse(_file);
            conn.setAutoCommit(false);
            ProteinManager.indexProteins(null, (Date)null);
            info("Import completed successfully");
            setStatus(TaskStatus.complete);
            success = true;
            transaction.commit();
        }
        catch (SAXException e)
        {
            error("Import failed due to XML parsing error", e);
        }
        catch (Exception e)
        {
            error("Import failed", e);
        }

        if (!success)
        {
            // Need to do this outside of the connection, so can't do it inside the catch block above
            setStatus(TaskStatus.error);
        }
    }
} // class XMLProteinLoader

