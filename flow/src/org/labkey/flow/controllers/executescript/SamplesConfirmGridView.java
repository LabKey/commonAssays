/*
 * Copyright (c) 2012-2017 LabKey Corporation
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
package org.labkey.flow.controllers.executescript;

import org.labkey.api.action.SpringActionController;
import org.labkey.api.collections.NamedObject;
import org.labkey.api.collections.NamedObjectList;
import org.labkey.api.collections.RowMapFactory;
import org.labkey.api.data.AbstractForeignKey;
import org.labkey.api.data.ActionButton;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.CachedResultSets;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.InputColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.Results;
import org.labkey.api.data.ResultsImpl;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.query.FieldKey;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.SimpleNamedObject;
import org.labkey.api.util.StringExpression;
import org.labkey.api.view.GridView;
import org.labkey.flow.analysis.model.ISampleInfo;
import org.labkey.flow.analysis.model.Workspace;
import org.labkey.flow.data.FlowFCSFile;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.util.KeywordUtil;
import org.springframework.validation.Errors;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 10/20/12
 */
public class SamplesConfirmGridView extends GridView
{
    /*package*/ public static String DATAREGION_NAME = "SamplesConfirm";

    static FieldKey MATCHED_FLAG_FIELD_KEY = new FieldKey(null, "MatchedFlag");
    static FieldKey MATCHED_FILE_FIELD_KEY = new FieldKey(null, "MatchedFile");
    static FieldKey CANDIDATE_FILES_FIELD_KEY = new FieldKey(null, "CandidateFiles");
    static FieldKey SAMPLE_ID_FIELD_KEY = new FieldKey(null, "SampleId");
    static FieldKey SAMPLE_NAME_FIELD_KEY = new FieldKey(null, "SampleName");
    static FieldKey GROUP_NAMES_FIELD_KEY = new FieldKey(null, "GroupNames");

    Map<Integer, FlowRun> _runs = new HashMap<>();

    public SamplesConfirmGridView(SelectedSamples data, boolean resolving, Errors errors)
    {
        this(data.getKeywords(), data.getSamples(), resolving, data.getRows(), errors);
    }

