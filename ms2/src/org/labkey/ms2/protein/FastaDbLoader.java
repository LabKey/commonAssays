/*
 * Copyright (c) 2005-2010 Fred Hutchinson Cancer Research Center
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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.labkey.api.data.Table;
import org.labkey.api.exp.XarContext;
import org.labkey.api.util.HashHelpers;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.ms2.protein.fasta.IdPattern;
import org.labkey.ms2.protein.fasta.Protein;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.fasta.*;
import org.labkey.ms2.protein.organism.GuessOrgByParsing;
import org.labkey.ms2.protein.organism.GuessOrgBySharedIdents;
import org.labkey.ms2.protein.organism.OrganismGuessStrategy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

public class FastaDbLoader extends DefaultAnnotationLoader implements AnnotationLoader
{
    private Connection conn = null;
    protected int currentInsertId = 0;
    protected String defaultOrganism = null;
    protected int associatedFastaId = 0;
    private FastaDbHelper fdbu = null;
    private boolean organismToBeGuessed;
    private static final Object LOCK = new Object();
    protected int mouthfulSize = 20000;
    /* number of fasta entries to skip when re-starting a load */
    protected int skipEntries = 0;
    private String _fileHash;
    public static final String UNKNOWN_ORGANISM = "Unknown unknown";

    private OrganismGuessStrategy _parsingStrategy;
    private OrganismGuessStrategy _sharedIdentsStrategy;

    public FastaDbLoader(File file, String hash, Logger log) throws IOException
    {
        this(file, hash);
        _log = log;
    }

    public FastaDbLoader(File file, String hash) throws IOException
    {
        this(file);
        _fileHash = hash;
    }

    public FastaDbLoader(File file) throws IOException
    {
        // Declare which package our individual parsers belong
        // to.  We assume that the package is a child of the
        // current package with the loaderPrefix appended
        _parseFName = getCanonicalPath(file);

        _parsingStrategy = new GuessOrgByParsing();
        _sharedIdentsStrategy = new GuessOrgBySharedIdents();
    }

    public int getId()
    {
        return currentInsertId;
    }

    public void validate() throws IOException
    {
        if (_parseFName == null)
        {
            throw new FileNotFoundException("No file name specified");
        }
        File file = new File(_parseFName);
        if (!NetworkDrive.exists(file) || !file.isFile())
        {
            throw new FileNotFoundException("File " + _parseFName + " does not exist.");
        }
    }

    public void setDefaultOrganism(String o)
    {
        this.defaultOrganism = o;
    }

    public void setAssociatedFastaId(int associatedFastaId)
    {
        this.associatedFastaId = associatedFastaId;
    }

    public void setOrganismIsToGuessed(boolean organismIsToBeGuessed)
    {
        this.organismToBeGuessed = organismIsToBeGuessed;
    }

    public boolean isFileAvailable() throws SQLException
    {
        File f = new File(getParseFName());

        if (!f.exists())
            NetworkDrive.ensureDrive(f.getPath());

        // Can't access file... set loaded date to null
        return f.exists();
    }

    public void parseFile() throws SQLException, IOException
    {
        if (_fileHash == null)
        {
            _fileHash = HashHelpers.hashFileContents(getParseFName());
        }

        synchronized (LOCK)
        {
            FastaFile file = new FastaFile();
            file.setFilename(getParseFName());
            file.setFileChecksum(_fileHash);
            file = Table.insert(null, ProteinManager.getTableInfoFastaFiles(), file);
            associatedFastaId = file.getFastaId();
        }

        if (isFileAvailable())
        {
            synchronized (LOCK)
            {
                try
                {
                    parse();

                    Table.execute(ProteinManager.getSchema(), "UPDATE " + ProteinManager.getTableInfoFastaFiles() + " SET Loaded=? WHERE FastaId=?", new Object[]{new Date(), associatedFastaId});
                }
                finally
                {
                    if (null != conn)
                        try { ProteinManager.getSchema().getScope().releaseConnection(conn); } catch (SQLException e) {}

                    // Release all resources used by the guessing strategies (e.g., caches)
                    _parsingStrategy.close();
                    _sharedIdentsStrategy.close();
                }
            }

            ProteinManager.indexProteins(null, (Date)null);
        }
        else
        {
            throw new IOException("Could not locate file: " + getParseFName());
        }
    }

    public int getFastaId()
    {
        return associatedFastaId;
    }

    protected int initAnnotLoad(String comment) throws SQLException
    {
        if (currentInsertId == 0)
        {
            fdbu._initialInsertionStmt.setString(1, getParseFName());
            if (comment == null) setComment("");
            fdbu._initialInsertionStmt.setString(2, comment);
            fdbu._initialInsertionStmt.setString(3, defaultOrganism);
            fdbu._initialInsertionStmt.setInt(4, organismToBeGuessed ? 1 : 0);
            fdbu._initialInsertionStmt.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
            fdbu._initialInsertionStmt.executeUpdate();
            ResultSet rs = null;
            try
            {
                rs = fdbu._getCurrentInsertIdStmt.executeQuery();
                if (rs.next())
                    currentInsertId = rs.getInt(1);
            }
            finally
            {
                ResultSetUtil.close(rs);
            }
        }
        else
        {
            Integer skip = Table.executeSingleton(ProteinManager.getSchema(), "SELECT RecordsProcessed FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=" + currentInsertId, null, Integer.class);
            if (skip != null)
                skipEntries = skip.intValue();
        }
        //c.commit();
        return currentInsertId;
    }

    public static String baseFileName(String fPath)
    {
        if (fPath == null) return null;
        return (new File(fPath)).getName();
    }


    private static final int SQL_BATCH_SIZE = 100;

    protected void preProcessSequences(List<ProteinPlus> mouthful, Connection c) throws SQLException
    {
        long startTime = System.currentTimeMillis();
        c.setAutoCommit(false);
        // The timestamp, at index 12, is set once for the whole batch
        fdbu._addSeqStmt.setTimestamp(12, new java.sql.Timestamp(System.currentTimeMillis()));
        int transactionCount = 0;
        for (ProteinPlus curSeq : mouthful)
        {
            transactionCount++;

            String orgString;
            if (organismToBeGuessed)
            {
                orgString = _parsingStrategy.guess(curSeq);
                if (orgString == null)
                {
                    orgString = _sharedIdentsStrategy.guess(curSeq);
                }
            }
            else
            {
                orgString = defaultOrganism;
            }

            curSeq.setFullOrg(orgString);
            curSeq.setSpecies(extractSpecies(orgString));
            curSeq.setGenus(extractGenus(orgString));

            fdbu._addSeqStmt.setString(1, curSeq.getProtein().getSequenceAsString());
            fdbu._addSeqStmt.setString(2, curSeq.getHash());
            String desc = curSeq.getDescription();
            if (desc == null)
            {
                fdbu._addSeqStmt.setNull(3, Types.VARCHAR);
            }
            else
            {
                if (desc.length() >= 200) desc = desc.substring(0, 195) + "...";
                fdbu._addSeqStmt.setString(3, desc);
            }
            fdbu._addSeqStmt.setDouble(4, curSeq.getProtein().getMass());
            fdbu._addSeqStmt.setInt(5, curSeq.getProtein().getBytes().length);
            String bestNameTmp = curSeq.getBestName();
            fdbu._addSeqStmt.setString(6, bestNameTmp);
            //todo: rethink best name
            fdbu._addSeqStmt.setString(7, baseFileName(getParseFName()));
            fdbu._addSeqStmt.setString(8, curSeq.getProtein().getLookup());
            if (curSeq.getGenus() == null)
            {
                fdbu._addSeqStmt.setNull(9, Types.VARCHAR);
            }
            else
            {
                fdbu._addSeqStmt.setString(9, StringUtils.abbreviate(curSeq.getGenus(), 100));
            }
            if (curSeq.getSpecies() == null)
            {
                fdbu._addSeqStmt.setNull(10, Types.VARCHAR);
            }
            else
            {
                fdbu._addSeqStmt.setString(10, StringUtils.abbreviate(curSeq.getSpecies(), 100));
            }
            if (curSeq.getFullOrg() == null)
            {
                fdbu._addSeqStmt.setNull(11, Types.VARCHAR);
            }
            else
            {
                fdbu._addSeqStmt.setString(11, StringUtils.abbreviate(curSeq.getFullOrg(), 200));
            }
            // The timestamp, at index 12, is set once for the whole batch
            fdbu._addSeqStmt.addBatch();

            if (0 == transactionCount % SQL_BATCH_SIZE)
            {
                fdbu._addSeqStmt.executeBatch();
                c.commit();
                fdbu._addSeqStmt.clearBatch();
            }
            handleThreadStateChangeRequests();
        }

        fdbu._addSeqStmt.executeBatch();
        c.commit();
        fdbu._addSeqStmt.clearBatch();
        handleThreadStateChangeRequests();
        c.setAutoCommit(true);
        _log.debug("Sequences = " + transactionCount + ".  preProcessSequences() total elapsed time was " + (System.currentTimeMillis() - startTime)/1000 + " seconds for this mouthful.");

       guessBySharedHash();
    }

    protected void guessBySharedHash() throws SQLException
    {
        // only guessing for those records that are still 'Unknown unknown'
        // could be changed to
        fdbu._guessOrgBySharedHashStmt.setString(1, "Unknown");
        fdbu._guessOrgBySharedHashStmt.setString(2, "unknown");

        int rc = fdbu._guessOrgBySharedHashStmt.executeUpdate();
        _log.debug("Updated " + rc + " Sequences in guessBySharedHash");

    }

    protected int insertOrganisms(Connection c) throws SQLException
    {
        if (this.organismToBeGuessed)
        {
            handleThreadStateChangeRequests("In insertSequences, before guessing organism");
            fdbu._updateSTempWithGuessedOrgStmt.executeUpdate();
            handleThreadStateChangeRequests("In insertSequences, before setting default organism");
            PreparedStatement stmt = c.prepareStatement("UPDATE " + fdbu._seqTableName + " SET fullorg = ?, genus = ?, species = ? WHERE fullorg IS NULL");
            stmt.setString(1, defaultOrganism);
            stmt.setString(2, extractGenus(defaultOrganism));
            stmt.setString(3, extractSpecies(defaultOrganism));
            stmt.executeUpdate();
        }

        return fdbu._insertIntoOrgsStmt.executeUpdate();
    }

    private String extractGenus(String fullOrg)
    {
        if (fullOrg == null)
        {
            return null;
        }
        String separateParts[] = fullOrg.split(" ");
        if (separateParts.length >= 1)
        {
            return separateParts[0];
        }
        else
        {
            return "Unknown";
        }
    }

    private String extractSpecies(String fullOrg)
    {
        if (fullOrg == null)
        {
            return null;
        }
        String separateParts[] = fullOrg.split(" ");
        if (separateParts.length == 0) return "unknown";
        if (separateParts.length == 1) return "sp.";
        return separateParts[1];
    }

    protected int insertSequences() throws SQLException
    {
        handleThreadStateChangeRequests("In insertSequences, before inserting org ids into temp table");
        fdbu._updateSTempWithOrgIDsStmt.executeUpdate();
        long currentTime = System.currentTimeMillis();
        handleThreadStateChangeRequests("In insertSequences, before appending de-duped sqns into real table");
        int iInserted = fdbu._insertIntoSeqsStmt.executeUpdate();
        handleThreadStateChangeRequests("insertion took " + ((System.currentTimeMillis() - currentTime) / 1000) + " seconds");
        handleThreadStateChangeRequests("In insertSequences, before getting seqids from real table into temp table");
        fdbu._updateSTempWithSeqIDsStmt.executeUpdate();
        return iInserted;
    }

    protected int insertIdentifiers(Connection c) throws SQLException
    {
        handleThreadStateChangeRequests("Entering insertIdentifiers, before collecting unparsed identifiers from temp table");
        ResultSet rs = fdbu._getIdentsStmt.executeQuery();
        c.setAutoCommit(false);
        fdbu._addIdentStmt.setTimestamp(4, new java.sql.Timestamp(new java.util.Date().getTime()));
        int transactionCount = 0;
        while (rs.next())
        {
            String rawIdentString = rs.getString(1);
            int seqid = rs.getInt(2);
            String desc = rs.getString(3);
            String wholeHeader=rawIdentString;
            if (null != desc )
                    wholeHeader += " " + desc;
             // fasta files from IPI have good annotation in their description field,
            // not their name field.
            // todo:  this is almost the same code as in Protein.getIdentifier map and should be consolidated
            // note that newer IPI files don't have this issue
            if (rawIdentString != null && rawIdentString.startsWith("IPI") && !rawIdentString.contains("|") && desc != null && (desc.contains("|")))
                 rawIdentString = desc;

            Map<String, Set<String>> identifiers = Protein.identParse(rawIdentString, wholeHeader);

            for (String key : identifiers.keySet())
            {
                String identTypeName = key;
                if (!Protein.IdentTypeMap.containsValue(key))
                {
                    // unexpected ident type.  first check to see if it is an alias
                    if (Protein.IdentTypeMap.containsKey(key))
                        identTypeName=Protein.IdentTypeMap.get(key);
                }
                Set<String> idvals = identifiers.get(key);
                for (String val : idvals)
                {
                    // catch blanks before they get into the db
                    if (val.equals(""))
                        continue;
                    transactionCount++;
                    fdbu._addIdentStmt.setString(1, val);
                    fdbu._addIdentStmt.setString(2, identTypeName);
                    fdbu._addIdentStmt.setInt(3, seqid);
                    // We have already set the timestamp, at index 4, once for all insertions in this batch
                    fdbu._addIdentStmt.addBatch();

                    if (transactionCount == 100)
                    {
                        transactionCount = 0;
                        fdbu._addIdentStmt.executeBatch();
                        c.commit();
                        fdbu._addIdentStmt.clearBatch();
                    }
                }
            }
        }
        rs.close();

        fdbu._addIdentStmt.executeBatch();
        c.commit();
        fdbu._addIdentStmt.clearBatch();
        fdbu._insertIdentTypesStmt.executeUpdate();
        fdbu._updateIdentsWithIdentTypesStmt.executeUpdate();
        handleThreadStateChangeRequests("In insertIdentifiers, before appending temp table into real table");
        int iInserted = fdbu._insertIntoIdentsStmt.executeUpdate();

        handleThreadStateChangeRequests("Exiting insertIdentifiers");
        c.setAutoCommit(true);
        return iInserted;
    }

    protected int insertAnnotations() throws SQLException
    {
        // The only annotations we take from FASTA files  (currently) is the full organism name
        // (if there is one)
        handleThreadStateChangeRequests("In insertAnnotations, before inserting full organism");

        int n_orgs_added = fdbu._insertOrgsIntoAnnotationsStmt.executeUpdate();
        handleThreadStateChangeRequests("In insertAnnotations, before inserting lookup strings");

        return n_orgs_added /* + n_lookups_added */;
    }
    //      Primary tasks are to parse header information and determine
    //      whether the records are already present.


    //TODO:  this code is temporary, while moving from Adam's system to Ted's
    protected int insertLookups(int fastaID) throws SQLException
    {
        if (fastaID <= 0)
        {
            throw new IllegalArgumentException("fastaId must be set");
        }
        handleThreadStateChangeRequests("Before inserting into FastaSeqences");
        fdbu._insertFastaSequencesStmt.setInt(1, fastaID);
        return fdbu._insertFastaSequencesStmt.executeUpdate();
    }

    protected int guessAssociatedFastaId() throws SQLException
    {
        String bfn = baseFileName(getParseFName());
        ResultSet rs = null;
        try
        {
            rs = Table.executeQuery(ProteinManager.getSchema(), "SELECT FastaId,FileName FROM " + ProteinManager.getTableInfoFastaFiles(), null);
            while (rs.next())
            {
                String curFastaFname = rs.getString(2);
                String curFastaFnameBase = baseFileName(curFastaFname);
                if (bfn.equals(curFastaFnameBase))
                {
                    return rs.getInt(1);
                }
            }
        }
        finally
        {
            if (rs != null) { try { rs.close(); } catch (SQLException e) {} }
        }
        return 0;
    }

    protected void processMouthful(Connection c, List<ProteinPlus> mouthful) throws SQLException
    {
        handleThreadStateChangeRequests("Entering Process Mouthful");
        preProcessSequences(mouthful, c);
        int orgsAdded = insertOrganisms(c);
        handleThreadStateChangeRequests();
        int seqsAdded = insertSequences();
        handleThreadStateChangeRequests();
        int identsAdded = insertIdentifiers(c);
        handleThreadStateChangeRequests();
        int annotsAdded = insertAnnotations();
        if (associatedFastaId <= 0) setAssociatedFastaId(guessAssociatedFastaId());
        int lookupsUpdated = insertLookups(associatedFastaId);

        _log.debug("Updated " + lookupsUpdated + " lookups");
        handleThreadStateChangeRequests("In Process mouthful - finished mouthful");

        fdbu._emptySeqsStmt.executeUpdate();
        fdbu._emptyIdentsStmt.executeUpdate();

        // housekeeping and bookkeeping
        _log.info(" Added: " +
                orgsAdded + " organisms; " +
                seqsAdded + " sequences; " +
                identsAdded + " identifiers; " +
                annotsAdded + " annotations");
        _log.info(" This batch of records processed successfully");
        fdbu._getCurrentInsertStatsStmt.setInt(1, currentInsertId);
        ResultSet r = null;

        int priorseqs;
        int priorannots;
        int prioridents;
        int priororgs;
        int mouthsful;
        int records;
        try
        {
            r = fdbu._getCurrentInsertStatsStmt.executeQuery();
            if (r.next())
            {
                priorseqs = r.getInt("SequencesAdded");
                priorannots = r.getInt("AnnotationsAdded");
                prioridents = r.getInt("IdentifiersAdded");
                priororgs = r.getInt("OrganismsAdded");
                mouthsful = r.getInt("Mouthsful");
                records = r.getInt("RecordsProcessed");
            }
            else
            {
                throw new SQLException("ResultSet came back empty");
            }
        }
        finally
        {
            if (r != null) { try { r.close(); } catch (SQLException e) {} }
        }

        int curNRecords = mouthful.size();
        fdbu._updateInsertionStmt.setInt(1, mouthsful + 1);
        fdbu._updateInsertionStmt.setInt(2, priorseqs + seqsAdded);
        fdbu._updateInsertionStmt.setInt(3, priorannots + annotsAdded);
        fdbu._updateInsertionStmt.setInt(4, prioridents + identsAdded);
        fdbu._updateInsertionStmt.setInt(5, priororgs + orgsAdded);
        fdbu._updateInsertionStmt.setInt(6, seqsAdded);
        fdbu._updateInsertionStmt.setInt(7, annotsAdded);
        fdbu._updateInsertionStmt.setInt(8, identsAdded);
        fdbu._updateInsertionStmt.setInt(9, orgsAdded);
        fdbu._updateInsertionStmt.setInt(10, curNRecords);
        fdbu._updateInsertionStmt.setInt(11, records + curNRecords);
        fdbu._updateInsertionStmt.setTimestamp(12, new java.sql.Timestamp(new java.util.Date().getTime()));
        fdbu._updateInsertionStmt.setInt(13, currentInsertId);
        fdbu._updateInsertionStmt.executeUpdate();
        handleThreadStateChangeRequests("Exiting Process Mouthful");
    }

    protected void finalizeAnnotLoad() throws SQLException
    {
        handleThreadStateChangeRequests("Setting BestGeneName");
        fdbu._updateBestGeneNameStmt.setInt(1, associatedFastaId);
        fdbu._updateBestGeneNameStmt.executeUpdate();

        handleThreadStateChangeRequests("Finalizing bookkeeping record");
        fdbu._finalizeInsertionStmt.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
        fdbu._finalizeInsertionStmt.setInt(2, currentInsertId);
        fdbu._finalizeInsertionStmt.executeUpdate();
    }

    protected void genProtFastaRecord(List<Integer> seqIds, String hash) throws SQLException
    {
        handleThreadStateChangeRequests("Creating FASTA record");

        fdbu._insertIntoFastasStmt.setString(1, getParseFName());
        fdbu._insertIntoFastasStmt.setInt(2, seqIds.size());
        fdbu._insertIntoFastasStmt.setString(3, _comment);
        fdbu._insertIntoFastasStmt.setString(4, hash);
        fdbu._insertIntoFastasStmt.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
        fdbu._insertIntoFastasStmt.executeUpdate();
        handleThreadStateChangeRequests("Done creating FASTA record");
    }

    public void parse() throws IOException, SQLException
    {
        File fastaFile = new File(getParseFName());
        FastaValidator validator = new FastaValidator(fastaFile);
        List<String> errors = validator.validate();

        if (!errors.isEmpty())
        {
            _log.error("This FASTA file has " + errors.size() + " duplicate protein name" + (1 == errors.size() ? "" : "s") + ", listed below.  " +
                    "Search engines and the Trans-Proteomic Pipeline use these names to link to specific protein sequeces so the names must be unique.  " +
                    "You should remove or otherwise disambiguate the duplicate entries from this FASTA file and re-run your search.");

            String errorString = StringUtils.join(errors, "\n");
            _log.error(errorString);

            throw new RuntimeException("Invalid FASTA file");
        }

        FastaLoader curLoader = new FastaLoader(fastaFile);

        conn = ProteinManager.getSchema().getScope().getConnection();
        //conn.setAutoCommit(false);
        //Determine whether this file has ever been
        //inserted before.  Warn or abort if it has.

        fdbu = new FastaDbHelper(conn);
        int loadCounter = 0;
        List<ProteinPlus> mouth = new ArrayList<ProteinPlus>();
        List<Integer> seqIds = new ArrayList<Integer>();

        if (_recoveryId == 0)
        {
            currentInsertId = initAnnotLoad(_comment);
        }
        else
        {
            currentInsertId = _recoveryId;
            ResultSet rs = null;
            try
            {
                rs = conn.createStatement().executeQuery("SELECT DefaultOrganism,OrgShouldBeGuessed FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE insertId=" + currentInsertId);
                if (rs.next())
                {
                    setDefaultOrganism(rs.getString(1));
                    setOrganismIsToGuessed(rs.getInt(2) == 1);
                }
                else
                    _log.error("Can't find insert id " + currentInsertId + " in parse recovery.");
            }
            finally
            {
                if (rs != null) try { rs.close(); } catch (SQLException e) {}
            }
            Integer skipCount = Table.executeSingleton(ProteinManager.getSchema(), "SELECT RecordsProcessed FROM " +
                    ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=" +
                    currentInsertId, null, Integer.class);
            if (skipCount != null)
                skipEntries = skipCount.intValue();
        }

        Thread.currentThread().setName("AnnotLoader" + currentInsertId);

        int protCount = 0;
        int negCount = 0;
        // TODO: Need a container to make this configurable.
        String negPrefix = MS2Manager.NEGATIVE_HIT_PREFIX;

        //Main loop
        for (FastaLoader.ProteinIterator proteinIterator = curLoader.iterator(); proteinIterator.hasNext();)
        {
            ProteinPlus p = new ProteinPlus(proteinIterator.next());

            if (skipEntries > 0)
            {
                skipEntries--;
                continue;
            }
            loadCounter++;
            handleThreadStateChangeRequests();
            mouth.add(p);

            protCount++;
            if (p.getProtein().getName().startsWith(negPrefix))
                negCount++;

            if (!proteinIterator.hasNext() || (loadCounter % mouthfulSize) == 0)
            {
                processMouthful(conn, mouth);
                mouth.clear();
            }

            Integer percentComplete = proteinIterator.getPercentCompleteIfChanged();

            if (null != percentComplete)
                _log.info("Importing FASTA file sequences: " + percentComplete + "% complete");
        }

        if (protCount / 3 < negCount)
        {
            Table.execute(ProteinManager.getSchema(), "UPDATE " + ProteinManager.getTableInfoFastaFiles() +
                    " SET ScoringAnalysis = ?" +
                    " WHERE FastaId = ?", new Object[] { true, associatedFastaId });
        }

        finalizeAnnotLoad();
        if (_fileHash == null)
        {
            _fileHash = HashHelpers.hashFileContents(getParseFName());
        }
        genProtFastaRecord(seqIds, _fileHash);
        fdbu._dropIdentTempStmt.executeUpdate();
        fdbu._dropSeqTempStmt.executeUpdate();
        cleanUp();
    }

    public void cleanUp()
    {
        try
        {
            ProteinManager.getSchema().getScope().releaseConnection(conn);
            this.requestThreadState(AnnotationLoader.Status.DYING);
        }
        catch (Exception e)
        {
            //FastaDbLoader.parseWarning("Problem closing database: "+e);
        }
    }

    // Same as File.getCanonicalPath() except we use forward slashes and lower case drive letters
    public static String getCanonicalPath(File f) throws IOException
    {
        if (f == null)
        {
            return null;
        }
        String newFileName = f.getCanonicalPath().replace('\\', '/');

        // This returns drive letter in canonical form (lower case) or null
        String drive = NetworkDrive.getDrive(newFileName);

        if (null == drive)
            return newFileName;   // No drive, just return
        else
            return drive + newFileName.substring(2, newFileName.length());      // Replace drive with canonical form
    }

    public static synchronized int loadAnnotations(String path, String fileName, String defaultOrganism, boolean shouldGuess, Logger log, XarContext context) throws SQLException, IOException
    {
        File f = context.findFile(fileName, new File(path));
        if (f == null)
        {
            throw new FileNotFoundException(fileName);
        }
        String convertedName = getCanonicalPath(f);
        String hash = HashHelpers.hashFileContents(convertedName);
        String[] hashArray = new String[] {hash};

        FastaFile[] files = Table.executeQuery(ProteinManager.getSchema(), "SELECT * FROM " + ProteinManager.getTableInfoFastaFiles() + " WHERE FileChecksum=? ORDER BY FastaId", hashArray, FastaFile.class);
        FastaFile loadedFile = null;
        for (FastaFile file : files)
        {
            if (file.getLoaded() == null)
            {
                ProteinManager.deleteFastaFile(file.getFastaId());
            }
            else
            {
                loadedFile = file;
            }
        }

        Long existingProtFastasCount = Table.executeSingleton(ProteinManager.getSchema(), "SELECT COUNT(*) FROM " + ProteinManager.getTableInfoFastaLoads() + " WHERE FileChecksum = ?", hashArray, Long.class);
        if (loadedFile != null && existingProtFastasCount != null && existingProtFastasCount.longValue() > 0)
        {
            String previousFileWithSameChecksum =
                    Table.executeSingleton(ProteinManager.getSchema(), "SELECT FileName FROM " + ProteinManager.getTableInfoFastaLoads() + " WHERE FileChecksum = ?", hashArray, String.class);

            if (convertedName.equals(previousFileWithSameChecksum))
                log.info("FASTA file \"" + convertedName + "\" has already been imported");
            else
                log.info("FASTA file \"" + convertedName + "\" not imported, but another file, '" + previousFileWithSameChecksum + "', has the same checksum");
            return loadedFile.getFastaId();
        }

        FastaDbLoader fdbl = new FastaDbLoader(f, hash, log);
        fdbl.setComment(new java.util.Date() + " " + convertedName);
        fdbl.setDefaultOrganism(defaultOrganism);
        fdbl.setOrganismIsToGuessed(shouldGuess);
        fdbl.parseFile();

        return fdbl.getFastaId();
    }


    public static void updateSeqIds(int[] fastaIds)
    {
        Thread thread = new Thread(new FastaReloader(fastaIds));
        thread.start();
    }

    //JUnit TestCase
    public static class TestCase extends junit.framework.TestCase
    {

        TestCase(String name)
        {
            super(name);
        }

        public static Test suite()
        {
            TestSuite suite = new TestSuite();
            suite.addTest(new TestCase("testValidateIdentParser"));
            return suite;
        }

        public void testValidateIdentParser()
        {
            Map<String, Set<String>> idMapE;

            idMapE = IdPattern.createIdMap("GI", "16758788");
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("REFSEQ", "NP_446360.1"));
            parseAndCompare("GI|16758788|ref|NP_446360.1|", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProt.toString(), "2A5D_YEAST"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(), "P38903"));
            parseAndCompare("P38903|2A5D_YEAST", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProt.toString(),"143G_BOVIN"));
            parseAndCompare("143G_BOVIN", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"B32382"));
            parseAndCompare("B32382", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","1350702"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProt.toString(),"RL26_XENLA"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"P49629"));
            parseAndCompare("gi|1350702|sp|P49629|RL26_XENLA", idMapE);   // check this

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("Genbank","EY193505.1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","159497289"));
            parseAndCompare("gi|159497289|gb|EY193505.1|EY193505", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","16758788"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("REFSEQ","NP_446360.1"));
            parseAndCompare("gi|16758788|ref|NP_446360.1|", idMapE);


            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","1705439"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProt.toString(),"BCCP_BACSU"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"P49786"));
            parseAndCompare("gi|1705439|sp|P49786|BCCP_BACSU", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","17233017"));
            parseAndCompare("gi|17233017|COG0589:COG0077", idMapE);


            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","17233017"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("REFSEQ","NC_003276.1"));
            parseAndCompare("gi|17233017|ref|NC_003276.1|_268495_270174|", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("Genbank","AAM45611.1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","21305377"));
            parseAndCompare("gi|21305377|gb|AAM45611.1|AF384285_1", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","2136708"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"I45994"));
            parseAndCompare("gi|2136708|pir||I45994", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","25140706"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("Genbank","DAA00377.1"));
            parseAndCompare("gi|25140706|tpg|DAA00377.1|", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("Genbank","BAA76286.1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","4521223"));
            parseAndCompare("gi|4521223|dbj|BAA76286.1|", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("Genbank","CAA34470.1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","61"));
            parseAndCompare("gi|61|emb|CAA34470.1|", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("Genbank","CAB95676.1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GI","8745138"));
            parseAndCompare("gi|8745138|emb|CAB95676.1|", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("SI","143GT"));
            parseAndCompare("gnl|si|143GT_SUFFIX", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00387615"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00387615.2"));
            parseAndCompare("ipi|IPI00387615|IPI00387615.2", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("ENSEMBL","ENSRNOP00000030679"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00387615.1"));
            parseAndCompare("IPI:IPI00387615.1|ENSEMBL:ENSRNOP00000030679", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00421289.1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("REFSEQ","NP_955791"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q6RVG2"));
            parseAndCompare("IPI:IPI00421289.1|REFSEQ_NP:NP_955791|UniProt/TrEMBL:Q6RVG2", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00454216.1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q6RVG1"));
            parseAndCompare("IPI:IPI00454216.1|UniProt/TrEMBL:Q6RVG1", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProt.toString(),"O05473_SULIS"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"O05473"));
            parseAndCompare("O05473|O05473_SULIS", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("UniRef100","Q4U9M9"));
            parseAndCompare("UniRef100_Q4U9M9", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00421289.1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q6RVG2"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q6RVG3"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q6RVG4"));
            parseAndCompare("IPI:IPI00421289.1|UniProt/TrEMBL:Q6RVG2;Q6RVG3;Q6RVG4;", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProt.toString(),"HPH2_YEAST"));
            parseAndCompare("UPSP:HPH2_YEAST", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProt.toString(),"Q6B2T6_YEAST"));
            parseAndCompare("UPTR:Q6B2T6_YEAST", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("SGDID","S000000001"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("ENSEMBL","YAL001C"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("SGD_GN","TFC3"));
            parseAndCompare("YAL001C", idMapE
                    ,"YAL001C TFC3 SGDID:S000000001, Chr I from 151168-151099,151008-147596, reverse complement, Verified ORF, \"Largest of six subunits of the RNA polymerase III transcription initiation factor complex (TFIIIC); part of the TauB domain of TFIIIC that binds DNA at the BoxB promoter sites of tRNA and similar genes; cooperates with Tfc6p in DNA binding\"");

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("REFSEQ","NP_001073825;XP_593190"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("ENSEMBL","ENSBTAP00000028878"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q2KIJ2"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GN","MGC137286;LOC515210"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00685094.1"));
            parseAndCompare("IPI:IPI00685094.1|SWISS-PROT:Q2KIJ2|ENSEMBL:ENSBTAP00000028878|REFSEQ:NP_001073825;XP_593190", idMapE,
                    "IPI:IPI00685094.1|SWISS-PROT:Q2KIJ2|ENSEMBL:ENSBTAP00000028878|REFSEQ:NP_001073825;XP_593190 Tax_Id=9913 Gene_Symbol=MGC137286;LOC515210 Uncharacterized protein C1orf156 homolog");

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("REFSEQ","NP_001020605"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("ENSEMBL","ENSP00000368334"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q3B792"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GN","NQO1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00619898.2"));
            parseAndCompare("IPI:IPI00619898.2|TREMBL:Q3B792|ENSEMBL:ENSP00000368334|REFSEQ:NP_001020605", idMapE, "IPI:IPI00619898.2|TREMBL:Q3B792|ENSEMBL:ENSP00000368334|REFSEQ:NP_001020605 Tax_Id=9606 Gene_Symbol=NQO1 NQO1 protein (Fragment)");

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("REFSEQ","NP_000894"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("ENSEMBL","ENSP00000319788"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q53G81;P15559"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GN","NQO1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00012069.1"));
            parseAndCompare("IPI:IPI00012069.1|SWISS-PROT:P15559|TREMBL:Q53G81|ENSEMBL:ENSP00000319788|REFSEQ:NP_000894|H-INV:HIT000191221|VEGA:OTTHUMP00000081321;OTTHUMP00000174897", idMapE, "IPI:IPI00012069.1|SWISS-PROT:P15559|TREMBL:Q53G81|ENSEMBL:ENSP00000319788|REFSEQ:NP_000894|H-INV:HIT000191221|VEGA:OTTHUMP00000081321;OTTHUMP00000174897 Tax_Id=9606 Gene_Symbol=NQO1 NAD");

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"P15559"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProt.toString(),"NQO1_HUMAN"));
            parseAndCompare("P15559|NQO1_HUMAN", idMapE);

            idMapE=null;
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("ENSEMBL","ENSP00000307953"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap(IdentifierType.SwissProtAccn.toString(),"Q7KYQ5;Q7KYY4;Q8IZZ8;Q8IZZ9;Q8J000;Q8J001;Q8TCE1;Q9UBW9"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("GN","SERPINC1"));
            idMapE = IdPattern.addIdMap(idMapE, IdPattern.createIdMap("IPI","IPI00844156.2"));
            parseAndCompare("IPI:IPI00844156.2|TREMBL:Q7KYQ5;Q7KYY4;Q8IZZ8;Q8IZZ9;Q8J000;Q8J001;Q8TCE1;Q9UBW9|ENSEMBL:ENSP00000307953", idMapE, "IPI:IPI00844156.2|TREMBL:Q7KYQ5;Q7KYY4;Q8IZZ8;Q8IZZ9;Q8J000;Q8J001;Q8TCE1;Q9UBW9|ENSEMBL:ENSP00000307953 Tax_Id=9606 Gene_Symbol=SERPINC1 SERPINC1 protein");


            // return empty map
            idMapE=new HashMap<String,Set<String>>();
            parseAndCompare("GENSCAN00000048050", idMapE);
            parseAndCompare("uniparc|UPI0000503605|UPI0000503605", idMapE);
            parseAndCompare("gnl|unk|UNKNOWN_ID", idMapE);

            parseAndCompare("spaces spaces", idMapE);
            parseAndCompare("    | ", idMapE);
            parseAndCompare("     ", idMapE);
            parseAndCompare("|||||||", idMapE);
            parseAndCompare("UPTR:", idMapE);
            parseAndCompare("IPI:", idMapE);
            parseAndCompare("ipi|", idMapE);
            parseAndCompare("ipi| ", idMapE);
            parseAndCompare("UniRef100_", idMapE);
            parseAndCompare("|a|a|:|test||", idMapE);
            parseAndCompare("BAD|a|a|:|test||", idMapE);
            parseAndCompare("Value,Value:other|a|a|:|test||", idMapE);
            parseAndCompare("||lots||of|||bars|||", idMapE);
            parseAndCompare("\\||\"\"", idMapE);
            parseAndCompare("sprot_like***", idMapE);
            parseAndCompare("Q12345_sprot_like", idMapE);
            parseAndCompare("\r\t\n\"", idMapE);
            parseAndCompare(null, idMapE);
            parseAndCompare("UniRef100_Q4U9M9;UniRef100_Q4U123;UniRef100_Q4U456;", idMapE);

        }

        protected boolean compareIdMaps (Map<String, Set<String>> idMapExpected, Map<String, Set<String>> idMapReturned)
        {
            if (null == idMapReturned)
            {
                return null == idMapExpected;
            }
            else
            {
                if (null == idMapExpected)
                    return false;

                if (idMapExpected.size() != idMapReturned.size())
                    return false;

                for (String keyE : idMapExpected.keySet())
                {
                    if (!idMapReturned.containsKey(keyE))
                        return false;
                    Set<String> valsE = idMapExpected.get(keyE);
                    if (valsE.size() != idMapReturned.get(keyE).size())
                        return false;
                    for (String valE : valsE)
                    {
                        if (!(idMapReturned.get(keyE)).contains(valE))
                            return false;
                    }
                }
            }
            return true;
        }
        protected void parseAndCompare(String strLookup, Map<String, Set<String>> idMapExpected)
        {
            parseAndCompare(strLookup, idMapExpected, null);
        }

        protected void parseAndCompare(String strLookup, Map<String, Set<String>> idMapExpected, String wholeHeader)
        {
            Map<String, Set<String>> idMapReturned = Protein.identParse(strLookup, wholeHeader);
            assert(compareIdMaps(idMapExpected, idMapReturned));
        }

    }

} // class FastaDbLoader


