/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
package org.labkey.microarray;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbSchema;
import org.labkey.api.security.User;
import org.labkey.api.util.ContainerUtil;
import org.labkey.microarray.query.MicroarrayUserSchema;

/**
 * User: bbimber
 * Date: 2/23/12
 * Time: 4:42 PM
 */
public class MicroarrayContainerListener extends ContainerManager.AbstractContainerListener
{
    public void containerDeleted(Container c, User user)
    {
        DbSchema ms = MicroarrayUserSchema.getSchema();
        ContainerUtil.purgeTable(ms.getTable(MicroarrayUserSchema.TABLE_GEO_PROPS), c, "Container");

        MicroarrayManager.get().delete(c);
    }
}
