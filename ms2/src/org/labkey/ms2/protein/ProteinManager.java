/*
 * Copyright (c) 2005-2017 Fred Hutchinson Cancer Research Center
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

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.*;
import org.labkey.api.data.dialect.SqlDialect;
import org.labkey.api.exp.DomainNotFoundException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.query.FieldKey;
import org.labkey.api.search.SearchService;
import org.labkey.api.security.User;
import org.labkey.api.settings.PreferenceService;
import org.labkey.api.util.HashHelpers;
import org.labkey.api.util.Path;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.NotFoundException;
import org.labkey.api.webdav.SimpleDocumentResource;
import org.labkey.ms2.MS2Controller;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.MS2Peptide;
import org.labkey.ms2.MS2Run;
import org.labkey.ms2.Protein;
import org.labkey.ms2.protein.fasta.FastaFile;
import org.labkey.ms2.protein.fasta.PeptideGenerator;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: arauch
 * Date: Mar 23, 2005
 * Time: 9:58:17 PM
 */
public class ProteinManager
{
    public static final SearchService.SearchCategory proteinCategory = new SearchService.SearchCategory("protein", "Protein");
    
    private static Logger _log = Logger.getLogger(ProteinManager.class);
    private static final String SCHEMA_NAME = "prot";

    public static final int RUN_FILTER = 1;
    public static final int URL_FILTER = 2;
    public static final int EXTRA_FILTER = 4;
    public static final int PROTEIN_FILTER = 8;
    public static final int ALL_FILTERS = RUN_FILTER + URL_FILTER + EXTRA_FILTER + PROTEIN_FILTER;
    private static final String ALL_PEPTIDES_PREFERENCE_NAME = ProteinManager.class.getName() + "." + MS2Controller.ProteinViewBean.ALL_PEPTIDES_URL_PARAM;

    public static String getSchemaName()
    {
        return SCHEMA_NAME;
    }


    public static DbSchema getSchema()
    {
        return DbSchema.get(SCHEMA_NAME, DbSchemaType.Module);
    }


    public static SqlDialect getSqlDialect()
    {
        return getSchema().getSqlDialect();
    }


    public static TableInfo getTableInfoFastaFiles()
    {
        return getSchema().getTable("FastaFiles");
    }


    public static TableInfo getTableInfoFastaSequences()
    {
        return getSchema().getTable("FastaSequences");
    }


    public static TableInfo getTableInfoFastaAdmin()
    {
        return getSchema().getTable("FastaAdmin");
    }


    public static TableInfo getTableInfoAnnotInsertions()
    {
        return getSchema().getTable("AnnotInsertions");
    }


    public static TableInfo getTableInfoCustomAnnotation()
    {
        return getSchema().getTable("CustomAnnotation");
    }

    public static TableInfo getTableInfoCustomAnnotationSet()
    {
        return getSchema().getTable("CustomAnnotationSet");
    }

    public static TableInfo getTableInfoAnnotations()
    {
        return getSchema().getTable("Annotations");
    }


    public static TableInfo getTableInfoAnnotationTypes()
    {
        return getSchema().getTable("AnnotationTypes");
    }


    public static TableInfo getTableInfoIdentifiers()
    {
        return getSchema().getTable("Identifiers");
    }


    public static TableInfo getTableInfoIdentTypes()
    {
        return getSchema().getTable("IdentTypes");
    }


    public static TableInfo getTableInfoOrganisms()
    {
        return getSchema().getTable("Organisms");
    }


    public static TableInfo getTableInfoInfoSources()
    {
        return getSchema().getTable("InfoSources");
    }


    public static TableInfo getTableInfoSequences()
    {
        return getSchema().getTable("Sequences");
    }


    public static TableInfo getTableInfoFastaLoads()
    {
        return getSchema().getTable("FastaLoads");
    }


    public static TableInfo getTableInfoSprotOrgMap()
    {
        return getSchema().getTable("SprotOrgMap");
    }

    public static TableInfo getTableInfoGoTerm()
    {
        return getSchema().getTable("GoTerm");
    }

    public static TableInfo getTableInfoGoTerm2Term()
    {
        return getSchema().getTable("GoTerm2Term");
    }

    public static TableInfo getTableInfoGoGraphPath()
    {
        return getSchema().getTable("GoGraphPath");
    }

    public static TableInfo getTableInfoGoTermDefinition()
    {
        return getSchema().getTable("GoTermDefinition");
    }

    public static TableInfo getTableInfoGoTermSynonym()
    {
        return getSchema().getTable("GoTermSynonym");
    }


    public static Protein getProtein(int seqId)
    {
        return new SqlSelector(getSchema(),
                "SELECT SeqId, ProtSequence AS Sequence, Mass, Description, BestName, BestGeneName FROM " + getTableInfoSequences() + " WHERE SeqId = ?",
                seqId).getObject(Protein.class);
    }

    public static Protein getProtein(String sequence, int organismId)
    {
        return new SqlSelector(getSchema(),
                "SELECT SeqId, ProtSequence AS Sequence, Mass, Description, BestName, BestGeneName FROM " + getTableInfoSequences() + " WHERE Hash = ? AND OrgId = ?",
                hashSequence(sequence), organismId).getObject(Protein.class);
    }

    public static List<Protein> getProtein(String sequence)
    {
        return new SqlSelector(getSchema(),
                "SELECT SeqId, ProtSequence AS Sequence, Mass, Description, BestName, BestGeneName FROM " + getTableInfoSequences() + " WHERE Hash = ?",
                hashSequence(sequence)).getArrayList(Protein.class);
    }

    public static String getProteinSequence(int seqId)
    {
        Protein protein = getProtein(seqId);
        return protein != null ? protein.getSequence() : null;
    }

    public static List<Protein> getProteinsContainingPeptide(MS2Peptide peptide, int... fastaIds) throws SQLException
    {
        if ((null == peptide) || ("".equals(peptide.getTrimmedPeptide())) || (peptide.getProteinHits() < 1))
            return Collections.emptyList();

        int hits = peptide.getProteinHits();
        SQLFragment sql = new SQLFragment();
        if (hits == 1 && peptide.getSeqId() != null)
        {
            sql.append("SELECT SeqId, ProtSequence AS Sequence, Mass, Description, ? AS BestName, BestGeneName FROM ");
            sql.append(getTableInfoSequences(), "s");
            sql.append(" WHERE SeqId = ?");
            sql.add(peptide.getProtein());
            sql.add(peptide.getSeqId());
        }
        else
        {
            // TODO: make search tryptic so that number that match = ProteinHits.
            sql.append("SELECT s.SeqId, s.ProtSequence AS Sequence, s.Mass, s.Description, fs.LookupString AS BestName, s.BestGeneName FROM ");
            sql.append(getTableInfoSequences(), "s");
            sql.append(", ");
            sql.append(getTableInfoFastaSequences(), "fs");
            sql.append(" WHERE fs.SeqId = s.SeqId AND fs.FastaId IN (");
            sql.append(StringUtils.repeat("?", ", ", fastaIds.length));
            sql.append(") AND ProtSequence ");
            sql.append(getSqlDialect().getCharClassLikeOperator());
            sql.append(" ?" );
            for (int fastaId : fastaIds)
            {
                sql.add(fastaId);
            }
            sql.add("%" + peptide.getTrimmedPeptide() + "%");

            //based on observations of 2 larger ms2 databases, TOP 20 causes better query plan generation in SQL Server
            sql = getSchema().getSqlDialect().limitRows(sql, Math.max(20, hits));
        }

        List<Protein> proteins = new SqlSelector(getSchema(), sql).getArrayList(Protein.class);

        if (proteins.isEmpty())
            _log.warn("getProteinsContainingPeptide: Could not find peptide " + peptide + " in FASTA files " + Arrays.asList(fastaIds));

        return proteins;
    }


    private static final NumberFormat generalFormat = new DecimalFormat("0.0#");

    public static FastaFile getFastaFile(int fastaId)
    {
        return new TableSelector(ProteinManager.getTableInfoFastaFiles()).getObject(fastaId, FastaFile.class);
    }