    public SamplesConfirmGridView(Collection<String> keywords, List<? extends ISampleInfo> samples, boolean resolving, Map<String, SelectedSamples.ResolvedSample> rows, Errors errors)
    {
        super(new SamplesConfirmDataRegion(), errors);

        boolean hasGroupInfo = samples.get(0) instanceof Workspace.SampleInfo;

        // Create the list of columns
        keywords = KeywordUtil.filterHidden(keywords);
        List<String> columns = new ArrayList<>();
        if (resolving)
        {
            columns.add(MATCHED_FLAG_FIELD_KEY.getName());
            columns.add(MATCHED_FILE_FIELD_KEY.getName());
            columns.add(CANDIDATE_FILES_FIELD_KEY.getName());
        }
        columns.add(SAMPLE_ID_FIELD_KEY.getName());
        columns.add(SAMPLE_NAME_FIELD_KEY.getName());
        if (hasGroupInfo)
            columns.add(GROUP_NAMES_FIELD_KEY.getName());
        columns.addAll(keywords);
        int columnCount = columns.size();
        RowMapFactory factory = new RowMapFactory(columns);

        // Create the data maps, one for each sample in the workspace
        List<Map<String, Object>> unmatchedList = new ArrayList<>(samples.size());
        List<Map<String, Object>> matchedList = new ArrayList<>(samples.size());
        for (ISampleInfo sample : samples)
        {
            Object[] row = new Object[columnCount];
            int i = 0;

            // MatchedFlag and MatchedFile
            SelectedSamples.ResolvedSample matched = null;
            if (resolving)
            {
                matched = rows.get(sample.getSampleId());
                row[i++] = matched != null && matched.hasMatchedFile();
                row[i++] = matched != null && matched.hasMatchedFile() ? matched.getMatchedFile() : null;
                row[i++] = matched != null ? matched.getCandidateFCSFiles() : null;
            }

            // SampleId and SampleName
            row[i++] = sample.getSampleId();
            row[i++] = sample.getLabel();

            // GroupNames
            if (hasGroupInfo)
            {
                String sep = "";
                StringBuilder sb = new StringBuilder();
                for (Workspace.GroupInfo group : ((Workspace.SampleInfo)sample).getGroups())
                {
                    if (group.isAllSamples())
                        continue;
                    sb.append(sep).append(group.getGroupName().toString());
                    sep = ", ";
                }
                row[i++] = sb.toString();
            }

            // Keywords
            Map<String, String> sampleKeywords = sample.getKeywords();
            for (String keyword : keywords)
                row[i++] = sampleKeywords.get(keyword);

            Map<String, Object> rowMap = factory.getRowMap(row);
            if (matched != null && matched.hasMatchedFile())
                matchedList.add(rowMap);
            else
                unmatchedList.add(rowMap);
        }

        // Combine unmatched and matched lists (unmatched are first so the user sees them)
        List<Map<String, Object>> maps = new ArrayList<>(samples.size());
        maps.addAll(unmatchedList);
        maps.addAll(matchedList);

        // Initialize the ResultSet and DataRegion
        ResultSet rs = CachedResultSets.create(maps);
        Results results = new ResultsImpl(rs);
        setResults(results);

        SamplesConfirmDataRegion dr = (SamplesConfirmDataRegion)getDataRegion();
        dr.setName(DATAREGION_NAME);
        dr.setShowFilters(false);
        dr.setSortable(false);
        dr.setShowPagination(true);

        dr.resolving = resolving;
        dr.sampleCount = samples.size();
        dr.matchedCount = matchedList.size();
        dr.unmatchedCount = unmatchedList.size();

        // Populate selection state with the sample ids for the selected rows.
        // If the sample id selector value is present in the selection state, the row is checked.
        //dr.setRecordSelectorValueColumns(SAMPLE_ID_FIELD_KEY.getName());
        dr.setShowRecordSelectors(true);
        Set<String> selected = new HashSet<>();
        for (Map.Entry<String, SelectedSamples.ResolvedSample> row : rows.entrySet())
        {
            if (row.getValue().isSelected())
                selected.add(row.getKey());
        }
        getRenderContext().setAllSelected(selected);

        ButtonBar buttonBar = new ButtonBar();
        dr.setButtonBar(buttonBar);

        DisplayColumn dc;
        if (resolving)
        {
            ActionButton button = new ActionButton("Update Matches");
            button.setActionType(ActionButton.Action.POST);
            // Set the form's step to the previous step and submit.
            button.setScript(
                    "document.forms[\"" + ImportAnalysisForm.NAME + "\"].elements[\"step\"].value = " + AnalysisScriptController.ImportAnalysisStep.SELECT_FCSFILES.getNumber() + "; " +
                    "document.forms[\"" + ImportAnalysisForm.NAME + "\"].submit();", false);
            buttonBar.add(button);
            dr.setButtonBarPosition(DataRegion.ButtonBarPosition.TOP);

            // Add MatchedFlag column
            dc = new MatchedFlagDisplayColumn();
            dr.addDisplayColumn(dc);

            // Add Matched column
            ColumnInfo matchCol = new ColumnInfo(MATCHED_FILE_FIELD_KEY, null, JdbcType.INTEGER);
            matchCol.setLabel("Matched FCS File");
            matchCol.setFk(new FCSFilesFilesForeignKey(FlowFCSFile.getOriginal(getViewContext().getContainer())));
            matchCol.setInputType("select");
            dc = new MatchedFileDisplayColumn(matchCol);
            dr.addDisplayColumn(dc);
        }


        // Add SampleName column
        dc = new SimpleDisplayColumn("${" + SAMPLE_NAME_FIELD_KEY.getName() + "}");
        dc.setCaption("Name");
        dr.addDisplayColumn(dc);

        // Add GroupNames column
        if (hasGroupInfo)
        {
            dc = new SimpleDisplayColumn("${" + GROUP_NAMES_FIELD_KEY.getName() + "}");
            dc.setCaption("Groups");
            dr.addDisplayColumn(dc);
        }

        // Add keyword columns
        for (String keyword : keywords)
        {
            dc = new SimpleDisplayColumn("${" + keyword + "}");
            dc.setCaption(keyword);
            dr.addDisplayColumn(dc);
        }
    }

