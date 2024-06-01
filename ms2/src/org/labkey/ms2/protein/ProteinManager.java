/*
 * Copyright (c) 2005-2018 Fred Hutchinson Cancer Research Center
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

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.protein.Organism;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.protein.fasta.FastaFile;
import org.labkey.api.protein.fasta.PeptideHelpers;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.HashHelpers;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.Link.LinkBuilder;
import org.labkey.api.view.NotFoundException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ProteinManager
{
    public static final String NEGATIVE_HIT_PREFIX = "rev_";

    public static Protein getProtein(int seqId)
    {
        return new SqlSelector(ProteinSchema.getSchema(),
                "SELECT SeqId, ProtSequence AS Sequence, Mass, Description, BestName, BestGeneName FROM " + ProteinSchema.getTableInfoSequences() + " WHERE SeqId = ?",
                seqId).getObject(Protein.class);
    }

    private static Protein getProtein(String sequence, int organismId)
    {
        return new SqlSelector(ProteinSchema.getSchema(),
                "SELECT SeqId, ProtSequence AS Sequence, Mass, Description, BestName, BestGeneName FROM " + ProteinSchema.getTableInfoSequences() + " WHERE Hash = ? AND OrgId = ?",
                hashSequence(sequence), organismId).getObject(Protein.class);
    }

    public static FastaFile getFastaFile(int fastaId)
    {
        return new TableSelector(ProteinSchema.getTableInfoFastaFiles()).getObject(fastaId, FastaFile.class);
    }

    public static int ensureProtein(String sequence, String organismName, String name, String description)
    {
        Protein protein = ensureProteinInDatabase(sequence, organismName, name, description);
        return protein.getSeqId();
    }

    private static Protein ensureProteinInDatabase(String sequence, String organismName, String name, String description)
    {
        String genus = FastaDbLoader.extractGenus(organismName);
        String species = FastaDbLoader.extractSpecies(organismName);
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("species"), species);
        filter.addCondition(FieldKey.fromParts("genus"), genus);
        Organism organism = new TableSelector(ProteinSchema.getTableInfoOrganisms(), filter, null).getObject(Organism.class);
        if (organism == null)
        {
            organism = new Organism();
            organism.setGenus(genus);
            organism.setSpecies(species);
            organism = Table.insert(null, ProteinSchema.getTableInfoOrganisms(), organism);
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
            map.put("Mass", PeptideHelpers.computeMass(sequenceBytes, 0, sequenceBytes.length, PeptideHelpers.AMINO_ACID_AVERAGE_MASSES));
            map.put("OrgId", organism.getOrgId());
            map.put("Hash", hashSequence(sequence));
            map.put("Description", description == null ? null : (description.length() > 200 ? description.substring(0, 196) + "..." : description));
            map.put("BestName", name);
            map.put("Length", sequence.length());
            map.put("InsertDate", new Date());
            map.put("ChangeDate", new Date());

            Table.insert(null, ProteinSchema.getTableInfoSequences(), map);
            protein = getProtein(sequence, organism.getOrgId());
        }
        return protein;
    }

    public static void ensureIdentifiers(int seqId, Map<String, Set<String>> typeAndIdentifiers)
    {
        Protein protein = getProtein(seqId);
        if (protein == null)
        {
            throw new NotFoundException("SeqId " + seqId + " does not exist.");
        }
        ensureIdentifiers(protein, typeAndIdentifiers);
    }

    private static void ensureIdentifiers(Protein protein, Map<String, Set<String>> typeAndIdentifiers)
    {
        if (typeAndIdentifiers == null || typeAndIdentifiers.isEmpty())
        {
            return;
        }

        for(Map.Entry<String, Set<String>> typeAndIdentifier: typeAndIdentifiers.entrySet())
        {
            String identifierType = typeAndIdentifier.getKey();
            Set<String> identifiers = typeAndIdentifier.getValue();

            Integer identifierTypeId = ensureIdentifierType(identifierType);
            if (identifierTypeId == null)
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
        if (identifier == null || identifier.equalsIgnoreCase(protein.getBestName()))
        {
            return;
        }
        if (!identifierExists(identifier, identifierTypeId, protein.getSeqId()))
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
        Table.insert(null, ProteinSchema.getTableInfoIdentifiers(), values);
    }

    private static boolean identifierExists(String identifier, int identifierTypeId, int seqId)
    {
        SimpleFilter filter = new SimpleFilter();
        filter.addCondition(FieldKey.fromParts("identifier"), identifier);
        filter.addCondition(FieldKey.fromParts("identTypeId"), identifierTypeId);
        filter.addCondition(FieldKey.fromParts("seqId"), seqId);
        return new TableSelector(ProteinSchema.getTableInfoIdentifiers(), filter, null).exists();
    }

    @Nullable
    private static Integer ensureIdentifierType(String identifierType)
    {
        identifierType = StringUtils.trimToNull(identifierType);
        if (identifierType == null)
            return null;

        Integer identTypeId = new SqlSelector(ProteinSchema.getSchema(),
                            "SELECT MIN(identTypeId) FROM " + ProteinSchema.getTableInfoIdentTypes() + " WHERE LOWER(name) = ?",
                            identifierType.toLowerCase()).getObject(Integer.class);

        if (identTypeId == null)
        {
            Map<String, Object> map = new HashMap<>();
            map.put("identTypeId", null);
            map.put("name", identifierType);
            map.put("entryDate", new Date());
            map = Table.insert(null, ProteinSchema.getTableInfoIdentTypes(), map);
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

    public static MultiValuedMap<String, String> getIdentifiersFromId(int seqid)
    {
        final MultiValuedMap<String, String> map = new ArrayListValuedHashMap<>();

        new SqlSelector(ProteinSchema.getSchema(),
                "SELECT T.name AS name, I.identifier\n" +
                "FROM " + ProteinSchema.getTableInfoIdentifiers() + " I INNER JOIN " + ProteinSchema.getTableInfoIdentTypes() + " T ON I.identtypeid = T.identtypeid\n" +
                "WHERE seqId = ?",
                seqid).forEach(rs -> {
                    String name = rs.getString(1).toLowerCase();
                    String id = rs.getString(2);
                    if (name.startsWith("go_"))
                        name = "go";
                    map.put(name, id);
                });

        return map;
    }

    public static Set<String> getOrganismsFromId(int id)
    {
        Set<String> retVal = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> rvString = new SqlSelector(ProteinSchema.getSchema(),
                "SELECT annotVal FROM " + ProteinSchema.getTableInfoAnnotations() + " WHERE annotTypeId in (SELECT annotTypeId FROM " + ProteinSchema.getTableInfoAnnotationTypes() + " WHERE name " + ProteinSchema.getSqlDialect().getCharClassLikeOperator() + " '%Organism%') AND SeqId = ?",
                id).getArrayList(String.class);

        retVal.addAll(rvString);

        SQLFragment sql = new SQLFragment("SELECT " + ProteinSchema.getSchema().getSqlDialect().concatenate("genus", "' '", "species") +
                " FROM " + ProteinSchema.getTableInfoOrganisms() + " WHERE OrgId = " +
                "(SELECT OrgId FROM " + ProteinSchema.getTableInfoSequences() + " WHERE SeqId = ?)", id);
        String org = new SqlSelector(ProteinSchema.getSchema(), sql).getObject(String.class);
        retVal.add(org);

        return retVal;
    }

    public static String makeIdentURLString(String identifier, String infoSourceURLString)
    {
        if (identifier == null || infoSourceURLString == null)
            return null;

        identifier = java.net.URLEncoder.encode(identifier, StandardCharsets.UTF_8);

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
            url = new SqlSelector(ProteinSchema.getSchema(),
                    "SELECT S.url\n" +
                    "FROM " + ProteinSchema.getTableInfoInfoSources() + " S INNER JOIN " + ProteinSchema.getTableInfoIdentTypes() +" T " +
                        "ON S.sourceId = T.cannonicalSourceId\n" +
                    "WHERE T.name=?",
                    identType).getObject(String.class);
            cacheURLs.put(identType, null==url ? NOTFOUND : url);
        }
        if (null == url || NOTFOUND.equals(url))
            return null;

        return makeIdentURLString(identifier, url);
    }

    public static HtmlString makeFullAnchorLink(String url, String target, String txt)
    {
        if (null == txt)
            return HtmlString.EMPTY_STRING;

        if (null == url)
            return HtmlString.of(txt);

        return new LinkBuilder(txt).href(url).target(target).clearClasses().getHtmlString();
    }

    public static List<HtmlString> makeFullAnchorLinks(Collection<String> idents, String target, String identType)
    {
        if (idents == null || idents.isEmpty() || identType == null)
            return Collections.emptyList();

        return idents.stream()
            .map(ident->makeFullAnchorLink(makeIdentURLStringWithType(ident, identType), target, ident))
            .collect(Collectors.toList());
    }

    public static List<HtmlString> makeFullGOAnchorLinks(Collection<String> goStrings, String target)
    {
        if (goStrings == null)
            return Collections.emptyList();

        return goStrings.stream()
            .map(go->{
                String sub = !go.contains(" ") ? go : go.substring(0, go.indexOf(" "));
                return makeFullAnchorLink(makeIdentURLStringWithType(sub, "GO"), target, go);
            })
            .collect(Collectors.toList());
    }

    /** Deletes all ProteinSequences, and the FastaFile record as well */
    public static void deleteFastaFile(int fastaId)
    {
        SqlExecutor executor = new SqlExecutor(ProteinSchema.getSchema());
        executor.execute("DELETE FROM " + ProteinSchema.getTableInfoFastaSequences() + " WHERE FastaId = ?", fastaId);
        executor.execute("UPDATE " + ProteinSchema.getTableInfoFastaFiles() + " SET Loaded=NULL WHERE FastaId = ?", fastaId);
        executor.execute("DELETE FROM " + ProteinSchema.getTableInfoFastaFiles() + " WHERE FastaId = ?", fastaId);
    }

    public static void deleteAnnotationInsertion(int id)
    {
        SQLFragment sql = new SQLFragment("DELETE FROM " + ProteinSchema.getTableInfoAnnotInsertions() + " WHERE InsertId = ?");
        sql.add(id);

        new SqlExecutor(ProteinSchema.getSchema()).execute(sql);
    }
}
