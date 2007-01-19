package org.labkey.ms2;

import org.labkey.api.view.ViewBackgroundInfo;
import org.labkey.api.data.SqlDialect;
import org.labkey.api.data.Table;
import org.labkey.api.data.Container;
import org.labkey.api.util.NetworkDrive;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.common.tools.ProtXmlReader;
import org.labkey.common.tools.ProteinGroup;
import org.labkey.common.tools.SimpleXMLStreamReader;
import org.labkey.api.pipeline.PipelineJob;
import org.labkey.ms2.pipeline.MS2PipelineManager;
import org.labkey.api.exp.XarContext;
import org.labkey.api.exp.ExperimentException;
import org.apache.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

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

    public ProteinProphetImporter(File f, String experimentRunLSID, XarContext context)
    {
        _file = f;
        _experimentRunLSID = experimentRunLSID;
        _context = context;
    }

    public void importFile(PipelineJob job) throws SQLException, XMLStreamException, IOException, ExperimentException
    {
        job.getLogger().info("Starting to load ProteinProphet file " + _file.getPath());

        if (!shouldImportFile(job.getLogger(), job.getContainer()))
        {
            return;
        }

        MS2Run run = importRun(job);

        if (run == null)
        {
            job.getLogger().error("Failed to import MS2 run " + getPepXMLFileName());
            return;
        }

        SqlDialect dialect = MS2Manager.getSchema().getSqlDialect();
        String tempTableName = dialect.getTempTablePrefix() +  "PeptideMembershipsTemp" + (new Random().nextInt(1000000000));

        Connection connection = MS2Manager.getSchema().getScope().beginTransaction();

        Statement stmt = null;
        PreparedStatement mergeStmt = null;
        PreparedStatement insertStmt = null;

        ProtXmlReader.ProteinGroupIterator iterator = null;
        boolean success = false;
        int proteinGroupIndex = 0;

        try
        {
            int fastaId = run.getFastaId();

            String createTempTableSql =
                "CREATE " +  dialect.getTempTableKeyword() +  " TABLE " + tempTableName + " ( " +
                    "\tTrimmedPeptide VARCHAR(200) NOT NULL,\n" +
                    "\tCharge INT NOT NULL,\n" +
                    "\tProteinGroupId INT NOT NULL,\n" +
                    "\tNSPAdjustedProbability REAL NOT NULL,\n" +
                    "\tWeight REAL NOT NULL,\n" +
                    "\tNondegenerateEvidence " + dialect.getBooleanDatatype() + " NOT NULL,\n" +
                    "\tEnzymaticTermini INT NOT NULL,\n" +
                    "\tSiblingPeptides REAL NOT NULL,\n" +
                    "\tSiblingPeptidesBin INT NOT NULL,\n" +
                    "\tInstances INT NOT NULL,\n" +
                    "\tContributingEvidence " + dialect.getBooleanDatatype() + " NOT NULL,\n" +
                    "\tCalcNeutralPepMass REAL NOT NULL" +
                    ")";

            stmt = connection.createStatement();
            stmt.execute(createTempTableSql);

            if (!NetworkDrive.exists(_file))
            {
                throw new FileNotFoundException(_file.toString());
            }
            ProtXmlReader reader = new ProtXmlReader(_file);

            insertStmt = connection.prepareStatement("INSERT INTO " + tempTableName + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

            iterator = reader.iterator();

            ProteinProphetFile file = insertProteinProphetFile(job.getInfo(), run, iterator.getReader());

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
                    loadProtein(protein, groupNumber, groupProbability, collectionId++, file, fastaId, job.getInfo(), insertStmt);
                }
                if (proteinGroupIndex % 50 == 0)
                {
                    // Don't leave too big of a transaction pending
                    // Commit directly on the connection so that we don't lose the underlying connection
                    insertStmt.executeBatch();
                    connection.commit();
                    job.getLogger().info("Loaded " + proteinGroupIndex + " protein groups...");
                }
            }

            insertStmt.executeBatch();

            // Move the peptide information of the temp table into the real table
            String mergeSQL = "INSERT INTO " + MS2Manager.getTableInfoPeptideMemberships() + " (" +
                    "\tPeptideId, ProteinGroupId, NSPAdjustedProbability, Weight, NondegenerateEvidence,\n" +
                    "\tEnzymaticTermini, SiblingPeptides, SiblingPeptidesBin, Instances, ContributingEvidence, CalcNeutralPepMass ) \n" +
                    "\tSELECT p.RowId, t.ProteinGroupId, t.NSPAdjustedProbability, t.Weight, t.NondegenerateEvidence,\n" +
                    "\tt.EnzymaticTermini, t.SiblingPeptides, t.SiblingPeptidesBin, t.Instances, t.ContributingEvidence, t.CalcNeutralPepMass\n" +
                    "FROM " + MS2Manager.getTableInfoPeptides() + " p, " + tempTableName + " t\n" +
                    "WHERE p.TrimmedPeptide = t.TrimmedPeptide AND p.Charge = t.Charge AND p.Run = ?";

            mergeStmt = connection.prepareStatement(mergeSQL);
            mergeStmt.setInt(1, run.getRun());
            mergeStmt.executeUpdate();

            file.setUploadCompleted(true);
            Table.update(job.getInfo().getUser(), MS2Manager.getTableInfoProteinProphetFiles(), file, file.getRowId(), null);
            success = true;
            connection.commit();

            job.getLogger().info("ProteinProphet file import finished successfully, " + proteinGroupIndex + " protein groups loaded");
        }
        finally
        {
            if (iterator != null)
            {
                iterator.close();
            }
            if (connection != null)
            {
                try
                {
                    connection.rollback();
                }
                catch (SQLException e) { job.getLogger().error("Failed to rollback to clear any potential error state", e); }
            }
            if (stmt != null)
            {
                try
                {
                    stmt.execute("DROP TABLE " + tempTableName);
                }
                catch (SQLException e) { job.getLogger().error("Failed to drop temporary table", e); }
                try { stmt.close(); } catch (SQLException e) {}
            }
            if (mergeStmt != null) { try { mergeStmt.close(); } catch (SQLException e) {} }
            if (insertStmt != null) { try { insertStmt.close(); } catch (SQLException e) {} }
            MS2Manager.getSchema().getScope().closeConnection();

            if (!success)
            {
                job.getLogger().error("Failed when importing group " + proteinGroupIndex);
            }
        }
    }

    private boolean shouldImportFile(Logger logger, Container c) throws SQLException, IOException
    {
        ProteinProphetFile ppFile = MS2Manager.getProteinProphetFile(_file, c);
        if (ppFile != null)
        {
            if (ppFile.isUploadCompleted())
            {
                logger.info(_file.getPath() + " had already been uploaded successfully, not uploading again.");
                return false;
            }
            else
            {
                logger.info(_file.getPath() + " had already been partially uploaded, deleting the existing data.");
                MS2Manager.purgeProteinProphetFile(ppFile.getRowId());
            }
        }
        return true;
    }

    private ProteinProphetFile insertProteinProphetFile(ViewBackgroundInfo info, MS2Run run, SimpleXMLStreamReader parser)
            throws IOException, SQLException, XMLStreamException
    {
        ProteinProphetFile file = new ProteinProphetFile(parser);
        file.setFilePath(_file.getCanonicalPath());
        file.setContainer(info.getContainer().getId());
        file.setRun(run.getRun());

        Table.insert(info.getUser(), MS2Manager.getTableInfoProteinProphetFiles(), file);
        return file;
    }

    private MS2Run importRun(PipelineJob job)
        throws IOException, XMLStreamException, SQLException, ExperimentException
    {
        String pepXMLFileName = getPepXMLFileName();
        // First, see if our usual XAR lookups can find it
        File pepXMLFile = _context.findFile(pepXMLFileName, _file.getParentFile());
        if (pepXMLFile == null)
        {
            // Second, try the file name in the XML in the current directory
            pepXMLFile = new File(_file.getParentFile(), new File(pepXMLFileName).getName());
            if (!NetworkDrive.exists(pepXMLFile))
            {
                // Third, try replacing the .pep-prot.xml on the file name with .pep.xml
                // and looking in the same directory
                if (MS2PipelineManager.isProtXMLFile(_file))
                {
                    String baseName = MS2PipelineManager.getBasename(_file);
                    pepXMLFile = MS2PipelineManager.getPepXMLFile(_file.getParentFile(), baseName);
                    if (!NetworkDrive.exists(pepXMLFile))
                    {
                        throw new FileNotFoundException(pepXMLFileName + " could not be found on disk.");
                    }
                }
            }
        }

        job.getLogger().info("Resolved referenced PepXML file to " + pepXMLFile.getPath());
        int runId = MS2Manager.addRun(job, pepXMLFile, false, _context);
        MS2Run run = MS2Manager.getRun(runId);
        if (_experimentRunLSID != null && run.getExperimentRunLSID() == null)
        {
            run.setExperimentRunLSID(_experimentRunLSID);
            MS2Manager.updateRun(run, job.getUser());
        }
        return run;
    }

    private void loadProtein(ProtXmlReader.Protein protein, int groupNumber, float groupProbability, int collectionId, ProteinProphetFile file, int fastaId, ViewBackgroundInfo info, PreparedStatement stmt)
            throws SQLException
    {
        ProteinGroup insertGroup = new ProteinGroup();
        insertGroup.setGroupNumber(groupNumber);
        insertGroup.setProbability(groupProbability);
        insertGroup.setProteinProbability(protein.getProbability());
        insertGroup.setIndistinguishableCollectionId(collectionId);
        insertGroup.setProteinProphetFileId(file.getRowId());
        insertGroup.setPctSpectrumIds(protein.getPctSpectrumIds());
        insertGroup.setUniquePeptidesCount(protein.getUniquePeptidesCount());
        insertGroup.setTotalNumberPeptides(protein.getTotalNumberPeptides());
        insertGroup.setPercentCoverage(protein.getPercentCoverage());
        insertGroup.setErrorRate(file.calculateErrorRate(groupProbability));
        Table.insert(info.getUser(), MS2Manager.getTableInfoProteinGroups(), insertGroup);

        ProtXmlReader.QuantitationRatio xpressRatio = protein.getQuantitationRatio();
        if (xpressRatio != null)
        {
            xpressRatio.setProteinGroupId(insertGroup.getRowId());
            Table.insert(info.getUser(), MS2Manager.getTableInfoProteinQuantitation(), xpressRatio);
        }

        Set<String> insertedSequences = new HashSet<String>();

        for (ProtXmlReader.Peptide pep : protein.getPeptides())
        {
            if (insertedSequences.add(pep.getPeptideSequence()))
            {
                int index = 1;
                stmt.setString(index++, pep.getPeptideSequence());
                stmt.setInt(index++, pep.getCharge());
                stmt.setLong(index++, insertGroup.getRowId());
                stmt.setFloat(index++, pep.getNspAdjustedProbability());
                stmt.setFloat(index++, pep.getWeight());
                stmt.setBoolean(index++, pep.isNondegenerateEvidence());
                stmt.setInt(index++, pep.getEnzymaticTermini());
                stmt.setFloat(index++, pep.getSiblingPeptides());
                stmt.setInt(index++, pep.getSiblingPeptidesBin());
                stmt.setInt(index++, pep.getInstances());
                stmt.setBoolean(index++, pep.isContributingEvidence());
                stmt.setFloat(index++, pep.getCalcNeutralPepMass());

                stmt.addBatch();
            }
        }

        List<Object> proteinParams = new ArrayList<Object>();
        proteinParams.add(insertGroup.getRowId());
        proteinParams.add(protein.getProbability());
        proteinParams.add(fastaId);
        proteinParams.add(protein.getProteinName());
        for (String indistinguishableProteinName : protein.getIndistinguishableProteinNames())
        {
            proteinParams.add(indistinguishableProteinName);
        }
        Table.execute(MS2Manager.getSchema(), "INSERT INTO " + MS2Manager.getTableInfoProteinGroupMemberships() + " (ProteinGroupId, Probability, SeqId) SELECT ?, ?, SeqId FROM " + ProteinManager.getTableInfoFastaSequences() + " WHERE FastaId = ? AND LookupString IN (" + getInClause(protein.getIndistinguishableProteinNames().size() + 1) + ") GROUP BY SeqId", proteinParams.toArray());
    }


    private String getInClause(int length)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("?");
        for (int i = 1; i < length; i++)
        {
            sb.append(", ?");
        }
        return sb.toString();
    }

    private String getPepXMLFileName() throws FileNotFoundException, XMLStreamException
    {
        BeanXMLStreamReader parser = null;
        FileInputStream fIn = null;
        try
        {
            fIn = new FileInputStream(_file);
            parser = new BeanXMLStreamReader(fIn);
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
