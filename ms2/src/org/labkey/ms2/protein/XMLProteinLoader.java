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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Based loosely on XERCES'
 * sample SAX2 counter.
 */

public class XMLProteinLoader extends DefaultAnnotationLoader implements AnnotationLoader
{
    protected String parseFName;

    public String getParseFName()
    {
        return parseFName;
    }

    public void setParseFName(String parseFName)
    {
        this.parseFName = parseFName;
    }

    protected InputSource parseSource = null;

    public InputSource getParseSource()
    {
        return parseSource;
    }

    public void setParseSource(InputSource parseSource)
    {
        this.parseSource = parseSource;
    }

    protected String comment = null;

    public void setComment(String c)
    {
        this.comment = c;
    }

    public String getComment()
    {
        return comment;
    }

    public int getId()
    {
        return 0;
    }

    private Connection fetchAConnection() throws SQLException
    {
        return ProteinManager.getSchema().getScope().getConnection();
    }

    public XMLProteinLoader(InputStream is)
    {
        setParseSource(new InputSource(is));
        setParseFName(getParseSource().getSystemId());
    }

    public XMLProteinLoader(String fileName)
    {
        // Declare which package our individual parsers belong
        // to.  We assume that the package is a child of the
        // current package with the loaderPrefix appended
        setParseFName(fileName);
    }

    public void parseFile() throws SQLException, IOException, SAXException
    {
        String fName = getParseFName();
        File f = new File(fName);
        if (!f.exists())
        {
            NetworkDrive.ensureDrive(f.getPath());
            if (!f.exists())
            {
                throw(new SAXException("Can't open file '" + fName + "'"));
            }
        }

        Connection conn = null;
        try
        {
            conn = this.fetchAConnection();
            XMLProteinHandler handler = new XMLProteinHandler(conn, this);
            handler.parse(fName);
        }
        finally
        {
            if (conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    public void parseInputSource(InputSource is) throws SQLException, IOException, SAXException
    {
        Connection conn = null;
        try
        {
            conn = this.fetchAConnection();
            XMLProteinHandler handler = new XMLProteinHandler(conn, this);
            handler.parse(is);
        }
        finally
        {
            if (conn != null) try
            {
                conn.close();
            }
            catch (SQLException e)
            {
            }
        }
    }

    public void parseInputSource() throws SQLException, IOException, SAXException
    {
        parseInputSource(getParseSource());
    }



    //
    // ContentHandler methods
    //
    public void cleanUp()
    {
    }

    public void parseInBackground()
    {
        AnnotationUploadManager.getInstance().enqueueAnnot(this);
    }

    public void parseInBackground(int recoveryId)
    {
        setRecoveryId(recoveryId);
        AnnotationUploadManager.getInstance().enqueueAnnot(this);
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

