/*
 * Copyright (c) 2004-2010 Fred Hutchinson Cancer Research Center
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: migra
 * Date: Jun 23, 2004
 * Time: 10:28:04 PM
 *
 */
public class Protein
{
    private String _header;
    private byte[] _bytes;
    private double _mass;
    private String _lookup;

    private String _origHeader;
    private Map<String, Set<String>> _identifierMap;

    //known identifier types.  Multiple identifiers found in fasta files can often
    //boil down to the same thing
    public static HashMap<String, String> IdentTypeMap = new HashMap<>();

    /* for parsing header lines of FASTA files */
    public static final String SEPARATOR_PATTERN = "\\|";

    //populate the hashmap of known identifier types
    static
    {
        IdentTypeMap.put("GI", "GI");
        IdentTypeMap.put("REF", "REFSEQ");
        IdentTypeMap.put("GB", "Genbank");
        IdentTypeMap.put("EMB", "Genbank");
        IdentTypeMap.put("SPROT_NAME", "SwissProt");
        IdentTypeMap.put("DBJ", "Genbank");
        IdentTypeMap.put("SP", "SwissProtAccn");
        IdentTypeMap.put("IPI", "IPI");
        IdentTypeMap.put("COG", "COG");
        IdentTypeMap.put("ENSEMBL", "ENSEMBL");
        IdentTypeMap.put("REFSEQ_NP", "REFSEQ");
        IdentTypeMap.put("PDB", "PDB");
        IdentTypeMap.put("UNIPROT/TREMBL", "SwissProtAccn");
        IdentTypeMap.put("TREMBL", "SwissProtAccn");
        IdentTypeMap.put("REFSEQ_XP", "REFSEQ");
        IdentTypeMap.put("ORFP", "SGD_LOCUS");
        IdentTypeMap.put("UNIPROT/SPROT", "SwissProtAccn");
        IdentTypeMap.put("SWISS-PROT", "SwissProtAccn");
        IdentTypeMap.put("TPG", "Genbank");
        IdentTypeMap.put("UG", "Unigene");
        IdentTypeMap.put("SI", "SI");
        IdentTypeMap.put("UPTR", "SwissProt");
        IdentTypeMap.put("UPSP", "SwissProt");
        IdentTypeMap.put("GP", "Genbank");
        IdentTypeMap.put("PIR", "PIR");
        IdentTypeMap.put("PIR2", "PIR");
        IdentTypeMap.put("UNIREF100", "UniRef100");
        IdentTypeMap.put("REFSEQ", "REFSEQ");
        IdentTypeMap.put("SGDID", "SGDID");
        IdentTypeMap.put("SGD_GN", "GeneName");
        IdentTypeMap.put("GN", "GeneName");
    }



    public Protein(String header, byte[] bytes)
    {
        _bytes = bytes;
        int firstAliasIndex = 0;

        _origHeader = header;

        // Check for special case of repeated gi| at start... if so, remove the initial text, but use it for lookup string
        if (header.startsWith("gi|"))
        {
            firstAliasIndex = header.indexOf(" gi|", 2) + 1;
            if (firstAliasIndex < 0 || firstAliasIndex > 30)
                firstAliasIndex = 0;
        }

        if (0 == firstAliasIndex)
        {
            header = header.replaceAll("\t", " "); // Some annoying FASTA files have tabs instead of spaces 

            int firstSpace = header.indexOf(" ");

            if (-1 != firstSpace)
                _lookup = header.substring(0, firstSpace).trim();
            else
                _lookup = header;

            if (_lookup.length() > 79)
                _lookup = _lookup.substring(0, 79);   // Comet truncates protein after first 79 characters
        }
        else
            _lookup = header.substring(0, firstAliasIndex).trim();

        int massStart = header.lastIndexOf("[MASS=");

        if (massStart >= 0)
        {
            try
            {
                int massEnd = header.indexOf(']', massStart);
                _mass = Double.parseDouble(header.substring(massStart + 6, massEnd));
            }
            catch(Exception e)
            {
                // fall through
            }
        }
        else
            massStart = header.length();

        if (0 == _mass)
            _mass = PeptideGenerator.computeMass(_bytes, 0, _bytes.length, PeptideGenerator.AMINO_ACID_AVERAGE_MASSES);

        _header = header.substring(firstAliasIndex, massStart);
    }

    public String getHeader()
    {
        return _header;
    }

    public void setHeader(String header)
    {
        _header = header;
    }

    public String getOrigHeader()
    {
       return _origHeader;
    }

    public void setOrigHeader(String h) {
       this._origHeader = h;
    }

    public String getName()
    {
        return getHeader().substring(0, Math.min(getHeader().length(), 80));
    }

    public String toString()
    {
        return getName();
    }

