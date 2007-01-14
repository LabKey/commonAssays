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

import org.labkey.api.data.Table;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.util.HashHelpers;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.fasta.FastaDbHelper;
import org.labkey.ms2.protein.organism.*;
import org.fhcrc.cpas.tools.FastaLoader;
import org.fhcrc.cpas.tools.Protein;
import org.labkey.api.exp.XarContext;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.Date;
import java.sql.*;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;

public class FastaDbLoader extends DefaultAnnotationLoader implements AnnotationLoader
{
    private Connection conn = null;
    protected String parseFName;
    protected String comment = null;
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

    //Todo:  this should be an enumerated type; this loader and the xml loader should be
    //Todo:  children of the same abstract type.


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
        parseFName = getCanonicalPath(file);

        _parsingStrategy = new GuessOrgByParsing();
        _sharedIdentsStrategy = new GuessOrgBySharedIdents();
    }

    public String getParseFName()
    {
        return parseFName;
    }

    public void setComment(String c)
    {
        this.comment = c;
    }

    public int getId()
    {
        return currentInsertId;
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
        if (f.exists())
        {
            Table.execute(ProteinManager.getSchema(), "UPDATE " + ProteinManager.getTableInfoFastaFiles() + " SET Loaded=? WHERE FastaId=?", new Object[]{new Date(), associatedFastaId});
            return true;
        }
        else
        {
            Table.execute(ProteinManager.getSchema(), "UPDATE " + ProteinManager.getTableInfoFastaFiles() + " SET Loaded=NULL WHERE FastaId=?", new Object[]{associatedFastaId});
            return false;
        }
    }

    public void parseFile() throws SQLException, IOException
    {
        long startTime = System.currentTimeMillis();
        if (_fileHash == null)
        {
            _fileHash = HashHelpers.hashFileContents(getParseFName());
        }

        synchronized (LOCK)
        {
            HashMap<String, String> dbMap = new HashMap<String, String>();
            dbMap.put("FileName", getParseFName());
            dbMap.put("FileChecksum", _fileHash);
            Map returnMap = Table.insert(null, ProteinManager.getTableInfoFastaFiles(), dbMap);
            associatedFastaId = ((Integer)returnMap.get("FastaId")).intValue();
        }

        if (isFileAvailable())
        {
            _log.info("Starting to load protein annotations");

            synchronized (LOCK)
            {
                try
                {
                    parse();
                }
                finally
                {
                    try { ProteinManager.getSchema().getScope().releaseConnection(conn); } catch (SQLException e) {}
                }
            }
            _log.info("Finished loading protein annotations");
            long seconds = (System.currentTimeMillis() - startTime) / 1000;
            _log.info("Loading took " + seconds + " seconds"); 
        }
    }

    public int getFastaId()
    {
        return associatedFastaId;
    }

    protected int initAnnotLoad(Connection c, String fileName, String comment) throws SQLException
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
                if (rs != null) try { rs.close(); } catch (SQLException e) {}
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
            String desc = null;
            int firstBlankIndex;
            if ((firstBlankIndex = curSeq.getProtein().getHeader().indexOf(" ")) != -1)
            {
                desc = curSeq.getProtein().getHeader().substring(firstBlankIndex + 1).replaceAll("\\01", " ").trim();
            }
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
            String bestNameTmp = curSeq.getProtein().getHeader();
            if (firstBlankIndex != -1) bestNameTmp = bestNameTmp.substring(0, firstBlankIndex).trim();
            if (bestNameTmp.length() > 500) bestNameTmp = bestNameTmp.substring(0, 499);
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
                fdbu._addSeqStmt.setString(9, curSeq.getGenus());
            }
            if (curSeq.getSpecies() == null)
            {
                fdbu._addSeqStmt.setNull(10, Types.VARCHAR);
            }
            else
            {
                fdbu._addSeqStmt.setString(10, curSeq.getSpecies());
            }
            if (curSeq.getFullOrg() == null)
            {
                fdbu._addSeqStmt.setNull(11, Types.VARCHAR);
            }
            else
            {
                fdbu._addSeqStmt.setString(11, curSeq.getFullOrg());
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

       guessBySharedHash(c);
    }

    protected void guessBySharedHash(Connection c) throws SQLException
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

    protected int insertSequences(Connection c) throws SQLException
    {
        handleThreadStateChangeRequests("In insertSequences, before inserting org ids into temp table");
        fdbu._updateSTempWithOrgIDsStmt.executeUpdate();
        long currentTime = System.currentTimeMillis();
        handleThreadStateChangeRequests("In insertSequences, before appending de-duped sqns into real table");
        int iInserted = fdbu._insertIntoSeqsStmt.executeUpdate();
        handleThreadStateChangeRequests("insertion took " + ((System.currentTimeMillis() - currentTime) / 1000) + " seconds");
        handleThreadStateChangeRequests("In insertSequences, before getting seqids from real table into temp table");
        fdbu._updateSTempWithSeqIDsStmt.executeUpdate();
//       c.commit();
        return iInserted;
    }

    protected int insertIdentifiers(Connection c, List<Integer> ids) throws SQLException
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

            // fasta files from IPI have good annotation in their description field,
            // not their name field.
            if (rawIdentString.startsWith("IPI") && rawIdentString.indexOf("|") == -1)
            {
                String possibleNewRawIdentString = rs.getString(3);
                if (possibleNewRawIdentString.indexOf("|") != -1)
                {
                    rawIdentString = possibleNewRawIdentString;
                }
            }
            else if((rawIdentString.indexOf("|") == -1) && Protein.mightBeASwissProtName(rawIdentString))
            {
                rawIdentString = "SPROT_NAME|" + rawIdentString;
            }

            Map<String, Set<String>> identifiers = Protein.identParse(rawIdentString);
            for (String key : identifiers.keySet())
            {
                Set<String> idvals = identifiers.get(key);
                for (String val : idvals)
                {
                    // catch blanks before they get into the db
                    if (val.equals(""))
                        continue;
                    transactionCount++;
                    fdbu._addIdentStmt.setString(1, val);
                    fdbu._addIdentStmt.setString(2, key);
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

//       c.commit();
        handleThreadStateChangeRequests("Exiting insertIdentifiers");
        c.setAutoCommit(true);
        return iInserted;
    }

    protected int insertAnnotations(Connection c) throws SQLException
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
    protected int insertLookups(Connection c, int fastaID) throws SQLException
    {
        if (fastaID <= 0)
        {
            throw new IllegalArgumentException("fastaId must be set");
        }
        handleThreadStateChangeRequests("Before inserting into FastaSeqences");
        fdbu._insertFastaSequencesStmt.setInt(1, fastaID);
        int n_lookups_updated =
                fdbu._insertFastaSequencesStmt.executeUpdate();
        return n_lookups_updated;
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

    protected void processMouthful(Connection c, List<ProteinPlus> mouthful, List<Integer> seqIDs) throws SQLException
    {
        handleThreadStateChangeRequests("Entering Process Mouthful");
        preProcessSequences(mouthful, c);
        int orgsAdded = insertOrganisms(c);
        handleThreadStateChangeRequests();
        int seqsAdded = insertSequences(c);
        handleThreadStateChangeRequests();
        int identsAdded = insertIdentifiers(c, seqIDs);
        handleThreadStateChangeRequests();
        int annotsAdded = insertAnnotations(c);
        if (associatedFastaId <= 0) setAssociatedFastaId(guessAssociatedFastaId());
        int lookupsUpdated = insertLookups(c, associatedFastaId);

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

//       c.commit();
    }

    protected void finalizeAnnotLoad(Connection c, int insertId) throws SQLException
    {
        handleThreadStateChangeRequests("Finalizing bookkeeping record");
        fdbu._finalizeInsertionStmt.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
        fdbu._finalizeInsertionStmt.setInt(2, currentInsertId);
        fdbu._finalizeInsertionStmt.executeUpdate();
//       c.commit();
    }

    protected void genProtFastaRecord(Connection c, List<Integer> seqIds, String hash) throws SQLException
    {
        handleThreadStateChangeRequests("Creating FASTA record");
        int idArr[] = new int[seqIds.size()];
        for (int i = 0; i < seqIds.size(); i++)
        {
            idArr[i] = seqIds.get(i).intValue();
        }
        fdbu._insertIntoFastasStmt.setString(1, getParseFName());
        fdbu._insertIntoFastasStmt.setInt(2, seqIds.size());
        fdbu._insertIntoFastasStmt.setString(3, comment);
        fdbu._insertIntoFastasStmt.setString(4, hash);
        fdbu._insertIntoFastasStmt.setTimestamp(5, new java.sql.Timestamp(new java.util.Date().getTime()));
        fdbu._insertIntoFastasStmt.executeUpdate();
        handleThreadStateChangeRequests("Done creating FASTA record");

        //c.commit();
    }

    public void parse() throws IOException, SQLException
    {
        FastaLoader curLoader = new FastaLoader(new File(getParseFName()));
        conn = ProteinManager.getSchema().getScope().getConnection();
        //conn.setAutoCommit(false);
        //Determine whether this file has ever been
        //inserted before.  Warn or abort if it has.

        fdbu = new FastaDbHelper(conn);
        int loadCounter = 0;
        List<ProteinPlus> mouth = new ArrayList<ProteinPlus>();
        List<Integer> seqIds = new ArrayList<Integer>();
        int insertId;

        if (_recoveryId == 0)
        {
            insertId = initAnnotLoad(conn, getParseFName(), comment);
        }
        else
        {
            insertId = _recoveryId;
            currentInsertId = insertId;
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
        String negPrefix = MS2Manager.getNegativeHitPrefix(null);

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
                processMouthful(conn, mouth, seqIds);
                mouth.clear();

                Integer percentComplete = proteinIterator.getPercentCompleteIfChanged();

                if (null != percentComplete)
                    _log.info("Loading FASTA file sequences: " + percentComplete + "% complete");
            }
        }

        if (protCount / 3 < negCount)
        {
            Table.execute(ProteinManager.getSchema(), "UPDATE " + ProteinManager.getTableInfoFastaFiles() +
                    " SET ScoringAnalysis = ?" +
                    " WHERE FastaId = ?", new Object[] { true, associatedFastaId });
        }

        finalizeAnnotLoad(conn, insertId);
        if (_fileHash == null)
        {
            _fileHash = HashHelpers.hashFileContents(getParseFName());
        }
        genProtFastaRecord(conn, seqIds, _fileHash);
        fdbu._dropIdentTempStmt.executeUpdate();
        fdbu._dropSeqTempStmt.executeUpdate();
        cleanUp();
    }

    //
    // Error handlers.
    //
    public static void parseWarning(String s)
    {
        _log.warn(s);
    }

    public static void parseError(String s)
    {
        _log.error(s);
    }

    //
    // Background Thread Stuff
    //
    public void parseInBackground()
    {
        AnnotationUploadManager.getInstance().enqueueAnnot(this);
    }

    public void parseInBackground(int recoveryId)
    {
        setRecoveryId(recoveryId);
        AnnotationUploadManager.getInstance().enqueueAnnot(this);
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
        Integer existingFastaId = Table.executeSingleton(ProteinManager.getSchema(), "SELECT FastaId FROM " + ProteinManager.getTableInfoFastaFiles() + " WHERE FileChecksum=?", hashArray, Integer.class);

        Long existingProtFastasCount = Table.executeSingleton(ProteinManager.getSchema(), "SELECT COUNT(*) FROM " + ProteinManager.getTableInfoFastaLoads() + " WHERE FileChecksum = ?", hashArray, Long.class);
        if (existingFastaId != null && existingProtFastasCount != null && existingProtFastasCount.longValue() > 0)
        {
            String previousFileWithSameChecksum =
                    Table.executeSingleton(ProteinManager.getSchema(), "SELECT FileName FROM " + ProteinManager.getTableInfoFastaLoads() + " WHERE FileChecksum = ?", hashArray, String.class);

            if (convertedName.equals(previousFileWithSameChecksum))
                log.info("FASTA file \"" + convertedName + "\" has already been loaded");
            else
                log.info("FASTA file \"" + convertedName + "\" not loaded; another file, '" + previousFileWithSameChecksum + "', has the same checksum");
            return existingFastaId.intValue();
        }

        FastaDbLoader fdbl = new FastaDbLoader(f, hash, log);
        fdbl.setComment(new java.util.Date() + " " + convertedName);
        fdbl.setDefaultOrganism(defaultOrganism);
        fdbl.setOrganismIsToGuessed(shouldGuess);
        fdbl.parseFile();

        return fdbl.getFastaId();
    }


    // Update the SeqIds in ProteinSequences table for a previously loaded FASTA file.  This is to help fix up
    // null SeqIds that, up until CPAS 1.4, occurred when a single mouthful contained two or more identical
    // sequences.
    private static void updateSeqIds(int fastaId) throws SQLException, IOException
    {
        Map fasta = Table.selectObject(ProteinManager.getTableInfoFastaFiles(), fastaId, Map.class);
        String filename = (String)fasta.get("FileName");

        FastaDbLoader fdbl = new FastaDbLoader(new File(filename), (String)fasta.get("filechecksum"));
        fdbl.setAssociatedFastaId(fastaId);
        fdbl.setComment(new java.util.Date() + " " + filename);
        fdbl.setDefaultOrganism(UNKNOWN_ORGANISM);
        fdbl.setOrganismIsToGuessed(true);
        fdbl.parseFile();
    }


    public static void updateSeqIds(List<Integer> fastaIds)
    {
        Thread thread = new Thread(new SeqIdUpdater(fastaIds));
        thread.start();
    }

    private static class SeqIdUpdater implements Runnable
    {
        private List<Integer> _fastaIds;

        SeqIdUpdater(List<Integer> fastaIds)
        {
            _fastaIds = fastaIds;
        }

        public void run()
        {
            for (Integer fastaId : _fastaIds)
            {
                try
                {
                    updateSeqIds(fastaId.intValue());
                }
                catch(Exception e)
                {
                    _log.error("Exception while updating SeqIds for FASTA id " + fastaId, e);
                }
            }
        }
    }
} // class FastaDbLoader

