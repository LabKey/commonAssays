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
package org.labkey.ms1.model;

import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExperimentService;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.ms1.MS1Module;

/**
 * Model for the PepSearchView.jsp
 * 
 * Created by IntelliJ IDEA.
 * User: Dave
 * Date: Jan 9, 2008
 * Time: 9:25:10 AM
 */
public class PepSearchModel
{
    private String _pepSequence;
    private Container _container;

    public PepSearchModel(Container container, String pepSequence)
    {
        _container = container;
        _pepSequence = null == pepSequence ? "" : pepSequence;
    }

    public String getPepSequence()
    {
        return _pepSequence;
    }

    public void setPepSequence(String pepSequence)
    {
        _pepSequence = pepSequence;
    }

    public Container getContainer()
    {
        return _container;
    }

    public ExpRun[] getRuns(Container container)
    {
        ExperimentService.Interface expSvc = ExperimentService.get();
        return expSvc.getExpRuns(container, expSvc.getExpProtocol(container, MS1Module.PROTOCOL_MS1), null);
    }

}