    public static void addExtraFilter(SimpleFilter filter, MS2Run run, ActionURL currentUrl)
    {
        String paramName = run.getChargeFilterParamName();

        boolean includeChargeFilter = false;
        Float[] values = new Float[3];

        for (int i = 0; i < values.length; i++)
        {
            String threshold = currentUrl.getParameter(paramName + (i + 1));

            if (null != threshold && !"".equals(threshold))
            {
                try
                {
                    values[i] = Float.parseFloat(threshold);  // Make sure this parses to a float
                    includeChargeFilter = true;
                }
                catch(NumberFormatException e)
                {
                    // Ignore any values that can't be converted to float -- leave them null
                }
            }
        }

        // Add charge filter only if there's one or more valid values
        if (includeChargeFilter && run.getChargeFilterColumnName() != null)
            filter.addClause(new ChargeFilter(FieldKey.fromString(run.getChargeFilterColumnName()), values));

        String tryptic = currentUrl.getParameter("tryptic");

        // Add tryptic filter
        if ("1".equals(tryptic))
            filter.addClause(new TrypticFilter(1));
        else if ("2".equals(tryptic))
            filter.addClause(new TrypticFilter(2));
    }

    public static Map<String, CustomAnnotationSet> getCustomAnnotationSets(Container container, boolean includeProject)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(getTableInfoCustomAnnotationSet());
        sql.append(" WHERE Container = ? ");
        sql.add(container.getId());
        if (includeProject)
        {
            Container project = container.getProject();
            if (project != null && !project.equals(container))
            {
                sql.append(" OR Container = ? ");
                sql.add(project.getId());
            }
        }
        sql.append(" ORDER BY Name");
        Collection<CustomAnnotationSet> allSets = new SqlSelector(getSchema(), sql).getCollection(CustomAnnotationSet.class);

        Set<String> setNames = new CaseInsensitiveHashSet();
        List<CustomAnnotationSet> dedupedSets = new ArrayList<>(allSets.size());
        // If there are any name collisions, we want sets in this container to mask the ones in the project

        // Take a first pass through to add all the ones from this container
        for (CustomAnnotationSet set : allSets)
        {
            if (set.getContainer().equals(container.getId()))
            {
                setNames.add(set.getName());
                dedupedSets.add(set);
            }
        }

        // Take a second pass through to add all the ones from the project that don't collide
        for (CustomAnnotationSet set : allSets)
        {
            if (!set.getContainer().equals(container.getId()) && setNames.add(set.getName()))
            {
                dedupedSets.add(set);
            }
        }

