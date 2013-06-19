/*
 * Copyright (c) 2007-2013 LabKey Corporation
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

package org.labkey.ms2.protein;

import org.labkey.ms2.protein.uniprot.*;

import java.util.*;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * User: jeckels
 * Date: Nov 30, 2007
 */
public class ParseContext
{
    private uniprot _uniprotRoot;
    private List<UniprotAnnotation> _annotations = new ArrayList<>();
    private List<UniprotIdentifier> _identifiers = new ArrayList<>();
    private Map<String, UniprotSequence> _sequences = new HashMap<>();
    private UniprotSequence _currentSequence;
    private UniprotOrganism _currentOrganism;
    private Map<String, UniprotOrganism> _organisms = new HashMap<>();
    private final Connection _conn;
    private boolean _clearExisting;

    public ParseContext(Connection conn, boolean clearExisting)
    {
        _conn = conn;
        _clearExisting = clearExisting;
    }

    public uniprot getUniprotRoot()
    {
        return _uniprotRoot;
    }

    public void setUniprotRoot(uniprot uniprotRoot)
    {
        _uniprotRoot = uniprotRoot;
    }

    public boolean isIgnorable()
    {
        return _uniprotRoot.getSkipEntries() > 0;
    }


    public UniprotSequence getCurrentSequence()
    {
        return _currentSequence;
    }

    public List<UniprotAnnotation> getAnnotations()
    {
        return _annotations;
    }

    public List<UniprotIdentifier> getIdentifiers()
    {
        return _identifiers;
    }

    public Collection<UniprotSequence> getSequences()
    {
        return _sequences.values();
    }

    public void setCurrentSequence(UniprotSequence currentSequence)
    {
        _currentSequence = currentSequence;
    }

    public void addCurrentSequence()
    {
        String uniqKey =
                _currentSequence.getGenus().toUpperCase() +
                        " " +
                        _currentSequence.getSpecies().toUpperCase() +
                        " " +
                        _currentSequence.getHash();
        _sequences.put(uniqKey, _currentSequence);
    }

    public void clear()
    {
        _identifiers.clear();
        _annotations.clear();
        _sequences.clear();
        _organisms.clear();
    }

    public void setCurrentOrganism(UniprotOrganism currentOrganism)
    {
        _currentOrganism = currentOrganism;
    }

    public UniprotOrganism getCurrentOrganism()
    {
        return _currentOrganism;
    }

    public void addCurrentOrganism()
    {
        String uniqKey =
                _currentOrganism.getGenus().toUpperCase() +
                        " " +
                        _currentOrganism.getSpecies().toUpperCase();
        _organisms.put(uniqKey, _currentOrganism);
    }

    public Collection<UniprotOrganism> getOrganisms()
    {
        return _organisms.values();
    }

    public boolean unBumpSkip()
    {
        return _uniprotRoot.unBumpSkip();
    }

    public void insert() throws SQLException
    {
        _uniprotRoot.insertTables(this, _conn);
    }

    public Connection getConnection()
    {
        return _conn;
    }

    public boolean isClearExisting()
    {
        return _clearExisting;
    }

    public void setClearExisting(boolean clearExisting)
    {
        _clearExisting = clearExisting;
    }
}
