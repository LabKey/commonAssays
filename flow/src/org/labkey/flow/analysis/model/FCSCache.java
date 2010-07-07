/*
 * Copyright (c) 2005-2009 LabKey Corporation
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

import org.labkey.api.cache.CacheManager;
import org.labkey.api.cache.CacheMap;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class FCSCache
{
    FCSHeaderCacheMap _fcsHeaderCache = new FCSHeaderCacheMap();
    FCSCacheMap _fcsCache = new FCSCacheMap();

    abstract static class AbstractCacheMap<K, V>
    {
        private Object READING_LOCK = new Object();
        CacheMap<K, V> _map;

        public AbstractCacheMap(int limit, String debugName)
        {
            _map = CacheManager.getTTLCacheMap(limit, debugName);
        }

        abstract protected V loadObject(K key) throws IOException;

        public V get(K key) throws IOException
        {
            V ret = _map.get(key);
            if (ret != null)
                return ret;

            synchronized(READING_LOCK)
            {
                ret = _map.get(key);
                if (ret != null)
                    return ret;
                ret = loadObject(key);
                if (ret != null)
                    _map.put(key, ret);
            }

            return ret;
        }
    }

    private static class FCSHeaderCacheMap extends AbstractCacheMap<URI, FCSHeader>
    {
        private static final int CACHE_SIZE = 100;

        private FCSHeaderCacheMap()
        {
            super(CACHE_SIZE, "FCS header cache");
        }

        protected FCSHeader loadObject(URI uri) throws IOException
        {
            return new FCSHeader(new File(uri));
        }
    }

    private static class FCSCacheMap extends AbstractCacheMap<URI, FCS>
    {
        private static final int CACHE_SIZE = 20;

        private FCSCacheMap()
        {
            super(CACHE_SIZE, "FCS cache");
        }

        protected FCS loadObject(URI uri) throws IOException
        {
            return new FCS(new File(uri));
        }
    }

    public FCS readFCS(URI uri) throws IOException
    {
        return _fcsCache.get(uri);
    }

    public FCSHeader readFCSHeader(URI uri) throws IOException
    {
        return _fcsHeaderCache.get(uri);
    }
}