        dedupedSets.sort(Comparator.comparing(CustomAnnotationSet::getName));
        Map<String, CustomAnnotationSet> result = new LinkedHashMap<>();
        for (CustomAnnotationSet set : dedupedSets)
        {
            result.put(set.getName(), set);
        }
        return result;
    }

    public static void deleteCustomAnnotationSet(CustomAnnotationSet set)
    {
        try
        {
            Container c = ContainerManager.getForId(set.getContainer());
            if (OntologyManager.getDomainDescriptor(set.getLsid(), c) != null)
            {
                OntologyManager.deleteOntologyObject(set.getLsid(), c, true);
                OntologyManager.deleteDomain(set.getLsid(), c);
            }
        }
        catch (DomainNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        try (DbScope.Transaction transaction = getSchema().getScope().ensureTransaction())
        {
            new SqlExecutor(getSchema()).execute("DELETE FROM " + getTableInfoCustomAnnotation() + " WHERE CustomAnnotationSetId = ?", set.getCustomAnnotationSetId());
            Table.delete(getTableInfoCustomAnnotationSet(), set.getCustomAnnotationSetId());
            transaction.commit();
        }
    }

    public static CustomAnnotationSet getCustomAnnotationSet(Container c, int id, boolean includeProject)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(getTableInfoCustomAnnotationSet());
        sql.append(" WHERE (Container = ?");
        sql.add(c.getId());
        if (includeProject)
        {
            sql.append(" OR Container = ?");
            sql.add(c.getProject().getId());
        }
        sql.append(") AND CustomAnnotationSetId = ?");
        sql.add(id);
        List<CustomAnnotationSet> matches = new SqlSelector(getSchema(), sql).getArrayList(CustomAnnotationSet.class);
        if (matches.size() > 1)
        {
            for (CustomAnnotationSet set : matches)
            {
                if (set.getContainer().equals(c.getId()))
                {
                    return set;
                }
            }
            assert false : "More than one matching set was found but none were in the current container";
            return matches.get(0);
        }
        if (matches.size() == 1)
        {
            return matches.get(0);
        }
        return null;
    }

    public static void migrateRuns(int oldFastaId, int newFastaId)
            throws SQLException
    {
        SQLFragment mappingSQL = new SQLFragment("SELECT fs1.seqid AS OldSeqId, fs2.seqid AS NewSeqId\n");
        mappingSQL.append("FROM \n");
        mappingSQL.append("\t(SELECT ff.SeqId, s.Hash, ff.LookupString FROM " + getTableInfoFastaSequences() + " ff, " + getTableInfoSequences() + " s WHERE ff.SeqId = s.SeqId AND ff.FastaId = " + oldFastaId + ") fs1 \n");
        mappingSQL.append("\tLEFT OUTER JOIN \n");
        mappingSQL.append("\t(SELECT ff.SeqId, s.Hash, ff.LookupString FROM " + getTableInfoFastaSequences() + " ff, " + getTableInfoSequences() + " s WHERE ff.SeqId = s.SeqId AND ff.FastaId = " + newFastaId + ") fs2 \n");
        mappingSQL.append("\tON (fs1.Hash = fs2.Hash AND fs1.LookupString = fs2.LookupString)");

        SQLFragment missingCountSQL = new SQLFragment("SELECT COUNT(*) FROM (");
        missingCountSQL.append(mappingSQL);
        missingCountSQL.append(") Mapping WHERE OldSeqId IN (\n");
        missingCountSQL.append("(SELECT p.SeqId FROM " + MS2Manager.getTableInfoPeptides() + " p, " + MS2Manager.getTableInfoRuns() + " r WHERE p.run = r.Run AND r.FastaId = " + oldFastaId + ")\n");
        missingCountSQL.append("UNION\n");
        missingCountSQL.append("(SELECT pgm.SeqId FROM ").append(MS2Manager.getTableInfoProteinGroupMemberships()).append(" pgm, ").append(MS2Manager.getTableInfoProteinGroups()).append(" pg, ").append(MS2Manager.getTableInfoProteinProphetFiles()).append(" ppf, ").append(MS2Manager.getTableInfoRuns()).append(" r WHERE pgm.ProteinGroupId = pg.RowId AND pg.ProteinProphetFileId = ppf.RowId AND ppf.Run = r.Run AND r.FastaId = ").append(oldFastaId).append("))\n");
        missingCountSQL.append("AND NewSeqId IS NULL");

        int missingCount = new SqlSelector(getSchema(), missingCountSQL).getObject(Integer.class);
        if (missingCount > 0)
        {
            throw new SQLException("There are " + missingCount + " protein sequences in the original FASTA file that are not in the new file");
        }

        SqlExecutor executor = new SqlExecutor(MS2Manager.getSchema());

        try (DbScope.Transaction transaction = MS2Manager.getSchema().getScope().ensureTransaction())
        {
            SQLFragment updatePeptidesSQL = new SQLFragment();
            updatePeptidesSQL.append("UPDATE " + MS2Manager.getTableInfoPeptidesData() + " SET SeqId = map.NewSeqId");
            updatePeptidesSQL.append("\tFROM " + MS2Manager.getTableInfoFractions() + " f \n");
            updatePeptidesSQL.append("\t, " + MS2Manager.getTableInfoRuns() + " r\n");
            updatePeptidesSQL.append("\t, " + MS2Manager.getTableInfoFastaRunMapping() + " frm\n");
            updatePeptidesSQL.append("\t, (");
            updatePeptidesSQL.append(mappingSQL);
            updatePeptidesSQL.append(") map \n");
            updatePeptidesSQL.append("WHERE f.Fraction = " + MS2Manager.getTableInfoPeptidesData() + ".Fraction\n");
            updatePeptidesSQL.append("\tAND r.Run = f.Run\n");
            updatePeptidesSQL.append("\tAND frm.Run = r.Run\n");
            updatePeptidesSQL.append("\tAND " + MS2Manager.getTableInfoPeptidesData() + ".SeqId = map.OldSeqId \n");
            updatePeptidesSQL.append("\tAND frm.FastaId = " + oldFastaId);

            executor.execute(updatePeptidesSQL);

            SQLFragment updateProteinsSQL = new SQLFragment();
            updateProteinsSQL.append("UPDATE " + MS2Manager.getTableInfoProteinGroupMemberships() + " SET SeqId= map.NewSeqId\n");
            updateProteinsSQL.append("FROM " + MS2Manager.getTableInfoProteinGroups() + " pg\n");
            updateProteinsSQL.append("\t, " + MS2Manager.getTableInfoProteinProphetFiles() + " ppf\n");
            updateProteinsSQL.append("\t, " + MS2Manager.getTableInfoRuns() + " r\n");
            updateProteinsSQL.append("\t, " + MS2Manager.getTableInfoFastaRunMapping() + " frm\n");
            updateProteinsSQL.append("\t, (");
            updateProteinsSQL.append(mappingSQL);
            updateProteinsSQL.append(") map \n");
            updateProteinsSQL.append("WHERE " + MS2Manager.getTableInfoProteinGroupMemberships() + ".ProteinGroupId = pg.RowId\n");
            updateProteinsSQL.append("\tAND pg.ProteinProphetFileId = ppf.RowId\n");
            updateProteinsSQL.append("\tAND r.Run = ppf.Run\n");
            updateProteinsSQL.append("\tAND frm.Run = r.Run\n");
            updateProteinsSQL.append("\tAND " + MS2Manager.getTableInfoProteinGroupMemberships() + ".SeqId = map.OldSeqId\n");
            updateProteinsSQL.append("\tAND frm.FastaId = " + oldFastaId);

            executor.execute(updateProteinsSQL);

            executor.execute("UPDATE " + MS2Manager.getTableInfoFastaRunMapping() + " SET FastaID = ? WHERE FastaID = ?", newFastaId, oldFastaId);
            transaction.commit();
        }
    }

    public static int ensureProtein(String sequence, String organismName, String name, String description)
    {
        Protein protein = ensureProteinInDatabase(sequence, organismName, name, description);
        return protein.getSeqId();
    }

    public static int ensureProtein(String sequence, int orgId, String name, String description)
    {
        Organism organism = new TableSelector(getTableInfoOrganisms()).getObject(orgId, Organism.class);
        if (organism == null)
            throw new IllegalArgumentException("Organism " + orgId + " does not exist");

        Protein protein = ensureProteinInDatabase(sequence, organism, name, description);
        return protein.getSeqId();
    }

    private static Protein ensureProteinInDatabase(String sequence, String organismName, String name, String description)
    {
        String genus = FastaDbLoader.extractGenus(organismName);
        String species = FastaDbLoader.extractSpecies(organismName);
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("species"), species);
        filter.addCondition(FieldKey.fromParts("genus"), genus);
        Organism organism = new TableSelector(getTableInfoOrganisms(), filter, null).getObject(Organism.class);
        if (organism == null)
        {
            organism = new Organism();
            organism.setGenus(genus);
            organism.setSpecies(species);
            organism = Table.insert(null, getTableInfoOrganisms(), organism);
        }

        return ensureProteinInDatabase(sequence, organism, name, description);
    }

    private static Protein ensureProteinInDatabase(String sequence, Organism organism, String name, String description)
    {
        Protein protein = getProtein(sequence, organism.getOrgId());
        if (protein == null)
        {
            Map<String, Object> map = new CaseInsensitiveHashMap<>();
            map.put("ProtSequence", sequence);
            byte[] sequenceBytes = getSequenceBytes(sequence);
            map.put("Mass", PeptideGenerator.computeMass(sequenceBytes, 0, sequenceBytes.length, PeptideGenerator.AMINO_ACID_AVERAGE_MASSES));
            map.put("OrgId", organism.getOrgId());
            map.put("Hash", hashSequence(sequence));
            map.put("Description", description == null ? null : (description.length() > 200 ? description.substring(0, 196) + "..." : description));
            map.put("BestName", name);
            map.put("Length", sequence.length());
            map.put("InsertDate", new Date());
            map.put("ChangeDate", new Date());

            Table.insert(null, getTableInfoSequences(), map);
            protein = getProtein(sequence, organism.getOrgId());
        }
        return protein;
    }

    public static int ensureProteinAndIdentifier(String sequence, String organismName, String identifier, String description, String identifierType)
    {
        Protein protein = ensureProteinInDatabase(sequence, organismName, identifier, description);

        Map<String, Set<String>> typeAndIdentifiers = Collections.singletonMap(identifierType, Collections.singleton(identifier));
        ensureIdentifiers(protein, typeAndIdentifiers);
        return protein.getSeqId();
    }

    public static void ensureIdentifiers(int seqId, Map<String, Set<String>> typeAndIdentifiers)
    {
        Protein protein = getProtein(seqId);
        if(protein == null)
        {
            throw new NotFoundException("SeqId " + seqId + " does not exist.");
        }
        ensureIdentifiers(protein, typeAndIdentifiers);
    }

    private static void ensureIdentifiers(Protein protein, Map<String, Set<String>> typeAndIdentifiers)
    {
        if(typeAndIdentifiers == null || typeAndIdentifiers.size() == 0)
        {
            return;
        }

        for(Map.Entry<String, Set<String>> typeAndIdentifier: typeAndIdentifiers.entrySet())
        {
            String identifierType = typeAndIdentifier.getKey();
            Set<String> identifiers = typeAndIdentifier.getValue();

            Integer identifierTypeId = ensureIdentifierType(identifierType);
            if(identifierTypeId == null)
                continue;

            for(String identifier: identifiers)
            {
                ensureIdentifier(protein, identifierTypeId, identifier);
            }
        }
    }

    private static void ensureIdentifier(Protein protein, Integer identifierTypeId, String identifier)
    {
        identifier = StringUtils.trimToNull(identifier);
        if(identifier == null || identifier.equalsIgnoreCase(protein.getBestName()))
        {
            return;
        }
        if(!identifierExists(identifier, identifierTypeId, protein.getSeqId()))
        {
           addIdentifier(identifier, identifierTypeId, protein.getSeqId());
        }
    }

    private static void addIdentifier(String identifier, int identifierTypeId, int seqId)
    {
        Map<String, Object> values = new HashMap<>();
        values.put("identifier", identifier);
        values.put("identTypeId", identifierTypeId);
        values.put("seqId", seqId);
        values.put("entryDate", new Date());
        Table.insert(null, getTableInfoIdentifiers(), values);
    }

    private static boolean identifierExists(String identifier, int identifierTypeId, int seqId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("identifier"), identifier);
        filter.addCondition(FieldKey.fromParts("identTypeId"), identifierTypeId);
        filter.addCondition(FieldKey.fromParts("seqId"), seqId);
        return new TableSelector(getTableInfoIdentifiers(), filter, null).exists();
    }

    @Nullable
    private static Integer ensureIdentifierType(String identifierType)
    {
        identifierType = StringUtils.trimToNull(identifierType);
        if(identifierType == null)
            return null;

        Integer identTypeId = new SqlSelector(getSchema(),
                            "SELECT MIN(identTypeId) FROM " + getTableInfoIdentTypes() + " WHERE LOWER(name) = ?",
                            identifierType.toLowerCase()).getObject(Integer.class);

        if(identTypeId == null)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("identTypeId", null);
            map.put("name", identifierType);
            map.put("entryDate", new Date());
            map = Table.insert(null, getTableInfoIdentTypes(), map);
            identTypeId = (Integer)map.get("identTypeId");
        }
        return identTypeId;
    }

    private static String hashSequence(String sequence)
    {
        return HashHelpers.hash(getSequenceBytes(sequence));
    }

    private static byte[] getSequenceBytes(String sequence)
    {
        byte[] bytes = sequence.getBytes();
        ByteArrayOutputStream bOut = new ByteArrayOutputStream(bytes.length);

        for (byte aByte : bytes)
    {
        if ((aByte >= 'A') && (aByte <= 'Z'))
        {
            bOut.write(aByte);
        }
    }
        return bOut.toByteArray();
    }


    public static class ChargeFilter extends SimpleFilter.FilterClause
    {
        private FieldKey _fieldKey;
        private Float[] _values;

        // At least one value must be non-null
        public ChargeFilter(FieldKey fieldKey, Float[] values)
        {
            _fieldKey = fieldKey;
            _values = values;
        }

        @Override
        public List<FieldKey> getFieldKeys()
        {
            return Arrays.asList(_fieldKey, FieldKey.fromParts("Charge"));
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            ColumnInfo colInfo = columnMap != null ? columnMap.get(_fieldKey) : null;
            String name = colInfo != null ? colInfo.getAlias() : _fieldKey.getName();
            String alias = dialect.getColumnSelectName(name);

            SQLFragment sql = new SQLFragment();
            sql.append(alias);
            sql.append(" >= CASE Charge");

            for (int i = 0; i < _values.length; i++)
            {
                if (null != _values[i])
                {
                    sql.append(" WHEN ");
                    sql.append(i + 1);
                    sql.append(" THEN ");
                    sql.append(generalFormat.format(_values[i]));
                }
            }

            return sql.append(" ELSE 0 END");
        }


        @Override
        protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
        {
            String sep = "";

            for (int i = 0; i < _values.length; i++)
            {
                if (null != _values[i])
                {
                    sb.append(sep);
                    sep = ", ";
                    sb.append('+').append(i + 1).append(':');
                    sb.append(formatter.format(_fieldKey));
                    sb.append(" >= ").append(generalFormat.format(_values[i]));
                }
            }
        }
    }


    public static class TrypticFilter extends SimpleFilter.FilterClause
    {
        private int _termini;

        public TrypticFilter(int termini)
        {
            _termini = termini;
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            SQLFragment sql = new SQLFragment();
            switch(_termini)
            {
                case(0):
                    sql.append("");
                    break;

                case(1):
                    sql.append(nTerm(dialect) + " OR " + cTerm(dialect));
                    break;

                case(2):
                    sql.append(nTerm(dialect) + " AND " + cTerm(dialect));
                    break;

                default:
                    throw new IllegalArgumentException("INVALID PARAMETER: TERMINI = " + _termini);
            }
            sql.addAll(getParamVals());
            return sql;
        }

        private String nTerm(SqlDialect dialect)
        {
            return "(StrippedPeptide " + dialect.getCharClassLikeOperator() + " '[KR][^P]%' OR StrippedPeptide " + dialect.getCharClassLikeOperator() + " '-%')";
        }

        private String cTerm(SqlDialect dialect)
        {
            return "(StrippedPeptide " + dialect.getCharClassLikeOperator() + " '%[KR][^P]' OR StrippedPeptide " + dialect.getCharClassLikeOperator() + " '%-')";
        }

        public List<FieldKey> getFieldKeys()
        {
            return Arrays.asList(FieldKey.fromParts("StrippedPeptide"));
        }

        @Override
        protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
        {
            sb.append("Trypic Ends ");
            sb.append(1 == _termini ? ">= " : "= ");
            sb.append(_termini);
        }
    }

    public static Sort getPeptideBaseSort()
    {
        // Always sort peptide lists by Fraction, Scan, HitRank, Charge
        return new Sort("Fraction,Scan,HitRank,Charge");
    }

    public static SimpleFilter getPeptideFilter(ActionURL currentUrl, List<MS2Run> runs, int mask, User user)
    {
        // Cop-out for now... we've already checked to make sure all runs are the same type
        // TODO: Allow runs of different type, by one of the following:
        // 1) verify that no search-engine-specific scores are used in the filter OR
        // 2) ignore filters that don't apply to a particular run, and provide a warning OR
        // 3) allowing picking one filter per run type
        return getPeptideFilter(currentUrl, mask, user, runs.get(0));
    }

    public static SimpleFilter reduceToValidColumns(SimpleFilter fullFilter, TableInfo... tables)
    {
        SimpleFilter validFilter = new SimpleFilter();
        for (SimpleFilter.FilterClause clause : fullFilter.getClauses())
        {
            boolean validClause = false;
            for (String columnName : clause.getColumnNames())
            {
                for (TableInfo table : tables)
                {
                    ColumnInfo column = table.getColumn(columnName);
                    if (column == null)
                    {
                        int index = columnName.lastIndexOf('.');
                        if (index != -1)
                        {
                            column = table.getColumn(columnName.substring(index + 1));
                        }
                    }

                    if (column != null)
                    {
                        try
                        {
                            // Coerce data types
                            Object[] values = clause.getParamVals();
                            if (values != null)
                            {
                                for (int i = 0; i < values.length; i++)
                                {
                                    if (values[i] != null)
                                    {
                                        values[i] = ConvertUtils.convert(values[i].toString(), column.getJavaClass());
                                    }
                                }
                            }
                            validClause = true;
                        }
                        catch (ConversionException ignored) {}
                    }
                }
            }
            if (validClause)
            {
                validFilter.addClause(clause);
            }
        }
        return validFilter;
    }

    public static Sort reduceToValidColumns(Sort fullSort, TableInfo... tables)
    {
        Sort validSort = new Sort();
        List<Sort.SortField> sortList = fullSort.getSortList();
        for (int i = sortList.size() - 1; i >=0; i--)
        {
            Sort.SortField field = sortList.get(i);
            boolean validClause = false;
            String columnName = field.getColumnName();
            for (TableInfo table : tables)
            {
                if (table.getColumn(columnName) != null)
                {
                    validClause = true;
                }
                else
                {
                    int index = columnName.lastIndexOf('.');
                    if (index != -1 && table.getColumn(columnName.substring(index + 1)) != null)
                    {
                        validClause = true;
                    }
                }
            }
            if (validClause)
            {
                validSort.insertSort(new Sort(field.getSortDirection().getDir() + field.getColumnName()));
            }
        }
        return validSort;
    }

    public static SimpleFilter getPeptideFilter(ActionURL currentUrl, int mask, User user, MS2Run... runs)
    {
        return getPeptideFilter(currentUrl, mask, null, user, runs);
    }

    public static SimpleFilter getPeptideFilter(ActionURL currentUrl, int mask, String runTableName, User user, MS2Run... runs)
    {
        return getTableFilter(currentUrl, mask, runTableName, MS2Manager.getDataRegionNamePeptides(), user, runs);
    }

    public static SimpleFilter getProteinFilter(ActionURL currentUrl, int mask, String runTableName, User user, MS2Run... runs)
    {
        return getTableFilter(currentUrl, mask, runTableName, MS2Manager.getDataRegionNameProteins(), user, runs);
    }

    public static SimpleFilter getProteinGroupFilter(ActionURL currentUrl, int mask, String runTableName, User user, MS2Run... runs)
    {
        return getTableFilter(currentUrl, mask, runTableName, MS2Manager.getDataRegionNameProteinGroups(), user, runs);
    }

    public static SimpleFilter getTableFilter(ActionURL currentUrl, int mask, String runTableName, String dataRegionName, User user, MS2Run... runs)
    {
        SimpleFilter filter = new SimpleFilter();

        if ((mask & RUN_FILTER) != 0)
        {
            addRunCondition(filter, runTableName, runs);
        }

        if ((mask & URL_FILTER) != 0)
            filter.addUrlFilters(currentUrl, dataRegionName);

        if ((mask & EXTRA_FILTER) != 0)
            addExtraFilter(filter, runs[0], currentUrl);

        if ((mask & PROTEIN_FILTER) != 0)
        {
            String groupNumber = currentUrl.getParameter("groupNumber");
            String indistId = currentUrl.getParameter("indistinguishableCollectionId");
            if (null != groupNumber)
            {
                try
                {
                    filter.addClause(new ProteinGroupFilter(Integer.parseInt(groupNumber), null == indistId ? 0 : Integer.parseInt(indistId)));
                    return filter;
                }
                catch (NumberFormatException e)
                {
                    throw new NotFoundException("Bad groupNumber or indistinguishableCollectionId " + groupNumber + ", " + indistId);
                }
            }

            String groupRowId = currentUrl.getParameter("proteinGroupId");
            if (groupRowId != null)
            {
                try
                {
                    filter.addCondition(FieldKey.fromParts("ProteinProphetData", "ProteinGroupId", "RowId"), Integer.parseInt(groupRowId));
                }
                catch (NumberFormatException e)
                {
                    throw new NotFoundException("Bad proteinGroupId " + groupRowId);
                }
                return filter;
            }

            String seqId = currentUrl.getParameter("seqId");
            if (null != seqId)
            {
                try
                {
                    // if "all peptides" flag is set, add a filter to match peptides to the seqid on the url
                    // rather than just filtering for search engine protein.
                    if (ProteinManager.showAllPeptides(currentUrl, user))
                    {
                        filter.addClause(new SequenceFilter(Integer.parseInt(seqId)));
                    }
                    else
                        filter.addCondition(FieldKey.fromParts("SeqId"), Integer.parseInt(seqId));
                }
                catch (NumberFormatException e)
                {
                    throw new NotFoundException("Bad seqId " + seqId);
                }
            }
        }
        return filter;
    }

    public static boolean showAllPeptides(ActionURL url, User user)
    {
        // First look for a value on the URL
        String param = url.getParameter(MS2Controller.ProteinViewBean.ALL_PEPTIDES_URL_PARAM);
        if (param != null)
        {
            boolean result = Boolean.parseBoolean(param);
            // Stash as the user's preference
            PreferenceService.get().setProperty(ALL_PEPTIDES_PREFERENCE_NAME, Boolean.toString(result), user);
            return result;
        }
        // Next check if the user has a preference stored
        param = PreferenceService.get().getProperty(ALL_PEPTIDES_PREFERENCE_NAME, user);
        if (param != null)
        {
            return Boolean.parseBoolean(param);
        }
        // Otherwise go with the default
        return false;
    }

    public static class SequenceFilter extends SimpleFilter.FilterClause
    {
        int _seqid;
        String _sequence;
        String _bestName;

        public SequenceFilter(int seqid)
        {
            _seqid = seqid;
            Protein prot = getProtein(seqid);
            _sequence = prot.getSequence();
            _bestName = prot.getBestName();
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            SQLFragment sqlf = new SQLFragment();
            sqlf.append(dialect.getStringIndexOfFunction(new SQLFragment("TrimmedPeptide"), new SQLFragment("?", _sequence)));
            sqlf.append( " > 0 ");
            return sqlf;
        }

        public List<FieldKey> getFieldKeys()
        {
            return Arrays.asList(FieldKey.fromParts("TrimmedPeptide"));
        }

        @Override
        protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
        {
            sb.append("Matches sequence of ");
            sb.append(_bestName);
        }
    }

    public static SimpleFilter.FilterClause getSequencesFilter(List<Integer> targetSeqIds)
    {
        SimpleFilter.FilterClause[] proteinClauses = new SimpleFilter.FilterClause[targetSeqIds.size()];
        int seqIndex = 0;
        for (Integer targetSeqId : targetSeqIds)
        {
            proteinClauses[seqIndex++] = (new ProteinManager.SequenceFilter(targetSeqId));
        }
        return new SimpleFilter.OrClause(proteinClauses);
    }

    public static class ProteinGroupFilter extends SimpleFilter.FilterClause
    {
        int _groupNum;
        int _indistinguishableProteinId;

        public ProteinGroupFilter(int groupNum, int indistId)
        {
            _groupNum = groupNum;
            _indistinguishableProteinId = indistId;
        }

        @Override
        public String getLabKeySQLWhereClause(Map<FieldKey, ? extends ColumnInfo> columnMap)
        {
            throw new UnsupportedOperationException();
        }

        public SQLFragment toSQLFragment(Map<FieldKey, ? extends ColumnInfo> columnMap, SqlDialect dialect)
        {
            SQLFragment sqlf = new SQLFragment();
            sqlf.append(" RowId IN (SELECT pm.PeptideId FROM ").append(String.valueOf(MS2Manager.getTableInfoPeptideMemberships())).append(" pm ");
            sqlf.append(" INNER JOIN ").append(String.valueOf(MS2Manager.getTableInfoProteinGroups())).append(" pg  ON (pm.ProteinGroupId = pg.RowId) \n");
            sqlf.append(" WHERE pg.GroupNumber = ").append(String.valueOf(_groupNum)).append("  and pg.IndistinguishableCollectionId = ").append(String.valueOf(_indistinguishableProteinId)).append(" ) ");
            return sqlf;
        }

        public List<FieldKey> getFieldKeys()
        {
            return Arrays.asList(FieldKey.fromParts("RowId"));
        }
         @Override
         protected void appendFilterText(StringBuilder sb, SimpleFilter.ColumnNameFormatter formatter)
         {
             sb.append("Peptide member of ProteinGroup ").append(_groupNum);
             if (_indistinguishableProteinId > 0)
             {
                 sb.append("-");
                 sb.append(_indistinguishableProteinId);
             }
         }
     }

    public static void addRunCondition(SimpleFilter filter, @Nullable String runTableName, MS2Run... runs)
    {
        String columnName = (runTableName == null ? "Run" : runTableName + ".Run");
        StringBuilder sb = new StringBuilder();
        sb.append(columnName);
        sb.append(" IN (");
        String separator = "";
        for (MS2Run run : runs)
        {
            sb.append(separator);
            separator = ", ";
            sb.append(run.getRun());
        }
        sb.append(")");
        filter.addWhereClause(sb.toString(), new Object[0], FieldKey.fromString("Run"));
    }


    // TODO: runTableName is null in all cases... remove parameter?
    public static void replaceRunCondition(SimpleFilter filter, @Nullable String runTableName, MS2Run... runs)
    {
        filter.deleteConditions(runTableName == null ? FieldKey.fromParts("Run") : FieldKey.fromParts(runTableName, "Run"));
        addRunCondition(filter, runTableName, runs);
    }


    public static void addProteinQuery(SQLFragment sql, MS2Run run, ActionURL currentUrl, String extraPeptideWhere, int maxRows, boolean peptideQuery, User user)
    {
        // SELECT (TOP n) Protein, SequenceMass, etc.
        SQLFragment proteinSql = new SQLFragment("SELECT Protein");

        // If this query is a subselect of proteins to which we join the peptide table, we need to:
        // 1. Alias Protein to prevent SELECT and ORDER BY ambiguity after joining to the peptides table (easier to do this than to disambiguate outside the subselect)
        // 2. Include SequenceMass since we're not joining again to the ProteinSequence table (we do this in the protein version to get Sequence, but don't need it here)
        if (peptideQuery)
            proteinSql.append(" AS PProtein, prot.BestName, prot.BestGeneName");

        proteinSql.append(", prot.Mass AS SequenceMass, COUNT(Peptide) AS Peptides, COUNT(DISTINCT Peptide) AS UniquePeptides, pep.SeqId AS sSeqId\n");
        proteinSql.append("FROM (SELECT * FROM ");
        proteinSql.append(MS2Manager.getTableInfoPeptides());
        proteinSql.append(' ');

        // Construct Peptide WHERE clause (no need to sort by peptide)
        SimpleFilter peptideFilter = getPeptideFilter(currentUrl, RUN_FILTER + URL_FILTER + EXTRA_FILTER, user, run);
        peptideFilter = ProteinManager.reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoPeptides());
        if (null != extraPeptideWhere)
            peptideFilter.addWhereClause(extraPeptideWhere, new Object[]{});
        proteinSql.append(peptideFilter.getWhereSQL(MS2Manager.getTableInfoPeptides()));
        sql.addAll(peptideFilter.getWhereParams(MS2Manager.getTableInfoPeptides()));

        proteinSql.append(") pep LEFT OUTER JOIN ");
        proteinSql.append(getTableInfoSequences(), "prot");
        proteinSql.append(" ON prot.SeqId = pep.SeqId\n");
        proteinSql.append("GROUP BY Protein, prot.Mass, pep.SeqId, prot.BestGeneName, prot.BestName, prot.Description, prot.SeqId\n");

        // Construct Protein HAVING clause
        SimpleFilter proteinFilter = new SimpleFilter(currentUrl, MS2Manager.getDataRegionNameProteins());
        proteinFilter = ProteinManager.reduceToValidColumns(proteinFilter, MS2Manager.getTableInfoProteins());
        String proteinHaving = proteinFilter.getWhereSQL(MS2Manager.getTableInfoProteins()).replaceFirst("WHERE", "HAVING");

        // Can't use SELECT aliases in HAVING clause, so replace names with aggregate functions & disambiguate Mass
        proteinHaving = proteinHaving.replaceAll("UniquePeptides", "COUNT(DISTINCT Peptide)");
        proteinHaving = proteinHaving.replaceAll("Peptides", "COUNT(Peptide)");
        proteinHaving = proteinHaving.replaceAll("SeqId", "prot.SeqId");
        proteinHaving = proteinHaving.replaceAll("SequenceMass", "prot.Mass");
        proteinHaving = proteinHaving.replaceAll("Description", "prot.Description");
        sql.addAll(proteinFilter.getWhereParams(MS2Manager.getTableInfoProteins()));
        proteinSql.append(proteinHaving);

        if (!"".equals(proteinHaving))
            proteinSql.append('\n');

        // If we're limiting the number of proteins (e.g., no more than 250) we need to add the protein ORDER BY clause so we get the right rows.
        if (maxRows > 0)
        {
            Sort proteinSort = new Sort("Protein");
            proteinSort.addURLSort(currentUrl, MS2Manager.getDataRegionNameProteins());
            String proteinOrderBy = proteinSort.getOrderByClause(getSqlDialect());
            proteinOrderBy = proteinOrderBy.replaceAll("Description", "prot.Description");
            proteinSql.append(proteinOrderBy);

            getSqlDialect().limitRows(proteinSql, maxRows + 1);
        }

        sql.append(proteinSql);
    }

    public static ResultSet getProteinRS(ActionURL currentUrl, MS2Run run, String extraPeptideWhere, int maxRows, User user)
    {
        SQLFragment sql = getProteinSql(currentUrl, run, extraPeptideWhere, maxRows, user);

        return new SqlSelector(getSchema(), sql).setMaxRows(maxRows).getResultSet();
    }

    public static SQLFragment getProteinSql(ActionURL currentUrl, MS2Run run, String extraPeptideWhere, int maxRows, User user)
    {
        SQLFragment sql = new SQLFragment();

        // Join the selected proteins to ProteinSequences to get the actual Sequence for computing AA coverage
        // We need to do a second join to ProteinSequences because we can't GROUP BY Sequence, a text data type
        sql.append("SELECT Protein, SequenceMass, Peptides, UniquePeptides, SeqId, ProtSequence AS Sequence, Description, BestName, BestGeneName FROM\n(");
        addProteinQuery(sql, run, currentUrl, extraPeptideWhere, maxRows, false, user);
        sql.append("\n) X LEFT OUTER JOIN ");
        sql.append(getTableInfoSequences(), "seq");
        sql.append(" ON seq.SeqId = sSeqId\n");

        // Have to sort again to ensure correct order after the join
        Sort proteinSort = new Sort("Protein");
        proteinSort.addURLSort(currentUrl, MS2Manager.getDataRegionNameProteins());
        sql.append(proteinSort.getOrderByClause(getSqlDialect()));

        return sql;
    }

    public static ResultSet getProteinProphetRS(ActionURL currentUrl, MS2Run run, String extraPeptideWhere, int maxRows, User user)
    {
        return new ResultSetCollapser(getProteinProphetPeptideRS(currentUrl, run, extraPeptideWhere, maxRows, "Scan", user), "ProteinGroupId", maxRows);
    }

    // Combine protein sort and peptide sort into a single ORDER BY.  Must sort by "Protein" before sorting peptides to ensure
    // grouping of peptides is in sync with protein query.  We add the columns in least to most significant order (peptide sort
    // + protein column + protein sort) and the Sort class ensures that only the most significant "Protein" column remains.
    public static String getCombinedOrderBy(ActionURL currentUrl, String orderByColumnName)
    {
        Sort peptideSort = ProteinManager.getPeptideBaseSort();
        peptideSort.addURLSort(currentUrl, MS2Manager.getDataRegionNamePeptides());
        Sort proteinSort = new Sort(currentUrl, MS2Manager.getDataRegionNameProteins());
        Sort combinedSort = new Sort();
        combinedSort.insertSort(peptideSort);
        combinedSort.insertSortColumn(orderByColumnName, false);
        combinedSort.insertSort(proteinSort);
        combinedSort = reduceToValidColumns(combinedSort, MS2Manager.getTableInfoPeptides(), MS2Manager.getTableInfoProteins());
        return combinedSort.getOrderByClause(ProteinManager.getSqlDialect());
    }

    // Combine protein sort and peptide sort into a single ORDER BY.  Must sort by "Protein" before sorting peptides to ensure
    // grouping of peptides is in sync with protein query.  We add the columns in least to most significant order (peptide sort
    // + protein column + protein sort) and the Sort class ensures that only the most significant "Protein" column remains.
    public static String getProteinGroupCombinedOrderBy(ActionURL currentUrl, String orderByColumnName)
    {
        Sort peptideSort = ProteinManager.getPeptideBaseSort();
        peptideSort.addURLSort(currentUrl, MS2Manager.getDataRegionNamePeptides());
        Sort proteinSort = new Sort(currentUrl, MS2Manager.getDataRegionNameProteinGroups());
        Sort combinedSort = new Sort();
        combinedSort.insertSort(peptideSort);
        combinedSort.insertSortColumn(orderByColumnName, false);
        combinedSort.insertSort(proteinSort);
        combinedSort = reduceToValidColumns(combinedSort, MS2Manager.getTableInfoSimplePeptides(), MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        return combinedSort.getOrderByClause(ProteinManager.getSqlDialect());
    }


    // extraWhere is used to insert an IN clause when exporting selected proteins
    public static GroupedResultSet getPeptideRS(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames, User user)
    {
        SQLFragment sql = getPeptideSql(currentUrl, run, extraWhere, maxProteinRows, columnNames, user);

        ResultSet rs = new SqlSelector(getSchema(), sql).getResultSet(false, true);
        return new GroupedResultSet(rs, "Protein");
    }

    public static SQLFragment getPeptideSql(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames, User user)
    {
        return getPeptideSql(currentUrl, run, extraWhere, maxProteinRows, columnNames, true, user);
    }

    // extraWhere is used to insert an IN clause when exporting selected proteins
    public static SQLFragment getPeptideSql(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames, boolean addOrderBy, User user)
    {
        SQLFragment sql = new SQLFragment();

        // SELECT TOP n m AS Run, Protein, etc.
        sql.append("SELECT ");
        sql.append(columnNames);
        sql.append(", Fraction AS Fraction$Fraction FROM ");
        sql.append(MS2Manager.getTableInfoPeptides());
        sql.append(" RIGHT OUTER JOIN\n(");

        ProteinManager.addProteinQuery(sql, run, currentUrl, extraWhere, maxProteinRows, true, user);
        sql.append(") s ON ");
        sql.append(MS2Manager.getTableInfoPeptides());
        sql.append(".SeqId = sSeqId\n");

        // Have to apply the peptide filter again, otherwise we'll just get all peptides mapping to each protein
        SimpleFilter peptideFilter = ProteinManager.getPeptideFilter(currentUrl, ProteinManager.RUN_FILTER + ProteinManager.URL_FILTER + ProteinManager.EXTRA_FILTER, user, run);
        peptideFilter = reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoPeptides());
        sql.append(peptideFilter.getWhereSQL(MS2Manager.getTableInfoPeptides()));
        sql.append('\n');
        sql.addAll(peptideFilter.getWhereParams(MS2Manager.getTableInfoPeptides()));
        if (addOrderBy)
            sql.append(getCombinedOrderBy(currentUrl, "Protein"));

        return sql;
    }

    // extraWhere is used to insert an IN clause when exporting selected proteins
    public static TableResultSet getProteinProphetPeptideRS(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames, User user)
    {
        SQLFragment sql = getProteinProphetPeptideSql(currentUrl, run, extraWhere, maxProteinRows, columnNames, user);

        return new SqlSelector(getSchema(), sql).getResultSet(false, true);
    }

    public static SQLFragment getProteinProphetPeptideSql(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames, User user)
    {
        return getProteinProphetPeptideSql(currentUrl, run, extraWhere, maxProteinRows, columnNames, true, user);
    }

    // extraWhere is used to insert an IN clause when exporting selected proteins
    public static SQLFragment getProteinProphetPeptideSql(ActionURL currentUrl, MS2Run run, String extraWhere, int maxProteinRows, String columnNames, boolean addOrderBy, User user)
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append(MS2Manager.getTableInfoPeptideMemberships());
        sql.append(".ProteinGroupId, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".IndistinguishableCollectionId, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".GroupNumber, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".GroupProbability, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".ErrorRate, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".ProteinProbability, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".UniquePeptidesCount, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".TotalNumberPeptides, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".PctSpectrumIds, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".PercentCoverage, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".RatioMean, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".RatioStandardDev, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".RatioNumberPeptides, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".Heavy2LightRatioMean, ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation() + ".Heavy2LightRatioStandardDev, ");
        sql.append(MS2Manager.getTableInfoSimplePeptides() + ".Fraction AS Fraction$Fraction, ");
        sql.append(columnNames);

        sql.append(" FROM ");
        sql.append(MS2Manager.getTableInfoSimplePeptides());
        sql.append(" INNER JOIN\n");
        sql.append(MS2Manager.getTableInfoPeptideMemberships());
        sql.append(" ON ");
        sql.append(MS2Manager.getTableInfoSimplePeptides());
        sql.append(".RowId = ");
        sql.append(MS2Manager.getTableInfoPeptideMemberships());
        sql.append(".PeptideId INNER JOIN\n");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        sql.append(" ON ");
        sql.append(MS2Manager.getTableInfoPeptideMemberships());
        sql.append(".ProteinGroupId = ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        sql.append(".RowId INNER JOIN\n");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles());
        sql.append(" ON ");
        sql.append(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        sql.append(".ProteinProphetFileId = ");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles());
        sql.append(".RowId");
        sql.append(" AND ");
        sql.append(MS2Manager.getTableInfoProteinProphetFiles());
        sql.append(".Run = ");
        sql.append(MS2Manager.getTableInfoSimplePeptides());
        sql.append(".Run\n");

        // Construct Peptide WHERE clause (no need to sort by peptide)
        SimpleFilter peptideFilter = getPeptideFilter(currentUrl, RUN_FILTER + URL_FILTER + EXTRA_FILTER, MS2Manager.getTableInfoSimplePeptides().toString(), user, run);
        peptideFilter = reduceToValidColumns(peptideFilter, MS2Manager.getTableInfoSimplePeptides());
        if (null != extraWhere)
            peptideFilter.addWhereClause(extraWhere, new Object[]{});
        sql.append(peptideFilter.getWhereSQL(MS2Manager.getTableInfoPeptides()));
        sql.addAll(peptideFilter.getWhereParams(MS2Manager.getTableInfoPeptides()));

        sql.append("\n");

        SimpleFilter proteinFilter = new SimpleFilter(currentUrl, MS2Manager.getDataRegionNameProteinGroups());
        // Translate filters from query-style nested ProteinProphet params to direct filters on the protein group table
        for (SimpleFilter.FilterClause clause : new SimpleFilter(currentUrl, MS2Manager.getDataRegionNamePeptides()).getClauses())
        {
            if (clause instanceof CompareType.CompareClause && !clause.getFieldKeys().isEmpty())
            {
                CompareType.CompareClause compareClause = (CompareType.CompareClause) clause;
                List<String> fieldKeyParts = clause.getFieldKeys().get(0).getParts();
                // Strip off the ProteinProphetData/ProteinGroupId FieldKey prefix
                if (fieldKeyParts.size() > 2 && "ProteinProphetData".equalsIgnoreCase(fieldKeyParts.get(0)) && "ProteinGroupId".equalsIgnoreCase(fieldKeyParts.get(1)))
                {
                    Object value = compareClause.getParamVals().length > 0 ? compareClause.getParamVals()[0] : null;
                    proteinFilter.addClause(new CompareType.CompareClause(FieldKey.fromParts(fieldKeyParts.subList(2, fieldKeyParts.size())), compareClause.getCompareType(), value));
                }
            }
        }
        proteinFilter = reduceToValidColumns(proteinFilter, MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        String proteinWhere = proteinFilter.getWhereSQL(MS2Manager.getTableInfoProteinGroupsWithQuantitation());
        if (proteinWhere != null && !"".equals(proteinWhere))
        {
            proteinWhere = proteinWhere.replaceFirst("WHERE", "AND");
            sql.addAll(proteinFilter.getWhereParams(MS2Manager.getTableInfoProteinGroupsWithQuantitation()));
            sql.append(proteinWhere);
            sql.append('\n');
        }
        if (addOrderBy)
        {
            // Work around ambiguous column problem on SQL Server 2000.  Need this string replacement hack since Sort
            // doesn't handle schema/table-qualified names correctly and a simple aliasing of the column name breaks
            // expectations in other code.  See #10460.
            String orderBy = getProteinGroupCombinedOrderBy(currentUrl, "ProteinGroupId");
            sql.append(orderBy.replace("ProteinGroupId", MS2Manager.getTableInfoPeptideMemberships() + ".ProteinGroupId"));
        }
        if (maxProteinRows > 0)
        {
            getSqlDialect().limitRows(sql, maxProteinRows + 1);
        }

        return sql;
    }

    public static MultiValuedMap<String, String> getIdentifiersFromId(int seqid)
    {
        final MultiValuedMap<String, String> map = new ArrayListValuedHashMap<>();

        new SqlSelector(getSchema(),
                "SELECT T.name AS name, I.identifier\n" +
                "FROM " + getTableInfoIdentifiers() + " I INNER JOIN " + getTableInfoIdentTypes() + " T ON I.identtypeid = T.identtypeid\n" +
                "WHERE seqId = ?",
                seqid).forEach(new Selector.ForEachBlock<ResultSet>()
        {
            @Override
            public void exec(ResultSet rs) throws SQLException
            {
                String name = rs.getString(1).toLowerCase();
                String id = rs.getString(2);
                if (name.startsWith("go_"))
                    name = "go";
                map.put(name, id);
            }
        });

        return map;
    }


    public static Set<String> getOrganismsFromId(int id)
    {
        HashSet<String> retVal = new HashSet<>();
        List<String> rvString = new SqlSelector(getSchema(),
                "SELECT annotVal FROM " + getTableInfoAnnotations() + " WHERE annotTypeId in (SELECT annotTypeId FROM " + getTableInfoAnnotationTypes() + " WHERE name " + getSqlDialect().getCharClassLikeOperator() + " '%Organism%') AND SeqId = ?",
                id).getArrayList(String.class);

        retVal.addAll(rvString);

        SQLFragment sql = new SQLFragment("SELECT " + getSchema().getSqlDialect().concatenate("genus", "' '", "species") +
                " FROM " + getTableInfoOrganisms() + " WHERE OrgId = " +
                "(SELECT OrgId FROM " + getTableInfoSequences() + " WHERE SeqId = ?)", id);
        String org = new SqlSelector(getSchema(), sql).getObject(String.class);
        retVal.add(org);

        return retVal;
    }


    public static String makeIdentURLString(String identifier, String infoSourceURLString)
    {
        if (identifier == null || infoSourceURLString == null)
            return null;

        try
        {
            identifier = java.net.URLEncoder.encode(identifier, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new UnexpectedException(e);
        }

        return infoSourceURLString.replaceAll("\\{\\}", identifier);
    }


    static final String NOTFOUND = "NOTFOUND";
    static final Map<String, String> cacheURLs = new ConcurrentHashMap<>(200);

    public static String makeIdentURLStringWithType(String identifier, String identType)
    {
        if (identifier == null || identType == null)
            return null;

        String url = cacheURLs.get(identType);
        if (url == null)
        {
            url = new SqlSelector(getSchema(),
                    "SELECT S.url\n" +
                    "FROM " + ProteinManager.getTableInfoInfoSources() + " S INNER JOIN " + ProteinManager.getTableInfoIdentTypes() +" T " +
                        "ON S.sourceId = T.cannonicalSourceId\n" +
                    "WHERE T.name=?",
                    identType).getObject(String.class);
            cacheURLs.put(identType, null==url ? NOTFOUND : url);
        }
        if (null == url || NOTFOUND.equals(url))
            return null;

        return makeIdentURLString(identifier, url);
    }


    public static String makeFullAnchorString(String url, String target, String txt)
    {
        if (txt == null) return "";
        String retVal = "";
        if (url != null) retVal += "<a ";
        if (url != null && target != null) retVal += "target='" + target + "' ";
        if (url != null) retVal += "href='" + url + "'>";
        retVal += txt;
        if (url != null) retVal += "</a>";
        return retVal;
    }

    public static String[] makeFullAnchorStringArray(Collection<String> idents, String target, String identType)
    {
        if (idents == null || idents.isEmpty() || identType == null)
            return new String[0];
        String[] retVal = new String[idents.size()];
        int i = 0;
        for (String ident : idents)
            retVal[i++] = makeFullAnchorString(makeIdentURLStringWithType(ident, identType), target, ident);
        return retVal;
    }

    public static String[] makeFullGOAnchorStringArray(Collection<String> goStrings, String target)
    {
        if (goStrings == null) return new String[0];
        String[] retVal = new String[goStrings.size()];
        int i=0;
        for (String go : goStrings)
        {
            String sub = go.indexOf(" ") == -1 ? go : go.substring(0, go.indexOf(" "));
            retVal[i++] = makeFullAnchorString(
                    makeIdentURLStringWithType(sub, "GO"),
                    target,
                    go
            );
        }
        return retVal;
    }


    /** Deletes all ProteinSequences, and the FastaFile record as well */
    public static void deleteFastaFile(int fastaId)
    {
        SqlExecutor executor = new SqlExecutor(getSchema());
        executor.execute("DELETE FROM " + getTableInfoFastaSequences() + " WHERE FastaId = ?", fastaId);
        executor.execute("UPDATE " + getTableInfoFastaFiles() + " SET Loaded=NULL WHERE FastaId = ?", fastaId);
        executor.execute("DELETE FROM " + getTableInfoFastaFiles() + " WHERE FastaId = ?", fastaId);
    }


    public static void deleteAnnotationInsertion(int id)
    {
        SQLFragment sql = new SQLFragment("DELETE FROM " + ProteinManager.getTableInfoAnnotInsertions() + " WHERE InsertId = ?");
        sql.add(id);

        new SqlExecutor(ProteinManager.getSchema()).execute(sql);
    }


    public static void indexProteins(@Nullable SearchService.IndexTask task, @Nullable Date modifiedSince)
    {
        if (1==1)
            return;
        if (null != modifiedSince)
        {
            Date d = new SqlSelector(getSchema(),"SELECT MAX(InsertDate) FROM prot.annotinsertions").getObject(Timestamp.class);
            if (null != d && d.compareTo(modifiedSince) <= 0)
                return;
        }

        if (null == task)
        {
            SearchService ss = SearchService.get();
            task = null == ss ? null : ss.createTask("Index Proteins");
            if (null == task)
                return;
        }

        final SearchService.IndexTask t = task;
        task.addRunnable(new Runnable(){
            public void run()
            {
                List<Integer> list = new SqlSelector(getSchema(), "SELECT SeqId FROM prot.Sequences").getArrayList(Integer.class);
                indexProteins(t, list);
            }
        }, SearchService.PRIORITY.background);
    }
    

    public static void indexProteins(final SearchService.IndexTask task, List<Integer> list)
    {
        int from = 0;
        while (from < list.size())
        {
            final int[] ids = new int[1000];
            int to=0;
            while (to < ids.length && from < list.size())
                ids[to++] = list.get(from++).intValue();
            task.addRunnable(new Runnable(){
                public void run()
                {
                    indexProteins(task,ids);
                }
            }, SearchService.PRIORITY.bulk);
        }
    }


    public static void indexProteins(SearchService.IndexTask task, int[] ids)
    {
        Container c = ContainerManager.getHomeContainer();
        ActionURL url = new ActionURL(MS2Controller.ShowProteinAction.class, c);

        if (0==1) // one at at time
        {
            for (int id : ids)
            {
                if (0==id)
                    continue;

                Protein p = getProtein(id);
                MultiValuedMap<String, String> map = getIdentifiersFromId(id);
                StringBuilder sb = new StringBuilder();
                sb.append(p.getBestName()).append("\n");
                sb.append(p.getDescription()).append("\n");
                for (String v : map.values())
                {
                    sb.append(v).append(" ");
                }

                String docid = "protein:" + id;
                Map<String,Object> m = new HashMap<>();
                m.put(SearchService.PROPERTY.categories.toString(), proteinCategory);
                m.put(SearchService.PROPERTY.title.toString(), "Protein " + p.getBestName());
                SimpleDocumentResource r = new SimpleDocumentResource(
                        new Path(docid),
                        docid,
                        c.getId(), "text/plain",
                        sb.toString(),
                        url.clone().addParameter("seqId",id),
                        m);
                task.addResource(r, SearchService.PRIORITY.item);
            }
        }
        else // fast query
        {
            SQLFragment sql = new SQLFragment();
            sql.append("SELECT I.seqid, S.BestName, S.Description, I.identifier\n");
            sql.append("FROM " + getTableInfoSequences() + " S INNER JOIN " + getTableInfoIdentifiers() + " I ON S.seqid = I.seqid\n");
            sql.append("WHERE I.seqid IN (");
            String comma = "";
            int count = 0;
            for (int id : ids)
            {
                if (id == 0) continue;
                count++;
                sql.append(comma);
                sql.append(id);
                comma = ",";
            }
            if (count == 0)
                return;
            sql.append(")\nORDER BY I.seqid");
            ResultSet rs = null;
            try
            {
                rs = new SqlSelector(getSchema(), sql).getResultSet(false);
                int curSeqId = 0;
                StringBuilder sb = null;

                int seqid;
                String bestName = "";
                String description = "";
                String ident = "";

                do
                {
                    seqid = 0;
                    if (rs.next())
                    {
                        seqid = rs.getInt(1);
                        bestName = rs.getString(2);
                        description = rs.getString(3);
                        ident = rs.getString(4);
                    }
                    if (seqid != curSeqId)
                    {
                        if (curSeqId > 0)
                        {
                            String docid = "protein:" + curSeqId;
                            Map<String, Object> m = new HashMap<>();
                            m.put(SearchService.PROPERTY.categories.toString(), proteinCategory);
                            m.put(SearchService.PROPERTY.title.toString(), "Protein " + bestName);
                            SimpleDocumentResource r = new SimpleDocumentResource(
                                    new Path(docid),
                                    docid,
                                    c.getId(), "text/plain",
                                    sb.toString(),
                                    url.clone().addParameter("seqId",curSeqId),
                                    m);
                            task.addResource(r, SearchService.PRIORITY.item);
                        }

                        sb = new StringBuilder(bestName + "\n" + description + "\n");
                        curSeqId = seqid;
                    }
                    sb.append(ident + " ");
                } while (seqid > 0);
            }
            catch (SQLException x)
            {
                throw new RuntimeSQLException(x);
            }
            finally
            {
                ResultSetUtil.close(rs);
            }
        }
    }
}
