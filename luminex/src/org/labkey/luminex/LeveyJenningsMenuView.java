/*
 * Copyright (c) 2013-2026 LabKey Corporation
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
package org.labkey.luminex;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.TableInfo;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.view.JspView;
import org.labkey.luminex.query.LuminexProtocolSchema;

import java.util.List;

/**
 * User: jeckels
 * Date: 8/22/13
 */
public class LeveyJenningsMenuView extends JspView<LeveyJenningsMenuView.Bean>
{
    public static class Bean
    {

        private final ExpProtocol _protocol;
        private final String _analyte;
        private final List<String> _titrations;
        private final List<String> _singlePointControls;
        private final List<String> _antigens;

        public Bean(ExpProtocol protocol, @Nullable String analyte, List<String> titrations, List<String> singlePointControls, List<String> antigens)
        {
            _protocol = protocol;
            _analyte = analyte;
            _titrations = titrations;
            _singlePointControls = singlePointControls;
            _antigens = antigens;
        }

        public @Nullable String getAnalyte()
        {
            return _analyte;
        }

        public List<String> getTitrations()
        {
            return _titrations;
        }

        public List<String> getSinglePointControls()
        {
            return _singlePointControls;
        }

        public List<String> getAntigens()
        {
            return _antigens;
        }

        public ExpProtocol getProtocol()
        {
            return _protocol;
        }
    }

    public LeveyJenningsMenuView(ExpProtocol protocol, @Nullable String analyte)
    {
        super("/org/labkey/luminex/view/leveyJenningsMenu.jsp");
        ContainerFilter.AllFolders containerFilter = new ContainerFilter.AllFolders(getViewContext().getUser());
        SQLFragment containerSQL = containerFilter.getSQLFragment(LuminexProtocolSchema.getSchema(), SQLFragment.unsafe("r.Container"));

        ControlSource titrationSource = new ControlSource(LuminexProtocolSchema.getTableInfoTitration(),
                SQLFragment.unsafe(" AND (c.Standard = ? OR c.QCControl = ?)", true, true),
                LuminexProtocolSchema.getTableInfoAnalyteTitration(), "TitrationId");
        ControlSource singlePointControlSource = new ControlSource(LuminexProtocolSchema.getTableInfoSinglePointControl(), null,
                LuminexProtocolSchema.getTableInfoAnalyteSinglePointControl(), "SinglePointControlId");

        List<String> titrations = getControlNames(titrationSource, protocol, containerSQL, analyte);
        List<String> singlePointControls = getControlNames(singlePointControlSource, protocol, containerSQL, analyte);

        List<String> antigens = List.of();
        if (analyte == null)
        {
            SQLFragment antigenSQL = getAntigenNamesSQL(titrationSource, protocol, containerSQL);
            antigenSQL.append(SQLFragment.unsafe(" UNION "));
            antigenSQL.append(getAntigenNamesSQL(singlePointControlSource, protocol, containerSQL));
            antigenSQL.append(SQLFragment.unsafe(" ORDER BY Name"));
            antigens = new SqlSelector(LuminexProtocolSchema.getSchema(), antigenSQL).getArrayList(String.class);
        }

        setModelBean(new Bean(protocol, analyte, titrations, singlePointControls, antigens));
    }

    private record ControlSource(TableInfo controlTable, @Nullable SQLFragment controlFilter, TableInfo analyteJunctionTable, String junctionControlColumn) {}

    private static List<String> getControlNames(ControlSource source, ExpProtocol protocol, SQLFragment containerSQL, @Nullable String analyte)
    {
        SQLFragment sql = SQLFragment.unsafe("SELECT DISTINCT c.Name");
        sql.append(getControlsFromSQL(source, protocol, containerSQL));
        if (analyte != null)
        {
            sql.append(SQLFragment.unsafe(" AND EXISTS (SELECT 1"));
            sql.append(getAnalyteJunctionFromSQL(source));
            sql.append(SQLFragment.unsafe(" = c.RowId AND a.Name = ?)", analyte));
        }
        sql.append(SQLFragment.unsafe(" ORDER BY c.Name"));
        return new SqlSelector(LuminexProtocolSchema.getSchema(), sql).getArrayList(String.class);
    }

    private static SQLFragment getAntigenNamesSQL(ControlSource source, ExpProtocol protocol, SQLFragment containerSQL)
    {
        SQLFragment sql = SQLFragment.unsafe("SELECT a.Name");
        sql.append(getAnalyteJunctionFromSQL(source));
        sql.append(SQLFragment.unsafe(" IN (SELECT c.RowId"));
        sql.append(getControlsFromSQL(source, protocol, containerSQL));
        sql.append(SQLFragment.unsafe(")"));
        return sql;
    }

    /** FROM/WHERE for the protocol's controls (alias c) and their runs (alias r) in the container scope */
    private static SQLFragment getControlsFromSQL(ControlSource source, ExpProtocol protocol, SQLFragment containerSQL)
    {
        SQLFragment sql = SQLFragment.unsafe(" FROM ");
        sql.append(source.controlTable(), "c");
        sql.append(SQLFragment.unsafe(" JOIN "));
        sql.append(ExperimentService.get().getTinfoExperimentRun(), "r");
        sql.append(SQLFragment.unsafe(" ON r.RowId = c.RunId WHERE r.ProtocolLSID = ? AND ", protocol.getLSID()));
        sql.append(containerSQL);
        if (source.controlFilter() != null)
            sql.append(source.controlFilter());
        return sql;
    }

    /** FROM the analyte junction (alias j) and analytes (alias a), ending with the junction's control column so the caller adds the comparison */
    private static SQLFragment getAnalyteJunctionFromSQL(ControlSource source)
    {
        SQLFragment sql = SQLFragment.unsafe(" FROM ");
        sql.append(source.analyteJunctionTable(), "j");
        sql.append(SQLFragment.unsafe(" JOIN "));
        sql.append(LuminexProtocolSchema.getTableInfoAnalytes(), "a");
        sql.append(SQLFragment.unsafe(" ON a.RowId = j.AnalyteId WHERE "));
        sql.append(source.analyteJunctionTable().getColumn(source.junctionControlColumn()).getValueSql("j"));
        return sql;
    }
}
