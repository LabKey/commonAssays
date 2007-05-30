/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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
import org.labkey.api.view.ViewServlet;
import org.labkey.common.tools.TabLoader;
import org.labkey.ms2.MS2Manager;
import org.labkey.ms2.protein.ProteinManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.Map;

/**
 * User: tholzman
 * Date: Sep 28, 2005
 * Time: 1:21:09 PM
 */
public class ProteinDictionaryHelpers
{
    private static Logger _log = Logger.getLogger(ProteinDictionaryHelpers.class);

    public static final String WEBAPP_ROOT = "MS2/externalData/";
    private static final String PROTSPROTORGMAP_FILE = ProteinDictionaryHelpers.WEBAPP_ROOT + "ProtSprotOrgMap.txt";
    private static final int SPOM_BATCH_SIZE = 1000;

    public static boolean loadProtSprotOrgMap(String fname) throws SQLException, IOException
    {
        int orgLineCount = 0;
        TabLoader t;
        PreparedStatement ps = null;
        Connection conn = null;
        TabLoader.TabLoaderIterator it = null;
        DbScope scope = null;
        try
        {
            Table.execute(ProteinManager.getSchema(), "DELETE FROM " + ProteinManager.getTableInfoSprotOrgMap(), null);

            t = new TabLoader(new InputStreamReader(ViewServlet.getViewServletContext().getResourceAsStream(fname)));
            scope = ProteinManager.getSchema().getScope();
            conn = scope.getConnection();
            ps = conn.prepareStatement(
                    "INSERT INTO " + ProteinManager.getTableInfoSprotOrgMap() +
                            " (SprotSuffix,SuperKingdomCode,TaxonId,FullName,Genus,Species,CommonName,Synonym) " +
                            " VALUES (?,?,?,?,?,?,?,?)"
            );

            for (it = t.iterator(); it.hasNext();)
            {
                Map curRec = (Map)it.next();

                // Set nullable parameters to NULL, using correct types
                ps.setNull(2, Types.CHAR);
                ps.setNull(3, Types.INTEGER);
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.VARCHAR);

                for (Object key : curRec.keySet())
                {
                    String k = (String) key;
                    int kindex = Integer.parseInt(k.substring(6)) + 1;
                    Object val = curRec.get(key);
                    if (val != null)
                    {
                        ps.setObject(kindex, val);
                    }
                }
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
        catch (SQLException e)
        {
            _log.debug("Problem loading ProtSprotOrgMap from " + fname + " on line " + orgLineCount + ": " + e);
            throw e;
        }
        finally
        {
            try{ps.close();}catch(Exception e){}
            try{scope.releaseConnection(conn);}catch(Exception e){}
            try{it.close();}catch(Exception e){}
        }
        return true;
    }

    public static boolean loadProtSprotOrgMap() throws SQLException
    {
        try
        {
            return loadProtSprotOrgMap(PROTSPROTORGMAP_FILE);
        }
        catch(IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String getGONameFromId(int id) throws SQLException
    {
        return Table.executeSingleton(ProteinManager.getSchema(), "SELECT Name FROM " + ProteinManager.getTableInfoGoTerm() + " WHERE Id=?", new Object[]{id}, String.class);
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


    public static String getThirdLevGoAccFromAcc(String myLev) throws SQLException
    {
        int myLevId = getGOIdFromAcc(myLev);

        String retVal = Table.executeSingleton(MS2Manager.getSchema(), "SELECT acc FROM " +
                            ProteinManager.getTableInfoGoTerm() + " WHERE id IN (SELECT term2id FROM " + ProteinManager.getTableInfoGoGraphPath() +
                            " c WHERE c.term1id=1 AND c.distance=3 AND EXISTS(SELECT * FROM " + ProteinManager.getTableInfoGoGraphPath() +
                            " d WHERE d.term1id=c.term2id AND d.term2id=" + myLevId + "))", null, String.class);

        if (null == retVal && myLevId != 0)
            retVal = myLev;

        String test = Table.executeSingleton(MS2Manager.getSchema(), "SELECT Acc FROM prot.GoTerm WHERE (Id IN\n" +
                "   (SELECT term1Id FROM prot.GoGraphPath WHERE (term1Id IN (SELECT term2Id AS ThirdLevelId FROM prot.GoGraphPath WHERE (term1Id = 1) AND (distance = 3))) AND (term2Id = ?)))", new Object[]{myLevId}, String.class);

        if (test != null)
            if (!test.equals(retVal))
                _log.error(myLevId + " " + retVal + " " + test);

        return retVal;
    }
}

