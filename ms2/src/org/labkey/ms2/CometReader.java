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

package org.labkey.ms2;

import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.util.NetworkDrive;
import org.labkey.common.tools.SimpleXMLStreamReader;
import org.labkey.api.exp.XarContext;
import org.apache.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


public class CometReader extends CometImporter
{
    // UNDONE: More patterns
    private static Pattern ionPercentPattern = Pattern.compile("100%|[1-9]?[0-9]%");
    private static final String validAminoAcids = "ABCDEFGHIKLMNOPQRSTUVWXYZ";
    private static Pattern peptidePattern = Pattern.compile("-|[" + validAminoAcids + "-]\\.([" + validAminoAcids + "]['\"~]?)+\\.[" + validAminoAcids + "-]");

    protected String _probFileName = null;
    protected PeptideProphetMap _ppMap = null;
    protected String rawScore, diffScore, zScore;

    // Only Comet runs have an (optional) PROB file with PeptideProphet scores; XML runs have this info inline
    public CometReader(User user, Container c, String description, String htmFullFileName, Logger log, XarContext context)
    {
        super(context);
        initFileUpload(user, c, description, htmFullFileName, log);
        _probFileName = _fileName + ".prob";
    }


    protected String getType()
    {
        return "Comet";
    }


    protected void prepareUpload() throws FileNotFoundException, XMLStreamException, IOException
    {
        File f = new File(_path + "/" + _fileName);

        if (NetworkDrive.exists(f))
        {
            _fIn = new FileInputStream(f);
            parser = new SimpleXMLStreamReader(_fIn);
        }
        else
            throw new FileNotFoundException(_path + "/" + _fileName);

        _scanCount = 0;

        _ppMap = new PeptideProphetMap(_path + "/" + _probFileName);  // TODO: NetworkDrive.exists()
    }


    protected String getTableColumnNames()
    {
        return super.getTableColumnNames() + ", Score1, Score2, Score3";
    }


    protected void prepareWrite()
    {
        prevAA = peptide.substring(0, 1);
        if ("-".equals(prevAA)) prevAA = " ";

        nextAA = peptide.substring(peptide.length() - 1, peptide.length());
        if ("-".equals(nextAA)) nextAA = " ";

        trimmedPeptide = ("-".equals(peptide) ? " " : MS2Peptide.stripPeptide(MS2Peptide.trimPeptide(peptide)));

        super.prepareWrite();
    }


    protected int setParameters(int n) throws SQLException
    {
        n = super.setParameters(n);

        _stmt.setFloat(n++, Float.parseFloat(rawScore));
        _stmt.setFloat(n++, Float.parseFloat(diffScore));
        _stmt.setFloat(n++, Float.parseFloat(zScore));

        return n;
    }


    // UNDONE: Tally warnings and errors, display to user after upload?  Abort upload and delete data if too many.
    public void uploadRun() throws FileNotFoundException, XMLStreamException, SQLException, IOException
    {
        String currentGzFileName;
        String dtaFileNamePrefix = null;
        Set<Integer> scans = null;

        prepareUpload();

        CometDefReader cdr = new CometDefReader(_user, _runId, _path, _log, _context);
        cdr.upload();

        prepareStatement();

        try
        {
            while (parser.skipToStart("PRE"))
            {
                String firstChunk, nextChunk, chargeFromHTML;

                firstChunk = parser.getAllText();

                // UNDONE: validate the tokenArray tokens
                String[] tokenArray = (firstChunk == null ? null : firstChunk.split("\\s+"));

                if (tokenArray == null || tokenArray.length < 3)
                {
                    _log.info("EOF? " + firstChunk);
                    break;
                }

                chargeFromHTML = tokenArray[0];

                if (chargeFromHTML.length() > 0)
                    chargeFromHTML = chargeFromHTML.substring(0, chargeFromHTML.length() - 1); // Chuck the plus sign
                else
                    logError("Bad charge: " + (_scanCount + 1) + " " + chargeFromHTML);

                rawScore = tokenArray[1];
                diffScore = tokenArray[2];
                zScore = tokenArray[3];

                // UNDONE: StringBuffer?  Deal with "100%" better
                ionPercent = parser.getAllText(ionPercentPattern);
                if (ionPercent.length() < 3)
                    ionPercent = (".0" + ionPercent).substring(0, 3);
                else
                {
                    if (ionPercent.length() < 4)
                        ionPercent = ("." + ionPercent).substring(0, 3);
                    else
                        ionPercent = "1";
                }

                // UNDONE: Finish the regular expressions
                nextChunk = parser.getAllText();     // Depending on version of HTML file, next token could be just mass OR mass then deltaMass
                tokenArray = nextChunk.split("\\s+");

                mass = tokenArray[0];

                if (1 < tokenArray.length)
                    deltaMass = tokenArray[1];
                else
                    deltaMass = "0";

                peptide = parser.getAllText(peptidePattern);
                parser.skipBlank();
                proteinHits = parser.getAllText();
                parser.skipBlank();
                protein = parser.getAllText();
                currentGzFileName = getGzFileName();
                dtaFileName = parser.getAllText();

                // If this is the first time through OR the file name listed for this peptide is new then create a new Fraction entry
                if (0 == _fractionId || !currentGzFileName.equals(gzFileName))
                {
                    // Upload the GZ file
                    if (0 != _fractionId)
                    {
                        SpectrumLoader sl = new SpectrumLoader(_path + "/" + gzFileName, dtaFileNamePrefix, null, scans, null, _fractionId, _log, false);
                        sl.upload();
                        updateFractionSpectrumFileName(sl.getFile());
                    }

                    gzFileName = currentGzFileName;

                    _fractionId = createFraction(_user, _container, _runId, _path, null);
                    scans = new HashSet<Integer>(1000);

                    int index = nthLastIndexOf(dtaFileName, ".", 3);

                    if (-1 != index)
                        dtaFileNamePrefix = dtaFileName.substring(0, index);
                    else
                        logError("uploadRun: " + dtaFileNamePrefix + " doesn't equal prefix of " + dtaFileName + " in row " + (_scanCount + 1));
                }

                if (dtaFileName.startsWith(dtaFileNamePrefix))
                {
                    int scanStart, scanEnd, chargeStart, chargeEnd, intCharge, intScan;
                    long scanCharge;

                    // Extract scan and charge from fileName
                    scanStart = nthLastIndexOf(dtaFileName, ".", 3) + 1;
                    scanEnd = dtaFileName.indexOf(".", scanStart + 1);
                    chargeStart = dtaFileName.lastIndexOf(".") + 1;
                    chargeEnd = dtaFileName.length();

                    scan = dtaFileName.substring(scanStart, scanEnd);
                    charge = dtaFileName.substring(chargeStart, chargeEnd);

                    intScan = Integer.parseInt(scan);
                    intCharge = Integer.parseInt(charge);

                    scanCharge = (((long) intScan) << 32) + intCharge;

                    // Record this scan number so SpectrumLoader will upload the corresponding spectrum
                    scans.add(new Integer(scan));

                    // Look up the peptide prophet score
                    Float pp = _ppMap.get(dtaFileNamePrefix, scanCharge);

                    // UNDONE: get rid of the next 9 lines... null check in verify should take care of this (maybe keep else to log error?)
                    peptideProphet = (null == pp) ? "0" : pp.toString();
                }
                else
                {
                    // UNDONE: Log error? Throw Exception?
                    scan = "0";
                    charge = "0";
                    peptideProphet = "0";
                }

                write();
            }
        }
        catch (XMLStreamException e)
        {
            // We expect end-of-file exception because Comet HTML files don't have an end tag
            // Log error if it's something else
            if (!(e.getNestedException() instanceof EOFException))
            {
                throw(e);
            }
        }

        // Upload last fraction GZ file OR upload GZ from stream
        SpectrumLoader sl = new SpectrumLoader(_path + "/" + gzFileName, dtaFileNamePrefix, null, scans, null, _fractionId, _log, false);
        sl.upload();
        updateFractionSpectrumFileName(sl.getFile());
    }


