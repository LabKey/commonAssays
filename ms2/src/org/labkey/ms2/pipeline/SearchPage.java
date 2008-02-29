/*
 * Copyright (c) 2007 LabKey Software Foundation
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
package org.labkey.ms2.pipeline;

import org.labkey.api.jsp.FormPage;
import org.labkey.api.exp.api.ExpRun;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * <code>SearchPage</code>
 */
abstract public class SearchPage extends FormPage<MS2SearchForm>
{
    private Map<File, FileStatus> mzXmlFileStatus;
    private Map<String, String[]> sequenceDBs;
    private String[] protocolNames;

    public Map<File, FileStatus> getMzXmlFileStatus()
    {
        return mzXmlFileStatus;
    }

    public void setMzXmlFileStatus(Map<File, FileStatus> mzXmlFileStatus)
    {
        this.mzXmlFileStatus = mzXmlFileStatus;
    }

    public Map<String, String[]> getSequenceDBs()
    {
        return sequenceDBs;
    }

    public void setSequenceDBs(Map<String, String[]> sequenceDBs)
    {
        this.sequenceDBs = sequenceDBs;
    }

    public String[] getProtocolNames()
    {
        return protocolNames;
    }

    public void setProtocolNames(String[] protocolNames)
    {
        this.protocolNames = protocolNames;
    }

}
