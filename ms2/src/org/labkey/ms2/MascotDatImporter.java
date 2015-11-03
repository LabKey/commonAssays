/*
 * Copyright (c) 2007-2015 LabKey Corporation
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

import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.security.User;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.reader.MS2Loader;
import org.labkey.ms2.reader.MascotDatLoader;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * User: adam
 * Date: Jul 23, 2007
 * Time: 11:06:32 AM
 */
public class MascotDatImporter extends PeptideImporter
{
    Map<Integer, MascotDatLoader.DatPeptide> _peptides = new HashMap<>();


    public MascotDatImporter(User user, Container c, String description, String fullFileName, Logger log, XarContext context)
    {
        super(user, c, description, fullFileName, log, context);
    }

    @Override
    public String getType()
    {
        return "Mascot";
    }

    @Override
    public void importRun(MS2Progress progress) throws IOException, XMLStreamException
    {

        File f = new File(_path + "/" + _fileName);
        NetworkDrive.ensureDrive(f.getPath());
        MascotDatLoader loader = new MascotDatLoader(f, _log);
        _fractionId = createFraction(_user, _container, _runId, _path, f);
        MS2Loader.PeptideFraction fraction = new MS2Loader.PeptideFraction();
        fraction.setSpectrumPath(f.getPath());
        progress.setMs2FileInfo(loader.getFileLength(), loader.getCharactersRead()*2);
        try
        {
            while (loader.findSection())
            {
                MascotDatLoader.Section section = loader.getCurrentSection();
                switch (section)
                {
                    case HEADER:
                    {
                        _log.info("Loading header");
                        loader.loadHeader(fraction, _container);
                        if (loader.isLoaded(MascotDatLoader.Section.PARAMETERS) && loader.isLoaded(MascotDatLoader.Section.MASSES))
                            writeRunInfo(fraction, progress);

                        break;
                    }
                    case PARAMETERS:
                    {
                        _log.info("Loading parameters");
                        loader.loadParameters(fraction, _container);
                        if (loader.isLoaded(MascotDatLoader.Section.HEADER) && loader.isLoaded(MascotDatLoader.Section.MASSES))
                            writeRunInfo(fraction, progress);
                        break;
                    }
                    case MASSES:
                    {
                        _log.info("Loading masses");
                        loader.loadMasses(fraction);
                        if (loader.isLoaded(MascotDatLoader.Section.HEADER) && loader.isLoaded(MascotDatLoader.Section.PARAMETERS))
                            writeRunInfo(fraction, progress);
                        break;
                    }
                    case PEPTIDES:
                    {
                        _log.info("Loading peptides");
                        if (!loader.isLoaded(MascotDatLoader.Section.PARAMETERS) || !loader.isLoaded(MascotDatLoader.Section.HEADER))
                            _log.error("Peptides section encountered before parameters and header; no run information available.");
                        progress.getCumulativeTimer().setCurrentTask(Tasks.ImportPeptides, " from file " + fraction.getSpectrumPath());
                        progress.setPeptideMode();
                        loader.loadPeptides(_peptides, fraction);
                        progress.setCurrentMs2FileOffset(loader.getCharactersRead()*2);
                        break;
                    }
                    case SUMMARY:
                    {
                        _log.info("Loading summary");
                        loader.loadSummary(_peptides);
                        progress.setCurrentMs2FileOffset(loader.getCharactersRead()*2);
                        break;
                    }
                    case QUERY:
                    {
                        if (loader.getCurrentQueryNum() == 1 )
                            _log.info("Loading query data");
                        loader.loadQuery(_peptides);
                        progress.setCurrentMs2FileOffset(loader.getCharactersRead()*2);
                        break;
                    }
                }
            }
            progress.setImportSpectra(false);
            progress.setCurrentMs2FileOffset(loader.getFileLength());
            writePeptides();
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
        finally
        {
            if (null != loader)
                loader.close();
        }
    }

    private void writePeptides() throws SQLException
    {
        _log.info("Writing peptides data");
        for (MS2Loader.Peptide peptide : _peptides.values())
        {
            // There can be some data for peptides that were not found(?).  We skip over those.
            if (peptide.getTrimmedPeptide() != null)
            {
                peptide.setDerivedFieldValues();
                write(peptide, null);
            }
        }

    }
}
