/*
 * Copyright (c) 2011-2012 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.Table;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.security.User;
import org.labkey.api.study.assay.AbstractAssayProvider;
import org.labkey.api.study.assay.AssayRunDatabaseContext;
import org.labkey.api.util.PageFlowUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Info source for running transform/validation script over a run that's already been imported into the database
 * User: jeckels
 * Date: Oct 7, 2011
 */
public class LuminexRunDatabaseContext extends AssayRunDatabaseContext<LuminexAssayProvider> implements LuminexRunContext
{
    private Map<String, Analyte> _analytes = new LinkedHashMap<String, Analyte>();
    private LuminexExcelParser _parser;

    public LuminexRunDatabaseContext(ExpRun run, User user, HttpServletRequest request)
    {
        super(run, user, request);

        // Cache the list of analytes since we'll need them to service a number of the methods
        SQLFragment sql = new SQLFragment("SELECT a.* FROM ");
        sql.append(LuminexSchema.getTableInfoAnalytes(), "a");
        sql.append(", ");
        sql.append(ExperimentService.get().getTinfoData(), "d");
        sql.append(" WHERE a.DataId = d.RowId AND d.RunId = ?");
        sql.add(run.getRowId());
        sql.append(" ORDER BY a.RowId");

        try
        {
            Analyte[] analytes = Table.executeQuery(LuminexSchema.getSchema(), sql, Analyte.class);
            for (Analyte analyte : analytes)
            {
                _analytes.put(analyte.getName(), analyte);
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    @Override
    public String[] getAnalyteNames()
    {
        return _analytes.keySet().toArray(new String[_analytes.size()]);
    }

    @Override
    public Map<DomainProperty, String> getAnalyteProperties(String analyteName)
    {
        Analyte analyte = getAnalyte(analyteName);
        Domain domain = AbstractAssayProvider.getDomainByPrefix(_protocol, LuminexAssayProvider.ASSAY_DOMAIN_ANALYTE);
        return getProperties(domain, OntologyManager.getPropertyObjects(_run.getContainer(), analyte.getLsid()));
    }

    @Override
    public Map<ColumnInfo, String> getAnalyteColumnProperties(String analyteName)
    {
        Map<ColumnInfo, String> properties = new HashMap<ColumnInfo, String>();
        Analyte analyte = getAnalyte(analyteName);
        ColumnInfo col = LuminexSchema.getTableInfoAnalytes().getColumn(LuminexDataHandler.POSITIVITY_THRESHOLD_COLUMN_NAME);
        Integer value = analyte.getPositivityThreshold();
        properties.put(col, value != null ? value.toString() : null);
        return properties;
    }

    private @NotNull
    Analyte getAnalyte(String analyteName)
    {
        Analyte analyte = _analytes.get(analyteName);
        if (analyte == null)
        {
            throw new IllegalArgumentException("Could not find analyte: " + analyteName + ", available analytes: " + _analytes.keySet());
        }
        return analyte;
    }

    @Override
    public Set<String> getTitrationsForAnalyte(String analyteName) throws ExperimentException
    {
        Analyte analyte = getAnalyte(analyteName);

        SQLFragment sql = new SQLFragment("SELECT t.Name FROM ");
        sql.append(LuminexSchema.getTableInfoTitration(), "t");
        sql.append(", ");
        sql.append(LuminexSchema.getTableInfoAnalyteTitration(), "at");
        sql.append(" WHERE t.RowId = at.TitrationId AND t.Standard = ? AND at.AnalyteId = ?");
        sql.add(Boolean.TRUE);
        sql.add(analyte.getRowId());

        try
        {
            String[] titrationNames = Table.executeArray(LuminexSchema.getSchema(), sql, String.class);
            return PageFlowUtil.set(titrationNames);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    @Override
    public List<Titration> getTitrations() throws ExperimentException
    {
        SQLFragment sql = new SQLFragment("SELECT t.* FROM ");
        sql.append(LuminexSchema.getTableInfoTitration(), "t");
        sql.append(" WHERE t.RunId = ?");
        sql.add(_run.getRowId());
        sql.append(" ORDER BY t.Name");

        try
        {
            Titration[] titrations = Table.executeQuery(LuminexSchema.getSchema(), sql, Titration.class);
            return Arrays.asList(titrations);
        }
        catch (SQLException e)
        {
            throw new RuntimeSQLException(e);
        }
    }

    @Override
    public LuminexExcelParser getParser() throws ExperimentException
    {
        if (_parser == null)
        {
            _parser = new LuminexExcelParser(getProtocol(), getUploadedData().values());
        }
        return _parser;

    }
}
