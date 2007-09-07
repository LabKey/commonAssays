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

package org.labkey.ms2.protein.uniprot;

import org.apache.log4j.Logger;
import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.SqlDialect;
import org.labkey.api.util.DateUtil;
import org.labkey.ms2.protein.ParseActions;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.XMLProteinHandler;
import org.labkey.ms2.protein.XMLProteinLoader;
import org.xml.sax.Attributes;

import java.sql.*;
import java.util.Date;
import java.util.*;

// Repository for routines specific to org.fhcrc.edi.protein.uniprot.uniprot parsing
// And initial stuff to do with the root element

public class uniprot extends ParseActions
{

    private static Logger _log = Logger.getLogger(XMLProteinLoader.class);
    private static SqlDialect _dialect = CoreSchema.getInstance().getSqlDialect();
    private long _startTime;

    private static final int TRANSACTION_ROW_COUNT = 100;

    // n of top-level "entries" to skip
    protected int skipEntries = 0;

    public void setSkipEntries(int s)
    {
        this.skipEntries = s;
    }

    public int getSkipEntries()
    {
        return this.skipEntries;
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

    public boolean beginElement(Connection c, Map<String,ParseActions> tables, Attributes attrs)
    {
        _startTime = System.currentTimeMillis();
        accumulated = null;
        this.clearCurItems();
        tables.put("UniprotRoot", this);
        tables.put("ProtIdentifiers", this);
        tables.put("ProtAnnotations", this);
        // Annotations and Identifiers are Vectors of Maps
        // The Vectors get cleared by insertTables
        this.getCurItem().put("Identifiers", new Vector());
        this.getCurItem().put("Annotations", new Vector());
        try
        {
            setupNames(c);
            if (getCurrentInsertId() == 0)
            {
                InitialInsertion.setString(1, this.getWhatImParsing());
                if (this.getComment() == null) this.setComment("");
                InitialInsertion.setString(2, this.getComment());
                InitialInsertion.setTimestamp(3, new java.sql.Timestamp(new java.util.Date().getTime()));
                InitialInsertion.executeUpdate();
                //c.commit();
                ResultSet idrs =
                        c.createStatement().executeQuery(_dialect.appendSelectAutoIncrement("", ProteinManager.getTableInfoAnnotInsertions(), "InsertId"));

                idrs.next();
                setCurrentInsertId(idrs.getInt(1));
                idrs.close();
            }
            else
            {
                ResultSet rs =
                        c.createStatement().executeQuery(
                                "SELECT RecordsProcessed FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=" + getCurrentInsertId()
                        );
                rs.next();
                setSkipEntries(rs.getInt("RecordsProcessed"));
                rs.close();
            }
            Thread.currentThread().setName("AnnotLoader" + getCurrentInsertId());
            //c.commit();
        }
        catch (Exception e)
        {
            XMLProteinHandler.parseError("Can't initialize insertion table entry: " + e);
        }
        return true;
    }

    public boolean endElement(Connection c, Map<String,ParseActions> tables)
    {
        try
        {
            insertTables(tables, c);
            FinalizeInsertion.setTimestamp(1, new java.sql.Timestamp(new java.util.Date().getTime()));
            FinalizeInsertion.setInt(2, getCurrentInsertId());
            FinalizeInsertion.executeUpdate();
            executeUpdate("DROP TABLE " + OTableName, c);
            executeUpdate("DROP TABLE " + ATableName, c);
            executeUpdate("DROP TABLE " + STableName, c);
            executeUpdate("DROP TABLE " + ITableName, c);

            //c.commit();
        }
        catch (Exception e)
        {
            XMLProteinHandler.parseError("Final table insert failed in uniprot's endElement: " + e);
        }
        long totalTime = System.currentTimeMillis() - _startTime;
        _log.info("Finished uniprot upload in " + totalTime + " milliseconds");
        return true;
    }

    public boolean characters(Connection c, Map<String,ParseActions> tables, char ch[], int start, int len)
    {
        return true;
    }

    // All Database Stuff Follows
    // Some day this should be refactored into per-DBM modules

    private static String OTableName = null;
    private static String STableName = null;
    private static String ITableName = null;
    private static String ATableName = null;
    private static PreparedStatement addOrg = null;
    private static PreparedStatement addSeq = null;
    private static PreparedStatement addAnnot = null;
    private static PreparedStatement addIdent = null;
    private static String InsertIntoOrgCommand = null;
    private static String DeleteFromTmpOrgCommand = null;
    private static String InsertOrgIDCommand = null;
    private static String UpdateOrgCommand = null;
    private static String InsertIntoSeqCommand = null;
    private static String UpdateSeqTableCommand = null;
    private static String InsertIdentTypesCommand = null;
    private static String InsertIntoIdentsCommand = null;
    private static String InsertInfoSourceFromSeqCommand = null;
    private static String InsertAnnotTypesCommand = null;
    private static String InsertIntoAnnotsCommand = null;
    private static String UpdateAnnotsWithSeqsCommand = null;
    private static String UpdateAnnotsWithIdentsCommand = null;
    private static String UpdateIdentsWithSeqsCommand = null;
    private static PreparedStatement InitialInsertion = null;
    private static PreparedStatement UpdateInsertion = null;
    private static PreparedStatement FinalizeInsertion;
    private static PreparedStatement GetCurrentInsertStats = null;

    private void setupNames(Connection c) throws Exception
    {
        int randomTableSuffix = (new Random().nextInt(1000000000));
        OTableName = _dialect.getTempTablePrefix() + "organism" + randomTableSuffix;
        String createOTableCommand = "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + OTableName + " ( " +
                "common_name varchar(50) NULL, " +
                "genus varchar(100), " +
                "species varchar(100), " +
                "comments varchar(200) NULL, " +
                "identID varchar(50), " +
                "entry_date " + _dialect.getDefaultDateTimeDatatype() + " NULL " +
                ")";
        executeUpdate(createOTableCommand, c);
        STableName = _dialect.getTempTablePrefix() + "sequences" + randomTableSuffix;
        String createSTableCommand =
                "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + STableName + " ( " +
                        "ProtSequence text NULL , " +
                        "hash varchar(100) NULL , " +
                        "description varchar(200) NULL ," +
                        "source_change_date " + _dialect.getDefaultDateTimeDatatype() + " NULL ," +
                        "source_insert_date " + _dialect.getDefaultDateTimeDatatype() + " NULL ," +
                        "genus varchar(100) NULL, " +
                        "species varchar(100) NULL, " +
                        "mass float NULL , " +
                        "length int NULL ," +
                        "best_name varchar(50) NULL, " +
                        "source varchar(50) NULL," +
                        "best_gene_name varchar(50) NULL, " +
                        "entry_date " + _dialect.getDefaultDateTimeDatatype() + " NULL" +
                        ")";
        executeUpdate(createSTableCommand, c);
        ITableName = _dialect.getTempTablePrefix() + "identifiers" + randomTableSuffix;
        String createITableCommand = "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + ITableName + " ( " +
                "identifier varchar(50)  NOT NULL, " +
                "identType varchar(50) NULL, " +
                "genus varchar(100) NULL, " +
                "species varchar(100) NULL, " +
                "hash varchar(100) NULL, " +
                "seq_id int NULL, " +
                "entry_date " + _dialect.getDefaultDateTimeDatatype() + " NULL" +
                ")";
        executeUpdate(createITableCommand, c);
        ATableName = _dialect.getTempTablePrefix() + "annotations" + randomTableSuffix;
        String createATableCommand =
                "CREATE " + _dialect.getTempTableKeyword() + " TABLE " + ATableName + " ( " +
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
                        "entry_date " + _dialect.getDefaultDateTimeDatatype() + " NULL" +
                        ")";
        executeUpdate(createATableCommand, c);

        addOrg = c.prepareStatement(
                "INSERT INTO " + OTableName + " (common_name,genus,species,comments,identID,entry_date) " +
                        " VALUES (?,?,?,?,?,?) "
        );
        addSeq = c.prepareStatement(
                "INSERT INTO " + STableName +
                        " (ProtSequence,hash,description,source_change_date,source_insert_date,genus,species," +
                        "  mass,length,source,best_name,best_gene_name,entry_date) " +
                        " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) "
        );
        addIdent = c.prepareStatement(
                "INSERT INTO " + ITableName + " (identifier,identType,genus,species,hash,entry_date)" +
                        " VALUES (?,?,?,?,?,?)");
        addAnnot = c.prepareStatement(
                "INSERT INTO " + ATableName + " (annot_val,annotType,genus,species,hash,start_pos,end_pos,identifier,identType,entry_date)" +
                        " VALUES (?,?,?,?,?,?,?,?,?,?)"
        );

        InsertIntoOrgCommand =
                "INSERT INTO " + ProteinManager.getTableInfoOrganisms() + " (Genus,Species,CommonName,Comments) " +
                        "SELECT genus,species,common_name,comments FROM " + OTableName +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM " + ProteinManager.getTableInfoOrganisms() + " WHERE " + OTableName + ".genus = " + ProteinManager.getTableInfoOrganisms() + ".genus AND " +
                        OTableName + ".species = " + ProteinManager.getTableInfoOrganisms() + ".species)";

        DeleteFromTmpOrgCommand =
                "DELETE FROM " + OTableName +
                        " WHERE EXISTS (" +
                        "   SELECT * FROM " + ProteinManager.getTableInfoOrganisms() + "," + OTableName +
                        "      WHERE " + OTableName + ".genus=" + ProteinManager.getTableInfoOrganisms() + ".genus AND " +
                        OTableName + ".species=" + ProteinManager.getTableInfoOrganisms() + ".species AND " + ProteinManager.getTableInfoOrganisms() + ".IdentId IS NOT NULL" +
                        " )";
        ResultSet rs = c.createStatement().executeQuery(
                "SELECT IdentTypeId FROM " + ProteinManager.getTableInfoIdentTypes() + " WHERE name='NCBI Taxonomy'"
        );
        rs.next();
        int taxonomyTypeIndex = rs.getInt(1);
        rs.close();
        InsertOrgIDCommand =
                "INSERT INTO " + ProteinManager.getTableInfoIdentifiers() + " (identifier,IdentTypeId,EntryDate) " +
                        "SELECT DISTINCT identID," + taxonomyTypeIndex + ",entry_date " +
                        "FROM " + OTableName + " " +
                        "WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoIdentifiers() + " WHERE " +
                        "" + ProteinManager.getTableInfoIdentifiers() + ".identifier = " + OTableName + ".identID AND " +
                        "" + ProteinManager.getTableInfoIdentifiers() + ".identtypeid=" + taxonomyTypeIndex + ")";
        UpdateOrgCommand =
                "UPDATE " + ProteinManager.getTableInfoOrganisms() + " SET identid=c.identid " +
                        "FROM " + ProteinManager.getTableInfoOrganisms() + " a," + OTableName + " b, " + ProteinManager.getTableInfoIdentifiers() + " c " +
                        "WHERE a.genus=b.genus AND a.species=b.species AND " +
                        "  c.identtypeid=" + taxonomyTypeIndex + " AND " +
                        "  c.identifier=b.identID";
        InsertIntoSeqCommand =
                "INSERT INTO " + ProteinManager.getTableInfoSequences() + " (ProtSequence,hash,description," +
                        "SourceChangeDate,SourceInsertDate,mass,length,OrgId," +
                        "SourceId,BestName,InsertDate,BestGeneName) " +
                        "SELECT a.ProtSequence,a.hash,a.description,a.source_change_date," +
                        "a.source_insert_date,a.mass,a.length,b.OrgId,c.SourceId," +
                        "a.best_name, a.entry_date, a.best_gene_name " +
                        "  FROM " + STableName + " a, " + ProteinManager.getTableInfoOrganisms() + " b, " + ProteinManager.getTableInfoInfoSources() + " c " +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM " + ProteinManager.getTableInfoSequences() + " WHERE " +
                        "a.hash = " + ProteinManager.getTableInfoSequences() + ".hash AND b.OrgId=" + ProteinManager.getTableInfoSequences() + ".OrgId AND " +
                        " UPPER(b.genus)=UPPER(a.genus) AND " +
                        " UPPER(a.species)=UPPER(b.species)) AND " +
                        " UPPER(a.species)=UPPER(b.species) AND  " + " " +
                        " UPPER(a.genus)=UPPER(b.genus) AND " +
                        "c.name=a.source";

        UpdateSeqTableCommand =
                "UPDATE "+ ProteinManager.getTableInfoSequences() +
                        " SET description=a.description, bestname=a.best_name, bestgenename=a.best_gene_name " +
                        " FROM " + STableName + " a, "+ProteinManager.getTableInfoOrganisms() + " b " +
                        " WHERE " + ProteinManager.getTableInfoSequences()+".hash = a.hash AND " +
                        ProteinManager.getTableInfoSequences()+".orgid=b.orgid AND UPPER(a.genus)=UPPER(b.genus) AND " +
                        " UPPER(a.species)=UPPER(b.species)";

        InsertIdentTypesCommand =
                "INSERT INTO " + ProteinManager.getTableInfoIdentTypes() + " (name,EntryDate) " +
                        " SELECT DISTINCT a.identType,max(a.entry_date) FROM " +
                        ITableName + " a WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoIdentTypes() + " " +
                        " WHERE a.identType = " + ProteinManager.getTableInfoIdentTypes() + ".name) GROUP BY a.identType";

        InsertInfoSourceFromSeqCommand =
                "INSERT INTO " + ProteinManager.getTableInfoInfoSources() + " (name,InsertDate) SELECT DISTINCT source,max(entry_date) FROM " +
                        STableName + " a WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoInfoSources() + " " +
                        " WHERE a.source = " + ProteinManager.getTableInfoInfoSources() + ".name) GROUP BY a.source";

        InsertIntoIdentsCommand =
                "INSERT INTO " + ProteinManager.getTableInfoIdentifiers() + " " +
                        "  (identifier,IdentTypeId,SeqId,EntryDate) " +
                        "  SELECT DISTINCT b.identifier,a.identtypeid,b.seq_id,max(b.entry_date) " +
                        "  FROM " + ProteinManager.getTableInfoIdentTypes() + " a," + ITableName + " b " +
                        "  WHERE " +
                        "    a.name = b.identType  AND " +
                        "    NOT EXISTS (" +
                        "       SELECT * FROM " + ProteinManager.getTableInfoIdentifiers() + " c WHERE " +
                        "    a.name = b.identType              AND " +
                        "    c.identtypeid = a.identtypeid AND " +
                        "    b.seq_id=c.seqid                 AND " +
                        "    b.identifier = c.identifier " +
                        "   ) GROUP BY b.identifier,a.identtypeid,b.seq_id";
        InsertAnnotTypesCommand =
                "INSERT INTO " + ProteinManager.getTableInfoAnnotationTypes() + " (name,EntryDate) SELECT DISTINCT a.annotType,max(a.entry_date) FROM " +
                        ATableName + " a WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoAnnotationTypes() + " " +
                        " WHERE a.annotType = " + ProteinManager.getTableInfoAnnotationTypes() + ".name) GROUP BY a.annotType";

        InsertIntoAnnotsCommand =
                "INSERT INTO " + ProteinManager.getTableInfoAnnotations() + " " +
                        "  (annotval,annottypeid,annotident,seqid,startpos,endpos,insertdate) " +
                        "  SELECT DISTINCT b.annot_val,a.annottypeid, b.ident_id, " +
                        "b.seq_id, b.start_pos, b.end_pos,max(b.entry_date) " +
                        "  FROM " + ProteinManager.getTableInfoAnnotationTypes() + " a," + ATableName + " b " +
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

        UpdateAnnotsWithSeqsCommand =
                "UPDATE " + ATableName + " SET seq_id = " +
                        "(SELECT c.seqId FROM " +
                        ProteinManager.getTableInfoOrganisms() + " b, " +
                        ProteinManager.getTableInfoSequences() + " c " +
                        " WHERE c.hash=" + ATableName + ".hash AND " + ATableName + ".genus=b.genus AND " + ATableName + ".species=b.species AND b.orgid=c.orgid" +
                        ")"
                ;

        UpdateAnnotsWithIdentsCommand =
                "UPDATE " + ATableName + " SET ident_id = (SELECT DISTINCT b.identID FROM " +
                        ProteinManager.getTableInfoIdentifiers() + " b, " + ProteinManager.getTableInfoIdentTypes() + " c " +
                        " WHERE " + ATableName + ".seq_id=b.seqid AND " + ATableName + ".identifier=b.identifier AND " +
                        "  b.identtypeid=c.identtypeid AND " + ATableName + ".identType=c.name)";

        UpdateIdentsWithSeqsCommand =
                "UPDATE " + ITableName + " SET seq_id = " +
                        "(SELECT c.seqId FROM " +
                        ProteinManager.getTableInfoOrganisms() + " b, " +
                        ProteinManager.getTableInfoSequences() + " c " +
                        " WHERE c.hash=" + ITableName + ".hash AND " + ITableName + ".genus=b.genus AND " + ITableName + ".species=b.species AND b.orgid=c.orgid" +
                        ")"
                ;

        String initialInsertionCommand = "INSERT INTO " + ProteinManager.getTableInfoAnnotInsertions() + " (FileName,FileType,Comment,InsertDate) VALUES (?,'uniprot',?,?)";
        InitialInsertion = c.prepareStatement(initialInsertionCommand);
        String getCurrentInsertStatsCommand =
                "SELECT SequencesAdded,AnnotationsAdded,IdentifiersAdded,OrganismsAdded,Mouthsful,RecordsProcessed FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=?";
        GetCurrentInsertStats = c.prepareStatement(getCurrentInsertStatsCommand);
        String updateInsertionCommand = "UPDATE " + ProteinManager.getTableInfoAnnotInsertions() + " SET " +
                " Mouthsful=?,SequencesAdded=?,AnnotationsAdded=?,IdentifiersAdded=?,OrganismsAdded=?, " +
                " MRMSequencesAdded=?,MRMAnnotationsAdded=?,MRMIdentifiersAdded=?,MRMOrganismsAdded=?,MRMSize=?," +
                " RecordsProcessed=?,ChangeDate=? " +
                " WHERE InsertId=?";
        UpdateInsertion = c.prepareStatement(updateInsertionCommand);
        String finalizeInsertionCommand = "UPDATE " + ProteinManager.getTableInfoAnnotInsertions() + " SET " +
                " CompletionDate=? WHERE InsertId=?";
        FinalizeInsertion = c.prepareStatement(finalizeInsertionCommand);
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

    public void insertTables(Map<String, ParseActions> tables, Connection conn)
    {

        try
        {
            conn.setAutoCommit(false);
            handleThreadStateChangeRequests();
            int orgsAdded = insertOrganisms(tables, conn);
            handleThreadStateChangeRequests();

            int seqsAdded = insertSequences(tables, conn);
            handleThreadStateChangeRequests();
            int identsAdded = insertIdentifiers(tables, conn);
            handleThreadStateChangeRequests();

            int annotsAdded = insertAnnotations(tables, conn);
            conn.setAutoCommit(true);
            handleThreadStateChangeRequests();
            _log.info(new java.util.Date() + " Added: " +
                    orgsAdded + " organisms; " +
                    seqsAdded + " sequences; " +
                    identsAdded + " identifiers; " +
                    annotsAdded + " annotations");
            _log.info(new java.util.Date() + " This batch of records processed successfully");
            GetCurrentInsertStats.setInt(1, getCurrentInsertId());
            ResultSet r = GetCurrentInsertStats.executeQuery();
            r.next();
            int priorseqs = r.getInt("SequencesAdded");
            int priorannots = r.getInt("AnnotationsAdded");
            int prioridents = r.getInt("IdentifiersAdded");
            int priororgs = r.getInt("OrganismsAdded");
            int mouthsful = r.getInt("Mouthsful");
            int records = r.getInt("RecordsProcessed");
            r.close();

            ParseActions p = tables.get("ProtSequences");
            int curNRecords = p.getAllItems().size();

            UpdateInsertion.setInt(1, mouthsful + 1);
            UpdateInsertion.setInt(2, priorseqs + seqsAdded);
            UpdateInsertion.setInt(3, priorannots + annotsAdded);
            UpdateInsertion.setInt(4, prioridents + identsAdded);
            UpdateInsertion.setInt(5, priororgs + orgsAdded);
            UpdateInsertion.setInt(6, seqsAdded);
            UpdateInsertion.setInt(7, annotsAdded);
            UpdateInsertion.setInt(8, identsAdded);
            UpdateInsertion.setInt(9, orgsAdded);
            UpdateInsertion.setInt(10, curNRecords);
            UpdateInsertion.setInt(11, records + curNRecords);
            UpdateInsertion.setInt(13, getCurrentInsertId());
            UpdateInsertion.setTimestamp(12, new java.sql.Timestamp(new java.util.Date().getTime()));
            UpdateInsertion.executeUpdate();
            //conn.commit();

            _log.info(
                    "Added: " +
                            orgsAdded + " organisms; " +
                            seqsAdded + " sequences; " +
                            identsAdded + " identifiers; " +
                            annotsAdded + " annotations"
            );
        }
        catch (Exception e)
        {
            XMLProteinHandler.parseWarning("Could not process this batch of records: " + e);
            e.printStackTrace();
        }
    }

    public int insertOrganisms(Map<String, ParseActions> tables, Connection conn) throws Exception
    {
        try
        {
            int transactionCount = 0;
            addOrg.setTimestamp(6, new Timestamp(new Date().getTime()));

            //Add current mouthful of Organisms
            _log.debug((new java.util.Date()) + " Processing organisms");
            ParseActions p = tables.get("Organism");

            // All organism records.  Each one is a HashMap
            for (Map<String, Object> curOrg : p.getAllItems().values())
            {
                transactionCount++;
                if (curOrg.get("common_name") == null)
                {
                    addOrg.setNull(1, Types.VARCHAR);
                }
                else
                {
                    addOrg.setString(1, (String) curOrg.get("common_name"));
                }
                addOrg.setString(2, (String) curOrg.get("genus"));
                addOrg.setString(3, (String) curOrg.get("species"));
                if (curOrg.get("comments") == null)
                {
                    addOrg.setNull(4, Types.VARCHAR);
                }
                else
                {
                    addOrg.setString(4, (String) curOrg.get("comments"));
                }
                addOrg.setString(5, (String) curOrg.get("identID"));
                // Timestamp at index 6 is set once for the whole PreparedStatement
                addOrg.addBatch();
                if (transactionCount == TRANSACTION_ROW_COUNT)
                {
                    transactionCount = 0;
                    addOrg.executeBatch();
                    conn.commit();
                    addOrg.clearBatch();
                }
                handleThreadStateChangeRequests();
            }

            try
            {
                addOrg.executeBatch();
                conn.commit();
                handleThreadStateChangeRequests();
                addOrg.clearBatch();
            }
            catch (Exception ee)
            {
                XMLProteinHandler.parseWarning("Org batch exeception: " + ee);
                throw ee;
            }
            // Insert Organisms into real table
            int result = executeUpdate(InsertIntoOrgCommand, conn);
            //get rid of previously entered vals in temp table
            executeUpdate(DeleteFromTmpOrgCommand, conn);

            //insert identifiers associated with the organism
            executeUpdate(InsertOrgIDCommand, conn);

            // update missing ident_ids in newly inserted organism records
            executeUpdate(UpdateOrgCommand, conn);

            executeUpdate("TRUNCATE TABLE " + OTableName, conn);

            return result;
        }
        catch (Exception e)
        {
            XMLProteinHandler.parseWarning("Problem in Organism table insert: " + e);
            throw e;
        }
    }

    public int insertSequences(Map tables, Connection conn) throws Exception
    {
        int transactionCount = 0;

        try
        {
            addSeq.setTimestamp(13, new Timestamp(new Date().getTime()));

            //Process current mouthful of sequences
            _log.debug(new java.util.Date() + " Processing sequences");
            ParseActions p = (ParseActions) tables.get("ProtSequences");
            for (Map<String, Object> curSeq : p.getAllItems().values())
            {
                transactionCount++;
                addSeq.setString(1, (String) curSeq.get("ProtSequence"));
                addSeq.setString(2, (String) curSeq.get("hash"));
                if (curSeq.get("description") == null)
                {
                    addSeq.setNull(3, Types.VARCHAR);
                }
                else
                {
                    String tmp = (String) curSeq.get("description");
                    if (tmp.length() >= 200) tmp = tmp.substring(0, 190) + "...";
                    addSeq.setString(3, tmp);
                }
                if (curSeq.get("source_change_date") == null)
                {
                    addSeq.setNull(4, Types.TIMESTAMP);
                }
                else
                {
                    addSeq.setTimestamp(4, new Timestamp(DateUtil.parseDateTime((String) curSeq.get("source_change_date"))));
                }
                if (curSeq.get("source_insert_date") == null)
                {
                    addSeq.setNull(5, Types.TIMESTAMP);
                }
                else
                {
                    addSeq.setTimestamp(5, new Timestamp(DateUtil.parseDateTime((String) curSeq.get("source_insert_date"))));
                }
                addSeq.setString(6, (String) curSeq.get("genus"));
                addSeq.setString(7, (String) curSeq.get("species"));
                if (curSeq.get("mass") == null)
                {
                    addSeq.setNull(8, Types.FLOAT);
                }
                else
                {
                    addSeq.setFloat(8, Float.parseFloat((String) curSeq.get("mass")));
                }
                if (curSeq.get("length") == null)
                {
                    addSeq.setNull(9, Types.INTEGER);
                }
                else
                {
                    addSeq.setInt(9, Integer.parseInt((String) curSeq.get("length")));
                }
                if (curSeq.get("source") == null)
                {
                    addSeq.setNull(10, Types.VARCHAR);
                }
                else
                {
                    addSeq.setString(10, (String) curSeq.get("source"));
                }
                if (curSeq.get("best_name") == null)
                {
                    addSeq.setNull(11, Types.VARCHAR);
                }
                else
                {
                    String tmp = (String) curSeq.get("best_name");
                    if (tmp.length() >= 50) tmp = tmp.substring(0, 45) + "...";
                    addSeq.setString(11, tmp);
                }
                if (curSeq.get("best_gene_name") == null)
                {
                    addSeq.setNull(12, Types.VARCHAR);
                }
                else
                {
                    String tmp = (String) curSeq.get("best_gene_name");
                    if (tmp.length() >= 50) tmp = tmp.substring(0, 45) + "...";
                    addSeq.setString(12, tmp);
                }
                // Timestamp at index 13 is set once for the whole prepared statement
                addSeq.addBatch();
                if (transactionCount == TRANSACTION_ROW_COUNT)
                {
                    transactionCount = 0;
                    addSeq.executeBatch();
                    conn.commit();
                    addSeq.clearBatch();
                }
                handleThreadStateChangeRequests();
            }
            try
            {
                addSeq.executeBatch();
                handleThreadStateChangeRequests();
                conn.commit();
                addSeq.clearBatch();
            }
            catch (Exception ee)
            {
                XMLProteinHandler.parseWarning("Seq batch exception: " + ee);
                throw ee;
            }
            executeUpdate(InsertInfoSourceFromSeqCommand, conn);

            executeUpdate(UpdateSeqTableCommand, conn);

            int result = executeUpdate(InsertIntoSeqCommand, conn);

            executeUpdate("TRUNCATE TABLE " + STableName, conn);

            return result;
        }
        catch (Exception e)
        {
            XMLProteinHandler.parseWarning("Problem in Sequence table insert: " + e);
            throw e;
        }
    }

    public int insertIdentifiers(Map tables, Connection conn) throws Exception
    {
        int transactionCount = 0;

        // Process current mouthful of identifiers
        try
        {
            _log.debug(new java.util.Date() + " Processing identifiers");
            addIdent.setTimestamp(6, new java.sql.Timestamp(new java.util.Date().getTime()));
            Vector idents = (Vector) ((ParseActions) tables.get("ProtIdentifiers")).getCurItem().get("Identifiers");
            for (Enumeration e = idents.elements(); e.hasMoreElements();)
            {
                transactionCount++;
                Map curIdent = (Map) e.nextElement();
                String curIdentVal = (String) curIdent.get("identifier");
                if (curIdentVal.length() > 50) curIdentVal = curIdentVal.substring(0, 45) + "...";
                addIdent.setString(1, curIdentVal);
                addIdent.setString(2, (String) curIdent.get("identType"));
                Map curSeq = (Map) curIdent.get("sequence");
                addIdent.setString(3, (String) curSeq.get("genus"));
                addIdent.setString(4, (String) curSeq.get("species"));
                addIdent.setString(5, (String) curSeq.get("hash"));
                // Timestamp at index 6 is set once for the whole PreparedStatement
                addIdent.addBatch();
                if (transactionCount == TRANSACTION_ROW_COUNT)
                {
                    transactionCount = 0;
                    addIdent.executeBatch();
                    conn.commit();
                    addIdent.clearBatch();
                }
                handleThreadStateChangeRequests();
            }
            try
            {
                addIdent.executeBatch();
                handleThreadStateChangeRequests();
                conn.commit();
                addIdent.clearBatch();
            }
            catch (Exception ee)
            {
                XMLProteinHandler.parseWarning("Ident batch exception: " + ee);
                throw ee;
            }

            _log.debug("Starting to create indices on " + ITableName);
            executeUpdate("create index iIdentifier on " + ITableName + "(Identifier)", conn);
            executeUpdate("create index iIdenttype on " + ITableName + "(IdentType)", conn);
            executeUpdate("create index iSpeciesGenusHash on " + ITableName + "(Species, Genus, Hash)", conn);

            executeUpdate(_dialect.getAnalyzeCommandForTable(ITableName), conn, "Analyzing " + ITableName);

            // Insert ident types
            executeUpdate(InsertIdentTypesCommand, conn, "InsertIdentTypes");

            executeUpdate(UpdateIdentsWithSeqsCommand, conn, "UpdateIdentsWithSeqs");

            int result = executeUpdate(InsertIntoIdentsCommand, conn, "InsertIntoIdents");

            executeUpdate(_dialect.getDropIndexCommand(ITableName, "iIdentifier"), conn);
            executeUpdate(_dialect.getDropIndexCommand(ITableName, "iIdenttype"), conn);
            executeUpdate(_dialect.getDropIndexCommand(ITableName, "iSpeciesGenusHash"), conn);
            executeUpdate("TRUNCATE TABLE " + ITableName, conn, "TRUNCATE TABLE " + ITableName);

            _log.debug("Done with identifiers");
            return result;
        }
        catch (Exception e)
        {
            XMLProteinHandler.parseWarning("Problem in Identifier table insert: " + e);
            throw e;
        }
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

    public int insertAnnotations(Map tables, Connection conn) throws Exception
    {
        int transactionCount = 0;
        // Process current mouthful of identifiers
        try
        {
            addAnnot.setTimestamp(10, new java.sql.Timestamp(new java.util.Date().getTime()));
            transactionCount++;
            _log.debug(new java.util.Date() + " Processing annotations");
            Vector annots = (Vector) ((ParseActions) tables.get("ProtAnnotations")).getCurItem().get("Annotations");
            for (Enumeration e = annots.elements(); e.hasMoreElements();)
            {
                Map curAnnot = (Map) e.nextElement();
                String annotVal = (String) curAnnot.get("annot_val");
                if (annotVal.length() > MAX_ANNOT_SIZE)
                    annotVal = annotVal.substring(0, MAX_ANNOT_SIZE) + "...";
                addAnnot.setString(1, annotVal);
                addAnnot.setString(2, (String) curAnnot.get("annotType"));
                Map curSeq = (Map) curAnnot.get("sequence");
                addAnnot.setString(3, (String) curSeq.get("genus"));
                addAnnot.setString(4, (String) curSeq.get("species"));
                addAnnot.setString(5, (String) curSeq.get("hash"));
                if (curAnnot.get("start_pos") == null)
                {
                    addAnnot.setInt(6, 0);
                }
                else
                {
                    addAnnot.setInt(6, Integer.parseInt((String) curAnnot.get("start_pos")));
                }
                if (curAnnot.get("end_pos") == null)
                {
                    addAnnot.setInt(7, 0);
                }
                else
                {
                    addAnnot.setInt(7, Integer.parseInt((String) curAnnot.get("end_pos")));
                }
                if (curAnnot.get("identifier") == null)
                {
                    addAnnot.setNull(8, Types.VARCHAR);
                }
                else
                {
                    addAnnot.setString(8, (String) curAnnot.get("identifier"));
                }
                if (curAnnot.get("identType") == null)
                {
                    addAnnot.setNull(9, Types.VARCHAR);
                }
                else
                {
                    addAnnot.setString(9, (String) curAnnot.get("identType"));
                }
                // Timestamp at index 10 is set once for the whole PreparedStatement
                addAnnot.addBatch();
                if (transactionCount == TRANSACTION_ROW_COUNT)
                {
                    transactionCount = 0;
                    addAnnot.executeBatch();
                    conn.commit();
                    addAnnot.clearBatch();
                }
                handleThreadStateChangeRequests();
            }
            try
            {
                addAnnot.executeBatch();
                conn.commit();
                handleThreadStateChangeRequests();
                addAnnot.clearBatch();
            }
            catch (Exception ee)
            {
                XMLProteinHandler.parseWarning("Annot batch exception: " + ee);
                throw ee;
            }

            _log.debug("Starting to create indices on " + ATableName);
            executeUpdate("create index aAnnot_val on " + ATableName + "(Annot_Val)", conn);
            executeUpdate("create index aAnnotType on " + ATableName + "(AnnotType)", conn);
            executeUpdate("create index aHashGenusSpecies on " + ATableName + "(Hash, Genus, Species)", conn);

            executeUpdate(_dialect.getAnalyzeCommandForTable(ATableName), conn, "Analyzing " + ATableName);

            // Insert ident types
            executeUpdate(InsertAnnotTypesCommand, conn, "InsertAnnotTypes");

            executeUpdate(UpdateAnnotsWithSeqsCommand, conn, "UpdateAnnotsWithSeqs");

            executeUpdate(UpdateAnnotsWithIdentsCommand, conn, "UpdateAnnotsWithIdents");

            int result = executeUpdate(InsertIntoAnnotsCommand, conn, "InsertIntoAnnots");

            executeUpdate(_dialect.getDropIndexCommand(ATableName, "aAnnot_val"), conn);
            executeUpdate(_dialect.getDropIndexCommand(ATableName, "aAnnotType"), conn);
            executeUpdate(_dialect.getDropIndexCommand(ATableName, "aHashGenusSpecies"), conn);
            executeUpdate("TRUNCATE TABLE " + ATableName, conn, "TRUNCATE TABLE " + ATableName);

            _log.debug("Done with annotations");
            return result;
        }
        catch (Exception e)
        {
            XMLProteinHandler.parseWarning("Problem in Annotation table insert: " + e);
            throw e;
        }
    }
}
