/*
 * Copyright (c) 2006-2013 LabKey Corporation
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

import org.labkey.api.data.DbSchema;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableSelector;
import org.labkey.ms2.protein.IdentifierType;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.ProteinPlus;
import org.labkey.ms2.protein.fasta.Protein;
import org.labkey.ms2.protein.tools.ProteinDictionaryHelpers;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * User: brittp
 * Date: Jan 2, 2006
 * Time: 3:42:03 PM
 */
public class GuessOrgBySharedIdents extends Timer implements OrganismGuessStrategy
{
    //TODO: improve this heuristic to guess organism from FASTA header line
    //TODO: consider checking genus against organism table; consider putting options in as parameters
    //TODO: consider parsing TAX_ID
    private static final DbSchema _schema = ProteinManager.getSchema();

    private Map<String, String> _sprotCache = new WeakHashMap<>();
    private static final String CACHED_MISS_VALUE = "GuessOrgBySharedIdents.CACHED_MISS_VALUE";

    private enum SPROTload
    {
        not_tried_yet, tried_and_failed, tried_and_succeeded
    }

    SPROTload sprotLoadStatus = SPROTload.not_tried_yet;

    public String guess(ProteinPlus p)
    {
        //Is the first token on the defn line an identifier.  If
        //  so, do we already have it?  If so, what organisms is
        //  it associated with? Very slow!  Good for sprot fastas

        String pName = p.getProtein().getLookup();
        String wholeHeader = p.getProtein().getHeader();
        Map<String, Set<String>> possibleIdents = Protein.identParse(pName, wholeHeader);

        if (null == possibleIdents)
            return null;

        if (possibleIdents.containsKey(IdentifierType.SwissProt.toString()))
        {
            pName = possibleIdents.get(IdentifierType.SwissProt.toString()).iterator().next();
            return guessOrganismBySprotSuffix(pName);
        }

        return null;
    }

    public String guessOrganismBySprotSuffix(String pName)
    {
        if (sprotLoadStatus == SPROTload.not_tried_yet)
        {
            if (!new TableSelector(ProteinManager.getTableInfoSprotOrgMap()).exists())
            {
                try
                {
                    ProteinDictionaryHelpers.loadProtSprotOrgMap();
                }
                catch (SQLException e)
                {
                    throw new RuntimeSQLException(e);
                }
                sprotLoadStatus = SPROTload.tried_and_succeeded;
            }
            else
            {
                sprotLoadStatus = SPROTload.tried_and_succeeded;
            }
        }
        if (sprotLoadStatus == SPROTload.tried_and_failed) return null;
        String retVal;

        pName = pName.substring(pName.indexOf("_") + 1);
        retVal = _sprotCache.get(pName);
        if (CACHED_MISS_VALUE.equals(retVal))
            return null;
        if (retVal != null)
            return retVal;

        retVal = new SqlSelector(_schema,
                "SELECT " + _schema.getSqlDialect().concatenate("genus", "' '", "species") + " FROM " + ProteinManager.getTableInfoSprotOrgMap() + " " +
                        " WHERE SprotSuffix=?",
                pName).getObject(String.class);
        _sprotCache.put(pName, retVal != null ? retVal : CACHED_MISS_VALUE);
        return retVal;
    }

    public void close()
    {
    }
}
