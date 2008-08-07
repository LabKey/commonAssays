/*
 * Copyright (c) 2008 LabKey Corporation
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
    /**
     * @gwt.typeArgs <java.lang.String>
     */
    private List sequenceDBs;
    private String currentPath;
    /**
     * @gwt.typeArgs <java.lang.String>
     */
    private List sequenceDbPaths;
    /**
     * @gwt.typeArgs <java.lang.String>
     */
    private List mascotTaxonomyList;

    /**
     * @gwt.typeArgs <java.lang.String, java.lang.String>
     */
    private Map enzymeMap;

    /**
     * @gwt.typeArgs <java.lang.String, java.lang.String>
     */
    private Map mod0Map;

    /**
     * @gwt.typeArgs <java.lang.String, java.lang.String>
     */
    private Map mod1Map;

    private String defaultSequenceDb;

    private String selectedProtocol;
    /**
     * @gwt.typeArgs <java.lang.String>
     */
    private List protocols;

    private String protocolDescription;

    private String protocolXml;
    /**
     * @gwt.typeArgs <java.lang.String>
     */
    private List fileInputNames;

    /**
     * @gwt.typeArgs <java.lang.String>
     */
    private List fileInputStatus;

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

    /**
     * @gwt.typeArgs <java.lang.String>
     */
    public List getSequenceDBs()
    {
        return sequenceDBs;
    }
    /**
     * @gwt.typeArgs sequenceDbs <java.lang.String>
     */
    public void setSequenceDbs(List sequenceDbs, String currentPath)
    {
        this.sequenceDBs = sequenceDbs;
        this.currentPath = currentPath;
    }

    public String getCurrentPath()
    {
        return currentPath;
    }

    /**
     * @gwt.typeArgs <java.lang.String>
     */
    public List getSequenceDbPaths()
    {
        return sequenceDbPaths;
    }

    /**
     * @gwt.typeArgs sequenceDbPaths <java.lang.String>
     */
    public void setSequenceDbPaths(List sequenceDbPaths)
    {
        this.sequenceDbPaths = sequenceDbPaths;
    }

    /**
     * @gwt.typeArgs <java.lang.String>
     */
    public List getProtocols()
    {
        return protocols;
    }

    /**
     * @gwt.typeArgs protocols <java.lang.String>
     */
    public void setProtocols(List protocols)
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

    /**
     * @gwt.typeArgs <java.lang.String>
     */
    public List getFileInputNames()
    {
        return fileInputNames;
    }

    /**
     * @gwt.typeArgs protocols <java.lang.String>
     */
    public void setFileInputNames(List names)
    {
        this.fileInputNames = names;
    }

    /**
     * @gwt.typeArgs <java.lang.String>
     */
    public List getFileInputStatus()
    {
        return fileInputStatus;
    }

    /**
     * @gwt.typeArgs protocols <java.lang.String>
     */
    public void setFileInputStatus(List fileInputStatus)
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

    /**
     * @gwt.typeArgs <java.lang.String, java.lang.String>
     */
    public Map getEnzymeMap()
    {
        return enzymeMap;
    }

    /**
     * @gwt.typeArgs  enzymeMap <java.lang.String, java.lang.String>
     */
    public void setEnzymeMap(Map enzymeMap)
    {
        this.enzymeMap = enzymeMap;
    }

    /**
     * @gwt.typeArgs <java.lang.String, java.lang.String>
     */
    public Map getMod0Map()
    {
        return mod0Map;
    }

    /**
     * @gwt.typeArgs  mod0Map <java.lang.String, java.lang.String>
     */
    public void setMod0Map(Map mod0Map)
    {
        this.mod0Map = mod0Map;
    }

    /**
     * @gwt.typeArgs <java.lang.String, java.lang.String>
     */
    public Map getMod1Map()
    {
        return mod1Map;
    }

    /**
     * @gwt.typeArgs  mod1Map <java.lang.String, java.lang.String>
     */
    public void setMod1Map(Map mod1Map)
    {
        this.mod1Map = mod1Map;
    }


    /**
     * @gwt.typeArgs <java.lang.String>
     */
    public List getMascotTaxonomyList()
    {
           return mascotTaxonomyList;
    }
    
    /**
     * @gwt.typeArgs mascotTaxonomyList <java.lang.String>
     */
    public void setMascotTaxonomyList(List mascotTaxonomyList)
    {
       this.mascotTaxonomyList = mascotTaxonomyList;
    }

}
