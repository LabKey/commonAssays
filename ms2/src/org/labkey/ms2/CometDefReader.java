/*
 * Copyright (c) 2003-2005 Fred Hutchinson Cancer Research Center
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

package org.labkey.ms2;

import org.apache.log4j.Logger;
import org.labkey.api.data.Table;
import org.labkey.ms2.protein.FastaDbLoader;
import org.labkey.api.security.User;
import org.labkey.api.util.NetworkDrive;
import org.fhcrc.cpas.tools.MS2Modification;
import org.labkey.api.exp.XarContext;

import java.io.*;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CometDefReader
{
    // Hard-coded Comet modification symbols
    private static String[] modSymbols = {"'", "\"", "~"};

    private User _user;
    private int _runId;
    private String _path;
    private Logger _log;
    private final XarContext _context;

    CometDefReader(User user, int runId, String path, Logger log, XarContext context)
    {
        _user = user;
        _runId = runId;
        _path = path;
        _log = log;
        _context = context;
    }


    void upload() throws SQLException, FileNotFoundException, IOException
    {
        File f = new File(_path + "/comet.def");

        if (!NetworkDrive.exists(f))
            throw new FileNotFoundException(_path + "/comet.def not found");

        InputStream is = new FileInputStream(f);

        Properties props = CometDefLoader.load(is);

        String fname = props.getProperty("DATABASE");
        int fastaId = FastaDbLoader.loadAnnotations(_path, fname, props.getProperty("DEFAULTORGANISM", FastaDbLoader.UNKNOWN_ORGANISM), props.getProperty("SHOULDGUESSORGANISM", "1").equals("1"), _log, _context);

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("SearchEngine", "COMET");
        m.put("FastaId", fastaId);
        m.put("MassSpecType", props.getProperty("MASSSPECTYPE"));
        m.put("SearchEnzyme", props.getProperty("SEARCHENZYME"));

        Enumeration<String> params = (Enumeration<String>) props.propertyNames();

        while (params.hasMoreElements())
        {
            String param = params.nextElement();
            String value = props.getProperty(param);

            if (param.startsWith("VARIABLEMOD"))
            {
                String[] modArray = value.split(" ");
                float massDiff = Float.parseFloat(modArray[0]);

                if (massDiff > 0)
                {
                    String symbol = "?";
                    int modIndex = param.charAt(11) - 49;
                    if (modIndex >= 0 && modIndex <= 2)
                        symbol = modSymbols[modIndex];
                    writeModification(_user, _runId, modArray[1].toUpperCase(), massDiff, true, symbol);
                }
            }
            else if (param.startsWith("ADD_"))
            {
                float massDiff = Float.parseFloat(value);

                if (0 != massDiff)
                    writeModification(_user, _runId, param.substring(4, 5), massDiff, false, "?");
            }
        }

        Table.update(_user, MS2Manager.getTableInfoRuns(), m, _runId, null);
    }


    private static void writeModification(User user, int run, String aa, float massDiff, boolean variable, String symbol) throws SQLException
    {
        MS2Modification mod = new MS2Modification();
        mod.setRun(run);
        mod.setAminoAcid(aa);
        mod.setMassDiff(massDiff);
        mod.setVariable(variable);
        mod.setSymbol(symbol);

        Table.insert(user, MS2Manager.getTableInfoModifications(), mod);
    }
}
