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

package org.labkey.ms2.query;

import org.labkey.api.query.CrosstabView;
import org.labkey.api.query.UserSchema;
import org.labkey.api.reports.ReportService;

/**
 * User: jeckels
 * Date: Feb 2, 2008
 */
public abstract class ComparisonCrosstabView extends CrosstabView
{
    public ComparisonCrosstabView(UserSchema schema)
    {
        super(schema);
        // Don't allow users to create R views or other reports, since they won't know how to create the special
        // context with the run list to be compared, etc
        setViewItemFilter(ReportService.EMPTY_ITEM_LIST);
    }
}
