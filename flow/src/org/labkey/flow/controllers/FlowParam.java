/*
 * Copyright (c) 2007-2019 LabKey Corporation
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

package org.labkey.flow.controllers;

import org.labkey.api.util.SafeToRenderEnum;

public enum FlowParam implements SafeToRenderEnum
{
    actionSequence,
    compId,
    scriptId,
    runId,
    wellId,
    workspaceId,
    statusFile,
    refresh,
    redirect,
    graph,
    experimentId,
    queryName,
    columnList,
    sampleId,
    sampleSetId,
    statisticSetId,
    objectId,
    protocolId,
    wellIds,
    objectId_graph
}
