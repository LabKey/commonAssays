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

import org.labkey.api.data.*;
import org.labkey.ms2.protein.ProteinManager;
import org.labkey.common.tools.TabLoader;
import org.labkey.api.view.ViewServlet;
import org.labkey.ms2.MS2Manager;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import java.sql.*;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;

/**
 * User: tholzman
 * Date: Sep 28, 2005
 * Time: 1:21:09 PM
 */
public class ProteinDictionaryHelpers
{
    private static final String DEFAULT_PROTSPROTORGMAP_FILE =
            "MS2/externalData/ProtSprotOrgMap.txt";
    private static final String DEFAULT_GOTERM_FILE =
            "MS2/externalData/term.txt";
    private static final String DEFAULT_GOTERM2TERM_FILE =
            "MS2/externalData/term2term.txt";
    private static final String DEFAULT_GOTERMDEFINITION_FILE =
            "MS2/externalData/term_definition.txt";
    private static final String DEFAULT_GOTERMSYNONYM_FILE =
            "MS2/externalData/term_synonym.txt";
    private static final String DEFAULT_GOGRAPHPATH_FILE =
            "MS2/externalData/graph_path.txt";


    private static final DbSchema _schema = ProteinManager.getSchema();
    private static Logger _log = Logger.getLogger(ProteinManager.class);


    public static boolean loadProtSprotOrgMap(String fname)
    {
        int orgLineCount = 0;
        TabLoader t;
        PreparedStatement ps = null;
        Connection conn = null;
        TabLoader.TabLoaderIterator it = null;
        try
        {
            Table.execute(_schema, "DELETE FROM " + ProteinManager.getTableInfoSprotOrgMap(), null);

            t = new TabLoader(new InputStreamReader(ViewServlet.getViewServletContext().getResourceAsStream(fname)));
            conn = _schema.getScope().getConnection();
            ps = conn.prepareStatement(
                    "INSERT INTO " + ProteinManager.getTableInfoSprotOrgMap() +
                            " (SprotSuffix,SuperKingdomCode,TaxonId,FullName,Genus,Species,CommonName,Synonym) " +
                            " VALUES (?,?,?,?,?,?,?,?)"
            );

            for (it = t.iterator(); it.hasNext();)
            {
                Map curRec = it.next();
                for (int i = 1; i <= 8; i++) ps.setNull(i, Types.VARCHAR);
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
            }
            ps.executeBatch();

        }
        catch (Exception e)
        {
            _log.debug("Problem loading ProtSprotOrgMap from " + fname + " on line " + orgLineCount + ": " + e);
            return false;
        } finally {
            try{ps.close();}catch(Exception e){}
            try{_schema.getScope().releaseConnection(conn);}catch(Exception e){}
            try{it.close();}catch(Exception e){}
        }
        return true;
    }

    public static boolean loadProtSprotOrgMap()
    {
        return loadProtSprotOrgMap(DEFAULT_PROTSPROTORGMAP_FILE);
    }

    private static final int GO_BATCH_SIZE = 5000;

