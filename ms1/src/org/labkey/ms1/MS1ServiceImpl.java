/*
 * Copyright (c) 2007-2017 LabKey Corporation
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

package org.labkey.ms1;

import org.labkey.api.data.TableInfo;
import org.labkey.api.data.Container;
import org.labkey.api.security.User;
import org.labkey.api.ms1.MS1Service;
import org.labkey.ms1.query.MS1Schema;

/**
 * Implementation of MS1Service.Service
 * User: Dave
 * Date: Oct 26, 2007
 * Time: 10:41:11 AM
 */
public class MS1ServiceImpl implements MS1Service
{
    public TableInfo createFeaturesTableInfo(User user, Container container)
    {
        return createFeaturesTableInfo(user, container, true);
    }

    public TableInfo createFeaturesTableInfo(User user, Container container, boolean includePepFk)
    {
        return new MS1Schema(user, container).getFeaturesTableInfo(includePepFk);
    }
}
