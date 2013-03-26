/*
 * Copyright (c) 2012-2013 LabKey Corporation
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
package org.labkey.nab.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ButtonBar;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.DataRegion;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.MenuButton;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SimpleDisplayColumn;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.exp.query.ExpRunTable;
import org.labkey.api.query.QuerySettings;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AssayDataLinkDisplayColumn;
import org.labkey.api.study.assay.AssayProtocolSchema;
import org.labkey.api.study.assay.RunListDetailsQueryView;
import org.labkey.api.study.query.ResultsQueryView;
import org.labkey.api.study.query.RunListQueryView;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.api.view.DataView;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.ViewContext;
import org.labkey.nab.NabAssayController;
import org.labkey.nab.NabAssayProvider;
import org.labkey.nab.NabManager;
import org.springframework.validation.BindException;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: kevink
 * Date: 10/13/12
 */
public class NabProtocolSchema extends AssayProtocolSchema
{
    /*package*/ static final String DATA_ROW_TABLE_NAME = "Data";
    public static final String CUTOFF_VALUE_TABLE_NAME = "CutoffValue";
    public static final String NAB_SPECIMEN_TABLE_NAME = "NAbSpecimen";
    public static final String NAB_DBSCHEMA_NAME = "nab";

    public NabProtocolSchema(User user, Container container, @NotNull ExpProtocol protocol, @Nullable Container targetStudy)
    {
        super(user, container, protocol, targetStudy);
    }

    @Override
    public NabRunDataTable createDataTable(boolean includeCopiedToStudyColumns)
    {
        NabRunDataTable table = new NabRunDataTable(this, getProtocol());

        if (includeCopiedToStudyColumns)
        {
            addCopiedToStudyColumns(table, true);
        }
        return table;
    }

