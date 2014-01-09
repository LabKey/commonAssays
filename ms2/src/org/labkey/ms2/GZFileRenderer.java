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

package org.labkey.ms2;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.labkey.api.util.NetworkDrive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.zip.GZIPInputStream;

/**
 * User: arauch
 * Date: Jan 25, 2005
 * Time: 10:15:15 AM
 */
public abstract class GZFileRenderer
{
    private String _gzFileName;
    private String _lastErrorMessage = null;

    public GZFileRenderer()
    {
    }


    public String getGZFileName()
    {
        return _gzFileName;
    }


    public void setGZFileName(String gzFileName)
    {
        _gzFileName = gzFileName;
    }


    public boolean render(PrintWriter out) throws IOException
    {
        File gzFile = new File(getGZFileName());

        if (!gzFile.exists())
            NetworkDrive.ensureDrive(gzFile.getPath());

        if (!gzFile.exists())
        {
            _lastErrorMessage = "GZ file not available.";
            return false;
        }

        try (TarArchiveInputStream tis = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(gzFile))))
        {
            TarArchiveEntry te = tis.getNextTarEntry();

            while (null != te)
            {
                // some archives prepend "./" (e.g "./foo.2170.2170.3.out")
                String filename = te.getName();
                if (filename.startsWith("./"))
                    filename = filename.substring(2);

                if (isSearchFile(filename))
                {
                    renderFile(out, te.getName(), tis);
                    return true;
                }

                te = tis.getNextTarEntry();
            }
        }

        _lastErrorMessage = "Not found.";
        return false;
    }


    protected abstract boolean isSearchFile(String filename);

    protected void renderFile(PrintWriter out, String fileName, TarArchiveInputStream tis) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(tis));
        String line;

        while ((line = br.readLine()) != null)
            out.println(line);

        out.println();
    }


    public String getLastErrorMessage()
    {
        return _lastErrorMessage;
    }
}
