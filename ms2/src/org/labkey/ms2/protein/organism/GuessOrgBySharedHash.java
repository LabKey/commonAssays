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

import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.ms2.protein.ProteinPlus;
import org.labkey.api.data.DbSchema;

import java.util.Map;
import java.util.HashMap;

/**
 * User: brittp
 * Date: Jan 2, 2006
 * Time: 3:42:03 PM
 */
public class GuessOrgBySharedHash extends Timer implements OrganismGuessStrategy
{
    private static final String CACHED_MISS_VALUE = "GuessOrgBySharedHash.CACHED_MISS_VALUE";
    private Map<String, String> _cache = new HashMap<>();  // TODO: This could easily blow out all available memory for large FASTA; once we enable this guessing strategy, switch to Map with limit
    private static final DbSchema _schema = ProteinManager.getSchema();
    private static final SQLFragment HASHCMD;

    static
    {
        SQLFragment sql = new SQLFragment("SELECT ");
        sql.append(_schema.getSqlDialect().concatenate("o.genus", "' '", "o.species"));
        sql.append(" FROM ");
        sql.append(ProteinManager.getTableInfoSequences(), "s");
        sql.append(" JOIN ");
        sql.append(ProteinManager.getTableInfoOrganisms(), "o");
        sql.append(" ON (o.orgid=s.orgid) WHERE s.hash = ? ");
        sql.append(" GROUP BY o.orgid, o.genus, o.species ORDER BY COUNT(*) DESC");
        _schema.getSqlDialect().limitRows(sql, 1);
        HASHCMD = sql;
    }

    public String guess(ProteinPlus p)
    {
        startTimer();
        String retVal = _cache.get(p.getHash());
        if (CACHED_MISS_VALUE.equals(retVal))
            return null;
        if (retVal != null)
            return retVal;

        SQLFragment sql = new SQLFragment(HASHCMD);
        sql.add(p.getHash());
        retVal = new SqlSelector(_schema, sql).getObject(String.class);
        _cache.put(p.getHash(), retVal != null ? retVal : CACHED_MISS_VALUE);
        stopTimer();
        return retVal;
    }

    public void close()
    {
    }
}
