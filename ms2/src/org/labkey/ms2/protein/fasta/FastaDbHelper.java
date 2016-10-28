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
package org.labkey.ms2.protein.fasta;

import org.labkey.api.data.CoreSchema;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.query.AliasManager;
import org.labkey.ms2.protein.ProteinManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/**
 * User: tholzman
 * Date: Jun 3, 2005
 */
public class FastaDbHelper
{
    private static final SqlDialect _dialect = CoreSchema.getInstance().getSqlDialect();

    private static final String INITIAL_INSERTION_COMMAND =
            "INSERT INTO " + ProteinManager.getTableInfoAnnotInsertions() + " (FileName,FileType,Comment,DefaultOrganism,OrgShouldBeGuessed,InsertDate) VALUES (?,'fasta',?,?,?,?)";

    private static final String UPDATE_INSERTION_COMMAND =
            "UPDATE " + ProteinManager.getTableInfoAnnotInsertions() + " SET " +
                    " Mouthsful=?,SequencesAdded=?,AnnotationsAdded=?,IdentifiersAdded=?," +
                    " OrganismsAdded=?, MRMSequencesAdded=?,MRMAnnotationsAdded=?,MRMIdentifiersAdded=?," +
                    " MRMOrganismsAdded=?,MRMSize=?,RecordsProcessed=?,ChangeDate=? " +
                    " WHERE  InsertId=?";

    private static final String UPDATE_BEST_GENE_NAME_COMMAND = "UPDATE " + ProteinManager.getTableInfoSequences() + " SET BestGeneName = (" +
            "SELECT MIN(identifier) FROM " + ProteinManager.getTableInfoIdentifiers() + " i, " + ProteinManager.getTableInfoIdentTypes() + " it WHERE i.identtypeid = it.identtypeid AND " +
            "i.seqid = " + ProteinManager.getTableInfoSequences() + ".seqid AND it.name='GeneName') WHERE BestGeneName IS NULL AND seqid IN (SELECT seqid FROM " +
            ProteinManager.getTableInfoFastaSequences() + " WHERE fastaid = ?)";

    private static final String FINALIZE_INSERTION_COMMAND =
            "UPDATE " + ProteinManager.getTableInfoAnnotInsertions() + " SET " +
                    " CompletionDate=? WHERE InsertId=?";

    private static final String GET_CURRENT_INSERT_STATS_COMMAND =
            "SELECT SequencesAdded,AnnotationsAdded,IdentifiersAdded,OrganismsAdded,Mouthsful,RecordsProcessed" +
                    " FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId=?";

    public final String _seqTableName = _dialect.getTempTablePrefix() +  "sequences" + (new Random().nextInt(1000000000));
    public final String _identTableName = _dialect.getTempTablePrefix() +  "identifiers" + (new Random().nextInt(1000000000));

    public final PreparedStatement _initialInsertionStmt;
    public final PreparedStatement _updateInsertionStmt;
    public final PreparedStatement _finalizeInsertionStmt;
    public final PreparedStatement _updateBestGeneNameStmt;
    public final PreparedStatement _getCurrentInsertStatsStmt;
    public final PreparedStatement _addIdentStmt;
    public final PreparedStatement _addSeqStmt;

    // replacement for GuessBySharedHash, fixes only records that would otherwise be designated "unknown".
    // If a record has a default orgainism other than "default" then we don't change it.
    // if there are multiple matching sequences, it just picks the matching hash with the lowest Orgid
    // that is not the orgid for the Unknown Unknown
    public final PreparedStatement _guessOrgBySharedHashStmt;

    public final PreparedStatement _insertIntoOrgsStmt;
    public final PreparedStatement _updateSTempWithOrgIDsStmt;
    public final PreparedStatement _insertIntoSeqsStmt;
    public final PreparedStatement _updateSTempWithSeqIDsStmt;
    public final PreparedStatement _getIdentsStmt;
    public final PreparedStatement _insertIdentTypesStmt;
    public final PreparedStatement _updateIdentsWithIdentTypesStmt;
    public final PreparedStatement _insertIntoIdentsStmt;
    public final PreparedStatement _emptySeqsStmt;
    public final PreparedStatement _emptyIdentsStmt;
    public final int _fullOrganismNameIdent;
    public final int _lookupStringIdent;
    public final PreparedStatement _insertOrgsIntoAnnotationsStmt;
    public final PreparedStatement _insertFastaSequencesStmt;
    public final PreparedStatement _insertIntoFastasStmt;
    public final PreparedStatement _dropSeqTempStmt;
    public final PreparedStatement _dropIdentTempStmt;
    public final PreparedStatement _updateSTempWithGuessedOrgStmt;

