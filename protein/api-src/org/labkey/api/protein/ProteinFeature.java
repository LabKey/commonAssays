/*
 * Copyright (c) 2024-2026 LabKey Corporation
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
package org.labkey.api.protein;

public class ProteinFeature
{
    int startIndex;
    int endIndex;
    String type;
    String description;
    String original;
    String variation;

    public ProteinFeature() {}

    public ProteinFeature(int startIndex, int endIndex, String type, String original, String variation, String description)
    {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.type = type;
        this.original = original;
        this.variation = variation;
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getStartIndex()
    {
        return startIndex;
    }

    public void setStartIndex(int startIndex)
    {
        this.startIndex = startIndex;
    }

    public int getEndIndex()
    {
        return endIndex;
    }

    public void setEndIndex(int endIndex)
    {
        this.endIndex = endIndex;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getOriginal()
    {
        return original;
    }

    public void setOriginal(String original)
    {
        this.original = original;
    }

    public String getVariation()
    {
        return variation;
    }

    public void setVariation(String variation)
    {
        this.variation = variation;
    }
    public boolean isVariation() {
        return "sequence variant".equals(type);
    }
}
