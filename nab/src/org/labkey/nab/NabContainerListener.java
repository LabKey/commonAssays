/*
 * Copyright (c) 2008-2018 LabKey Corporation
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
package org.labkey.nab;

import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.RuntimeSQLException;
import org.labkey.api.security.User;

import java.sql.SQLException;

/**
 * User: adam
 * Date: Nov 5, 2008
 * Time: 3:09:06 PM
 */
public class NabContainerListener extends ContainerManager.AbstractContainerListener
{
    @Override
    public void containerDeleted(Container c, User user)
    {
        NabManager.get().deleteContainerData(c);
    }
}