    public FastaDbHelper(Connection c) throws SQLException
    {
        SQLFragment initialInsert = new SQLFragment(INITIAL_INSERTION_COMMAND);
        _dialect.addReselect(initialInsert, ProteinManager.getTableInfoAnnotInsertions().getColumn("InsertId"), null);
        _initialInsertionStmt = c.prepareStatement(initialInsert.getSQL());
        _getCurrentInsertStatsStmt = c.prepareStatement(GET_CURRENT_INSERT_STATS_COMMAND);
        _updateInsertionStmt = c.prepareStatement(UPDATE_INSERTION_COMMAND);
        _finalizeInsertionStmt = c.prepareStatement(FINALIZE_INSERTION_COMMAND);
        _updateBestGeneNameStmt = c.prepareStatement(UPDATE_BEST_GENE_NAME_COMMAND);

        c.createStatement().execute("CREATE " +  _dialect.getTempTableKeyword() + " TABLE " + _seqTableName + " ( " +
                "srowid " + _dialect.getUniqueIdentType() + " PRIMARY KEY NOT NULL, " +
                "ProtSequence text NULL , " +
                "hash varchar(100) NULL , " +
                "description varchar(200) NULL ," +
                "genus varchar(100) NULL, " +
                "species varchar(100) NULL, " +
                "mass float NULL," +
                "length int NULL ," +
                "best_name varchar(500) NULL, " +
                "seqId int NULL," +
                "fname varchar(100) NULL," +
                "fullorg varchar(200) NULL," +
                "lookup varchar(200) NULL," +
                "orgId int NULL, " +
                "entry_date " + _dialect.getDefaultDateTimeDataType() + " NULL" +
                ")");

        c.createStatement().execute("CREATE INDEX IX_" + AliasManager.makeLegalName(_seqTableName,null) + "_HASH_ORGID_SROWID ON " + _seqTableName + "(hash, orgId, srowid)");
        c.createStatement().execute("CREATE INDEX IX_" + AliasManager.makeLegalName(_seqTableName,null) + "_ORGID ON " + _seqTableName + "(orgId)");
 
        c.createStatement().execute("CREATE " + _dialect.getTempTableKeyword() + " TABLE " + _identTableName + " ( " +
                "Identifier varchar(50)  NOT NULL, " +
                "identType varchar(50) NULL, " +
                "IdentTypeID int NULL, " +
                "SeqId int NULL, " +
                "entry_date " + _dialect.getDefaultDateTimeDataType() + " NULL" +
                ")");

        c.createStatement().execute("CREATE INDEX IX_" + AliasManager.makeLegalName(_identTableName,null) + " ON " + _identTableName + "(Identifier,IdentTypeId,SeqId)");

        _addSeqStmt = c.prepareStatement("INSERT INTO " + _seqTableName +
                " (ProtSequence,hash,description,mass,length,best_name,fname,lookup,genus,species,fullOrg,entry_date) " +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ");

        _insertIntoOrgsStmt = c.prepareStatement("INSERT INTO " + ProteinManager.getTableInfoOrganisms() + " (Genus,Species) " +
                "SELECT DISTINCT genus,species FROM " + _seqTableName +
                " WHERE " + _seqTableName + ".orgid IS NULL AND NOT EXISTS (" +
                "SELECT * FROM " + ProteinManager.getTableInfoOrganisms() + " WHERE UPPER(" + _seqTableName + ".genus) = UPPER(" + ProteinManager.getTableInfoOrganisms() + ".genus) AND UPPER(" +
                _seqTableName + ".species) = UPPER(" + ProteinManager.getTableInfoOrganisms() + ".species))");

        _updateSTempWithGuessedOrgStmt = c.prepareStatement("UPDATE " + _seqTableName + " SET genus = x.genus, species = x.species, fullorg = x.fullorg \n" +
            "FROM ( SELECT " + _dialect.concatenate("a.genus", "' '", "a.species") + " AS fullorg, a.species as species, a.genus as genus, c.identifier as identifier " +
                "  FROM " + ProteinManager.getTableInfoOrganisms() + " a JOIN " + ProteinManager.getTableInfoSequences() + " b ON (a.orgid=b.orgid) JOIN " + ProteinManager.getTableInfoIdentifiers() + " c ON (c.seqid=b.seqid)) x \n" +
                " WHERE " + _seqTableName + ".fullorg IS NULL AND x.identifier=" + _seqTableName + ".lookup");

        _updateSTempWithOrgIDsStmt = c.prepareStatement("UPDATE " + _seqTableName +
                " SET orgid = " + ProteinManager.getTableInfoOrganisms() + ".orgid FROM " + ProteinManager.getTableInfoOrganisms() +
                " WHERE UPPER(" + ProteinManager.getTableInfoOrganisms() + ".genus) = UPPER(" + _seqTableName + ".genus)" +
                " AND UPPER(" + ProteinManager.getTableInfoOrganisms() + ".species) = UPPER(" + _seqTableName + ".species)");

        _insertIntoSeqsStmt = c.prepareStatement("INSERT INTO " + ProteinManager.getTableInfoSequences() + " (ProtSequence, Hash, Description, Mass, Length, OrgId, BestName, InsertDate) " +
                " SELECT a.ProtSequence,a.hash,a.description,a.mass,a.length,a.OrgId,substring(a.best_name,1,50),entry_date " +
                " FROM " + _seqTableName + " a " +
                " WHERE NOT EXISTS (" +
                "   SELECT * FROM " + ProteinManager.getTableInfoSequences() + " " +
                "   WHERE " +
                "     a.Hash = " + ProteinManager.getTableInfoSequences() + ".Hash AND a.OrgId=" + ProteinManager.getTableInfoSequences() + ".OrgId" +
                " ) AND srowid IN (SELECT MIN(srowid) FROM " + _seqTableName + " GROUP BY Hash, OrgId)");

        _updateSTempWithSeqIDsStmt = c.prepareStatement("UPDATE " + _seqTableName + " SET seqid = " + ProteinManager.getTableInfoSequences() + ".seqid FROM " + ProteinManager.getTableInfoSequences() + " WHERE " + ProteinManager.getTableInfoSequences() + ".hash=" + _seqTableName + ".hash" +
                " AND " + ProteinManager.getTableInfoSequences() + ".orgid=" + _seqTableName + ".orgid");

        _getIdentsStmt = c.prepareStatement("SELECT best_name,seqid,description FROM " + _seqTableName);

        _insertIdentTypesStmt = c.prepareStatement("INSERT INTO " + ProteinManager.getTableInfoIdentTypes() + " (name,EntryDate) SELECT DISTINCT a.identType,max(a.entry_date) FROM " +
            _identTableName + " a WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoIdentTypes() + " " +
                " WHERE a.identType = " + ProteinManager.getTableInfoIdentTypes() + ".name) GROUP BY a.identType");

        _updateIdentsWithIdentTypesStmt = c.prepareStatement("UPDATE " + _identTableName + " SET identTypeId = a.IdentTypeId " +
                " FROM " + ProteinManager.getTableInfoIdentTypes() + " a WHERE a.name=" + _identTableName + ".identType");

        _addIdentStmt = c.prepareStatement("INSERT INTO " + _identTableName + " (identifier,identTYpe,seqid,entry_date) VALUES (?,?,?,?)");

        _insertIntoIdentsStmt = c.prepareStatement("INSERT INTO " + ProteinManager.getTableInfoIdentifiers() + " (Identifier,IdentTypeId,SeqId,EntryDate) " +
                " SELECT DISTINCT Identifier,IdentTypeId,SeqId,max(entry_date) FROM " + _identTableName +
                "  WHERE NOT EXISTS (SELECT * FROM " + ProteinManager.getTableInfoIdentifiers() + " a " +
                "    WHERE a.Identifier = " + _identTableName + ".Identifier AND " +
                "          a.IdentTypeId = " + _identTableName + ".IdentTypeId AND " +
                "          a.SeqId = " + _identTableName + ".SeqId) " +
                "  GROUP BY Identifier,IdentTypeId,SeqId");

        _emptySeqsStmt = c.prepareStatement("TRUNCATE TABLE " + _seqTableName);
        _emptyIdentsStmt = c.prepareStatement("TRUNCATE TABLE " + _identTableName);

        _guessOrgBySharedHashStmt = c.prepareStatement("UPDATE " + _seqTableName + " SET orgid = ( SELECT MIN(PS.orgid) " +
                    " FROM " + ProteinManager.getTableInfoSequences() + " PS INNER JOIN " +  ProteinManager.getTableInfoOrganisms() + " PO ON (PS.orgid = PO.orgid) " +
                    " AND PS.hash = " + _seqTableName + ".hash " +
                    " AND (PO.genus<>'Unknown' OR PO.species<>'unknown')  )" +
                " WHERE " + _seqTableName + ".genus= ? AND "+ _seqTableName + ".species= ? " +
                " AND 1 = ( SELECT COUNT(*) " +
                    " FROM " + ProteinManager.getTableInfoSequences() + " PS INNER JOIN " +  ProteinManager.getTableInfoOrganisms() + " PO ON (PS.orgid = PO.orgid) " +
                    " AND PS.hash = " + _seqTableName + ".hash " +
                    " AND (PO.genus<>'Unknown' OR PO.species<>'unknown')  )");


        ResultSet rs =
                c.createStatement().executeQuery("SELECT AnnotTypeId FROM " + ProteinManager.getTableInfoAnnotationTypes() + " WHERE name='FullOrganismName'");
        rs.next();
        _fullOrganismNameIdent = rs.getInt(1);
        rs =
                c.createStatement().executeQuery("SELECT AnnotTypeID FROM " + ProteinManager.getTableInfoAnnotationTypes() + " WHERE name='LookupString'");
        rs.next();
        _lookupStringIdent = rs.getInt(1);
        rs.close();
        _insertOrgsIntoAnnotationsStmt = c.prepareStatement(
                "INSERT INTO " + ProteinManager.getTableInfoAnnotations() + " (annotval,annotTypeId,seqid,InsertDate) " +
                        "  SELECT DISTINCT s.fullorg," + _fullOrganismNameIdent + ",s.seqid,max(s.entry_date) " +
                        "     FROM " + _seqTableName + " s " +
                        "     WHERE " +
                        "        s.fullorg IS NOT NULL AND " +
                        "        NOT EXISTS (" +
                        "           SELECT * FROM " + ProteinManager.getTableInfoOrganisms() +
                        "             WHERE " +  _dialect.concatenate("genus", "' '", "species") + " = s.fullorg " +
                        "        )                     AND " +
                        "        NOT EXISTS (" +
                        "           SELECT * FROM " + ProteinManager.getTableInfoAnnotations() + " c " +
                        "              WHERE c.annotTypeId=" + _fullOrganismNameIdent + " AND " +
                        "                    s.fullorg = c.annotval                  AND " +
                        "                    s.seqid = c.seqid " +
                        "        )" +
                        " GROUP BY s.fullorg,s.seqid"
        );

        _insertFastaSequencesStmt = c.prepareStatement("INSERT INTO " + ProteinManager.getTableInfoFastaSequences() + " (fastaid, lookupstring, seqid) ( SELECT ? AS fastaid, lookup, seqid FROM " + _seqTableName + ")");

        _insertIntoFastasStmt = c.prepareStatement("INSERT INTO " + ProteinManager.getTableInfoFastaLoads() + " (filename,nsequences,comment,filechecksum,InsertDate) VALUES (?,?,?,?,?)");
        _dropSeqTempStmt = c.prepareStatement("DROP TABLE " + _seqTableName);
        _dropIdentTempStmt = c.prepareStatement("DROP TABLE " + _identTableName);
    }
}
