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
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Based loosely on XERCES'
 * sample SAX2 counter.
 */

public class XMLProteinLoader extends DefaultAnnotationLoader implements AnnotationLoader
{
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
        // Declare which package our individual parsers belong
        // to.  We assume that the package is a child of the
        // current package with the loaderPrefix appended
        _parseFName = fileName;
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
        File f = new File(fName);
        if (!NetworkDrive.exists(f))
        {
            throw new IOException("Can't open file '" + fName + "'");
        }
        return fName;
    }

    //
    // ContentHandler methods
    //
    public void cleanUp()
    {
    }

    //
    // MAIN
    //
    public static void main(String argv[]) throws SQLException, IOException, SAXException
    {

        // process arguments
        if (argv.length == 2)
        {
            XMLProteinLoader loader = new XMLProteinLoader(argv[0]);
            loader.parseFile();
        }
        else
        {
            System.err.println("Usage: java XMLProteinLoader <xml-filename>");
            System.exit(1);
        }
        // set parser features
        System.out.println("Done.");
    } // main(String[])
} // class XMLProteinLoader

