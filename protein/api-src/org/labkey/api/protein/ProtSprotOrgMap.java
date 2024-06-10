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
package org.labkey.api.protein;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.util.UnexpectedException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProtSprotOrgMap
{
    private static final Logger _log = LogManager.getLogger(ProtSprotOrgMap.class);
    private static final String FILE = "ProtSprotOrgMap.txt";
    private static final int SPOM_BATCH_SIZE = 1000;

    private static final Object SPOM_LOCK = new Object();

    public static void loadProtSprotOrgMap() throws SQLException
    {
        synchronized(SPOM_LOCK)
        {
            long start = System.currentTimeMillis();
            _log.info("Reloading ProtSprotOrgMap");
            int orgLineCount = 0;

            new SqlExecutor(ProteinSchema.getSchema()).execute("DELETE FROM " + ProteinSchema.getTableInfoSprotOrgMap());

            DbScope scope = ProteinSchema.getSchema().getScope();

            try (Connection conn = scope.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + ProteinSchema.getTableInfoSprotOrgMap() +
                            " (SprotSuffix,SuperKingdomCode,TaxonId,FullName,Genus,Species,CommonName,Synonym) " +
                            " VALUES (?,?,?,?,?,?,?,?)");
                 Reader reader = new InputStreamReader(getSProtOrgMap(), StandardCharsets.UTF_8);
                 TabLoader t = new TabLoader(reader, false)
            )
            {
                Set<String> sprotSuffixes = new HashSet<>();

                for (Map<String, Object> curRec : t)
                {
                    // SprotSuffix
                    String sprotSuffix = (String) curRec.get("column0");
                    if (sprotSuffix == null)
                    {
                        throw new IllegalArgumentException("Sprot suffix was blank on line " + (orgLineCount + 1));
                    }
                    ps.setString(1, sprotSuffix);

                    if (!sprotSuffixes.add(sprotSuffix))
                    {
                        throw new IllegalArgumentException("Duplicate SprotSuffix: " + sprotSuffix);
                    }

                    // SuperKingdomCode
                    ps.setString(2, (String)curRec.get("column1"));
                    // TaxonId
                    if (curRec.get("column2") == null)
                    {
                        ps.setNull(3, Types.INTEGER);
                    }
                    else
                    {
                        ps.setInt(3, ((Number)curRec.get("column2")).intValue());
                    }
                    String fullName = (String) curRec.get("column3");
                    if (fullName == null)
                    {
                        throw new IllegalArgumentException("Full name was blank on line " + (orgLineCount + 1));
                    }
                    ps.setString(4, fullName);

                    String genus = (String) curRec.get("column4");
                    if (genus == null)
                    {
                        throw new IllegalArgumentException("Genus was blank on line " + (orgLineCount + 1));
                    }
                    ps.setString(5, genus);

                    String species = (String) curRec.get("column5");
                    if (species == null)
                    {
                        throw new IllegalArgumentException("Species was blank on line " + (orgLineCount + 1));
                    }
                    ps.setString(6, species);

                    // CommonName
                    ps.setNull(7, Types.VARCHAR);
                    // Synonym
                    ps.setNull(8, Types.VARCHAR);

                    ps.addBatch();
                    orgLineCount++;

                    if (0 == orgLineCount % SPOM_BATCH_SIZE)
                    {
                        ps.executeBatch();
                        ps.clearBatch();
                        _log.debug("SprotOrgMap: " + orgLineCount + " lines loaded");
                    }
                }

                ps.executeBatch();
            }
            catch (IOException e)
            {
                throw new UnexpectedException(e, "Problem loading ProtSprotOrgMap on line " + (orgLineCount + 1));
            }

            _log.info("Finished reloading ProtSprotOrgMap in " + (System.currentTimeMillis() - start)/1000.0 + " seconds");
        }
    }

    private static InputStream getSProtOrgMap() throws IOException
    {
        return ProtSprotOrgMap.class.getClassLoader().getResourceAsStream(FILE);
    }
}

