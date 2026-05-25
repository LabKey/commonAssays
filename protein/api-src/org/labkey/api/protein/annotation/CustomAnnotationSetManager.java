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
package org.labkey.api.protein.annotation;

import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SQLFragment;
import org.labkey.api.data.SqlExecutor;
import org.labkey.api.data.SqlSelector;
import org.labkey.api.data.Table;
import org.labkey.api.exp.DomainNotFoundException;
import org.labkey.api.exp.OntologyManager;
import org.labkey.api.protein.ProteinSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomAnnotationSetManager
{
    public static Map<String, CustomAnnotationSet> getCustomAnnotationSets(Container container, boolean includeProject)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(ProteinSchema.getTableInfoCustomAnnotationSet());
        sql.append(" WHERE Container = ? ");
        sql.add(container.getId());
        if (includeProject)
        {
            Container project = container.getProject();
            if (project != null && !project.equals(container))
            {
                sql.append(" OR Container = ? ");
                sql.add(project.getId());
            }
        }
        sql.append(" ORDER BY Name");
        Collection<CustomAnnotationSet> allSets = new SqlSelector(ProteinSchema.getSchema(), sql).getCollection(CustomAnnotationSet.class);

        Set<String> setNames = new CaseInsensitiveHashSet();
        List<CustomAnnotationSet> dedupedSets = new ArrayList<>(allSets.size());
        // If there are any name collisions, we want sets in this container to mask the ones in the project

        // Take a first pass through to add all the ones from this container
        for (CustomAnnotationSet set : allSets)
        {
            if (set.getContainer().equals(container.getId()))
            {
                setNames.add(set.getName());
                dedupedSets.add(set);
            }
        }

        // Take a second pass through to add all the ones from the project that don't collide
        for (CustomAnnotationSet set : allSets)
        {
            if (!set.getContainer().equals(container.getId()) && setNames.add(set.getName()))
            {
                dedupedSets.add(set);
            }
        }

        dedupedSets.sort(Comparator.comparing(CustomAnnotationSet::getName));
        Map<String, CustomAnnotationSet> result = new LinkedHashMap<>();
        for (CustomAnnotationSet set : dedupedSets)
        {
            result.put(set.getName(), set);
        }
        return result;
    }

    public static void deleteCustomAnnotationSet(CustomAnnotationSet set)
    {
        try
        {
            Container c = ContainerManager.getForId(set.getContainer());
            if (OntologyManager.getDomainDescriptor(set.getLsid(), c) != null)
            {
                OntologyManager.deleteOntologyObject(set.getLsid(), c, true);
                OntologyManager.deleteDomain(set.getLsid(), c);
            }
        }
        catch (DomainNotFoundException e)
        {
            throw new RuntimeException(e);
        }

        try (DbScope.Transaction transaction = ProteinSchema.getSchema().getScope().ensureTransaction())
        {
            new SqlExecutor(ProteinSchema.getSchema()).execute("DELETE FROM " + ProteinSchema.getTableInfoCustomAnnotation() + " WHERE CustomAnnotationSetId = ?", set.getCustomAnnotationSetId());
            Table.delete(ProteinSchema.getTableInfoCustomAnnotationSet(), set.getCustomAnnotationSetId());
            transaction.commit();
        }
    }

    public static CustomAnnotationSet getCustomAnnotationSet(Container c, long id, boolean includeProject)
    {
        SQLFragment sql = new SQLFragment();
        sql.append("SELECT * FROM ");
        sql.append(ProteinSchema.getTableInfoCustomAnnotationSet());
        sql.append(" WHERE (Container = ?");
        sql.add(c.getId());
        if (includeProject)
        {
            sql.append(" OR Container = ?");
            sql.add(c.getProject().getId());
        }
        sql.append(") AND CustomAnnotationSetId = ?");
        sql.add(id);
        List<CustomAnnotationSet> matches = new SqlSelector(ProteinSchema.getSchema(), sql).getArrayList(CustomAnnotationSet.class);
        if (matches.size() > 1)
        {
            for (CustomAnnotationSet set : matches)
            {
                if (set.getContainer().equals(c.getId()))
                {
                    return set;
                }
            }
            assert false : "More than one matching set was found but none were in the current container";
            return matches.getFirst();
        }
        if (matches.size() == 1)
        {
            return matches.getFirst();
        }
        return null;
    }
}