    @Override
    public ExpRunTable createRunsTable()
    {
        final ExpRunTable runTable = super.createRunsTable();
        ColumnInfo nameColumn = runTable.getColumn(ExpRunTable.Column.Name);
        // NAb has two detail type views of a run - the filtered results/data grid, and the run details page that
        // shows the graph. Set the run's name to be a link to the grid instead of the default details page.
        nameColumn.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new AssayDataLinkDisplayColumn(colInfo, runTable.getContainerFilter());
            }
        });
        return runTable;
    }

    @Nullable
    @Override
    protected ResultsQueryView createDataQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new NabResultsQueryView(getProtocol(), context, settings);
    }

    public static class NabResultsQueryView extends ResultsQueryView
    {
        private Map<String, Object> _extraDetailsUrlParams = new HashMap<String, Object>();

        public NabResultsQueryView(ExpProtocol protocol, ViewContext context, QuerySettings settings)
        {
            super(protocol, context, settings);
        }

        private void addGraphSubItems(NavTree parent, Domain domain, String dataRegionName, Set<String> excluded)
        {
            ActionURL graphSelectedURL = new ActionURL(NabAssayController.GraphSelectedAction.class, getContainer());
            for (DomainProperty prop : domain.getProperties())
            {
                if (!excluded.contains(prop.getName()))
                {
                    NavTree menuItem = new NavTree(prop.getLabel(), "#");
                    menuItem.setScript("document.forms['" + dataRegionName + "'].action = '" + graphSelectedURL.getLocalURIString() + "';\n" +
                            "document.forms['" + dataRegionName + "'].captionColumn.value = '" + prop.getName() + "';\n" +
                            "document.forms['" + dataRegionName + "'].chartTitle.value = 'Neutralization by " + prop.getLabel() + "';\n" +
                            "document.forms['" + dataRegionName + "'].method = 'GET';\n" +
                            "document.forms['" + dataRegionName + "'].submit(); return false;");
                    parent.addChild(menuItem);
                }
            }
        }

        public DataView createDataView()
        {
            DataView view = super.createDataView();
            DataRegion rgn = view.getDataRegion();
            if (!NabManager.useNewNab)
                rgn.setRecordSelectorValueColumns("ObjectId");
            else
                rgn.setRecordSelectorValueColumns("RowId");
            rgn.addHiddenFormField("protocolId", "" + _protocol.getRowId());
            ButtonBar bbar = new ButtonBar(view.getDataRegion().getButtonBar(DataRegion.MODE_GRID));
            view.getDataRegion().setButtonBar(bbar);

            ActionURL graphSelectedURL = new ActionURL(NabAssayController.GraphSelectedAction.class, getContainer());
            MenuButton graphSelectedButton = new MenuButton("Graph");
            rgn.addHiddenFormField("captionColumn", "");
            rgn.addHiddenFormField("chartTitle", "");

            graphSelectedButton.addMenuItem("Default Graph", "#",
                    "document.forms['" + rgn.getName() + "'].action = '" + graphSelectedURL.getLocalURIString() + "';\n" +
                    "document.forms['" + rgn.getName() + "'].method = 'GET';\n" +
                    "document.forms['" + rgn.getName() + "'].submit(); return false;");

            Domain sampleDomain = ((NabAssayProvider) _provider).getSampleWellGroupDomain(_protocol);
            NavTree sampleSubMenu = new NavTree("Custom Caption (Sample)");
            Set<String> excluded = new HashSet<String>();
            excluded.add(NabAssayProvider.SAMPLE_METHOD_PROPERTY_NAME);
            excluded.add(NabAssayProvider.SAMPLE_INITIAL_DILUTION_PROPERTY_NAME);
            excluded.add(NabAssayProvider.SAMPLE_DILUTION_FACTOR_PROPERTY_NAME);
            addGraphSubItems(sampleSubMenu, sampleDomain, rgn.getName(), excluded);
            graphSelectedButton.addMenuItem(sampleSubMenu);

            Domain runDomain = _provider.getRunDomain(_protocol);
            NavTree runSubMenu = new NavTree("Custom Caption (Run)");
            excluded.clear();
            excluded.add(NabAssayProvider.CURVE_FIT_METHOD_PROPERTY_NAME);
            excluded.add(NabAssayProvider.LOCK_AXES_PROPERTY_NAME);
            excluded.addAll(Arrays.asList(NabAssayProvider.CUTOFF_PROPERTIES));
            addGraphSubItems(runSubMenu, runDomain, rgn.getName(), excluded);
            graphSelectedButton.addMenuItem(runSubMenu);
            graphSelectedButton.setRequiresSelection(true);
            bbar.add(graphSelectedButton);

            rgn.addDisplayColumn(0, new SimpleDisplayColumn()
            {
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    Object runId = ctx.getRow().get(NabRunDataTable.RUN_ID_COLUMN_NAME);
                    if (runId != null)
                    {
                        ActionURL url = new ActionURL(NabAssayController.DetailsAction.class, ctx.getContainer()).addParameter("rowId", "" + runId);
                        if (!_extraDetailsUrlParams.isEmpty())
                            url.addParameters(_extraDetailsUrlParams);

                        Map<String, String> title = new HashMap<String, String>();
                        title.put("title", "View run details");
                        out.write(PageFlowUtil.textLink("run details", url, "", "", title));
                    }
                }

                @Override
                public void addQueryColumns(Set<ColumnInfo> set)
                {
                    super.addQueryColumns(set);
                    ColumnInfo runIdColumn = getTable().getColumn(NabRunDataTable.RUN_ID_COLUMN_NAME);
                    if (runIdColumn != null)
                        set.add(runIdColumn);
                }
            });
            return view;
        }

        public void setExtraDetailsUrlParams(Map<String, Object> extraDetailsUrlParams)
        {
            _extraDetailsUrlParams = extraDetailsUrlParams;
        }
    }

    public static class NabRunListQueryView extends RunListDetailsQueryView
    {
        public NabRunListQueryView(AssayProtocolSchema schema, QuerySettings settings)
        {
            super(schema, settings, NabAssayController.DetailsAction.class, "rowId", ExpRunTable.Column.RowId.toString());
        }
    }

    @Nullable
    @Override
    protected RunListQueryView createRunsQueryView(ViewContext context, QuerySettings settings, BindException errors)
    {
        return new NabRunListQueryView(this, settings);
    }

    @Override
    protected TableInfo createProviderTable(String tableType)
    {
        if(tableType != null)
        {
            if (CUTOFF_VALUE_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createCutoffValueTable();
            }

            if (NAB_SPECIMEN_TABLE_NAME.equalsIgnoreCase(tableType))
            {
                return createNAbSpecimenTable();
            }
        }
        return super.createProviderTable(tableType);
    }

    public static DbSchema getSchema()
    {
        return DbSchema.get(NAB_DBSCHEMA_NAME);
    }

    public static TableInfo getTableInfoNAbSpecimen()
    {
        return getSchema().getTable(NAB_SPECIMEN_TABLE_NAME);
    }

    public static TableInfo getTableInfoCutoffValue()
    {
        return getSchema().getTable(CUTOFF_VALUE_TABLE_NAME);
    }

    private NAbSpecimenTable createNAbSpecimenTable()
    {
        return new NAbSpecimenTable(this);
    }

    private CutoffValueTable createCutoffValueTable()
    {
        return new CutoffValueTable(this);
    }

    public Set<String> getTableNames()
    {
        Set<String> result = super.getTableNames();
        result.add(CUTOFF_VALUE_TABLE_NAME);
        return result;
    }
}
