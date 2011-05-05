/*
 * Copyright (c) 2007-2010 LabKey Corporation
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

package org.labkey.flow.persist;

import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.apache.log4j.Logger;

import java.beans.PropertyChangeEvent;

public class FlowContainerListener implements ContainerManager.ContainerListener
{
    static final private Logger _log = Logger.getLogger(FlowContainerListener.class);

    public void containerCreated(Container c, User user)
    {
    }

    /**
     * Delete all Flow data from the container.
     * This code should not really be necessary since all flow data should get deleted when the associated Experiment Data object is deleted.
     * However, sometimes the FlowModule might not get notified when the data is deleted.  The Experiment Module uses the filename of the Data
     * to determine who should be notified when the data is deleted.  If something has gone wrong (i.e. a bad build was used at some point
     * to add data) we still want the user to be able to delete the corrupted container.
     *
     * For this reason, the FlowContainerListener should be registered before the ExperimentContainerListener.
     */
    public void containerDeleted(Container c, User user)
    {
        FlowManager.get().deleteContainer(c);
    }

    @Override
    public void containerMoved(Container c, Container oldParent, User user)
    {        
    }

    public void propertyChange(PropertyChangeEvent evt)
    {
    }
}
