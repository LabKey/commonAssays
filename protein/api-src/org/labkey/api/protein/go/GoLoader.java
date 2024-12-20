/*
 * Copyright (c) 2007-2018 LabKey Corporation
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

package org.labkey.api.protein.go;

import jakarta.servlet.ServletException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.protein.ProteinSchema;
import org.labkey.api.reader.Readers;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.util.CheckedInputStream;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.ExceptionUtil;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.Formats;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.HtmlStringBuilder;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.HtmlView;
import org.labkey.api.view.WebPartView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public abstract class GoLoader implements Closeable
{
    private static final Logger _log = LogHelper.getLogger(GoLoader.class, "Imports Gene Ontology archives");

    private static final String GOTERM_FILE = "term.txt";
    private static final String GOTERM2TERM_FILE = "term2term.txt";
    private static final String GOTERMDEFINITION_FILE = "term_definition.txt";
    private static final String GOTERMSYNONYM_FILE = "term_synonym.txt";
    private static final String GOGRAPHPATH_FILE = "graph_path.txt";

    private static Boolean _goLoaded = null;
    private static GoLoader _currentLoader = null;

    private static final Object LOCK = new Object(); // Used to synchronize access to _status
    private final HtmlStringBuilder _status = HtmlStringBuilder.of();
    private boolean _complete = false;

    public static WebPartView<?> getCurrentStatus(String message)
    {
        HtmlStringBuilder html = HtmlStringBuilder.of();

        if (null != message)
            html.append(message).append(HtmlString.BR);

        if (null == _currentLoader)
        {
            html.append("No GO annotation loads have been attempted during this server session");
        }
        else
        {
            HtmlString status = _currentLoader.getStatus();
            if (!status.toString().contains("failed"))
                html.unsafeAppend("Refresh this page to update status<br>\n");
            html.append(status);
        }

        return new HtmlView("GO Annotation Load Status", html);
    }

    public static GoLoader getHttpLoader()
    {
        return ensureOneLoader(new HttpGoLoader());
    }

    public static GoLoader getStreamLoader(InputStream is)
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
        DbSchema schema = ProteinSchema.getSchema();
        Map<String, GoLoadBean> map = getGoLoadMap();
        long start = System.currentTimeMillis();

        clearGoLoaded();
        new SqlExecutor(schema).execute(schema.getSqlDialect().execute(schema, "drop_go_indexes", ""));

        logStatus("Starting to load GO annotation files");
        logStatus("");

        try (InputStream in = getInputStream();
            TarArchiveInputStream tais = new TarArchiveInputStream(new GZIPInputStream(new CheckedInputStream(in))))
        {
            TarArchiveEntry te = tais.getNextEntry();

            while (te != null)
            {
                String filename = te.getName();
                int index = filename.lastIndexOf('/');
                String shortFilename = filename.substring(index + 1);

                GoLoadBean bean = map.get(shortFilename);

                if (null != bean)
                    loadSingleGoFile(bean, shortFilename, tais);

                te = tais.getNextEntry();
            }
        }

        new SqlExecutor(schema).execute(schema.getSqlDialect().execute(schema, "create_go_indexes", ""));
        long elapsed = System.currentTimeMillis() - start;

        logStatus("Successfully loaded all GO annotation files (" + DateUtil.formatDuration(elapsed) + ")");
    }

    private static final int GO_BATCH_SIZE = 5000;

    private void loadSingleGoFile(GoLoadBean bean, String filename, InputStream is) throws SQLException
    {
        int orgLineCount = 0;
        String[] cols = bean.cols;
        TableInfo ti = bean.tinfo;

        logStatus("Clearing table " + bean.tinfo);
        new SqlExecutor(ProteinSchema.getSchema()).execute("TRUNCATE TABLE " + bean.tinfo);

        logStatus("Starting to load " + filename);
        BufferedReader isr = Readers.getReader(is);
        TabLoader t = new TabLoader(isr, false);
        StringBuilder sql = new StringBuilder("INSERT INTO " + ti + "(");
        StringBuilder valuesSql = new StringBuilder("VALUES (");

        for (int i = 0; i < cols.length; i++)
        {
            sql.append(cols[i]);
            valuesSql.append("?");
            if (i < (cols.length - 1))
            {
                sql.append(",");
                valuesSql.append(",");
            }
            else
            {
                sql.append(") ");
                valuesSql.append(") ");
            }
        }

        List<ColumnInfo> columns = ti.getColumns();

        DbScope scope = ProteinSchema.getSchema().getScope();
        Connection conn = null;
        PreparedStatement ps = null;

        try
        {
            conn = scope.getConnection();
            conn.setAutoCommit(false);
            sql.append(valuesSql);
            ps = conn.prepareStatement(sql.toString());

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
                    if (val instanceof String s)
                    {
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
                    logStatus(Formats.commaf0.format(orgLineCount) + " rows loaded");
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
        synchronized (LOCK)
        {
            _status.insert(0, HtmlString.unsafe("See below for complete log<br>"));
            _status.insert(0, ExceptionUtil.renderException(e));
            _status.insert(0, HtmlString.unsafe("Loading GO annotations failed with the following exception:<br>"));
        }

        logStatus(HtmlString.unsafe("<br>Loading GO annotations failed with the following exception:"));
        logStatus(ExceptionUtil.renderException(e));
        ExceptionUtil.logExceptionToMothership(null, e);
    }

    private HtmlString getStatus()
    {
        synchronized (LOCK)
        {
            return _status.getHtmlString();
        }
    }

    private boolean isComplete()
    {
        return _complete;
    }

    protected void logStatus(String message)
    {
        logStatus(HtmlString.of(message));
    }

    protected void logStatus(HtmlString message)
    {
        if (!message.isEmpty())
            _log.debug(message.toString());

        synchronized (LOCK)
        {
            _status.append(HtmlString.BR).append("\n").append(message);
        }
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

        map.put(GOTERM_FILE, new GoLoadBean(ProteinSchema.getTableInfoGoTerm(), new String[]{"Id", "Name", "TermType", "Acc", "IsObsolete", "IsRoot"}));
        map.put(GOTERM2TERM_FILE, new GoLoadBean(ProteinSchema.getTableInfoGoTerm2Term(), new String[]{"Id", "RelationshipTypeId", "Term1Id", "Term2Id", "Complete"}));
        map.put(GOTERMDEFINITION_FILE, new GoLoadBean(ProteinSchema.getTableInfoGoTermDefinition(), new String[]{"TermId", "TermDefinition", "DbXrefId", "TermComment", "Reference"}));
        map.put(GOTERMSYNONYM_FILE, new GoLoadBean(ProteinSchema.getTableInfoGoTermSynonym(), new String[]{"TermId", "TermSynonym", "AccSynonym", "SynonymTypeId"}));
        map.put(GOGRAPHPATH_FILE, new GoLoadBean(ProteinSchema.getTableInfoGoGraphPath(), new String[]{"Id", "Term1Id", "Term2Id", "Distance"}));

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
                _goLoaded = new TableSelector(ProteinSchema.getTableInfoGoTerm()).exists();
            }
            catch(Exception e)
            {
                _log.error("isGoLoaded", e);
                _goLoaded = false;    // Don't try this again if there's a SQL error
            }
        }

        return _goLoaded;
    }

    private static class HttpGoLoader extends GoLoader
    {
        private final static String SERVER = "release.geneontology.org";
        private final static String PATH = "/2017-01-01/mysql_dumps/";
        private final static String FILENAME = "go_monthly-termdb-tables.tar.gz";

        private File _file;

        @Override
        protected InputStream getInputStream() throws IOException
        {
            logStatus("Searching for the latest GO annotation files at " + SERVER);
            logStatus("Starting to download " + PATH + FILENAME + " from " + SERVER);
            _file = FileUtil.createTempFile(FILENAME, null);
            try (InputStream in = new BufferedInputStream(new URL("http://" + SERVER + PATH + FILENAME).openStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(_file)))
            {
                IOUtils.copy(in, out);
            }

            _file.deleteOnExit();
            logStatus("Finished downloading " + FILENAME);
            logStatus("");

            return new FileInputStream(_file);
        }

        @Override
        public void close()
        {
            if (_file != null)
            {
                _file.delete();
                _file = null;
            }
        }
    }

    private static class StreamGoLoader extends GoLoader
    {
        private final InputStream _is;

        private StreamGoLoader(InputStream is)
        {
            _is = is;
        }

        @Override
        protected InputStream getInputStream()
        {
            return _is;
        }

        @Override
        public void close()
        {
        }
    }
}