    public byte[] getBytes()
    {
        return _bytes;
    }

    public void setBytes(byte[] bytes)
    {
        _bytes = bytes;
    }

    public String getSequenceAsString()
    {
        return new String(getBytes());
    }

    public double getMass()
    {
        return _mass;
    }

    public String getLookup()
    {
        return _lookup;
    }

    public void setLookup(String lookup)
    {
        _lookup = lookup;
    }

    //lazily parse the header for identifiers
    public Map<String, Set<String>> getIdentifierMap()
    {
        if (_identifierMap == null)
        {
            String lookupString = _lookup;
            if (lookupString.startsWith("IPI") && !lookupString.contains("|") && _header.contains(" "))
            {
                lookupString = _header.substring(_header.indexOf(" ") + 1);
            }
            _identifierMap = identParse(lookupString,_header);
        }
        return _identifierMap;
    }

    /**
     *  New version of parseIdent using regular expressions.  Identifiers found in the lookup string
     *  portion of the header come in two basic flavors-- typed and untyped.  Typed ids look like
     *          <typename>|<idvalue>|<typename>|<idvalue>...
     *  Untyped ids are not separated into typename and value, but sometimes have a leading character
     *  sequence that identifies them (e.g. IPI id) or a reasonably well defined pattern (e.g. SwissProt
     *  names like HPH2_YEAST).  Regular expressions are used by the IdPattern class to recognize
     *  untyped identiers and to transform them as necessary.  The IdPattern class is also used
     *  to validate and transform typed identifiers in a small number of cases..
     *
     * 2/09/2008 added a third mechanism, a single regex that looks at the whole header rather than tokens
     *
     * @return a map of identifiers parsed from the header;  might be empty
     */
    public static Map<String, Set<String>> identParse(String fastaIdentifierString, String wholeHeader)
    {
        Map<String, Set<String>> identifiers = new HashMap<>();
        if (fastaIdentifierString == null) return identifiers;
        if (fastaIdentifierString.indexOf(" ") != -1) fastaIdentifierString = fastaIdentifierString.substring(0, fastaIdentifierString.indexOf(" "));
        fastaIdentifierString = fastaIdentifierString.replaceAll(":", "|");
        fastaIdentifierString = fastaIdentifierString.replace("|$", "");
        String tokens[] = fastaIdentifierString.split(SEPARATOR_PATTERN);

        for (int i = 0; i < tokens.length; i++)
        {
            Map<String, Set<String>>  additionalIds=null;
            // if the current token is the last or only token, or the token is
            // not recognized as an Identtype name, see if it matches a pattern in the list
            if ((i == tokens.length - 1) || (!IdentTypeMap.containsKey(tokens[i].toUpperCase())))
            {
                for (String typeName : IdPattern.UNTYPED_ID_PATTERN_LIST)
                {
                    additionalIds = IdPattern.ID_PATTERN_MAP.get(typeName).getIdFromPattern(tokens, i);
                    if (null!=additionalIds) break;
                }
            }

            // if the pattern matching found identifiers, add them to the map and
            // go to the next token.  if the pattern matching found multiple identifiers,
            // bump the token an extra bump for each identifier beyond 1
            if (null!=additionalIds && additionalIds.size() > 0)
            {
                identifiers = IdPattern.addIdMap(identifiers, additionalIds);
                i = i + additionalIds.size() -1 ;
                continue;
            }

            String key = tokens[i];

            if (key.equalsIgnoreCase("gnl"))
            {
                if (i < (tokens.length - 2))
                {
                    key = tokens[++i];
                }
            }

            String value = null;

            if (i + 1 < tokens.length)
            {
                value = tokens[++i];
            }

            if (value != null && IdentTypeMap.containsKey(key.toUpperCase()))
            {
                String newKey = IdentTypeMap.get(key.toUpperCase());
                if (IdPattern.TYPED_ID_PATTERN_LIST.contains(newKey))
                    additionalIds = IdPattern.ID_PATTERN_MAP.get(newKey).getIdFromPattern(new String[]{value}, 0);
                else
                    additionalIds = IdPattern.createIdMap(newKey, value);

                identifiers = IdPattern.addIdMap(identifiers, additionalIds);
            }
        }

        if (wholeHeader != null)
        {
            for (String typeName : IdPattern.WHOLE_HEADER_ID_PATTERN_LIST)
            {
                Map<String, Set<String>> additionalIds = IdPattern.ID_PATTERN_MAP.get(typeName).getIdFromPattern(new String[]{wholeHeader}, 0);

                if (null != additionalIds && additionalIds.size() > 0)
                {
                    identifiers = IdPattern.addIdMap(identifiers, additionalIds);
                }
            }

        }

        return identifiers;
    }
}
