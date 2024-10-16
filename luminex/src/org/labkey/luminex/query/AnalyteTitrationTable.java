/*
 * Copyright (c) 2014-2019 LabKey Corporation
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
package org.labkey.luminex.query;

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.JavaScriptDisplayColumn;
import org.labkey.api.data.JdbcType;
import org.labkey.api.data.RenderContext;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.statistics.StatsService;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.query.ExprColumn;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.LookupForeignKey;
import org.labkey.api.query.QueryForeignKey;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.settings.AppProps;
import org.labkey.api.util.Pair;
import org.labkey.api.view.NavTree;
import org.labkey.api.view.PopupMenu;
import org.labkey.luminex.AbstractLuminexControlUpdateService;
import org.labkey.luminex.LuminexDataHandler;
import org.labkey.luminex.model.Analyte;
import org.labkey.luminex.model.AnalyteTitration;
import org.labkey.luminex.model.GuideSet;
import org.labkey.luminex.model.Titration;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * User: jeckels
 * Date: 7/8/11
 */
public class AnalyteTitrationTable extends AbstractCurveFitPivotTable
{
    public AnalyteTitrationTable(final LuminexProtocolSchema schema, ContainerFilter cf, boolean filter)
    {
        super(LuminexProtocolSchema.getTableInfoAnalyteTitration(), schema, cf, filter, "AnalyteId");
        setName(LuminexProtocolSchema.ANALYTE_TITRATION_TABLE_NAME);

        var analyteCol = addColumn(wrapColumn("Analyte", getRealTable().getColumn("AnalyteId")));
        analyteCol.setFk(new LookupForeignKey(cf, "RowId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createAnalyteTable(getLookupContainerFilter(),false);
            }
        });
        setTitleColumn(analyteCol.getName());
        var titrationCol = addColumn(wrapColumn("Titration", getRealTable().getColumn("TitrationId")));
        LookupForeignKey titrationFk = new LookupForeignKey(cf,"RowId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                return _userSchema.createTitrationTable(getLookupContainerFilter(),false);
            }
        };
        titrationFk.setPrefixColumnCaption(false);
        titrationCol.setFk(titrationFk);

        var maxFiCol = wrapColumn(getRealTable().getColumn("MaxFI"));
        maxFiCol.setLabel("High MFI");
        maxFiCol.setDisplayColumnFactory(new DisplayColumnFactory()
        {
            @Override
            public DisplayColumn createRenderer(ColumnInfo colInfo)
            {
                return new QCFlagHighlightDisplayColumn(colInfo, "MaxFIQCFlagsEnabled");
            }
        });
        addColumn(maxFiCol);
        SQLFragment maxFiFlagEnabledSQL = AbstractLuminexTable.createQCFlagEnabledSQLFragment(this.getSqlDialect(), LuminexDataHandler.QC_FLAG_HIGH_MFI_FLAG_TYPE, null, LuminexDataHandler.QC_FLAG_TITRATION_ID);
        ExprColumn maxFiFlagEnabledColumn = new ExprColumn(this, "MaxFIQCFlagsEnabled", maxFiFlagEnabledSQL, JdbcType.VARCHAR);
        maxFiFlagEnabledColumn.setLabel("High MFI QC Flags Enabled State");
        maxFiFlagEnabledColumn.setHidden(true);
        addColumn(maxFiFlagEnabledColumn);

        var guideSetCol = addColumn(wrapColumn("GuideSet", getRealTable().getColumn("GuideSetId")));
        guideSetCol.setFk(QueryForeignKey.from(schema, getContainerFilter()).to("GuideSet", "RowId", "AnalyteName"));

        addColumn(wrapColumn(getRealTable().getColumn("IncludeInGuideSetCalculation")));

        List<String> curveTypes = _userSchema.getCurveTypes();
        addCurveTypeColumns();

        var ljPlots = addWrapColumn("L-J Plots", getRealTable().getColumn(FieldKey.fromParts("TitrationId")));
        ljPlots.setTextAlign("left");
        ljPlots.setDisplayColumnFactory(colInfo -> {
            // using JavaScriptDisplayColumn for dependencies
            return new JavaScriptDisplayColumn(colInfo, List.of("clientapi/ext3", "vis/vis", "luminex/LeveyJenningsPlotHelpers.js", "luminex/LeveyJenningsReport.css"))
            {
                @Override
                public void renderGridCellContents(RenderContext ctx, Writer out) throws IOException
                {
                    int protocolId = schema.getProtocol().getRowId();
                    int analyte = (int)ctx.get("analyte");
                    int titration = (int)ctx.get("titration");

                    String jsFuncCall = "LABKEY.LeveyJenningsPlotHelper.getLeveyJenningsPlotWindow(%d,%d,%d,'%s','Titration'); return false;";

                    NavTree ljPlotsNav = new NavTree("Levey-Jennings Plot Menu");
                    ljPlotsNav.setImage(AppProps.getInstance().getContextPath() + "/luminex/ljPlotIcon.png", 27, 20);
                    if (curveTypes.contains(StatsService.CurveFitType.FOUR_PARAMETER.getLabel()))
                        ljPlotsNav.addChild(new NavTree("EC50 - 4PL").setScript(String.format(jsFuncCall, protocolId, analyte, titration, "EC504PL")));
                    if (curveTypes.contains(StatsService.CurveFitType.FIVE_PARAMETER.getLabel()))
                        ljPlotsNav.addChild(new NavTree("EC50 - 5PL Rumi").setScript(String.format(jsFuncCall, protocolId, analyte, titration, "EC505PL")));
                    ljPlotsNav.addChild(new NavTree("AUC").setScript(String.format(jsFuncCall, protocolId, analyte, titration, "AUC")));
                    ljPlotsNav.addChild(new NavTree("High MFI").setScript(String.format(jsFuncCall, protocolId, analyte, titration, "HighMFI")));

                    PopupMenu ljPlotsMenu = new PopupMenu(ljPlotsNav, PopupMenu.Align.LEFT, PopupMenu.ButtonStyle.IMAGE);
                    ljPlotsMenu.renderMenuButton(ctx, out, false, null);
                }

                @Override
                public boolean isSortable()
                {
                    return false;
                }

                @Override
                public boolean isFilterable()
                {
                    return false;
                }
            };
        });

        // set the default columns for this table to be those used for the QC Report
        List<FieldKey> defaultCols = new ArrayList<>();
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Name"));
        defaultCols.add(FieldKey.fromParts("L-J Plots"));
        defaultCols.add(FieldKey.fromParts("Titration"));
        defaultCols.add(FieldKey.fromParts("Titration", "Standard"));
        defaultCols.add(FieldKey.fromParts("Titration", "QCControl"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Batch", "Network"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Batch", "CustomProtocol"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Folder"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "NotebookNo"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "AssayType"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "ExpPerformer"));
        defaultCols.add(FieldKey.fromParts("Analyte", "Data", "AcquisitionDate"));
        defaultCols.add(FieldKey.fromParts("Analyte"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Isotype"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "Conjugate"));
        defaultCols.add(FieldKey.fromParts("Analyte", "Properties", "LotNumber"));
        defaultCols.add(FieldKey.fromParts("GuideSet", "Created"));
        if (curveTypes.contains(StatsService.CurveFitType.FOUR_PARAMETER.getLabel()))
            defaultCols.add(FieldKey.fromParts(StatsService.CurveFitType.FOUR_PARAMETER.getLabel() + "CurveFit", "EC50"));
        if (curveTypes.contains(StatsService.CurveFitType.FIVE_PARAMETER.getLabel()))
            defaultCols.add(FieldKey.fromParts(StatsService.CurveFitType.FIVE_PARAMETER.getLabel() + "CurveFit", "EC50"));
        defaultCols.add(FieldKey.fromParts("MaxFI"));
        defaultCols.add(FieldKey.fromParts("TrapezoidalCurveFit", "AUC"));
        defaultCols.add(FieldKey.fromParts("Titration", "Run", "QCFlags"));
        setDefaultVisibleColumns(defaultCols);
    }

    @Override
    protected LookupForeignKey createCurveFitFK(ContainerFilter cf, final String curveType)
    {
        LookupForeignKey fk = new LookupForeignKey(cf,"AnalyteId", null)
        {
            @Override
            public TableInfo getLookupTableInfo()
            {
                CurveFitTable result = _userSchema.createCurveFitTable(getLookupContainerFilter(),false);
                result.addCondition(result.getRealTable().getColumn("CurveType"), curveType);
                return result;
            }
        };
        fk.addJoin(FieldKey.fromParts("Titration"), "TitrationId", false);
        return fk;
    }

    @Override
    protected SQLFragment createContainerFilterSQL(ContainerFilter filter)
    {
        // Handle both the container and protocol filtering
        SQLFragment sql = new SQLFragment("AnalyteId IN (SELECT a.RowId FROM ");
        sql.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
        sql.append(" INNER JOIN ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" ON a.DataId = d.RowId ");
        sql.append(" INNER JOIN ");
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(" ON d.RunId = r.RowId AND r.ProtocolLSID = ? AND ");
        sql.add(getUserSchema().getProtocol().getLSID());
        sql.append(filter.getSQLFragment(getSchema(), new SQLFragment("r.Container")));
        sql.append(")");
        return sql;
    }

    @Override
    public boolean hasPermission(@NotNull UserPrincipal user, @NotNull Class<? extends Permission> perm)
    {
        checkedPermissions.add(perm);
        return (perm.equals(UpdatePermission.class) || perm.equals(ReadPermission.class))
                && _userSchema.getContainer().hasPermission(user, perm);
    }

    @Override
    public QueryUpdateService getUpdateService()
    {
        // Pair<Integer, Integer> is analyteid/titrationid combo
        return new AbstractLuminexControlUpdateService<>(this, AnalyteTitration.class)
        {
            @Override
            protected AnalyteTitration createNewBean()
            {
                return new AnalyteTitration();
            }

            @Override
            protected Pair<Integer, Integer> keyFromMap(Map<String, Object> map) throws InvalidKeyException
            {
                Integer analyteId = getInteger(map, map.containsKey("analyte") ? "analyte" : "analyteid");
                Integer titrationId = getInteger(map, map.containsKey("titration") ? "titration" : "titrationid");
                return new Pair<>(analyteId, titrationId);
            }

            @Override
            protected AnalyteTitration get(User user, Container container, Pair<Integer, Integer> key)
            {
                SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("AnalyteId"), key.getKey());
                filter.addCondition(FieldKey.fromParts("TitrationId"), key.getValue());
                return new TableSelector(LuminexProtocolSchema.getTableInfoAnalyteTitration(), filter, null).getObject(AnalyteTitration.class);
            }


            @Override
            protected void validate(AnalyteTitration bean, GuideSet guideSet, Analyte analyte) throws ValidationException
            {
                Titration titration = bean.getTitrationFromId();
                if (!Objects.equals(analyte.getName(), guideSet.getAnalyteName()))
                {
                    throw new ValidationException("GuideSet is for analyte " + guideSet.getAnalyteName(), " but this row is mapped to analyte " + analyte.getName());
                }
                if (!Objects.equals(titration.getName(), guideSet.getControlName()))
                {
                    throw new ValidationException("GuideSet is for titration " + guideSet.getControlName(), " but this row is mapped to titration " + titration.getName());
                }
            }
        };
    }
}
