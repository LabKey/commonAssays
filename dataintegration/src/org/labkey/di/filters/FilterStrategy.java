/*
 * Copyright (c) 2013 LabKey Corporation
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
package org.labkey.di.filters;

import org.labkey.api.data.SimpleFilter;
import org.labkey.di.VariableMap;
import org.labkey.di.pipeline.TransformJobContext;
import org.labkey.di.steps.StepMeta;

/**
 * Created with IntelliJ IDEA.
 * User: matthew
 * Date: 4/22/13
 * Time: 11:55 AM
 *
 *
 * The Filter strategy combines two pieces of funcitonality.
 *
 * 1) checker: determines whether there is incremental work to perform
 * 2) filter: select a subset of rows to process
 *
 */
public interface FilterStrategy
{
    interface Factory
    {
        FilterStrategy getFilterStrategy(TransformJobContext context, StepMeta stepMeta);
        boolean checkStepsSeparately();
    }

    boolean hasWork();

    /* Has side effect of setting parameters */
    SimpleFilter getFilter(VariableMap variables);
}