    private static class SamplesConfirmDataRegion extends DataRegion
    {
        protected boolean resolving = false;
        protected int sampleCount = 0;
        protected int unmatchedCount = 0;
        protected int matchedCount = 0;

        @Override
        protected Map<String, String> prepareMessages(RenderContext ctx) throws IOException
        {
            Map<String, String> messages = super.prepareMessages(ctx);

            if (resolving)
            {
                String matchedMsg;
                if (matchedCount == sampleCount)
                    matchedMsg = String.format("Matched all %d samples.", sampleCount);
                else
                    matchedMsg = String.format("Matched %d of %d samples.", matchedCount, sampleCount);
                messages.put("matches", matchedMsg);
            }

            return messages;
        }

        @Override
        protected void renderFormBegin(RenderContext ctx, Writer out, int mode) throws IOException
        {
            renderHiddenFormFields(ctx, out, mode);
        }

        @Override
        protected void renderFormEnd(RenderContext ctx, Writer out) throws IOException
        {
            // No-op.  Don't close the form.
        }

        @Override
        protected String getRecordSelectorName(RenderContext ctx)
        {
            // Bind select checkbox to ImportAnalyisForm.selectedSamples.select
            String sampleId = ctx.get(SAMPLE_ID_FIELD_KEY, String.class);
            return "selectedSamples.rows[" + sampleId + "].selected";
        }

        @Override
        protected String getRecordSelectorValue(RenderContext ctx)
        {
            return "1";
        }

        @Override
        protected boolean isRecordSelectorChecked(RenderContext ctx, String checkboxValue)
        {
            String sampleId = ctx.get(SAMPLE_ID_FIELD_KEY, String.class);
            Set<String> selectedValues = ctx.getAllSelected();
            return selectedValues.contains(sampleId);
        }

        @Override
        protected void renderExtraRecordSelectorContent(RenderContext ctx, Writer out) throws IOException
        {
            // Add a hidden input for spring form binding -- if this value is posed, the row was unchecked.
            out.write("<input type=\"hidden\" name=\"");
            out.write(SpringActionController.FIELD_MARKER + getRecordSelectorName(ctx));
            out.write("\" value=\"0\">");
        }

        @Override
        protected boolean isErrorRow(RenderContext ctx, int rowIndex)
        {
            if (resolving)
            {
                // Render unmatched rows as errors
                Boolean match = ctx.get(MATCHED_FLAG_FIELD_KEY, Boolean.class);
                if (match != null && match)
                    return false;

                // If the row isn't selected and won't be imported, don't render as an error.
                // Unmatched and row is selected for import.
                return isRecordSelectorChecked(ctx, getRecordSelectorValue(ctx));
            }

            return false;
        }
    }

    private class MatchedFlagDisplayColumn extends SimpleDisplayColumn
    {

        public MatchedFlagDisplayColumn()
        {
            super();
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            Boolean match = ctx.get(MATCHED_FLAG_FIELD_KEY, Boolean.class);
            if (match != null)
            {
                out.write("<img src=\"");
                out.write(AppProps.getInstance().getContextPath());
                if (match)
                {
                    out.write("/_images/check.png\" />");
                    String fileName = ctx.get(SAMPLE_NAME_FIELD_KEY, String.class);
                    out.write(PageFlowUtil.helpPopup("Matched", "Matched the previously imported FCS file '" + fileName + "'", true));
                }
                else
                {
                    out.write("/_images/cancel.png\" />");
                    out.write(PageFlowUtil.helpPopup("Not matched", "Failed to match a previously imported FCS file.  Please manually select a matching FCS file or skip importing this row.", true));
                }
            }
        }
    }

