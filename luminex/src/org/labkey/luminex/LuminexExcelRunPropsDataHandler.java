/*
 * Copyright (c) 2006-2009 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.luminex;

import org.labkey.api.exp.ExperimentException;
import org.labkey.api.exp.Handler;
import org.labkey.api.exp.Lsid;
import org.labkey.api.exp.api.DataType;
import org.labkey.api.exp.api.ExpData;
import org.labkey.api.exp.api.ExpProtocol;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.TabLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data handler for transformed Luminex run propss data.
 *
 * Created by IntelliJ IDEA.
 * User: klum
 * Date: May 14, 2009
 */
public class LuminexExcelRunPropsDataHandler extends LuminexDataHandler
{
    public static final DataType LUMINEX_TSV_RUN_PROPS_DATA_TYPE = new DataType("LuminexTsvRunPropsDataFile");

    @Override
    protected LuminexDataHandler.LuminexDataFileParser getDataFileParser(ExpProtocol protocol, File dataFile)
    {
        return new TsvParser(dataFile);
    }

    static class TsvParser implements LuminexDataHandler.LuminexDataFileParser
    {
        private File _dataFile;
        private Map<Analyte, List<LuminexDataRow>> _sheets = new LinkedHashMap<Analyte, List<LuminexDataRow>>();
        private Map<String, Object> _excelRunProps = new HashMap<String, Object>();
        private boolean _fileParsed;

        public TsvParser(File dataFile)
        {
            _dataFile = dataFile;
        }
        public Map<Analyte, List<LuminexDataRow>> getSheets() throws ExperimentException
        {
            parseFile(_dataFile);
            return _sheets;
        }

        public Map<String, Object> getExcelRunProps() throws ExperimentException
        {
            parseFile(_dataFile);
            return _excelRunProps;
        }

        private void parseFile(File dataFile) throws ExperimentException
        {
            if (_fileParsed) return;

            assert(dataFile.getName().toLowerCase().endsWith(".tsv"));
            DataLoader<Map<String, Object>> loader = null;

            try {
                loader = new TabLoader(dataFile, true);
                List<Map<String, Object>> dataRows = loader.load();

                assert(dataRows.size() == 1);
                _excelRunProps = dataRows.get(0);
                _fileParsed = true;
            }
            catch (IOException ioe)
            {
                throw new ExperimentException(ioe);
            }
            finally
            {
                if (loader != null)
                    loader.close();
            }
        }
    }

    public Handler.Priority getPriority(ExpData data)
    {
        Lsid lsid = new Lsid(data.getLSID());
        if (LUMINEX_TSV_RUN_PROPS_DATA_TYPE.matches(lsid))
        {
            return Handler.Priority.HIGH;
        }
        return null;
    }
}
