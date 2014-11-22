/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
package org.labkey.ms2.pipeline.client;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.util.List;
import java.util.Map;

/**
 * User: billnelson@uky.edu
 * Date: Feb 14, 2008
 */

public class GWTSearchServiceResult implements IsSerializable
{
    private List<String> sequenceDBs;
    private String currentPath;

    private List<String> sequenceDbPaths;
    private List<String> mascotTaxonomyList;

    private Map<String, List<String>> enzymeMap;

    private Map<String, String> mod0Map;

    private Map<String, String> mod1Map;

    private String defaultSequenceDb;

    private String selectedProtocol;
    private List<String> protocols;

    private String protocolDescription;

    private String protocolXml;
    private List<String> fileInputNames;

    private List<String> fileInputStatus;

    private boolean activeJobs;

    private String errors = "";


    public String getSelectedProtocol()
    {
        return selectedProtocol;
    }

    public void setSelectedProtocol(String selectedProtocol)
    {
        this.selectedProtocol = selectedProtocol;
    }

    public List<String> getSequenceDBs()
    {
        return sequenceDBs;
    }
    public void setSequenceDbs(List<String> sequenceDbs, String currentPath)
    {
        this.sequenceDBs = sequenceDbs;
        this.currentPath = currentPath;
    }

    public String getCurrentPath()
    {
        return currentPath;
    }

    public List<String> getSequenceDbPaths()
    {
        return sequenceDbPaths;
    }

    public void setSequenceDbPaths(List<String> sequenceDbPaths)
    {
        this.sequenceDbPaths = sequenceDbPaths;
    }

    public List<String> getProtocols()
    {
        return protocols;
    }

    public void setProtocols(List<String> protocols)
    {
        this.protocols = protocols;
    }

    public String getErrors()
    {
        return errors;
    }

    public void appendError(String error)
    {
        if(error.trim().length() == 0) return;
        if(errors.length() > 0)
            errors += "\n";
        errors += error;
    }

    public String getDefaultSequenceDb()
    {
        return defaultSequenceDb;
    }

    public void setDefaultSequenceDb(String defaultSequenceDb)
    {
        this.defaultSequenceDb = defaultSequenceDb;
    }

    public String getProtocolXml()
    {
        if(protocolXml == null || protocolXml.length() == 0)
            return "";
        return protocolXml;
    }

    public void setProtocolXml(String protocolXml)
    {
        this.protocolXml = protocolXml;
    }

    public String getProtocolDescription()
    {
        return protocolDescription;
    }

    public void setProtocolDescription(String protocolDescription)
    {
        this.protocolDescription = protocolDescription;
    }

    public List<String> getFileInputNames()
    {
        return fileInputNames;
    }

    public void setFileInputNames(List<String> names)
    {
        this.fileInputNames = names;
    }

    public List<String> getFileInputStatus()
    {
        return fileInputStatus;
    }

    public void setFileInputStatus(List<String> fileInputStatus)
    {
        this.fileInputStatus = fileInputStatus;
    }

    public boolean isActiveJobs()
    {
        return activeJobs;
    }

    public void setActiveJobs(boolean activeJobs)
    {
        this.activeJobs = activeJobs;
    }

    public Map<String, List<String>> getEnzymeMap()
    {
        return enzymeMap;
    }

    public void setEnzymeMap(Map<String, List<String>> enzymeMap)
    {
        this.enzymeMap = enzymeMap;
    }

    public Map<String, String> getMod0Map()
    {
        return mod0Map;
    }

    public void setMod0Map(Map<String, String> mod0Map)
    {
        this.mod0Map = mod0Map;
    }

    public Map<String, String> getMod1Map()
    {
        return mod1Map;
    }

    public void setMod1Map(Map<String, String> mod1Map)
    {
        this.mod1Map = mod1Map;
    }


    public List<String> getMascotTaxonomyList()
    {
           return mascotTaxonomyList;
    }
    
    public void setMascotTaxonomyList(List<String> mascotTaxonomyList)
    {
       this.mascotTaxonomyList = mascotTaxonomyList;
    }

}
