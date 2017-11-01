/*
 * Copyright (c) 2017 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.api.collections.CaseInsensitiveArrayListValuedMap;
import org.labkey.api.collections.CaseInsensitiveHashMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Collection of flow data indexed by unique id (possibly the sample id) and non-unique sample name.
 *
 * User: kevink
 * Date: 9/18/17
 */
public class SampleIdMap<V>
{
    public static <V1> SampleIdMap<V1> emptyMap()
    {
        return new SampleIdMap<V1>() {
            @Override
            public void put(String id, String name, Object value)
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Map<String, V> _idToDataMap;
    private Map<String, String> _idToNameMap;
    private CaseInsensitiveArrayListValuedMap<String> _nameToIdMap;

    public SampleIdMap()
    {
        _idToDataMap = new CaseInsensitiveHashMap<>(new LinkedHashMap<>());
        _idToNameMap = new CaseInsensitiveHashMap<>(new LinkedHashMap<>());
        _nameToIdMap = new CaseInsensitiveArrayListValuedMap<>();
    }

    public final V getById(String id)
    {
        return _idToDataMap.get(id);
    }

    public final List<V> getByName(String name)
    {
        Collection<String> ids = _nameToIdMap.get(name);
        ArrayList<V> datas = new ArrayList<>(ids.size());
        for (String id : ids)
            datas.add(getById(id));
        return Collections.unmodifiableList(datas);
    }

    public final boolean containsId(String id)
    {
        return _idToDataMap.containsKey(id);
    }

    public final boolean containsName(String name)
    {
        return _nameToIdMap.containsKey(name);
    }

    public final String getNameForId(String id)
    {
        return _idToNameMap.get(id);
    }

    public final List<String> getIdsForNames(String name)
    {
        return Collections.unmodifiableList(_nameToIdMap.get(name));
    }

    public final Set<String> idSet()
    {
        return Collections.unmodifiableSet(_idToDataMap.keySet());
    }

    public final Set<String> nameSet()
    {
        return Collections.unmodifiableSet(_nameToIdMap.keySet());
    }

    public final Collection<V> values()
    {
        return Collections.unmodifiableCollection(_idToDataMap.values());
    }

    public final int size()
    {
        return _idToDataMap.size();
    }

    public final boolean isEmpty()
    {
        return _idToDataMap.isEmpty();
    }


    public void put(@NotNull String id, @NotNull String name, @NotNull V value)
    {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);

        if (_idToDataMap.containsKey(id))
            throw new IllegalArgumentException("All IDs must be unique within the data collection: " + id);

        _idToDataMap.put(id, value);
        _idToNameMap.put(id, name);
        _nameToIdMap.put(name, id);
    }

    public final void put(@NotNull ISampleInfo info, @NotNull V value)
    {
        Objects.requireNonNull(info);
        Objects.requireNonNull(value);
        put(info.getSampleId(), info.getLabel(), value);
    }

    public final V computeIfAbsent(@NotNull String id, @NotNull String name, Function<String, V> mappingFunction)
    {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = getById(id)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(id)) != null) {
                put(id, name, newValue);
                return newValue;
            }
        }

        return v;
    }

}
