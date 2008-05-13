/*
 * Copyright (c) 2007 LabKey Corporation
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

package org.labkey.flow.analysis.model;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matthewb
 * Date: Sep 18, 2007
 * Time: 2:03:40 PM
 *
 * Defines a 1 or 2 region
 */
public abstract class RegionGate extends Gate
{
    public abstract String getXAxis();
    public abstract String getYAxis();
}