    private String getGzFileName() throws XMLStreamException
    {
        String href = parser.getHref();
        int start = href.indexOf("TarFile=");

        if (-1 != start)
        {
            start += 8;
            int end = href.indexOf("&", start);

            if (-1 != end)
            {
                String fullFileName = href.substring(start, end);
                int slash = fullFileName.lastIndexOf('/');

                if (-1 != slash)
                    return fullFileName.substring(slash + 1);
            }
        }

        return null;
    }


    private class PeptideProphetMap
    {
        Map<Long, Float> _probMap = null;
        Map<String, Short> _fileNameMap = null;

        PeptideProphetMap(String probFileName) throws IOException
        {
            try
            {
                File f = new File(probFileName);

                if (NetworkDrive.exists(f))
                {
                    FileInputStream probStream = new FileInputStream(f);
                    init(probStream);
                }
            }
            catch (FileNotFoundException e)
            {
                _log.warn(e);
            }
        }


        PeptideProphetMap(InputStream probStream) throws IOException
        {
            init(probStream);
        }


        private void init(InputStream probStream) throws IOException
        {
            long startTime;
            int firstTab, secondTab, scanStart, scanEnd, chargeStart, chargeEnd, scan;
            short currentIndex, charge;
            Float pp;
            String fileName, name;
            Short index;

            startTime = System.currentTimeMillis();
            _log.info("Starting to create peptide prophet map");
            BufferedReader probReader = new BufferedReader(new InputStreamReader(probStream));
            _probMap = new HashMap<Long, Float>(1000);
            _fileNameMap = new HashMap<String, Short>(10);
            currentIndex = 0;

            while (true)
            {
                String line = probReader.readLine();
                if (null == line) break;
                firstTab = line.indexOf('\t');
                fileName = line.substring(0, firstTab);  // Chuck extra .1, .2, or .3

                // Parse fileName portion, find (or create) fileName index in map
                scanStart = nthLastIndexOf(fileName, ".", 4) + 1;
                name = fileName.substring(0, scanStart - 1);

                index = _fileNameMap.get(name);

                if (null == index)
                {
                    currentIndex++;
                    index = currentIndex;
                    _fileNameMap.put(name, index);
                }

                // Extract scan and charge from fileName
                scanEnd = fileName.indexOf(".", scanStart + 1);
                chargeStart = nthLastIndexOf(fileName, ".", 2) + 1;
                chargeEnd = fileName.indexOf(".", chargeStart + 1);

                scan = Integer.valueOf(fileName.substring(scanStart, scanEnd));
                charge = Short.valueOf(fileName.substring(chargeStart, chargeEnd));

                // Encode scan (32 bits), fileNameIndex (16 bits), and charge (16 bits) into a long
                Long encode = (((long) scan) << 32) + ((long) currentIndex << 16) + charge;

                secondTab = line.indexOf('\t', firstTab + 1);
                pp = Float.parseFloat(line.substring(firstTab + 1, secondTab));
                _probMap.put(encode, pp);
            }

            probReader.close();
            logElapsedTime(startTime, "create peptide prophet map");
        }


        Float get(String fileName, long scanCharge)
        {
            Short index = null;

            if (null != _fileNameMap)
                index = _fileNameMap.get(fileName);

            if (null == index)
                return 0f;

            long encode = scanCharge | (long) index.shortValue() << 16;

            return _probMap.get(encode);
        }
    }
}
