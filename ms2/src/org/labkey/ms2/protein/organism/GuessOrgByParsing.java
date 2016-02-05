/*
 * Copyright (c) 2006-2016 LabKey Corporation
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

package org.labkey.ms2.protein.organism;

import org.apache.log4j.Logger;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.SqlSelector;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.ProteinPlus;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * User: brittp
 * Date: Jan 2, 2006
 * Time: 3:42:03 PM
 */
public class GuessOrgByParsing extends Timer implements OrganismGuessStrategy
{
    private Map<String, String> _cache = new HashMap<>();
    private static final DbSchema _schema = ProteinManager.getSchema();
    private static final String CACHED_MISS_VALUE = "GuessOrgByParsing.CACHED_MISS_VALUE";
    private static Logger _log = Logger.getLogger(GuessOrgByParsing.class);

    private static final String _organismFromTaxIdSql =
                                    "SELECT " + _schema.getSqlDialect().concatenate("genus", "' '", "species") + " FROM " +
                                            ProteinManager.getTableInfoOrganisms() +
                                            " WHERE identid = (SELECT identid FROM " +
                                            ProteinManager.getTableInfoIdentifiers() +
                                            " WHERE identifier=? AND identtypeid = " + getNcbiId() + ")";

    private static Integer _ncbiId = null;

    public static Integer getNcbiId()
    {
        if (null == _ncbiId)
            _ncbiId = new SqlSelector(_schema,
                "SELECT identtypeid FROM " + ProteinManager.getTableInfoIdentTypes() + " WHERE Name='NCBI Taxonomy'").getObject(Integer.class);

        return _ncbiId;
    }


    public String guess(ProteinPlus p)
    {
        //First check for Tax_id=nnnnn.  If we don't find it, check for
        // [Genus species].  Two gotchas:  sometimes there are several
        // organisms mentioned. (1) We must make sure that we only have one.
        // (2) Lots of stuff other than [Genus species] appears in square
        // brackets.  We throw out the obvious garbage.
        startTimer();
        String header = p.getProtein().getOrigHeader().toUpperCase();
        int taxpos = header.indexOf("TAX_ID");
        if (taxpos != -1)
        {
            String tmp[] = header.substring(taxpos).split("=");
            if (tmp.length > 1)
            {
                String taxid = tmp[1].split("[^\\d]")[0];
                String retVal = _cache.get(taxid);
                boolean cachedMiss = retVal != null && retVal.equals(CACHED_MISS_VALUE);
                if (retVal != null && !cachedMiss)
                    return retVal;

                if (!cachedMiss)
                {
                    retVal = new SqlSelector(_schema, _organismFromTaxIdSql, taxid).getObject(String.class);

                    _cache.put(taxid, retVal != null ? retVal : CACHED_MISS_VALUE);
                    if (retVal != null)
                        return retVal;
                }
            }
        }


        String line = p.getProtein().getOrigHeader().replaceAll("\\01", "]").trim();
        int leftPos;
        int rightPos = 0;
        String phrase;
        Hashtable<String, String> org = new Hashtable<>();
        String key = null;

        while ((leftPos = line.indexOf('[', rightPos)) > -1)
        {
            rightPos = line.indexOf(']', leftPos);
            if (rightPos == -1)
            {
                break;
            }
            phrase = line.substring(leftPos + 1, rightPos).toUpperCase();
            //TODO: These *so* much need to be databases as "exceptions"
            if (phrase.isEmpty()) continue;
            if (!Character.isUpperCase(phrase.charAt(0))) continue;
            if (phrase.indexOf('>') != -1) continue;
            if (phrase.indexOf('|') != -1) continue;
            if (phrase.startsWith("MASS=")) continue;
            if (phrase.startsWith("IMPORTED")) continue;
            if (phrase.startsWith("VALIDATED")) continue;
            if (phrase.startsWith("SIMILARITY")) continue;
            if (phrase.startsWith("ACCEPTED")) continue;
            if (phrase.startsWith("ACCEPTOR")) continue;
            if (phrase.contains("-COA")) continue;
            if (phrase.startsWith("ACYL-")) continue;
            if (phrase.startsWith("ACYLATING")) continue;
            if (phrase.startsWith("PROTEIN")) continue;
            if (phrase.startsWith("ACYL ")) continue;
            if (phrase.startsWith("GDP-")) continue;
            if (phrase.startsWith("GLUTAMINE-")) continue;
            if (phrase.startsWith("ADP-")) continue;
            if (phrase.startsWith("ALPHA-")) continue;
            if (phrase.contains("CONTAINS:")) continue;
            if (phrase.contains("INCLUDES:")) continue;
            if (phrase.contains("-QUINONE")) continue;
            if (phrase.contains("RIBULOSE")) continue;
            if (phrase.startsWith("PYRUVATE")) continue;
            if (phrase.length() < 7) continue;

            phrase = phrase.replaceAll(",", " ");
            phrase = phrase.replaceAll("=", " ");
            phrase = phrase.replaceAll("'", "");
            String words[] = phrase.toUpperCase().split("\\s+");

            if (words[0].indexOf('(') != -1) continue;
            if (words[0].equalsIgnoreCase("ORF")) continue;
            if (words[0].contains("POTASSIUM")) continue;
            if (words[0].equalsIgnoreCase("SODIUM")) continue;
            if (words[0].startsWith("NAD")) continue;
            if (words[0].contains("N-OXIDE")) continue;

            if (words.length >= 1) key = words[0].trim().toUpperCase();
            if (words.length >= 2) key += " " + words[1].trim().toUpperCase();
            if (words.length == 1) phrase += " sp.";
            org.put(key, phrase);
            leftPos = rightPos;
        }
        stopTimer();
        if (org.size() == 1)
        {
            return org.elements().nextElement();
        }
        else
        {
            return null;
        }
    }

    public void close()
    {
    }
}
