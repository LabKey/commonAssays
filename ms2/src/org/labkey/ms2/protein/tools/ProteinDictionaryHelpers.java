/*
 * Copyright (c) 2005-2010 Fred Hutchinson Cancer Research Center
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
package org.labkey.ms2.protein.tools;

import org.apache.log4j.Logger;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.Table;
import org.labkey.api.util.ResultSetUtil;
import org.labkey.api.util.UnexpectedException;
import org.labkey.api.iterator.CloseableIterator;
import org.labkey.api.view.ViewServlet;
import org.labkey.api.webdav.WebdavResource;
import org.labkey.api.webdav.WebdavService;
import org.labkey.api.reader.TabLoader;
import org.labkey.ms2.protein.ProteinManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.sql.*;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * User: tholzman
 * Date: Sep 28, 2005
 * Time: 1:21:09 PM
 */
public class ProteinDictionaryHelpers
{
    private static final Logger _log = Logger.getLogger(ProteinDictionaryHelpers.class);
    private static final String FILE = "/MS2/externalData/ProtSprotOrgMap.txt";
    private static final int SPOM_BATCH_SIZE = 1000;

    private static final Object SPOM_LOCK = new Object();

    public static void loadProtSprotOrgMap() throws SQLException
    {
        synchronized(SPOM_LOCK)
        {
            long start = System.currentTimeMillis();
            _log.info("Reloading ProtSprotOrgMap");
            int orgLineCount = 0;
            PreparedStatement ps = null;
            Connection conn = null;
            CloseableIterator<Map<String, Object>> it = null;
            DbScope scope = ProteinManager.getSchema().getScope();
            try
            {
                Table.execute(ProteinManager.getSchema(), "DELETE FROM " + ProteinManager.getTableInfoSprotOrgMap(), null);

                TabLoader t = new TabLoader(new InputStreamReader(getSProtOrgMap()), false);
                conn = scope.getConnection();
                ps = conn.prepareStatement(
                        "INSERT INTO " + ProteinManager.getTableInfoSprotOrgMap() +
                                " (SprotSuffix,SuperKingdomCode,TaxonId,FullName,Genus,Species,CommonName,Synonym) " +
                                " VALUES (?,?,?,?,?,?,?,?)");

                Set<String> sprotSuffixes = new HashSet<String>();

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
            finally
            {
                if (ps != null) { try { ps.close(); } catch (SQLException e) {} }
                try{ if (null != conn) scope.releaseConnection(conn); } catch (SQLException e) {}
                if (it != null) { try { it.close(); } catch (IOException e) {} }
            }

            _log.info("Finished reloading ProtSprotOrgMap in " + (System.currentTimeMillis() - start)/1000.0 + " seconds");
        }
    }


    private static InputStream getSProtOrgMap() throws IOException
    {
        WebdavResource r = WebdavService.get().lookup(FILE);
        if (null != r)
        {
            InputStream is = r.getInputStream(null);
            if (null != is)
                return is;
        }
        return ViewServlet.getViewServletContext().getResourceAsStream(FILE);
    }


    private static String getGODefinitionFromId(int id) throws SQLException
    {
        return Table.executeSingleton(ProteinManager.getSchema(), "SELECT TermDefinition FROM " + ProteinManager.getTableInfoGoTermDefinition() + " WHERE TermId=?", new Object[]{id}, String.class);
    }

    public static String getGODefinitionFromAcc(String acc) throws SQLException
    {
        return getGODefinitionFromId(getGOIdFromAcc(acc));
    }

    public static int getGOIdFromAcc(String acc) throws SQLException
    {
        Integer goId = Table.executeSingleton(ProteinManager.getSchema(), "SELECT Id FROM " + ProteinManager.getTableInfoGoTerm() + " WHERE Acc = ?", new Object[]{acc}, Integer.class);

        return (null == goId ? 0 : goId);
    }

    public static enum GoTypes
    {
        CELL_LOCATION {
            public String toString()
            {
                return "Cellular Location";
            }
        },
        FUNCTION {
            public String toString()
            {
                return "Molecular Function";
            }
        },
        PROCESS {
            public String toString()
            {
                return "Metabolic Process";
            }
        },
        ALL {
            public String toString()
            {
                return "All GO Ontologies";
            }
        }
    }

    public static GoTypes GTypeStringToEnum(String label)
    {
        for (GoTypes g : GoTypes.values())
        {
            if (label.equals(g.toString())) return g;
        }
        return null;
    }

    public static int gTypeC = 0;
    public static int gTypeF = 0;
    public static int gTypeP = 0;

    public static int getgTypeC() throws SQLException
    {
        if (gTypeC == 0) getGOTypes();
        return gTypeC;
    }


    public static int getgTypeF() throws SQLException
    {
        if (gTypeF == 0) getGOTypes();
        return gTypeF;
    }


    public static int getgTypeP() throws SQLException
    {
        if (gTypeP == 0) getGOTypes();
        return gTypeP;
    }


    private static final Object _lock = new Object();

    protected static void getGOTypes() throws SQLException
    {
        synchronized (_lock)
        {
            if (gTypeC == 0 || gTypeF == 0 || gTypeP == 0)
            {
                ResultSet rs = null;

                try
                {
                    rs = Table.executeQuery(ProteinManager.getSchema(), "SELECT annottypeid,name FROM " + ProteinManager.getTableInfoAnnotationTypes() + " WHERE name in ('GO_C','GO_F','GO_P')", null);

                    while (rs.next())
                    {
                        int antypeid = rs.getInt(1);
                        String gt = rs.getString(2);
                        if (gt.equals("GO_C"))
                        {
                            gTypeC = antypeid;
                        }
                        if (gt.equals("GO_F"))
                        {
                            gTypeF = antypeid;
                        }
                        if (gt.equals("GO_P"))
                        {
                            gTypeP = antypeid;
                        }
                    }
                }
                finally
                {
                    ResultSetUtil.close(rs);
                }
            }
        }
    }


    public static String getAnnotTypeWhereClause(GoTypes kind) throws SQLException
    {
        switch (kind)
        {
            case CELL_LOCATION:
                return "AnnotTypeId=" + getgTypeC();
            case FUNCTION:
                return "AnnotTypeId=" + getgTypeF();
            case PROCESS:
                return "AnnotTypeId=" + getgTypeP();
            case ALL:
                return "AnnotTypeId IN (" + getgTypeC() + "," + getgTypeF() + "," + getgTypeC() + ")";
        }

        return null;
    }
}

