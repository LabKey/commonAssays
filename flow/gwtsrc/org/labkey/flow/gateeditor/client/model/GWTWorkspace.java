/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.flow.gateeditor.client.model;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.Map;
import java.util.HashMap;
import java.io.Serializable;

public class GWTWorkspace implements IsSerializable, Serializable
{
    boolean readOnly;
    GWTEditingMode editingMode;
    GWTScript script;
    GWTRun run;
    GWTWell[] wells = new GWTWell[0];
    String[] parameterNames = new String[0];
    String[] parameterLabels = new String[0];
    /**
     * For compensation calculation, selecting a specific subset (e.g. "L/FITC+") should result in the UI automatically
     * selecting the FITC+ FCS file.
     * @gwt.typeArgs <java.lang.String, org.labkey.flow.gateeditor.client.model.GWTWell>
     */
    Map subsetReleventWellMap = new HashMap();

    public boolean isReadOnly()
    {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly)
    {
        this.readOnly = readOnly;
    }

    public GWTEditingMode getEditingMode()
    {
        return editingMode;
    }

    public void setEditingMode(GWTEditingMode editingMode)
    {
        this.editingMode = editingMode;
    }

    public GWTScript getScript()
    {
        return this.script;
    }

    public void setScript(GWTScript script)
    {
        this.script = script;
    }

    public GWTRun getRun()
    {
        return run;
    }

    public void setRun(GWTRun run)
    {
        this.run = run;
    }

    public GWTWell[] getWells()
    {
        return wells;
    }

    public void setWells(GWTWell[] wells)
    {
        this.wells = wells;
    }

    public String[] getParameterNames()
    {
        return parameterNames;
    }

    public void setParameterNames(String[] parameterNames)
    {
        this.parameterNames = parameterNames;
    }

    public String[] getParameterLabels()
    {
        return parameterLabels;
    }

    public void setParameterLabels(String[] parameterLabels)
    {
        this.parameterLabels = parameterLabels;
    }


    public Map getSubsetReleventWellMap()
    {
        return subsetReleventWellMap;
    }

    public void setSubsetReleventWellMap(Map subsetReleventWellMap)
    {
        this.subsetReleventWellMap = subsetReleventWellMap;
    }
}
