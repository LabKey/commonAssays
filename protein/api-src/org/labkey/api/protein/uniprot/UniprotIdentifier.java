/*
 * Copyright (c) 2007-2008 LabKey Corporation
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

package org.labkey.api.protein.uniprot;

/**
 * User: jeckels
 * Date: Nov 30, 2007
 */
public class UniprotIdentifier
{
    private String _identType;
    private String _identifier;
    private UniprotSequence _sequence;

    public UniprotIdentifier(String identType, String identifier, UniprotSequence sequence)
    {
        _identType = identType;
        _identifier = identifier;
        _sequence = sequence;
    }

    public String getIdentType()
    {
        return _identType;
    }

    public String getIdentifier()
    {
        return _identifier;
    }

    public UniprotSequence getSequence()
    {
        return _sequence;
    }
}