    public static void loadAGoFile(TableInfo ti, String cols[], String fname) throws SQLException, IOException, ServletException
    {
        int orgLineCount = 0;
        Connection conn = null;
        PreparedStatement ps = null;
        TabLoader.TabLoaderIterator it = null;

        try
        {
            Table.execute(_schema, "DELETE FROM " + ti, null);

            InputStream is = ViewServlet.getViewServletContext().getResourceAsStream(fname);

            if (null == is)
                throw new ServletException("File not found: " + fname);

            InputStreamReader isr = new InputStreamReader(is);
            TabLoader t = new TabLoader(isr);
            conn = _schema.getScope().getConnection();
            String SQLCommand = "INSERT INTO " + ti + "(";
            String QMarkPart = "VALUES (";
            String typeList = "SELECT ";
            for (int i = 0; i < cols.length; i++)
            {
                SQLCommand += cols[i];
                typeList += cols[i];
                QMarkPart += "?";
                if (i < (cols.length - 1))
                {
                    SQLCommand += ",";
                    QMarkPart += ",";
                    typeList += ",";
                }
                else
                {
                    SQLCommand += ") ";
                    QMarkPart += ") ";
                    typeList += " FROM " + ti + " WHERE 1=0";
                }
            }
            ResultSetMetaData rsmd = conn.createStatement().executeQuery(typeList).getMetaData();
            HashMap<String, Integer> typeMap = new HashMap<String, Integer>();
            for (int i = 1; i <= rsmd.getColumnCount(); i++)
            {
                String key = rsmd.getColumnName(i).toUpperCase();
                Integer val = new Integer(rsmd.getColumnType(i));
                typeMap.put(key, val);
            }

            ps = conn.prepareStatement(SQLCommand + QMarkPart);
            for (it = t.iterator(); it.hasNext();)
            {
                Map curRec = it.next();
                for (int i = 1; i <= cols.length; i++) ps.setNull(i, typeMap.get(cols[i - 1].toUpperCase()).intValue());
                for (Object key : curRec.keySet())
                {
                    String k = (String) key;
                    int kindex = Integer.parseInt(k.substring(6)) + 1;
                    Object val = curRec.get(key);
                    if (val instanceof String && val.equals("\\N")) continue;
                    if (val != null)
                    {
                        ps.setObject(kindex, val);
                    }
                }
                ps.addBatch();
                orgLineCount++;
                if (orgLineCount % GO_BATCH_SIZE == 0)
                {
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }
        }
        finally
        {
            if (null != ps)
            {
                ps.executeBatch();
                ps.close();
            }
            if (null != conn)
            {
                _schema.getScope().releaseConnection(conn);
            }
            if (null != it)
                it.close();
        }
    }

    public static void loadGo() throws SQLException, IOException, ServletException
    {
        loadAGoFile(ProteinManager.getTableInfoGoTerm(), new String[]{"Id", "Name", "TermType", "Acc", "IsObsolete", "IsRoot"}, DEFAULT_GOTERM_FILE);
        loadAGoFile(ProteinManager.getTableInfoGoTerm2Term(), new String[]{"Id", "RelationshipTypeId", "Term1Id", "Term2Id", "Complete"}, DEFAULT_GOTERM2TERM_FILE);
        loadAGoFile(ProteinManager.getTableInfoGoTermDefinition(), new String[]{"TermId", "TermDefinition", "DbXrefId", "TermComment", "Reference"}, DEFAULT_GOTERMDEFINITION_FILE);
        loadAGoFile(ProteinManager.getTableInfoGoTermSynonym(), new String[]{"TermId", "TermSynonym", "AccSynonym", "SynonymTypeId"}, DEFAULT_GOTERMSYNONYM_FILE);
        loadAGoFile(ProteinManager.getTableInfoGoGraphPath(), new String[]{"Id", "Term1Id", "Term2Id", "Distance"}, DEFAULT_GOGRAPHPATH_FILE);
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
                ResultSet rs = Table.executeQuery(ProteinManager.getSchema(), "SELECT annottypeid,name FROM " + ProteinManager.getTableInfoAnnotationTypes() + " WHERE name in ('GO_C','GO_F','GO_P')", null);

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
                rs.close();
            }
        }
    }


    public static HashSet<String> OLDgetGOAccsFromSeqid(int seqid, GoTypes kind) throws SQLException
    {
        String gTypes = "";
        ResultSet rs = null;
        HashSet<String> retVal = new HashSet<String>();

        switch (kind)
        {
            case CELL_LOCATION:
                gTypes = "annotTypeId=" + getgTypeC();
                break;
            case FUNCTION:
                gTypes = "annotTypeId=" + getgTypeF();
                break;
            case PROCESS:
                gTypes = "annotTypeId=" + getgTypeP();
                break;
            case ALL:
                gTypes = "annotTypeId in (" + getgTypeC() + "," + getgTypeF() + "," + getgTypeC() + ")";
                break;
        }

        String command = "SELECT annotval FROM " + ProteinManager.getTableInfoAnnotations() + " WHERE seqid=" + seqid + " AND " + gTypes;

        try
        {
            rs = Table.executeQuery(ProteinManager.getSchema(), command, null);

            while (rs.next())
            {
                String val = rs.getString(1);
                retVal.add(val.substring(0, 10).trim());
            }
        }
        finally
        {
            if (null != rs)
                rs.close();
        }

        return retVal;
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

    public static String OLDgetThirdLevGoAccFromAcc(String myLev) throws SQLException
    {
        String retVal = null;
        int myLevId = getGOIdFromAcc(myLev);
        Connection conn = _schema.getScope().getConnection();
        Statement s = conn.createStatement();
        ResultSet rs = s.executeQuery(
                    "SELECT a.acc FROM " +
                            ProteinManager.getTableInfoGoTerm() + " a " +
                            " WHERE a.id IN (SELECT term2id FROM " + ProteinManager.getTableInfoGoGraphPath() +
                            " c WHERE c.term1id=1 AND c.distance=3 AND EXISTS(SELECT * FROM " + ProteinManager.getTableInfoGoGraphPath() +
                            " d WHERE d.term1id=c.term2id AND d.term2id=" + myLevId + "))"
                );
        if (rs.next())
        {
            retVal = rs.getString(1);
        }
        else
        {
            if (myLevId != 0) retVal = myLev;
        }
        rs.close();
        s.close();
        _schema.getScope().releaseConnection(conn);
        return retVal;
    }
}

