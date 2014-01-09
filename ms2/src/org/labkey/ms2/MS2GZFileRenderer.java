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

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * User: arauch
 * Date: Jan 25, 2005
 * Time: 10:50:07 AM
 */
public class MS2GZFileRenderer extends GZFileRenderer
{
    private String _searchFilename;
    private String _oldFormatSearchFilename;

    public MS2GZFileRenderer(MS2Peptide peptide, String extension)
    {
        super();
        MS2Fraction fraction = MS2Manager.getFraction(peptide.getFraction());
        MS2Run run = MS2Manager.getRun(peptide.getRun());
        setGZFileName(run.getPath() + "/" + fraction.getFileName());
        _searchFilename = getFileNameInGZFile(fraction, peptide.getScan(), peptide.getCharge(), extension);
        _oldFormatSearchFilename = getOldFormatFileNameInGZFile(fraction, peptide.getScan(), peptide.getCharge(), extension);
    }


    @Override
    protected boolean isSearchFile(String filename)
    {
        return (_searchFilename.equalsIgnoreCase(filename) || _oldFormatSearchFilename.equalsIgnoreCase(filename));
    }


    @Override
    protected void renderFile(PrintWriter out, String fileName, TarArchiveInputStream tis) throws IOException
    {
        renderFileHeader(out, fileName);
        super.renderFile(out, fileName, tis);
    }


    public static void renderFileHeader(PrintWriter out, String fileName)
    {
        out.println(fileName);
        out.println(StringUtils.repeat("=", fileName.length()));
    }


    public static String getFileNameInGZFile(MS2Fraction fraction, int scan, int charge, String extension)
    {
        return getFileNameInGZFile((null == fraction ? "unknown" : fraction.getFileName()), scan, charge, extension, 5);
    }


    public static String getOldFormatFileNameInGZFile(MS2Fraction fraction, int scan, int charge, String extension)
    {
        return getFileNameInGZFile((null == fraction ? "unknown" : fraction.getFileName()), scan, charge, extension, 4);
    }


    public static String getFileNameInGZFile(String gzFileName, int scan, int charge, String extension)
    {
        return getFileNameInGZFile(gzFileName, scan, charge, extension, 5);
    }


    private static String getFileNameInGZFile(String gzFileName, int scan, int charge, String extension, int digits)
    {
        gzFileName = (null == gzFileName ? "unknown" : gzFileName);

        String scanString = String.valueOf(scan);
        int len = scanString.length();

        if (len <= digits)
            scanString = (StringUtils.repeat("0", digits) + scanString).substring(len, len + digits);

        int index = gzFileName.indexOf(".");

        if (-1 == index)
            index = gzFileName.length();

        return gzFileName.substring(0, index) + "." + scanString + "." + scanString + "." + charge + "." + extension;
    }
}
