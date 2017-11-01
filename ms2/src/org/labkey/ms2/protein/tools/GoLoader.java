/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.log4j.Logger;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.reader.Readers;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.util.CheckedInputStream;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.FTPUtil;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.WebPartView;
import org.labkey.ms2.protein.ProteinManager;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * User: adam
 * Date: May 28, 2007
 * Time: 2:56:10 PM
 */
public abstract class GoLoader
{
    private static final Logger _log = Logger.getLogger(GoLoader.class);

    private static final String GOTERM_FILE = "term.txt";
    private static final String GOTERM2TERM_FILE = "term2term.txt";
    private static final String GOTERMDEFINITION_FILE = "term_definition.txt";
    private static final String GOTERMSYNONYM_FILE = "term_synonym.txt";
    private static final String GOGRAPHPATH_FILE = "graph_path.txt";

    private static Boolean _goLoaded = null;
    private static GoLoader _currentLoader = null;

    private final StringBuffer _status = new StringBuffer();  // Can't use StringBuilder -- needs to be synchronized
    private boolean _complete = false;

    public static WebPartView getCurrentStatus(String message)
    {
        StringBuilder html = new StringBuilder(null == message ? "" : PageFlowUtil.filter(message) + "<br><br>");

        if (null == _currentLoader)
            html.append("No GO annotation loads have been attempted during this server session");
        else
        {
            StringBuffer status = _currentLoader.getStatus();
            if (-1 == status.indexOf("failed"))
                html.append("Refresh this page to update status<br><br>\n");
            html.append(status);
        }

        return new HtmlView("GO Annotation Load Status", html.toString());
    }


    public static GoLoader getFtpLoader() throws IOException, ServletException
    {
        return ensureOneLoader(new FtpGoLoader());
    }


    public static GoLoader getStreamLoader(InputStream is) throws IOException, ServletException
    {
        return ensureOneLoader(new StreamGoLoader(is));
    }


    private static synchronized GoLoader ensureOneLoader(GoLoader newLoader)
    {
        if (null == _currentLoader || _currentLoader.isComplete())
            return _currentLoader = newLoader;
        else
            return null;
    }


    protected abstract InputStream getInputStream() throws IOException, ServletException;

    public void load()
    {
        if (isComplete())
            return;

        JobRunner.getDefault().execute(() ->
        {
            try
            {
                loadGoFromGz();
            }
            catch (Exception e)
            {
                logException(e);
            }
            finally
            {
                _complete = true;
            }
        });
    }


    private void loadGoFromGz() throws SQLException, IOException, ServletException
    {
        DbSchema schema = ProteinManager.getSchema();
        Map<String, GoLoadBean> map = getGoLoadMap();

        clearGoLoaded();
        new SqlExecutor(schema).execute(schema.getSqlDialect().execute(schema, "drop_go_indexes", ""));

        logStatus("Starting to load GO annotation files");
        logStatus("");

        try (TarArchiveInputStream tais = new TarArchiveInputStream(new GZIPInputStream(new CheckedInputStream(getInputStream()))))
        {
            TarArchiveEntry te = tais.getNextTarEntry();

            while (te != null)
            {
                String filename = te.getName();
                int index = filename.lastIndexOf('/');
                String shortFilename = filename.substring(index + 1);

                GoLoadBean bean = map.get(shortFilename);

                if (null != bean)
                    loadSingleGoFile(bean, shortFilename, tais);

                te = tais.getNextTarEntry();
            }
        }

        new SqlExecutor(schema).execute(schema.getSqlDialect().execute(schema, "create_go_indexes", ""));

        logStatus("Successfully loaded all GO annotation files");
    }


    private static final int GO_BATCH_SIZE = 5000;

