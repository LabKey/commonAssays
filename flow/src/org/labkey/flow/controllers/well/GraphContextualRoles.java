/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
package org.labkey.flow.controllers.well;

import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.exp.api.ExpRun;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.roles.HasContextualRoles;
import org.labkey.api.security.roles.Role;
import org.labkey.api.study.assay.RunDatasetContextualRoles;
import org.labkey.api.view.ViewContext;
import org.labkey.flow.controllers.FlowParam;
import org.labkey.flow.data.FlowDataObject;
import org.labkey.flow.data.FlowRun;
import org.labkey.flow.data.FlowWell;

import java.util.Set;

/**
 * User: kevink
 * Date: 7/5/14
 *
 * Grants users read permission to see flow graph images in a flow dataset that
 * has been copied-to-study even if they don't have read permission to original
 * flow assay container.
 *
 * TODO: This is an expensive check to perform for every image in a grid.
 */
public class GraphContextualRoles implements HasContextualRoles
{
    /**
     * Returns a contextual ReaderRole if the user has permission to
     * <b>at least one of</b> the study datasets that the run results have
     * been copied to.
     *
     * @return a singleton ReaderRole set or null
     */
    @Nullable
    @Override
    public Set<Role> getContextualRoles(ViewContext context)
    {
        // skip the check if the user has ReadPermission to the container
        Container container = context.getContainer();
        User user = context.getUser();
        if (container.hasPermission(user, ReadPermission.class))
            return null;

        String objectIdStr = context.getRequest().getParameter(FlowParam.objectId.toString());
        if (objectIdStr != null)
        {
            int objectId = NumberUtils.toInt(objectIdStr);
            if (objectId == 0)
                return null;

            FlowDataObject obj = FlowDataObject.fromAttrObjectId(objectId);
            if (!(obj instanceof FlowWell))
                return null;

            FlowRun run = obj.getRun();
            if (run == null)
                return null;

            ExpRun expRun = run.getExperimentRun();
            FieldKey runIdFieldKey = FieldKey.fromParts("run");
            return RunDatasetContextualRoles.getContextualRolesForRun(context.getContainer(), context.getUser(), expRun, runIdFieldKey);
        }

        return null;
    }
}
