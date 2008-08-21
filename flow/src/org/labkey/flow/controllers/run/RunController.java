/*
 * Copyright (c) 2006-2008 LabKey Corporation
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

package org.labkey.flow.controllers.run;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.labkey.api.action.FormViewAction;
import org.labkey.api.action.SimpleViewAction;
import org.labkey.api.data.*;
import org.labkey.api.query.CustomView;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryDefinition;
import org.labkey.api.security.ACL;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.view.*;
import org.labkey.flow.analysis.model.FCS;
import org.labkey.flow.analysis.model.ScriptSettings;
import org.labkey.flow.controllers.SpringFlowController;
import org.labkey.flow.controllers.editscript.ScriptController;
import org.labkey.flow.data.*;
import org.labkey.flow.persist.FlowManager;
import org.labkey.flow.query.FlowTableType;
import org.labkey.flow.script.FlowAnalyzer;
import org.labkey.flow.script.MoveRunFromWorkspaceJob;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class RunController extends SpringFlowController<RunController.Action>
{
    static private final Logger _log = Logger.getLogger(RunController.class);
    public enum Action
    {
        begin,
        showRun,
        showCompensation,
        export,
        details,
        download,
        moveToWorkspace,
        moveToAnalysis
    }

    static DefaultActionResolver _actionResolver = new DefaultActionResolver(RunController.class);

    public RunController() throws Exception
    {
        super();
        setActionResolver(_actionResolver);
    }


    @RequiresPermission(ACL.PERM_NONE)
    public class BeginAction extends SimpleViewAction<RunForm>
    {
        public ModelAndView getView(RunForm runForm, BindException errors) throws Exception
        {
            return HttpView.redirect(urlFor(RunController.Action.showRun));
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermission(ACL.PERM_READ)
    public class ShowRunAction extends SimpleViewAction<RunForm>
    {
        FlowRun run;

        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            run = form.getRun();
            return new JspView<RunForm>(RunController.class, "showRun.jsp", form, errors);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? null : "Run not found";
            return appendFlowNavTrail(root, run, label, Action.showRun);
        }
    }

    
    @RequiresPermission(ACL.PERM_READ)
    public class DownloadAction extends SimpleViewAction<DownloadRunForm>
    {
        public ModelAndView getView(DownloadRunForm form, BindException errors) throws Exception
        {
            HttpServletResponse response = getViewContext().getResponse();
            FlowRun run = form.getRun();
            if (run == null)
            {
                response.getWriter().write("Error: no run found");
                return null;
            }

            Map<String, File> files = new TreeMap<String, File>();
            FlowWell[] wells = run.getWells(true);
            if (wells.length == 0)
            {
                response.getWriter().write("Error: no wells in run");
                return null;
            }

            for (FlowWell well : wells)
            {
                URI uri = FlowAnalyzer.getFCSUri(well);
                File file = new File(uri);
                files.put(file.getName(), file);
            }

            response.reset();
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + run.getName() + ".zip\"");
            ZipOutputStream stream = new ZipOutputStream(response.getOutputStream());
            byte[] buffer = new byte[524288];
            for (File file : files.values())
            {
                ZipEntry entry = new ZipEntry(file.getName());
                stream.putNextEntry(entry);
                InputStream is;
                if (form.getEventCount() == null)
                {
                    is = new FileInputStream(file);
                }
                else
                {
                    is = new ByteArrayInputStream(new FCS(file).getFCSBytes(file, form.getEventCount().intValue()));
                }
                int cb;
                while((cb = is.read(buffer)) > 0)
                {
                    stream.write(buffer, 0, cb);
                }
            }
            stream.close();

            return null;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class MoveToWorkspaceAction extends FormViewAction<RunForm>
    {
        FlowRun run;

        public void validateCommand(RunForm target, Errors errors)
        {
        }

        public ModelAndView getView(RunForm form, boolean reshow, BindException errors) throws Exception
        {
            run = form.getRun();
            return new JspView<RunForm>(RunController.class, "moveToWorkspace.jsp", form, errors);
        }

        public boolean handlePost(RunForm form, BindException errors) throws Exception
        {
            FlowRun run = form.getRun();
            if (run.getStep() != FlowProtocolStep.analysis)
            {
                errors.reject(ERROR_MSG, "This run cannot be moved to the workspace because it is not the analysis step.");
                return false;
            }
            FlowExperiment workspace = FlowExperiment.ensureWorkspace(getUser(), getContainer());
            FlowRun[] existing = workspace.findRun(new File(run.getPath()), FlowProtocolStep.analysis);
            if (existing.length != 0)
            {
                errors.reject(ERROR_MSG, "This run cannot be moved to the workspace because the workspace already contains a run from this directory.");
                return false;
            }
            run.moveToWorkspace(getUser());
            return true;
        }

        public ActionURL getSuccessURL(RunForm form)
        {
            return form.getRun().urlFor(ScriptController.Action.gateEditor);
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? "Move '" + run.getLabel() + "' to the workspace" : "Run not found";
            return appendFlowNavTrail(root, run, label, Action.moveToWorkspace);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class MoveToAnalysisAction extends FormViewAction<MoveToAnalysisForm>
    {
        FlowRun run;
        ActionURL successURL;

        public void validateCommand(MoveToAnalysisForm target, Errors errors)
        {
        }

        public ModelAndView getView(MoveToAnalysisForm form, boolean reshow, BindException errors) throws Exception
        {
            run = form.getRun();
            return new JspView<MoveToAnalysisForm>(RunController.class, "moveToAnalysis.jsp", form, errors);
        }

        public boolean handlePost(MoveToAnalysisForm form, BindException errors) throws Exception
        {
            FlowRun run = form.getRun();
            if (!run.isInWorkspace())
            {
                errors.reject(ERROR_MSG, "This run is not in the workspace");
                return false;
            }

            FlowExperiment experiment = FlowExperiment.fromExperimentId(form.getExperimentId());
            if (experiment.findRun(new File(run.getPath()), FlowProtocolStep.analysis).length != 0)
            {
                errors.reject(ERROR_MSG, "This run cannot be moved to this analysis because there is already a run there.");
                return false;
            }
            MoveRunFromWorkspaceJob job = new MoveRunFromWorkspaceJob(getViewBackgroundInfo(), experiment, run);
            successURL = executeScript(job);
            return true;
        }

        public ActionURL getSuccessURL(MoveToAnalysisForm form)
        {
            return successURL;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            String label = run != null ? "Move '" + run.getLabel() + "' to an analysis" : "Run not found";
            return appendFlowNavTrail(root, run, label, Action.moveToWorkspace);
        }
    }

    @RequiresPermission(ACL.PERM_UPDATE)
    public class ExportToSpiceAction extends SimpleViewAction<RunForm> //extends ExportAction<RunForm>
    {
//        public void export(RunForm form, HttpServletResponse response, BindException errors) throws Exception
        public ModelAndView getView(RunForm form, BindException errors) throws Exception
        {
            FlowRun run = form.getRun();
            if (run == null)
                throw new IllegalArgumentException("runId required");
            if (run.getDefaultQuery() != FlowTableType.FCSAnalyses) // XXX: is this expensive?
                throw new IllegalArgumentException("only analysis runs allowed");

            FlowProtocol protocol = FlowProtocol.getForContainer(getContainer());
            ICSMetadata metadata = protocol.getICSMetadata();
            if (metadata == null)
                throw new IllegalStateException("need to configure ICSMetadata");

            SubtractBackgroundQuery export = new SubtractBackgroundQuery(form.getViewContext().getActionURL(), form, metadata);
            export.genSql(errors);
            ResultSet rs = export.createResultSet(true, 0);
//            DbSchema schema = FlowManager.get().getSchema();
//            ResultSet rs = Table.executeQuery(schema, export);

//            TSVWriter tsv = new TSVGridWriter(rs, export._columns.toArray(new ColumnInfo[0]));
//            tsv.write(response);

            DataRegion rgn = new DataRegion();
            rgn.setName("export");
            rgn.setMaxRows(0);
            rgn.setShowRows(ShowRows.ALL);
            rgn.setDisplayColumns(export.getDisplayColumns());
            RenderContext ctx = new RenderContext(getViewContext(), errors);
            ctx.setResultSet(rs);
            DataView view = new GridView (rgn, ctx);


//            Writer writer = response.getWriter();
//            writer.write(export.toString());
            return view;
        }

        public NavTree appendNavTrail(NavTree root)
        {
            return null;
        }
    }

    // XXX: rename to BackgroundSubtractQuery
    /**
     * SELECT
     *   A.Name, A.OtherNonStatColumns, ...
     *   A.MatchColumn1, A.MatchColumn2,
     *   A.BackgroundColumn,
     *   A.Stat_1 - BG.Stat_1 AS "Corrected Stat_1",
     *   A.Stat_2 - BG.Stat_2 AS "Corrected Stat_2"
     * FROM
     *   (SELECT
     *      FCSAnalyses.Name, FCSAnalyses.OtherNonStatColumns, ...
     *      FCSAnalyses.MatchColumn1, FCSAnalyses.MatchColumn2,
     *      FCSAnalyses.BackgroundColumn,
     *      FCSAnalyses.Stat_1, FCSAnlyses.Stat_2
     *    FROM FCSAnalyses
     *    WHERE
     *      -- is not a background well
     *    ) AS A
     * INNER JOIN
     *   (SELECT
     *      FCSAnalyses.MatchColumn1, FCSAnalyses.MatchColumn2,
     *      AVG(FCSAnalyses.Stat_1) AS Stat_1,
     *      AVG(FCSAnalyses.Stat_2) AS Stat_2
     *    FROM FCSAnalyses
     *    WHERE
     *      -- is a background well
     *      FCSAnalyses.BackgroundColumn = BackgroundValue,
     *    GROUP BY FCSAnalyses.MatchColumn1, FCSAnalyses.MatchColumn2
     *    ) AS BG
     *  ON A.MatchColumn1 = BG.MatchColumn1 AND A.MatchColumn2 = BG.MatchColumn2
     */
    static class SubtractBackgroundQuery extends SQLFragment
    {
        protected RunForm _form;
        protected FlowRun _run;
        protected ActionURL _currentUrl;
        protected ICSMetadata _metadata;

        protected QueryDefinition _query;
        protected CustomView _customView;
        protected TableInfo _table;
        protected List<ColumnInfo> _columns;
        protected List<ColumnInfo> _ordinaryCols = new ArrayList<ColumnInfo>();
        protected List<ColumnInfo> _matchCols = new ArrayList<ColumnInfo>();
        protected List<ColumnInfo> _statCols = new ArrayList<ColumnInfo>();
        protected List<ColumnInfo> _backgroundCols = new ArrayList<ColumnInfo>();
        protected SimpleFilter _backgroundFilter = new SimpleFilter();
        private int _indent = 0;

        protected SubtractBackgroundQuery(ActionURL currentUrl, RunForm form, ICSMetadata metadata)
        {
            _currentUrl = currentUrl;
            _form = form;
            _run = form.getRun();
            _metadata = metadata;

            _query = _form.getQueryDef();
            _customView = _form.getCustomView();
            _table = _query.getMainTable();
            _columns = _query.getColumns(_customView, _table);

            scanColumns();
        }

        protected ResultSet createResultSet(boolean export, int maxRows) throws SQLException
        {
            return Table.executeQuery(FlowManager.get().getSchema(), getSQL(), getParams().toArray(), maxRows, !export);
//            return Table.executeQuery(FlowManager.get().getSchema(), this);
        }

        // XXX: only keep %P stats?
        protected boolean isStatColumn(ColumnInfo column)
        {
            return column.getName().startsWith("Statistic/") &&
                   column.getName().endsWith(":Freq_Of_Parent");
        }

        protected List<DisplayColumn> getDisplayColumns()
        {
            List<DisplayColumn> cols = new LinkedList<DisplayColumn>();
            for (ColumnInfo column : _columns)
            {
//                if (isStatColumn(column))
//                {
//                    ColumnInfo corrected = new ColumnInfo(column);
//                    corrected.setAlias("Corrected " + column.getName());
//                    DisplayColumn renderer = corrected.getRenderer();
//                    cols.add(renderer);
//                }
//                else
                {
                    cols.add(column.getRenderer());
                }
            }
            return cols;
        }

        protected void scanColumns()
        {
            for (ColumnInfo column : _columns)
            {
                FieldKey key = FieldKey.fromString(column.getName());
                ScriptSettings.FilterInfo background = null;
                if (isStatColumn(column))
                {
                    _statCols.add(column);
                }
                else if (_metadata.getMatchColumns().contains(key))
                {
                    _matchCols.add(column);
                }
                else if (null != (background = _metadata.getBackgroundFilter(key)))
                {
                    _backgroundCols.add(column);
//                    _backgroundFilter.addCondition(column.getSelectName(), background.getValue(), background.getOp());
                    _backgroundFilter.addCondition(column.getValueSql().toString(), background.getValue(), background.getOp());
                }
                else
                {
                    _ordinaryCols.add(column);
                }
            }

            if (_statCols.size() == 0)
                throw new IllegalArgumentException("at least one statistic column is required");
            if (_matchCols.size() == 0)
                throw new IllegalArgumentException("at least one column matching background to stimulated wells is required");
            if (_backgroundCols.size() == 0)
                throw new IllegalArgumentException("at least one background column is required");
        }

        protected void genSql(BindException errors)
        {
            SqlDialect dialect = _table.getSqlDialect();

            append("SELECT");
            indent();

            String strComma = "";
            for (ColumnInfo column : _ordinaryCols)
            {
                assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
//                column.declareJoins(joins);
                append(strComma);
                appendNewLine();
                append("A.").append(column.getSelectName());
//                column.getSelectName();
//                column.getLegalName();
//                column.getColumnName();
                strComma = ",";
            }
            for (ColumnInfo column : _matchCols)
            {
                append(",");
                appendNewLine();
                append("A.").append(column.getSelectName());
            }
            for (ColumnInfo column: _backgroundCols)
            {
                append(",");
                appendNewLine();
                append("A.").append(column.getSelectName());
            }
            for (ColumnInfo column : _statCols)
            {
                append(",");
                appendNewLine();
                append("A.").append(column.getSelectName()).append(" - ").append("BG.").append(column.getSelectName());
//                append(" AS ").append("\"Corrected ").append(column.getSelectName()).append("\"");
                append(" AS ").append(column.getSelectName());
            }

            appendNewLine();
            append("FROM");
            indent();
            // XXX: get original Filter and Sort
            // XXX: add "NOT backgroundFilter"
            appendNewLine();
            SQLFragment originalFrag = Table.getSelectSQL(_table, _columns, null, null, 0, 0);
            append("(").append(originalFrag).append(") AS A");
            outdent();

            Map<String, SQLFragment> joins = new LinkedHashMap<String, SQLFragment>();

            appendNewLine();
            append("INNER JOIN");
            indent();
            appendNewLine();
            append("(SELECT");
            indent();

            strComma = "";
            for (ColumnInfo column : _matchCols)
            {
                assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
                column.declareJoins(joins);
                append(strComma);
                appendNewLine();
                append(column.getSelectSql());
                strComma = ",";
            }
//            for (ColumnInfo column : _backgroundCols)
//            {
//                assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
//                column.declareJoins(joins);
//                append(",");
//                appendNewLine();
//                append(column.getSelectSql());
//            }
            for (ColumnInfo column : _statCols)
            {
                assert column.getParentTable() == _table : "Column is from the wrong table: " + column.getParentTable() + " instead of " + _table;
                column.declareJoins(joins);
                append(",");
                appendNewLine();
                append("AVG(").append(column.getValueSql()).append(") AS ").append(column.getSelectName());
            }
            outdent();

            appendNewLine();
            append("FROM");
            indent();
            appendNewLine();
            append(_table.getFromSQL());
            for (Map.Entry<String, SQLFragment> entry : joins.entrySet())
            {
                appendNewLine();
                append(entry.getValue());
            }
            outdent();

//            append("WHERE");
//            indent();
            appendNewLine();
            SQLFragment filterFrag = _backgroundFilter.getSQLFragment(dialect, Table.createColumnMap(_table, _backgroundCols));
            append(filterFrag);
//            outdent();

            appendNewLine();
            append("GROUP BY");
            indent();

            strComma = "";
            for (ColumnInfo column : _matchCols)
            {
                append(strComma);
                appendNewLine();
                append(column.getValueSql());
                strComma = ", ";
            }
            outdent();

            appendNewLine();
            append(") AS BG");
            outdent();

            strComma = "";
            appendNewLine();
            append("ON");
            indent();

            for (ColumnInfo column : _matchCols)
            {
                append(strComma);
                appendNewLine();
                append("A.").append(column.getSelectName());
                append(" = BG.").append(column.getSelectName());
                strComma = " AND ";
            }
        }

        protected void appendNewLine()
        {
            append(getNewLine());
        }

        protected String getNewLine()
        {
            assert _indent >= 0;
            return "\n" + StringUtils.repeat("  ", _indent);
        }

        protected void indent()
        {
            _indent++;
        }

        protected void outdent()
        {
            _indent--;
        }
    }

}