    private void loadSingleGoFile(GoLoadBean bean, String filename, InputStream is) throws SQLException, IOException, ServletException
    {
        int orgLineCount = 0;
        String[] cols = bean.cols;
        TableInfo ti = bean.tinfo;

        logStatus("Clearing table " + bean.tinfo);
        new SqlExecutor(ProteinManager.getSchema()).execute("TRUNCATE TABLE " + bean.tinfo);

        logStatus("Starting to load " + filename);
        BufferedReader isr = Readers.getReader(is);
        TabLoader t = new TabLoader(isr, false);
        StringBuilder SQLCommand = new StringBuilder("INSERT INTO " + ti + "(");
        StringBuilder QMarkPart = new StringBuilder("VALUES (");

        for (int i = 0; i < cols.length; i++)
        {
            SQLCommand.append(cols[i]);
            QMarkPart.append("?");
            if (i < (cols.length - 1))
            {
                SQLCommand.append(",");
                QMarkPart.append(",");
            }
            else
            {
                SQLCommand.append(") ");
                QMarkPart.append(") ");
            }
        }

        List<ColumnInfo> columns = ti.getColumns();

        DbScope scope = ProteinManager.getSchema().getScope();
        Connection conn = null;
        PreparedStatement ps = null;

        try
        {
            conn = scope.getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement(SQLCommand + QMarkPart.toString());

            for (Map<String, Object> curRec : t)
            {
                boolean addRow = true;
                for (int i = 0; i < cols.length; i++)
                    ps.setNull(i + 1, columns.get(i).getJdbcType().sqlType);

                for (String key : curRec.keySet())
                {
                    int kindex = Integer.parseInt(key.substring(6));

                    // bug #6085 -- ignore any columns that we don't know about, e.g., term_synonym.synonym_category_id that was added recently
                    if (kindex >= columns.size())
                        continue;

                    ColumnInfo column = columns.get(kindex);
                    Object val = curRec.get(key);
                    if (val instanceof String)
                    {
                        String s = (String)val;
                        if (s.equals("\\N"))
                            continue;

                        if (column.getJdbcType() == JdbcType.VARCHAR)
                        {
                            int limit = column.getScale();

                            if (s.length() > limit)
                            {
                                val = s.substring(0, column.getScale());
                                _log.warn(ti + ": value in " + cols[kindex] + " column in row " + (orgLineCount + 1) + " truncated from " + s.length() + " to " + limit + " characters.");
                            }
                        }
                    }
                    if (val != null)
                    {
                        ps.setObject(kindex + 1, val);
                    }
                    else if (!column.isNullable())
                    {
                        addRow = false;   // Skip any record with null in a non-nullable column (e.g., row 21 of 200806 term.txt)
                        break;
                    }
                }
                if (addRow)
                    ps.addBatch();
                orgLineCount++;
                if (orgLineCount % GO_BATCH_SIZE == 0)
                {
                    logStatus(orgLineCount + " rows loaded");
                    ps.executeBatch();
                    conn.commit();
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
                conn.commit();
                conn.setAutoCommit(true);
                scope.releaseConnection(conn);
            }
        }

        logStatus("Completed loading " + filename);
        logStatus("");
    }


    private void logException(Exception e)
    {
        _status.insert(0, "See below for complete log<br><br>");
        _status.insert(0, ExceptionUtil.renderException(e));
        _status.insert(0, "Loading GO annotations failed with the following exception:<br>");

        logStatus("Loading GO annotations failed with the following exception:");
        logStatus(ExceptionUtil.renderException(e));
        ExceptionUtil.logExceptionToMothership(null, e);
    }


    private StringBuffer getStatus()
    {
        return _status;
    }


    private boolean isComplete()
    {
        return _complete;
    }


    protected void logStatus(String message)
    {
        if (message.length() > 0)
            _log.debug(message);

        message += "<br>\n";

        _status.append(message);
    }


    private static class GoLoadBean
    {
        TableInfo tinfo;
        String[] cols;

        private GoLoadBean(TableInfo tinfo, String[] cols)
        {
            this.tinfo = tinfo;
            this.cols = cols;
        }
    }


    private static Map<String, GoLoadBean> getGoLoadMap()
    {
        Map<String, GoLoadBean> map = new HashMap<>(10);

        map.put(GOTERM_FILE, new GoLoadBean(ProteinManager.getTableInfoGoTerm(), new String[]{"Id", "Name", "TermType", "Acc", "IsObsolete", "IsRoot"}));
        map.put(GOTERM2TERM_FILE, new GoLoadBean(ProteinManager.getTableInfoGoTerm2Term(), new String[]{"Id", "RelationshipTypeId", "Term1Id", "Term2Id", "Complete"}));
        map.put(GOTERMDEFINITION_FILE, new GoLoadBean(ProteinManager.getTableInfoGoTermDefinition(), new String[]{"TermId", "TermDefinition", "DbXrefId", "TermComment", "Reference"}));
        map.put(GOTERMSYNONYM_FILE, new GoLoadBean(ProteinManager.getTableInfoGoTermSynonym(), new String[]{"TermId", "TermSynonym", "AccSynonym", "SynonymTypeId"}));
        map.put(GOGRAPHPATH_FILE, new GoLoadBean(ProteinManager.getTableInfoGoGraphPath(), new String[]{"Id", "Term1Id", "Term2Id", "Distance"}));

        return map;
    }


    public static void clearGoLoaded()
    {
        _goLoaded = null;
    }


    public static Boolean isGoLoaded()
    {
        if (null == _goLoaded)
        {
            try
            {
                _goLoaded = new TableSelector(ProteinManager.getTableInfoGoTerm()).exists();
            }
            catch(Exception e)
            {
                _log.error("isGoLoaded", e);
                _goLoaded = false;    // Don't try this again if there's a SQL error
            }
        }

        return _goLoaded;
    }


    private static class FtpGoLoader extends GoLoader
    {
        private final static String SERVER = "ftp.geneontology.org";
        private final static String PATH = "godatabase/archive/latest-full";
        private final static String FILENAME = "go_monthly-termdb-tables.tar.gz";

        protected InputStream getInputStream() throws IOException, ServletException
        {
            logStatus("Searching for the latest GO annotation files at " + SERVER);
            logStatus("Starting to download " + FILENAME + " from " + SERVER);
            File file = FTPUtil.downloadFile("anonymous", "anonymous", SERVER, PATH, FILENAME);
            file.deleteOnExit();
            logStatus("Finished downloading " + FILENAME);
            logStatus("");

            return new FileInputStream(file);
        }
    }


    private static class StreamGoLoader extends GoLoader
    {
        private InputStream _is;

        private StreamGoLoader(InputStream is)
        {
            _is = is;
        }

        protected InputStream getInputStream() throws IOException, ServletException
        {
            return _is;
        }
    }
}
