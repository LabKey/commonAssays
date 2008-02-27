/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

import org.labkey.api.util.NetworkDrive;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Based loosely on XERCES'
 * sample SAX2 counter.
 */

public class XMLProteinLoader extends DefaultAnnotationLoader implements AnnotationLoader
{
    private final boolean _clearExisting;

    public int getId()
    {
        return 0;
    }

    public void validate() throws IOException
    {
        getFile();
    }

    public XMLProteinLoader(String fileName)
    {
        this(fileName, false);
    }

    public XMLProteinLoader(String fileName, boolean clearExisting)
    {
        _clearExisting = clearExisting;
        // Declare which package our individual parsers belong
        // to.  We assume that the package is a child of the
        // current package with the loaderPrefix appended
        _parseFName = fileName;
    }

    public boolean isClearExisting()
    {
        return _clearExisting;
    }

    public void parseFile() throws SQLException, IOException, SAXException
    {
        String fName = getFile();

        try
        {
            Connection conn = ProteinManager.getSchema().getScope().beginTransaction();
            XMLProteinHandler handler = new XMLProteinHandler(conn, this);
            handler.parse(fName);
            conn.setAutoCommit(false);
            ProteinManager.getSchema().getScope().commitTransaction();
        }
        finally
        {
            ProteinManager.getSchema().getScope().closeConnection();
        }
    }

    private String getFile() throws IOException
    {
        String fName = getParseFName();
        if (fName == null)
        {
            throw new FileNotFoundException("No file name specified");
        }
        File f = new File(fName);
        if (!NetworkDrive.exists(f))
        {
            throw new FileNotFoundException("Can't open file '" + fName + "'");
        }
        if (!f.isFile())
        {
            throw new FileNotFoundException("Can't open file '" + fName + "'");
        }
        return fName;
    }

    //
    // ContentHandler methods
    //
    public void cleanUp()
    {
    }
} // class XMLProteinLoader

