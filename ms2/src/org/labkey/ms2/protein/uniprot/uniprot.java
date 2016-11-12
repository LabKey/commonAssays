/*
 * Copyright (c) 2005-2016 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2.protein.uniprot;

import org.apache.log4j.Logger;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.ms2.protein.ParseActions;
import org.labkey.ms2.protein.ParseContext;
import org.labkey.ms2.protein.ProteinManager;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Random;

// Repository for routines specific to org.fhcrc.edi.protein.uniprot.uniprot parsing
// And initial stuff to do with the root element

public class uniprot extends ParseActions
{
    private final Logger _log;
    private static SqlDialect _dialect = CoreSchema.getInstance().getSqlDialect();
    private long _startTime;

    private static final int TRANSACTION_ROW_COUNT = 100;

    // n of top-level "entries" to skip
    protected int _skipEntries = 0;

    public uniprot(Logger log)
    {
        _log = log;
    }

    public void setSkipEntries(int s)
    {
        _skipEntries = s;
    }

    public int getSkipEntries()
    {
        return _skipEntries;
    }

    public boolean unBumpSkip()
    {
        if (getSkipEntries() > 0)
        {
            setSkipEntries(getSkipEntries() - 1);
            return false;
        }
        else
        {
            return true;
        }
    }

    public void beginElement(ParseContext context, Attributes attrs) throws SAXException
    {
        _startTime = System.currentTimeMillis();
        context.setUniprotRoot(this);

        // Annotations and Identifiers are Vectors of Maps
        // The Vectors get cleared by insertTables
        try
        {
            setupNames(context.getConnection());

            if (getCurrentInsertId() == 0)
            {
                _initialInsertion.setString(1, getFile().getPath());
                if (getComment() == null) setComment("");
                _initialInsertion.setString(2, getComment());
                _initialInsertion.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));

                try (ResultSet idrs = _dialect.executeWithResults(_initialInsertion))
                {
                    idrs.next();
                    setCurrentInsertId(idrs.getInt(1));
                }

                context.getConnection().commit();
            }
            else
            {
                try (ResultSet rs = context.getConnection().createStatement().executeQuery(
                            "SELECT RecordsProcessed FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=" + getCurrentInsertId()))
                {
                    rs.next();
                    setSkipEntries(rs.getInt("RecordsProcessed"));
                }
            }
        }
        catch (SQLException e)
        {
            throw new SAXException(e);
        }
    }

    public void endElement(ParseContext context) throws SAXException
    {
        try
        {
            context.insert();
            _finalizeInsertion.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
            _finalizeInsertion.setInt(2, getCurrentInsertId());
            _finalizeInsertion.executeUpdate();
            executeUpdate("DROP TABLE " + _oTableName, context.getConnection());
            executeUpdate("DROP TABLE " + _aTableName, context.getConnection());
            executeUpdate("DROP TABLE " + _sTableName, context.getConnection());
            executeUpdate("DROP TABLE " + _iTableName, context.getConnection());
        }
        catch (SQLException e)
        {
            throw new SAXException(e);
        }

        if (_addOrg != null) { try { _addOrg.close(); } catch (SQLException e) {} }
        if (_addSeq != null) { try { _addSeq.close(); } catch (SQLException e) {} }
        if (_addAnnot != null) { try { _addAnnot.close(); } catch (SQLException e) {} }
        if (_addIdent != null) { try { _addIdent.close(); } catch (SQLException e) {} }

        //c.commit();

        long totalTime = System.currentTimeMillis() - _startTime;
        _log.info("Finished uniprot upload in " + totalTime + " milliseconds");
    }
    
    // All Database Stuff Follows
    // Some day this should be refactored into per-DBM modules

    private String _oTableName = null;
    private String _sTableName = null;
    private String _iTableName = null;
    private String _aTableName = null;
    private PreparedStatement _addOrg = null;
    private PreparedStatement _addSeq = null;
    private PreparedStatement _addAnnot = null;
    private PreparedStatement _addIdent = null;
    private String _insertIntoOrgCommand = null;
    private String _deleteFromTmpOrgCommand = null;
    private String _insertOrgIDCommand = null;
    private String _updateOrgCommand = null;
    private String _insertIntoSeqCommand = null;
    private String _clearExistingIdentifiersCommand;
    private String _clearExistingAnnotationsCommand;
    private String _updateSeqTableCommand = null;
    private String _insertIdentTypesCommand = null;
    private String _insertIntoIdentsCommand = null;
    private String _insertInfoSourceFromSeqCommand = null;
    private String _insertAnnotTypesCommand = null;
    private String _insertIntoAnnotsCommand = null;
    private String _updateAnnotsWithSeqsCommand = null;
    private String _updateAnnotsWithIdentsCommand = null;
    private String _updateIdentsWithSeqsCommand = null;
    private PreparedStatement _initialInsertion = null;
    private PreparedStatement _updateInsertion = null;
    private PreparedStatement _finalizeInsertion;
    private PreparedStatement _getCurrentInsertStats = null;

    private void setupNames(Connection c) throws SQLException
    {
        int randomTableSuffix = (new Random().nextInt(1000000000));
        _oTableName = _dialect.getTempTablePrefix() + "organism" + randomTableSuffix;
        String createOTableCommand = "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + _oTableName + " ( " +
                "common_name varchar(100) NULL, " +
                "genus varchar(100), " +
                "species varchar(100), " +
                "comments varchar(200) NULL, " +
                "identID varchar(50), " +
                "entry_date " + _dialect.getDefaultDateTimeDataType() + " NULL " +
                ")";
        executeUpdate(createOTableCommand, c);
        _sTableName = _dialect.getTempTablePrefix() + "sequences" + randomTableSuffix;
        String createSTableCommand =
                "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + _sTableName + " ( " +
                        "ProtSequence text NULL , " +
                        "hash varchar(100) NULL , " +
                        "description varchar(200) NULL ," +
                        "source_change_date " + _dialect.getDefaultDateTimeDataType() + " NULL ," +
                        "source_insert_date " + _dialect.getDefaultDateTimeDataType() + " NULL ," +
                        "genus varchar(100) NULL, " +
                        "species varchar(100) NULL, " +
                        "mass float NULL , " +
                        "length int NULL ," +
                        "best_name varchar(50) NULL, " +
                        "source varchar(50) NULL," +
                        "best_gene_name varchar(50) NULL, " +
                        "entry_date " + _dialect.getDefaultDateTimeDataType() + " NULL" +
                        ")";
        executeUpdate(createSTableCommand, c);
        _iTableName = _dialect.getTempTablePrefix() + "identifiers" + randomTableSuffix;
        String createITableCommand = "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + _iTableName + " ( " +
                "identifier varchar(50)  NOT NULL, " +
                "identType varchar(50) NULL, " +
                "genus varchar(100) NULL, " +
                "species varchar(100) NULL, " +
                "hash varchar(100) NULL, " +
                "seq_id int NULL, " +
                "entry_date " + _dialect.getDefaultDateTimeDataType() + " NULL" +
                ")";
        executeUpdate(createITableCommand, c);
        _aTableName = _dialect.getTempTablePrefix() + "annotations" + randomTableSuffix;
        String createATableCommand =
                "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + _aTableName + " ( " +
                        "annot_val varchar(200) NOT NULL, " +
                        "annotType varchar(50) NULL, " +
                        "genus varchar(100) NULL, " +
                        "species varchar(100) NULL, " +
                        "hash varchar(100) NULL, " +
                        "seq_id int NULL, " +
                        "start_pos int NULL, " +
                        "end_pos int NULL, " +
                        "identifier varchar(50) NULL," +
                        "identType varchar(50) NULL, " +
                        "ident_id int NULL, " +
                        "entry_date " + _dialect.getDefaultDateTimeDataType() + " NULL" +
                        ")";
        executeUpdate(createATableCommand, c);

        _addOrg = c.prepareStatement(
                "INSERT INTO " + _oTableName + " (common_name,genus,species,comments,identID,entry_date) " +
                        " VALUES (?,?,?,?,?,?) "
        );
        _addSeq = c.prepareStatement(
                "INSERT INTO " + _sTableName +
                        " (ProtSequence,hash,description,source_change_date,source_insert_date,genus,species," +
                        "  mass,length,source,best_name,best_gene_name,entry_date) " +
                        " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) "
        );
        _addIdent = c.prepareStatement(
                "INSERT INTO " + _iTableName + " (identifier,identType,genus,species,hash,entry_date)" +
                        " VALUES (?,?,?,?,?,?)");
        _addAnnot = c.prepareStatement(
                "INSERT INTO " + _aTableName + " (annot_val,annotType,genus,species,hash,start_pos,end_pos,identifier,identType,entry_date)" +
                        " VALUES (?,?,?,?,?,?,?,?,?,?)"
        );

        _insertIntoOrgCommand =
                "INSERT INTO " + ProteinManager.getTableInfoOrganisms() + " (Genus,Species,CommonName,Comments) " +
                        "SELECT genus,species,common_name,comments FROM " + _oTableName +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM " + ProteinManager.getTableInfoOrganisms() + " WHERE " + _oTableName + ".genus = " + ProteinManager.getTableInfoOrganisms() + ".genus AND " +
                        _oTableName + ".species = " + ProteinManager.getTableInfoOrganisms() + ".species)";

        _deleteFromTmpOrgCommand =
                "DELETE FROM " + _oTableName +
                        " WHERE EXISTS (" +
                        "   SELECT * FROM " + ProteinManager.getTableInfoOrganisms() + "," + _oTableName +
                        "      WHERE " + _oTableName + ".genus=" + ProteinManager.getTableInfoOrganisms() + ".genus AND " +
                        _oTableName + ".species=" + ProteinManager.getTableInfoOrganisms() + ".species AND " + ProteinManager.getTableInfoOrganisms() + ".IdentId IS NOT NULL" +
                        " )";
        ResultSet rs = c.createStatement().executeQuery(
                "SELECT IdentTypeId FROM " + ProteinManager.getTableInfoIdentTypes() + " WHERE name='NCBI Taxonomy'"
        );
        rs.next();
        int taxonomyTypeIndex = rs.getInt(1);
        rs.close();
        _insertOrgIDCommand =
                "INSERT INTO " + ProteinManager.getTableInfoIdentifiers() + " (identifier,IdentTypeId,EntryDate) " +
                        "SELECT DISTINCT identID," + taxonomyTypeIndex + ",entry_date " +
                        "FROM " + _oTableName + " " +
                        "WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoIdentifiers() + " WHERE " +
                        "" + ProteinManager.getTableInfoIdentifiers() + ".identifier = " + _oTableName + ".identID AND " +
                        "" + ProteinManager.getTableInfoIdentifiers() + ".identtypeid=" + taxonomyTypeIndex + ")";
        _updateOrgCommand =
                "UPDATE " + ProteinManager.getTableInfoOrganisms() + " SET identid=c.identid " +
                        "FROM " + ProteinManager.getTableInfoOrganisms() + " a," + _oTableName + " b, " + ProteinManager.getTableInfoIdentifiers() + " c " +
                        "WHERE a.genus=b.genus AND a.species=b.species AND " +
                        "  c.identtypeid=" + taxonomyTypeIndex + " AND " +
                        "  c.identifier=b.identID";

        _clearExistingIdentifiersCommand =
                "DELETE FROM " + ProteinManager.getTableInfoIdentifiers() + " WHERE SeqId IN (SELECT seq_id FROM " + _iTableName + ")";

        _clearExistingAnnotationsCommand =
                "DELETE FROM " + ProteinManager.getTableInfoAnnotations() + " WHERE SeqId IN (SELECT seq_id FROM " + _aTableName + ")";

        _insertIntoSeqCommand =
                "INSERT INTO " + ProteinManager.getTableInfoSequences() + " (ProtSequence,hash,description," +
                        "SourceChangeDate,SourceInsertDate,mass,length,OrgId," +
                        "SourceId,BestName,InsertDate,BestGeneName) " +
                        "SELECT a.ProtSequence,a.hash,a.description,a.source_change_date," +
                        "a.source_insert_date,a.mass,a.length,b.OrgId,c.SourceId," +
                        "a.best_name, a.entry_date, a.best_gene_name " +
                        "  FROM " + _sTableName + " a, " + ProteinManager.getTableInfoOrganisms() + " b, " + ProteinManager.getTableInfoInfoSources() + " c " +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM " + ProteinManager.getTableInfoSequences() + " WHERE " +
                        "a.hash = " + ProteinManager.getTableInfoSequences() + ".hash AND b.OrgId=" + ProteinManager.getTableInfoSequences() + ".OrgId AND " +
                        " UPPER(b.genus)=UPPER(a.genus) AND " +
                        " UPPER(a.species)=UPPER(b.species)) AND " +
                        " UPPER(a.species)=UPPER(b.species) AND  " + " " +
                        " UPPER(a.genus)=UPPER(b.genus) AND " +
                        "c.name=a.source";

        _updateSeqTableCommand =
                "UPDATE "+ ProteinManager.getTableInfoSequences() +
                        " SET description=a.description, bestgenename=a.best_gene_name " +
                        " FROM " + _sTableName + " a, "+ProteinManager.getTableInfoOrganisms() + " b " +
                        " WHERE " + ProteinManager.getTableInfoSequences()+".hash = a.hash AND " +
                        ProteinManager.getTableInfoSequences()+".orgid=b.orgid AND UPPER(a.genus)=UPPER(b.genus) AND " +
                        " UPPER(a.species)=UPPER(b.species)";

        _insertIdentTypesCommand =
                "INSERT INTO " + ProteinManager.getTableInfoIdentTypes() + " (name,EntryDate) " +
                        " SELECT DISTINCT a.identType,max(a.entry_date) FROM " +
                        _iTableName + " a WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoIdentTypes() + " " +
                        " WHERE a.identType = " + ProteinManager.getTableInfoIdentTypes() + ".name) GROUP BY a.identType";

        _insertInfoSourceFromSeqCommand =
                "INSERT INTO " + ProteinManager.getTableInfoInfoSources() + " (name,InsertDate) SELECT DISTINCT source,max(entry_date) FROM " +
                        _sTableName + " a WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoInfoSources() + " " +
                        " WHERE a.source = " + ProteinManager.getTableInfoInfoSources() + ".name) GROUP BY a.source";

        _insertIntoIdentsCommand =
                "INSERT INTO " + ProteinManager.getTableInfoIdentifiers() + " " +
                        "  (identifier,IdentTypeId,SeqId,EntryDate) " +
                        "  SELECT DISTINCT b.identifier,a.identtypeid,b.seq_id,max(b.entry_date) " +
                        "  FROM " + ProteinManager.getTableInfoIdentTypes() + " a," + _iTableName + " b " +
                        "  WHERE " +
                        "    a.name = b.identType  AND " +
                        "    NOT EXISTS (" +
                        "       SELECT * FROM " + ProteinManager.getTableInfoIdentifiers() + " c WHERE " +
                        "    a.name = b.identType              AND " +
                        "    c.identtypeid = a.identtypeid AND " +
                        "    b.seq_id=c.seqid                 AND " +
                        "    b.identifier = c.identifier " +
                        "   ) GROUP BY b.identifier,a.identtypeid,b.seq_id";
        _insertAnnotTypesCommand =
                "INSERT INTO " + ProteinManager.getTableInfoAnnotationTypes() + " (name,EntryDate) SELECT DISTINCT a.annotType,max(a.entry_date) FROM " +
                        _aTableName + " a WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoAnnotationTypes() + " " +
                        " WHERE a.annotType = " + ProteinManager.getTableInfoAnnotationTypes() + ".name) GROUP BY a.annotType";

        _insertIntoAnnotsCommand =
                "INSERT INTO " + ProteinManager.getTableInfoAnnotations() + " " +
                        "  (annotval,annottypeid,annotident,seqid,startpos,endpos,insertdate) " +
                        "  SELECT DISTINCT b.annot_val,a.annottypeid, b.ident_id, " +
                        "b.seq_id, b.start_pos, b.end_pos,max(b.entry_date) " +
                        "  FROM " + ProteinManager.getTableInfoAnnotationTypes() + " a," + _aTableName + " b " +
                        "  WHERE " +
                        "    a.name = b.annotType              AND " +
                        "    NOT EXISTS (" +
                        "       SELECT * FROM " + ProteinManager.getTableInfoAnnotations() + " c WHERE " +
                        "    a.name = b.annotType              AND " +
                        "    b.annot_val = c.annotval          AND " +
                        "    b.seq_id = c.seqid                AND " +
                        "    a.annottypeid = c.annottypeid     AND " +
                        "    b.start_pos = c.startpos AND b.end_pos=c.endpos " +
                        "   ) GROUP BY b.annot_val,a.annottypeid,b.ident_id,b.seq_id,b.start_pos,b.end_pos";

        _updateAnnotsWithSeqsCommand =
                "UPDATE " + _aTableName + " SET seq_id = " +
                        "(SELECT c.seqId FROM " +
                        ProteinManager.getTableInfoOrganisms() + " b, " +
                        ProteinManager.getTableInfoSequences() + " c " +
                        " WHERE c.hash=" + _aTableName + ".hash AND " + _aTableName + ".genus=b.genus AND " + _aTableName + ".species=b.species AND b.orgid=c.orgid" +
                        ")"
                ;

        _updateAnnotsWithIdentsCommand =
                "UPDATE " + _aTableName + " SET ident_id = (SELECT DISTINCT b.identID FROM " +
                        ProteinManager.getTableInfoIdentifiers() + " b, " + ProteinManager.getTableInfoIdentTypes() + " c " +
                        " WHERE " + _aTableName + ".seq_id=b.seqid AND " + _aTableName + ".identifier=b.identifier AND " +
                        "  b.identtypeid=c.identtypeid AND " + _aTableName + ".identType=c.name)";

        _updateIdentsWithSeqsCommand =
                "UPDATE " + _iTableName + " SET seq_id = " +
                        "(SELECT c.seqId FROM " +
                        ProteinManager.getTableInfoOrganisms() + " b, " +
                        ProteinManager.getTableInfoSequences() + " c " +
                        " WHERE c.hash=" + _iTableName + ".hash AND " + _iTableName + ".genus=b.genus AND " + _iTableName + ".species=b.species AND b.orgid=c.orgid" +
                        ")";

        SQLFragment initialInsertionCommand = new SQLFragment("INSERT INTO " + ProteinManager.getTableInfoAnnotInsertions() + " (FileName,FileType,Comment,InsertDate) VALUES (?,'uniprot',?,?)");
        _dialect.addReselect(initialInsertionCommand, ProteinManager.getTableInfoAnnotInsertions().getColumn("InsertId"), null);
        _initialInsertion = c.prepareStatement(initialInsertionCommand.getSQL());
        String getCurrentInsertStatsCommand =
                "SELECT SequencesAdded,AnnotationsAdded,IdentifiersAdded,OrganismsAdded,Mouthsful,RecordsProcessed FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=?";
        _getCurrentInsertStats = c.prepareStatement(getCurrentInsertStatsCommand);
        String updateInsertionCommand = "UPDATE " + ProteinManager.getTableInfoAnnotInsertions() + " SET " +
                " Mouthsful=?,SequencesAdded=?,AnnotationsAdded=?,IdentifiersAdded=?,OrganismsAdded=?, " +
                " MRMSequencesAdded=?,MRMAnnotationsAdded=?,MRMIdentifiersAdded=?,MRMOrganismsAdded=?,MRMSize=?," +
                " RecordsProcessed=?,ChangeDate=? " +
                " WHERE InsertId=?";
        _updateInsertion = c.prepareStatement(updateInsertionCommand);
        String finalizeInsertionCommand = "UPDATE " + ProteinManager.getTableInfoAnnotInsertions() + " SET " +
                " CompletionDate=? WHERE InsertId=?";
        _finalizeInsertion = c.prepareStatement(finalizeInsertionCommand);
    }

    protected void handleThreadStateChangeRequests()
    {
/*        if (Thread.currentThread() instanceof XMLProteinLoader.BackgroundAnnotInsertions)
        {
            XMLProteinLoader.BackgroundAnnotInsertions thisThread =
                    (XMLProteinLoader.BackgroundAnnotInsertions) (Thread.currentThread());
            thisThread.getParser().handleThreadStateChangeRequests();
        }
        */
    }

    public void insertTables(ParseContext context, Connection conn) throws SQLException
    {
        conn.setAutoCommit(false);
        handleThreadStateChangeRequests();
        int orgsAdded = insertOrganisms(context, conn);
        handleThreadStateChangeRequests();

        int seqsAdded = insertSequences(context, conn);
        handleThreadStateChangeRequests();
        int identsAdded;
        int annotsAdded;
        try
        {
            int identsProcessed = insertIdentifiers(context, conn);
            _log.debug("Inserted " + identsProcessed + " identifiers into temp table");
            handleThreadStateChangeRequests();

            try
            {
                int annotsProcessed = insertAnnotations(context, conn);
                _log.debug("Inserted " + annotsProcessed + " annotations into temp table");

                identsAdded = mergeIdentifiers(context, conn);
                annotsAdded = mergeAnnotations(context, conn);
            }
            finally
            {
                try { executeUpdate(_dialect.getDropIndexCommand(_aTableName, "aAnnot_val"), conn); } catch (SQLException e) {}
                try { executeUpdate(_dialect.getDropIndexCommand(_aTableName, "aAnnotType"), conn); } catch (SQLException e) {}
                try { executeUpdate(_dialect.getDropIndexCommand(_aTableName, "aHashGenusSpecies"), conn); } catch (SQLException e) {}
                try { executeUpdate("TRUNCATE TABLE " + _aTableName, conn, "TRUNCATE TABLE " + _aTableName); } catch (SQLException e) {}
            }
        }
        finally
        {
            try { executeUpdate(_dialect.getDropIndexCommand(_iTableName, "iIdentifier"), conn); } catch (SQLException e) {}
            try { executeUpdate(_dialect.getDropIndexCommand(_iTableName, "iIdenttype"), conn); } catch (SQLException e) {}
            try { executeUpdate(_dialect.getDropIndexCommand(_iTableName, "iSpeciesGenusHash"), conn); } catch (SQLException e) {}
            try { executeUpdate("TRUNCATE TABLE " + _iTableName, conn, "TRUNCATE TABLE " + _iTableName); } catch (SQLException e) {}
        }

        conn.setAutoCommit(true);
        handleThreadStateChangeRequests();
        _log.info("Batch complete. Added: " +
                orgsAdded + " organisms; " +
                seqsAdded + " sequences; " +
                identsAdded + " identifiers; " +
                annotsAdded + " annotations");
        _getCurrentInsertStats.setInt(1, getCurrentInsertId());
        ResultSet r = _getCurrentInsertStats.executeQuery();
        r.next();
        int priorseqs = r.getInt("SequencesAdded");
        int priorannots = r.getInt("AnnotationsAdded");
        int prioridents = r.getInt("IdentifiersAdded");
        int priororgs = r.getInt("OrganismsAdded");
        int mouthsful = r.getInt("Mouthsful");
        int records = r.getInt("RecordsProcessed");
        r.close();

        int curNRecords = context.getSequences().size();

        _updateInsertion.setInt(1, mouthsful + 1);
        _updateInsertion.setInt(2, priorseqs + seqsAdded);
        _updateInsertion.setInt(3, priorannots + annotsAdded);
        _updateInsertion.setInt(4, prioridents + identsAdded);
        _updateInsertion.setInt(5, priororgs + orgsAdded);
        _updateInsertion.setInt(6, seqsAdded);
        _updateInsertion.setInt(7, annotsAdded);
        _updateInsertion.setInt(8, identsAdded);
        _updateInsertion.setInt(9, orgsAdded);
        _updateInsertion.setInt(10, curNRecords);
        _updateInsertion.setInt(11, records + curNRecords);
        _updateInsertion.setInt(13, getCurrentInsertId());
        _updateInsertion.setTimestamp(12, new java.sql.Timestamp(new java.util.Date().getTime()));
        _updateInsertion.executeUpdate();
        //conn.commit();

        _log.info(
                "Added: " +
                        orgsAdded + " organisms; " +
                        seqsAdded + " sequences; " +
                        identsAdded + " identifiers; " +
                        annotsAdded + " annotations"
        );
    }

    public int insertOrganisms(ParseContext context, Connection conn) throws SQLException
    {
        int transactionCount = 0;
        _addOrg.setTimestamp(6, new Timestamp(new Date().getTime()));

        //Add current mouthful of Organisms
        _log.debug((new java.util.Date()) + " Processing organisms");

        // All organism records.  Each one is a HashMap
        for (UniprotOrganism curOrg : context.getOrganisms())
        {
            transactionCount++;
            if (curOrg.getCommonName() == null)
            {
                _addOrg.setNull(1, Types.VARCHAR);
            }
            else
            {
                _addOrg.setString(1, curOrg.getCommonName());
            }
            _addOrg.setString(2, curOrg.getGenus());
            _addOrg.setString(3, curOrg.getSpecies());
            if (curOrg.getComments() == null)
            {
                _addOrg.setNull(4, Types.VARCHAR);
            }
            else
            {
                _addOrg.setString(4, curOrg.getComments());
            }
            _addOrg.setString(5, curOrg.getIdentID());
            // Timestamp at index 6 is set once for the whole PreparedStatement
            _addOrg.addBatch();
            if (transactionCount == TRANSACTION_ROW_COUNT)
            {
                transactionCount = 0;
                _addOrg.executeBatch();
                conn.commit();
                _addOrg.clearBatch();
            }
            handleThreadStateChangeRequests();
        }

        _addOrg.executeBatch();
        conn.commit();
        handleThreadStateChangeRequests();
        _addOrg.clearBatch();

        // Insert Organisms into real table
        int result = executeUpdate(_insertIntoOrgCommand, conn);
        //get rid of previously entered vals in temp table
        executeUpdate(_deleteFromTmpOrgCommand, conn);

        //insert identifiers associated with the organism
        executeUpdate(_insertOrgIDCommand, conn);

        // update missing ident_ids in newly inserted organism records
        executeUpdate(_updateOrgCommand, conn);

        executeUpdate("TRUNCATE TABLE " + _oTableName, conn);

        return result;
    }

    public int insertSequences(ParseContext context, Connection conn) throws SQLException
    {
        int transactionCount = 0;

        _addSeq.setTimestamp(13, new Timestamp(new Date().getTime()));

        //Process current mouthful of sequences
        _log.debug(new java.util.Date() + " Processing sequences");
        for (UniprotSequence curSeq : context.getSequences())
        {
            transactionCount++;
            _addSeq.setString(1, curSeq.getProtSequence());
            _addSeq.setString(2, curSeq.getHash());
            if (curSeq.getDescription() == null)
            {
                _addSeq.setNull(3, Types.VARCHAR);
            }
            else
            {
                String tmp = curSeq.getDescription();
                if (tmp.length() >= 200) tmp = tmp.substring(0, 190) + "...";
                _addSeq.setString(3, tmp);
            }
            if (curSeq.getSourceChangeDate() == null)
            {
                _addSeq.setNull(4, Types.TIMESTAMP);
            }
            else
            {
                _addSeq.setTimestamp(4, curSeq.getSourceChangeDate());
            }
            if (curSeq.getSourceInsertDate() == null)
            {
                _addSeq.setNull(5, Types.TIMESTAMP);
            }
            else
            {
                _addSeq.setTimestamp(5, curSeq.getSourceInsertDate());
            }
            _addSeq.setString(6, curSeq.getGenus());
            _addSeq.setString(7, curSeq.getSpecies());
            if (curSeq.getMass() == null)
            {
                _addSeq.setNull(8, Types.FLOAT);
            }
            else
            {
                _addSeq.setFloat(8, curSeq.getMass());
            }
            if (curSeq.getLength() == null)
            {
                _addSeq.setNull(9, Types.INTEGER);
            }
            else
            {
                _addSeq.setInt(9, curSeq.getLength());
            }
            if (curSeq.getSource() == null)
            {
                _addSeq.setNull(10, Types.VARCHAR);
            }
            else
            {
                _addSeq.setString(10, curSeq.getSource());
            }
            if (curSeq.getBestName() == null)
            {
                _addSeq.setNull(11, Types.VARCHAR);
            }
            else
            {
                String tmp = curSeq.getBestName();
                if (tmp.length() >= 50) tmp = tmp.substring(0, 45) + "...";
                _addSeq.setString(11, tmp);
            }
            if (curSeq.getBestGeneName() == null)
            {
                _addSeq.setNull(12, Types.VARCHAR);
            }
            else
            {
                String tmp = curSeq.getBestGeneName();
                if (tmp.length() >= 50) tmp = tmp.substring(0, 45) + "...";
                _addSeq.setString(12, tmp);
            }
            // Timestamp at index 13 is set once for the whole prepared statement
            _addSeq.addBatch();
            if (transactionCount == TRANSACTION_ROW_COUNT)
            {
                transactionCount = 0;
                _addSeq.executeBatch();
                conn.commit();
                _addSeq.clearBatch();
            }
            handleThreadStateChangeRequests();
        }

        _addSeq.executeBatch();
        handleThreadStateChangeRequests();
        conn.commit();
        _addSeq.clearBatch();

        executeUpdate(_insertInfoSourceFromSeqCommand, conn);

        executeUpdate(_updateSeqTableCommand, conn);

        int result = executeUpdate(_insertIntoSeqCommand, conn);

        executeUpdate("TRUNCATE TABLE " + _sTableName, conn);

        return result;
    }

    public int insertIdentifiers(ParseContext context, Connection conn) throws SQLException
    {
        int transactionCount = 0;

        // Process current mouthful of identifiers
        _log.debug(new java.util.Date() + " Processing identifiers");
        _addIdent.setTimestamp(6, new java.sql.Timestamp(new java.util.Date().getTime()));
        for (UniprotIdentifier curIdent : context.getIdentifiers())
        {
            transactionCount++;
            String curIdentVal = curIdent.getIdentifier();
            if (curIdentVal.length() > 50) curIdentVal = curIdentVal.substring(0, 45) + "...";
            _addIdent.setString(1, curIdentVal);
            _addIdent.setString(2, curIdent.getIdentType());
            UniprotSequence curSeq = curIdent.getSequence();
            _addIdent.setString(3, curSeq.getGenus());
            _addIdent.setString(4, curSeq.getSpecies());
            _addIdent.setString(5, curSeq.getHash());
            // Timestamp at index 6 is set once for the whole PreparedStatement
            _addIdent.addBatch();
            if (transactionCount == TRANSACTION_ROW_COUNT)
            {
                transactionCount = 0;
                _addIdent.executeBatch();
                conn.commit();
                _addIdent.clearBatch();
            }
            handleThreadStateChangeRequests();
        }

        _addIdent.executeBatch();
        handleThreadStateChangeRequests();
        conn.commit();
        _addIdent.clearBatch();

        _log.debug("Starting to create indices on " + _iTableName);
        executeUpdate("create index iIdentifier on " + _iTableName + "(Identifier)", conn);
        executeUpdate("create index iIdenttype on " + _iTableName + "(IdentType)", conn);
        executeUpdate("create index iSpeciesGenusHash on " + _iTableName + "(Species, Genus, Hash)", conn);

        executeUpdate(_dialect.getAnalyzeCommandForTable(_iTableName), conn, "Analyzing " + _iTableName);

        // Insert ident types
        executeUpdate(_insertIdentTypesCommand, conn, "InsertIdentTypes");

        executeUpdate(_updateIdentsWithSeqsCommand, conn, "UpdateIdentsWithSeqs");
        return context.getIdentifiers().size();
    }

    private int mergeIdentifiers(ParseContext context, Connection conn) throws SQLException
    {
        if (context.isClearExisting())
        {
            // Clear these after the annotations that reference them have already been deleted
            int identifiersDeleted = executeUpdate(_clearExistingIdentifiersCommand, conn, "DeleteExistingIdents");
            _log.debug("Deleted " + identifiersDeleted + " existing identifiers");
        }

        int result = executeUpdate(_insertIntoIdentsCommand, conn, "InsertIntoIdents");

        _log.debug("Done with identifiers");
        return result;
    }

    private int executeUpdate(String sql, Connection conn, String description) throws SQLException
    {
        long startTime = System.currentTimeMillis();
        int result = executeUpdate(sql, conn);
        long totalTime = System.currentTimeMillis() - startTime;
        _log.debug(description + " took " + totalTime + " milliseconds");
        return result;
    }

    private int executeUpdate(String sql, Connection conn) throws SQLException
    {
        handleThreadStateChangeRequests();
        PreparedStatement stmt = null;
        try
        {
            stmt = conn.prepareStatement(sql);
            int result = stmt.executeUpdate();
            if (!conn.getAutoCommit())
            {
                conn.commit();
            }
            return result;
        }
        finally
        {
            if (stmt != null) { try { stmt.close(); } catch (SQLException e) {} }
        }
    }

    private static final int MAX_ANNOT_SIZE = 190;

    public int insertAnnotations(ParseContext context, Connection conn) throws SQLException
    {
        int transactionCount = 0;
        // Process current mouthful of identifiers

        _addAnnot.setTimestamp(10, new java.sql.Timestamp(new java.util.Date().getTime()));
        transactionCount++;
        _log.debug(new java.util.Date() + " Processing annotations");
        for (UniprotAnnotation curAnnot : context.getAnnotations())
        {
            String annotVal = curAnnot.getAnnotVal();
            if (annotVal.length() > MAX_ANNOT_SIZE)
                annotVal = annotVal.substring(0, MAX_ANNOT_SIZE) + "...";
            _addAnnot.setString(1, annotVal);
            _addAnnot.setString(2, curAnnot.getAnnotType());
            UniprotSequence curSeq = curAnnot.getSequence();
            _addAnnot.setString(3, curSeq.getGenus());
            _addAnnot.setString(4, curSeq.getSpecies());
            _addAnnot.setString(5, curSeq.getHash());
            if (curAnnot.getStartPos() == null)
            {
                _addAnnot.setInt(6, 0);
            }
            else
            {
                _addAnnot.setInt(6, curAnnot.getStartPos().intValue());
            }
            if (curAnnot.getEndPos() == null)
            {
                _addAnnot.setInt(7, 0);
            }
            else
            {
                _addAnnot.setInt(7, curAnnot.getEndPos().intValue());
            }
            UniprotIdentifier ident = curAnnot.getIdentifier();
            if (ident == null)
            {
                _addAnnot.setNull(8, Types.VARCHAR);
                _addAnnot.setNull(9, Types.VARCHAR);
            }
            else
            {
                _addAnnot.setString(8, ident.getIdentifier());
                _addAnnot.setString(9, ident.getIdentType());
            }

            // Timestamp at index 10 is set once for the whole PreparedStatement
            _addAnnot.addBatch();
            if (transactionCount == TRANSACTION_ROW_COUNT)
            {
                transactionCount = 0;
                _addAnnot.executeBatch();
                conn.commit();
                _addAnnot.clearBatch();
            }
            handleThreadStateChangeRequests();
        }

        _addAnnot.executeBatch();
        conn.commit();
        handleThreadStateChangeRequests();
        _addAnnot.clearBatch();

        _log.debug("Starting to create indices on " + _aTableName);
        executeUpdate("create index aAnnot_val on " + _aTableName + "(Annot_Val)", conn);
        executeUpdate("create index aAnnotType on " + _aTableName + "(AnnotType)", conn);
        executeUpdate("create index aHashGenusSpecies on " + _aTableName + "(Hash, Genus, Species)", conn);

        executeUpdate(_dialect.getAnalyzeCommandForTable(_aTableName), conn, "Analyzing " + _aTableName);

        // Insert ident types
        executeUpdate(_insertAnnotTypesCommand, conn, "InsertAnnotTypes");

        executeUpdate(_updateAnnotsWithSeqsCommand, conn, "UpdateAnnotsWithSeqs");

        if (context.isClearExisting())
        {
            int annotationsDeleted = executeUpdate(_clearExistingAnnotationsCommand, conn, "DeleteExistingAnnots");
            _log.debug("Deleted " + annotationsDeleted + " existing annotations.");
        }

        return context.getAnnotations().size();
    }

    private int mergeAnnotations(ParseContext context, Connection conn) throws SQLException
    {
        executeUpdate(_updateAnnotsWithIdentsCommand, conn, "UpdateAnnotsWithIdents");

        int result = executeUpdate(_insertIntoAnnotsCommand, conn, "InsertIntoAnnots");

        _log.debug("Done with annotations");
        return result;
    }
}