    // Simple lookup to list of original FlowFCSFiles
    private class FCSFilesFilesForeignKey extends AbstractForeignKey
    {
        private static final boolean INCLUDE_ALL_FILES = false;

        List<FlowFCSFile> _files;
        NamedObjectList _list;

        FCSFilesFilesForeignKey(List<FlowFCSFile> files)
        {
            _files = files;

            _list = new NamedObjectList();
            for (FlowFCSFile file : _files)
                _list.put(new SimpleNamedObject(String.valueOf(file.getRowId()), file));
        }

        @Override
        public ColumnInfo createLookupColumn(ColumnInfo parent, String displayField)
        {
            return null;
        }

        @Override
        public TableInfo getLookupTableInfo()
        {
            return null;
        }

        @Override
        public StringExpression getURL(ColumnInfo parent)
        {
            return null;
        }

        @Override
        public NamedObjectList getSelectList(RenderContext ctx)
        {
            List<FlowFCSFile> candidates = (List<FlowFCSFile>)ctx.get(CANDIDATE_FILES_FIELD_KEY, List.class);
            if (candidates == null || candidates.isEmpty())
                return _list;

            // Put most likely canidates on the top of the list
            Set<Integer> candidateRowIds = new HashSet<>(candidates.size());
            NamedObjectList list = new NamedObjectList();
            for (FlowFCSFile candidate : candidates)
            {
                candidateRowIds.add(candidate.getRowId());
                list.put(new SimpleNamedObject(String.valueOf(candidate.getRowId()), candidate));
            }

            // Issue 18728: flow import: resolve step is too slow
            // Workaround for now is to not include all files if there are better candidates -- ideally the resolve step would have an auto-complete field or something instead of this giant combo box
            if (INCLUDE_ALL_FILES)
            {
                if (list.size() < _list.size())
                {
                    // Add a divider (TODO: Disable this option item in the select list)
                    list.put(new SimpleNamedObject("--------", "--------"));

                    // Add the remaining FCS files (maybe add an option to not show all FCS files?)
                    for (NamedObject no : _list)
                        if (!candidateRowIds.contains(((FlowFCSFile)no.getObject()).getRowId()))
                            list.put(no);
                }
            }

            return list;
        }
    }

    private class MatchedFileDisplayColumn extends InputColumn
    {
        public MatchedFileDisplayColumn(ColumnInfo col)
        {
            super(col);
        }

        @Override
        public String getFormFieldName(RenderContext ctx)
        {
            // Bind select combobox to ImportAnalyisForm.selectedSamples.matchedFile
            String sampleId = ctx.get(SAMPLE_ID_FIELD_KEY, String.class);
            return "selectedSamples.rows[" + sampleId + "].matchedFile";
        }

        @Override
        protected String getSelectInputDisplayValue(NamedObject entry)
        {
            Object o = entry.getObject();
            if (o instanceof String)
               return (String)o;
            
            if (!(o instanceof FlowFCSFile))
                return null;

            FlowFCSFile file = (FlowFCSFile)o;
            ExpData data = file.getData();
            FlowRun run = null;
            if (data != null && data.getRunId() != null)
            {
                run = _runs.get(data.getRunId());
                if (run == null)
                    _runs.put(data.getRunId(), run = file.getRun());
            }

            if (run != null)
                return file.getName() + " (" + run.getName() + ")";
            else
                return file.getName();
        }

        @Override
        public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
        {
            super.renderGridCellContents(ctx, out);

            // Render hidden form inputs for candidate well ids.
            List<FlowFCSFile> candidates = (List<FlowFCSFile>)ctx.get(CANDIDATE_FILES_FIELD_KEY, List.class);
            if (candidates != null)
            {
                String sampleId = ctx.get(SAMPLE_ID_FIELD_KEY, String.class);
                for (FlowFCSFile candidate : candidates)
                {
                    out.write("<input type='hidden' name='selectedSamples.rows[" + sampleId + "].candidateFile' value='" + candidate.getRowId() + "'>\n");
                }
            }
        }
    }
}

