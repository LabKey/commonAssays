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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.data.TableSelector;
import org.labkey.api.protein.Organism;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.protein.fasta.FastaDbLoader;
import org.labkey.api.protein.fasta.PeptideHelpers;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.HashHelpers;
import org.labkey.api.view.NotFoundException;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProteinManager
{
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

    // Note: All methods below have been migrated to org.labkey.api.protein.ProteinManager already

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
}
