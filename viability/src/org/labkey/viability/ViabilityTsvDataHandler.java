/*
 * Copyright (c) 2007-2009 LabKey Corporation
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

package org.labkey.viability;

import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.PropertyDescriptor;
import org.labkey.api.exp.PropertyType;
import org.labkey.api.exp.property.Domain;
import org.labkey.api.exp.property.DomainProperty;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.reader.ColumnDescriptor;
import org.apache.commons.beanutils.converters.StringArrayConverter;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * User: kevink
 * Date: Sep 20, 2009
 */
public class ViabilityTsvDataHandler extends ViabilityAssayDataHandler
{
    public static final DataType DATA_TYPE = new DataType("ViabilityAssay-TsvData");

    public Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (DATA_TYPE.matches(lsid))
            return Priority.HIGH;
        return null;
    }

    protected Parser getParser(Domain runDomain, Domain resultsDomain, File dataFile)
    {
        return new Parser(runDomain, resultsDomain, dataFile);
    }

    public static class Parser extends ViabilityAssayDataHandler.Parser
    {
        public Parser(Domain runDomain, Domain resultsDomain, File dataFile)
        {
            super(runDomain, resultsDomain, dataFile);
        }

        public DataType getDataType() { return DATA_TYPE; }

        protected void _parse() throws IOException
        {
            TabLoader tl = new TabLoader(_dataFile, true);
            tl.setParseQuotes(true);

            ColumnDescriptor[] columns = tl.getColumns();
            for (ColumnDescriptor cd : columns)
            {
                if (cd.name.equals(ViabilityAssayProvider.SPECIMENIDS_PROPERTY_NAME))
                {
                    // parses a comma separated list
                    cd.converter = new StringArrayConverter();
                }
            }

            _runData = (Map<String, Object>)tl.getComments();
            _resultData = tl.load();
        }


    }
}
