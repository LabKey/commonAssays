/*
 * Copyright (c) 2006-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.data.Table;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.XarContext;
import org.labkey.api.query.AliasManager;
import org.labkey.api.reader.SimpleXMLStreamReader;
import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.ms2.pipeline.TPPTask;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.fasta.Protein;
import org.labkey.ms2.reader.ITraqProteinQuantitation;
import org.labkey.ms2.reader.ProtXmlReader;
import org.labkey.ms2.reader.ProteinGroup;
import org.labkey.api.util.*;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * User: jeckels
 * Date: Mar 7, 2006
 */
public class ProteinProphetImporter
{
    private static final Logger _log = Logger.getLogger(ProteinProphetImporter.class);

    private final File _file;
    private final String _experimentRunLSID;
    private final XarContext _context;
    private final SqlDialect _dialect = MS2Manager.getSchema().getSqlDialect();

    private static final int STREAM_BUFFER_SIZE = 128 * 1024;

    public ProteinProphetImporter(File f, String experimentRunLSID, XarContext context)
    {
        _file = f;
        _experimentRunLSID = experimentRunLSID;
        _context = context;
    }

    public MS2Run importFile(ViewBackgroundInfo info, Logger log) throws SQLException, XMLStreamException, IOException, ExperimentException
    {
        long startTime = System.currentTimeMillis();
        log.info("Starting to load ProteinProphet file " + _file.getPath());

        if (!NetworkDrive.exists(_file))
        {
            throw new FileNotFoundException(_file.toString());
        }

        MS2Run run = findExistingRun(log, info.getContainer());
        if (run != null)
        {
            return run;
        }

        run = importRun(info, log);

        if (run == null)
        {
            throw new ExperimentException("Failed to import MS2 run " + getPepXMLFileName());
        }

        ProteinProphetFile proteinProphetFile = run.getProteinProphetFile();
        if (proteinProphetFile != null)
        {
            throw new ExperimentException("MS2 run already has ProteinProphet data loaded from file " + proteinProphetFile.getFilePath());
        }

        int suffix = new Random().nextInt(1000000000);
        String peptidesTempTableName = _dialect.getTempTablePrefix() +  "PeptideMembershipsTemp" + suffix;
        String proteinsTempTableName = _dialect.getTempTablePrefix() +  "ProteinGroupMembershipsTemp" + suffix;

        Statement stmt = null;
        PreparedStatement mergePeptideStmt = null;
        PreparedStatement mergeProteinStmt = null;
        PreparedStatement peptideStmt = null;
        PreparedStatement groupStmt = null;
        PreparedStatement proteinStmt = null;
        PreparedStatement peptideIndexStmt = null;
        PreparedStatement proteinIndexStmt = null;
        PreparedStatement proteinIndex2Stmt = null;

        ProtXmlReader.ProteinGroupIterator iterator = null;
        boolean success = false;
        boolean createdTempTables = false;
        int proteinGroupIndex = 0;

        try (DbScope.Transaction transaction = MS2Manager.getSchema().getScope().ensureTransaction())
        {
            Connection connection = transaction.getConnection();
            try
            {
                String createPeptidesTempTableSQL =
                        "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + peptidesTempTableName + " ( " +
                                "\tTrimmedPeptide VARCHAR(200) NOT NULL,\n" +
                                "\tCharge INT NOT NULL,\n" +
                                "\tProteinGroupId INT NOT NULL,\n" +
                                "\tNSPAdjustedProbability REAL NOT NULL,\n" +
                                "\tWeight REAL NOT NULL,\n" +
                                "\tNondegenerateEvidence " + _dialect.getBooleanDataType() + " NOT NULL,\n" +
                                "\tEnzymaticTermini INT NOT NULL,\n" +
                                "\tSiblingPeptides REAL NOT NULL,\n" +
                                "\tSiblingPeptidesBin INT NOT NULL,\n" +
                                "\tInstances INT NOT NULL,\n" +
                                "\tContributingEvidence " + _dialect.getBooleanDataType() + " NOT NULL,\n" +
                                "\tCalcNeutralPepMass REAL NOT NULL" +
                                ")";

                stmt = connection.createStatement();
                stmt.execute(createPeptidesTempTableSQL);

                String createProteinsTempTableSQL =
                        "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + proteinsTempTableName + " ( " +
                                "\tProteinGroupId INT NOT NULL,\n" +
                                "\tProbability REAL NOT NULL,\n" +
                                "\tLookupString VARCHAR(200)\n" +
                                ")";

                stmt = connection.createStatement();
                stmt.execute(createProteinsTempTableSQL);

                connection.commit();
                createdTempTables = true;

                ProtXmlReader reader = new ProtXmlReader(_file, run);

                peptideStmt = connection.prepareStatement("INSERT INTO " + peptidesTempTableName + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                proteinStmt = connection.prepareStatement("INSERT INTO " + proteinsTempTableName + " (ProteinGroupId, Probability, LookupString) VALUES (?, ?, ?)");

                SQLFragment groupInsert = new SQLFragment("INSERT INTO " + MS2Manager.getTableInfoProteinGroups() + " (groupnumber, groupprobability, proteinprobability, indistinguishablecollectionid, proteinprophetfileid, uniquepeptidescount, totalnumberpeptides, pctspectrumids,  percentcoverage, errorrate) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                _dialect.addReselect(groupInsert, MS2Manager.getTableInfoProteinGroups().getColumn("RowId"), null);
                groupStmt = connection.prepareStatement(groupInsert.getSQL());

                iterator = reader.iterator();

                ProteinProphetFile file = insertProteinProphetFile(info, run, iterator.getReader());

                while (iterator.hasNext())
                {
                    proteinGroupIndex++;
                    ProteinGroup group = iterator.next();
                    float groupProbability = group.getProbability();
                    int groupNumber = group.getGroupNumber();

                    List<ProtXmlReader.Protein> proteins = group.getProteins();

                    // collectionId 0 means it's the only collection in the group
                    int collectionId = proteins.size() == 1 ? 0 : 1;
                    for (ProtXmlReader.Protein protein : proteins)
                    {
                        loadProtein(protein, groupNumber, groupProbability, collectionId++, file, info, groupStmt, peptideStmt, proteinStmt);
                    }
                    if (proteinGroupIndex % 50 == 0)
                    {
                        // Don't leave too big of a transaction pending
                        // Commit directly on the connection so that we don't lose the underlying connection
                        peptideStmt.executeBatch();
                        connection.commit();
                    }
                    if (proteinGroupIndex % 10000 == 0)
                    {
                        log.info("Loaded " + proteinGroupIndex + " protein groups...");
                    }
                }

                peptideStmt.executeBatch();

                long insertStartTime = System.currentTimeMillis();
                log.info("Starting to move data into ms2.PeptidesMemberships");

                peptideIndexStmt = connection.prepareStatement("CREATE INDEX idx_" + AliasManager.makeLegalName(peptidesTempTableName,null) + " ON " + peptidesTempTableName + "(TrimmedPeptide, Charge)");
                peptideIndexStmt.execute();

                // Move the peptide information of the temp table into the real table
                String mergePeptideSQL = "INSERT INTO " + MS2Manager.getTableInfoPeptideMemberships() + " (" +
                        "\tPeptideId, ProteinGroupId, NSPAdjustedProbability, Weight, NondegenerateEvidence,\n" +
                        "\tEnzymaticTermini, SiblingPeptides, SiblingPeptidesBin, Instances, ContributingEvidence, CalcNeutralPepMass ) \n" +
                        "\tSELECT p.RowId, t.ProteinGroupId, t.NSPAdjustedProbability, t.Weight, t.NondegenerateEvidence,\n" +
                        "\tt.EnzymaticTermini, t.SiblingPeptides, t.SiblingPeptidesBin, t.Instances, t.ContributingEvidence, t.CalcNeutralPepMass\n" +
                        "FROM " + MS2Manager.getTableInfoPeptides() + " p, " + peptidesTempTableName + " t\n" +
                        "WHERE p.TrimmedPeptide = t.TrimmedPeptide AND p.Charge = t.Charge AND p.Run = ?";

                mergePeptideStmt = connection.prepareStatement(mergePeptideSQL);
                mergePeptideStmt.setInt(1, run.getRun());
                mergePeptideStmt.executeUpdate();
                log.info("Finished with moving data into ms2.PeptidesMemberships after " + (System.currentTimeMillis() - insertStartTime) + " ms");

                insertStartTime = System.currentTimeMillis();
                log.info("Starting to move data into ms2.ProteinGroupMemberships");

                // Create an index to use for the join with prot.fastasequences
                proteinIndexStmt = connection.prepareStatement("CREATE INDEX idx_" + AliasManager.makeLegalName(proteinsTempTableName,null) + " ON " + proteinsTempTableName + "(LookupString)");
                proteinIndexStmt.execute();

                // Create an index to use for the GROUP BY
                proteinIndex2Stmt = connection.prepareStatement("CREATE INDEX idx_" + AliasManager.makeLegalName(proteinsTempTableName,null) + "2 ON " + proteinsTempTableName + "(ProteinGroupId, Probability)");
                proteinIndex2Stmt.execute();

                int[] fastaIds = run.getFastaIds();

                String mergeProteinSQL = "INSERT INTO " + MS2Manager.getTableInfoProteinGroupMemberships() + " " +
                        "(ProteinGroupId, Probability, SeqId) " +
                        "SELECT p.ProteinGroupId, p.Probability, s.SeqId " +
                        "   FROM " + ProteinManager.getTableInfoFastaSequences() + " s, " + proteinsTempTableName + " p" +
                        "   WHERE s.FastaId IN(" + StringUtils.repeat("?", ", ", fastaIds.length) + ") AND s.LookupString = p.LookupString GROUP BY p.ProteinGroupId, p.Probability, s.SeqId";

                mergeProteinStmt = connection.prepareStatement(mergeProteinSQL);
                int index = 1;
                for (int fastaId : fastaIds)
                {
                    mergeProteinStmt.setInt(index++, fastaId);
                }
                mergeProteinStmt.executeUpdate();
                log.info("Finished with moving data into ms2.ProteinGroupMemberships after " + (System.currentTimeMillis() - insertStartTime) + " ms");

                file.setUploadCompleted(true);
                Table.update(info.getUser(), MS2Manager.getTableInfoProteinProphetFiles(), file, file.getRowId());

                success = true;

                log.info("ProteinProphet file import finished successfully, " + proteinGroupIndex + " protein groups loaded");
            }
            finally
            {
                if (iterator != null)
                {
                    iterator.close();
                }
                if (!success && connection != null)
                {
                    try
                    {
                        connection.rollback();
                    }
                    catch (SQLException e) { log.error("Failed to rollback to clear any potential error state", e); }
                }
                if (stmt != null && createdTempTables)
                {
                    try
                    {
                        stmt.execute("DROP TABLE " + peptidesTempTableName);
                    }
                    catch (SQLException e) { log.error("Failed to drop temporary peptides table", e); }
                    try
                    {
                        stmt.execute("DROP TABLE " + proteinsTempTableName);
                    }
                    catch (SQLException e) { log.error("Failed to drop temporary proteins table", e); }
                    try { stmt.close(); } catch (SQLException ignored) {}
                }
                if (mergePeptideStmt != null) { try { mergePeptideStmt.close(); } catch (SQLException ignored) {} }
                if (mergeProteinStmt != null) { try { mergeProteinStmt.close(); } catch (SQLException ignored) {} }
                if (peptideStmt != null) { try { peptideStmt.close(); } catch (SQLException ignored) {} }
                if (groupStmt != null) { try { groupStmt.close(); } catch (SQLException ignored) {} }
                if (proteinStmt != null) { try { proteinStmt.close(); } catch (SQLException ignored) {} }
                if (proteinIndexStmt != null) { try { proteinIndexStmt.close(); } catch (SQLException ignored) {} }
                if (proteinIndex2Stmt != null) { try { proteinIndex2Stmt.close(); } catch (SQLException ignored) {} }
                if (peptideIndexStmt != null) { try { peptideIndexStmt.close(); } catch (SQLException ignored) {} }

                transaction.commit();
            }

            if (!success)
            {
                log.error("Failed when importing group " + proteinGroupIndex);
            }
        }
        long endTime = System.currentTimeMillis();
        log.info("ProteinProphet import took " + ((endTime - startTime) / 1000) + " seconds.");
        return run;
    }

    private MS2Run findExistingRun(Logger logger, Container c) throws IOException
    {
        ProteinProphetFile ppFile = MS2Manager.getProteinProphetFile(_file, c);
        if (ppFile != null)
        {
            if (ppFile.isUploadCompleted())
            {
                logger.info(_file.getPath() + " had already been uploaded successfully, not uploading again.");
                return MS2Manager.getRun(ppFile.getRun());
            }
            else
            {
                logger.info(_file.getPath() + " had already been partially uploaded, deleting the existing data.");
                MS2Manager.purgeProteinProphetFile(ppFile.getRowId());
            }
        }
        return null;
    }

    private ProteinProphetFile insertProteinProphetFile(ViewBackgroundInfo info, MS2Run run, SimpleXMLStreamReader parser)
            throws IOException, SQLException, XMLStreamException
    {
        ProteinProphetFile file = new ProteinProphetFile(parser);
        file.setFilePath(_file.getCanonicalPath());
        file.setRun(run.getRun());

        Table.insert(info.getUser(), MS2Manager.getTableInfoProteinProphetFiles(), file);
        return file;
    }

    private MS2Run importRun(ViewBackgroundInfo info, Logger log)
        throws IOException, XMLStreamException, SQLException, ExperimentException
    {
        String pepXMLFileNameOriginal = getPepXMLFileName();
        File pepXMLFile = null;

        List<String> attemptedFiles = new ArrayList<>();

        for (int attempts=0;attempts++<2;) // try it straight up, then try finding .gz version
        {
            // First, see if our usual XAR lookups can find it
            String pepXMLFileName = pepXMLFileNameOriginal + ((attempts>1)?".gz":"");
            attemptedFiles.add(pepXMLFileName);
            pepXMLFile = _context.findFile(pepXMLFileName, _file.getParentFile());
            if (pepXMLFile == null)
            {
                // Second, try the file name in the XML in the current directory
                pepXMLFile = new File(_file.getParentFile(), new File(pepXMLFileName).getName());
                attemptedFiles.add(pepXMLFile.getAbsolutePath());
                if (!NetworkDrive.exists(pepXMLFile))
                {
                    // Third, try replacing the .pep-prot.xml on the file name with .pep.xml
                    // and looking in the same directory
                    if (TPPTask.isProtXMLFile(_file))
                    {
                        String baseName = TPPTask.FT_PROT_XML.getBaseName(_file);
                        pepXMLFile = TPPTask.getPepXMLFile(_file.getParentFile(), baseName);
                        attemptedFiles.add(pepXMLFile.getAbsolutePath());
                        if (!NetworkDrive.exists(pepXMLFile))
                        {
                            if (attempts>1)
                            {
                                throw new FileNotFoundException("Unable to resolve pepXML file: " + pepXMLFileNameOriginal + " could not be found on disk. Additional file paths checked include: " + attemptedFiles);
                            }
                        }
                    }
                }
            }
        }

        log.info("Resolved referenced PepXML file to " + pepXMLFile.getPath());
        MS2Run run = MS2Manager.addRun(info, log, pepXMLFile, false, _context);
        if (_experimentRunLSID != null && run.getExperimentRunLSID() == null)
        {
            run.setExperimentRunLSID(_experimentRunLSID);
            MS2Manager.updateRun(run, info.getUser());
        }
        return run;
    }

    private void loadProtein(ProtXmlReader.Protein protein, int groupNumber, float groupProbability, int collectionId, ProteinProphetFile file, ViewBackgroundInfo info, PreparedStatement groupStmt, PreparedStatement peptideStatement, PreparedStatement proteinStmt)
            throws SQLException
    {
        int groupId = insertProteinGroup(protein, groupStmt, groupNumber, groupProbability, collectionId, file, info);

        insertPeptides(protein, peptideStatement, groupId);

        insertProteins(protein, proteinStmt, groupId);
    }

    private void insertProteins(ProtXmlReader.Protein protein, PreparedStatement proteinStmt, int groupId)
        throws SQLException
    {

        int proteinIndex = 1;
        proteinStmt.setInt(proteinIndex++, groupId);
        proteinStmt.setFloat(proteinIndex++, protein.getProbability());
        Protein p = new org.labkey.ms2.protein.fasta.Protein(protein.getProteinName(), new byte[0]);
        proteinStmt.setString(proteinIndex, p.getLookup());
        proteinStmt.execute();

        for (String indistinguishableProteinName : protein.getIndistinguishableProteinNames())
        {
            p = new Protein(indistinguishableProteinName, new byte[0]);
            proteinStmt.setString(proteinIndex, p.getLookup());
            proteinStmt.execute();
        }
    }

    private void insertPeptides(ProtXmlReader.Protein protein, PreparedStatement peptideStatement, int groupId)
        throws SQLException
    {
        Set<Pair<String, Integer>> insertedSequences = new HashSet<>();

        for (ProtXmlReader.Peptide pep : protein.getPeptides())
        {
            if (insertedSequences.add(new Pair<>(pep.getPeptideSequence(), pep.getCharge())))
            {
                int index = 1;
                peptideStatement.setString(index++, pep.getPeptideSequence());
                peptideStatement.setInt(index++, pep.getCharge());
                peptideStatement.setLong(index++, groupId);
                peptideStatement.setFloat(index++, pep.getNspAdjustedProbability());
                peptideStatement.setFloat(index++, pep.getWeight());
                peptideStatement.setBoolean(index++, pep.isNondegenerateEvidence());
                peptideStatement.setInt(index++, pep.getEnzymaticTermini());
                peptideStatement.setFloat(index++, pep.getSiblingPeptides());
                peptideStatement.setInt(index++, pep.getSiblingPeptidesBin());
                peptideStatement.setInt(index++, pep.getInstances());
                peptideStatement.setBoolean(index++, pep.isContributingEvidence());
                peptideStatement.setFloat(index++, pep.getCalcNeutralPepMass());

                peptideStatement.addBatch();
            }
        }
    }

    private int insertProteinGroup(ProtXmlReader.Protein protein, PreparedStatement groupStmt, int groupNumber, float groupProbability, int collectionId, ProteinProphetFile file, ViewBackgroundInfo info)
        throws SQLException
    {
        IcatProteinQuantitation icatRatio = protein.getQuantitationRatio();
        ITraqProteinQuantitation itraqRatio = protein.getITraqQuantitationRatio();

        int groupIndex = 1;
        groupStmt.setInt(groupIndex++, groupNumber);
        groupStmt.setFloat(groupIndex++, groupProbability);
        groupStmt.setFloat(groupIndex++, protein.getProbability());
        groupStmt.setInt(groupIndex++, collectionId);
        groupStmt.setInt(groupIndex++, file.getRowId());
        groupStmt.setInt(groupIndex++, protein.getUniquePeptidesCount());
        groupStmt.setInt(groupIndex++, protein.getTotalNumberPeptides());
        if (protein.getPctSpectrumIds() != null)
        {
            groupStmt.setFloat(groupIndex++, protein.getPctSpectrumIds());
        }
        else
        {
            groupStmt.setNull(groupIndex++, Types.FLOAT);
        }
        if (protein.getPercentCoverage() != null)
        {
            groupStmt.setFloat(groupIndex++, protein.getPercentCoverage());
        }
        else
        {
            groupStmt.setNull(groupIndex++, Types.FLOAT);
        }
        Float errorRate = file.calculateErrorRate(groupProbability);
        if (errorRate == null)
        {
            groupStmt.setNull(groupIndex++, Types.FLOAT);
        }
        else
        {
            groupStmt.setFloat(groupIndex++, errorRate);
        }

        ResultSet rs = _dialect.executeWithResults(groupStmt);

        int groupId;

        try
        {
            if (!rs.next())
            {
                throw new SQLException("Expected a result set with the new group's rowId");
            }
            groupId = rs.getInt(1);
        }
        finally
        {
            try { rs.close(); } catch (SQLException ignored) {}
        }

        if (icatRatio != null)
        {
            icatRatio.setProteinGroupId(groupId);
            Table.insert(info.getUser(), MS2Manager.getTableInfoProteinQuantitation(), icatRatio);
        }
        
        if (itraqRatio != null)
        {
            itraqRatio.setProteinGroupId(groupId);
            Table.insert(info.getUser(), MS2Manager.getTableInfoITraqProteinQuantitation(), itraqRatio);
        }
        return groupId;
    }

    private String getPepXMLFileName() throws FileNotFoundException, XMLStreamException
    {
        SimpleXMLStreamReader parser = null;
        InputStream fIn = null;
        try
        {
            fIn = new BufferedInputStream(PossiblyGZIPpedFileInputStreamFactory.getStream(_file), STREAM_BUFFER_SIZE);
            parser = new SimpleXMLStreamReader(fIn);
            if (parser.skipToStart("protein_summary_header"))
            {
                for (int i = 0; i < parser.getAttributeCount(); i++)
                {
                    if ("source_files".equals(parser.getAttributeLocalName(i)))
                    {
                        return parser.getAttributeValue(i);
                    }
                }
            }
        }
        finally
        {
            if (parser != null)
            {
                try
                {
                    parser.close();
                }
                catch (XMLStreamException e)
                {
                    _log.error(e);
                }
            }
            if (fIn != null)
            {
                try
                {
                    fIn.close();
                }
                catch (IOException e)
                {
                    _log.error(e);
                }
            }
        }
        throw new XMLStreamException("Could not find protein_summary_header element with attribute source_files");
    }

}
