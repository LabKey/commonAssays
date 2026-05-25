/*
 * Copyright (c) 2019-2026 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.flow.analysis.model.FlowException;
import org.labkey.flow.data.AttributeType;

import java.util.List;

public class FlowCasingMismatchException extends FlowException
{
    String _sampleLabel;
    AttributeType _type;
    List<FlowManager.FlowEntry> _existingNames;
    String _mismatchedName;

    public FlowCasingMismatchException(@Nullable String detail, @Nullable String sampleLabel, AttributeType type, List<FlowManager.FlowEntry> existingNames, String mismatchedName)
    {
        super(casingMismatchMessage(detail, sampleLabel, type, existingNames, mismatchedName));
        _sampleLabel = sampleLabel;
        _type = type;
        _existingNames = existingNames;
        _mismatchedName = mismatchedName;
    }

    public static String casingMismatchMessage(@Nullable String detail, @Nullable String sampleLabel, AttributeType type, List<FlowManager.FlowEntry> existingNames, String mismatchedName)
    {
        StringBuilder msg = new StringBuilder();
        if (sampleLabel != null)
            msg.append("Sample ").append(sampleLabel).append(": ");

        if (detail != null)
            msg.append(detail).append(" ");

        msg.append("Existing ");
        if (existingNames.size() > 1)
            msg.append(type.name()).append("s");
        else
            msg.append(type.name());

        msg.append(" with different casing from the requested name '").append(mismatchedName).append("'");
        msg.append(":\n");

        String sep = "";
        for (FlowManager.FlowEntry other : existingNames)
        {
            msg.append(sep).append(other._name).append(" (id=").append(other._rowId).append(", aliasId=").append(other._aliasId).append(")");
            sep = ", ";
        }

        return msg.toString();
    }
}
