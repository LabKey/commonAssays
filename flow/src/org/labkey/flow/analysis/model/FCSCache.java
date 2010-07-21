/*
 * Copyright (c) 2005-2010 LabKey Corporation
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

import org.labkey.api.cache.Cache;
import org.labkey.api.cache.CacheManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class FCSCache
{
    FCSHeaderCache _fcsHeaderCache = new FCSHeaderCache();
    FCSCacheMap _fcsCache = new FCSCacheMap();

    abstract static class AbstractCache<K, V>
    {
        private final Object READING_LOCK = new Object();
        private final Cache<K, V> _cache;

        public AbstractCache(int limit, String debugName)
        {
            _cache = CacheManager.getCache(limit, CacheManager.DAY, debugName);
        }

        abstract protected V loadObject(K key) throws IOException;

        public V get(K key) throws IOException
        {
            V ret = _cache.get(key);
            if (ret != null)
                return ret;

            synchronized(READING_LOCK)
            {
                ret = _cache.get(key);
                if (ret != null)
                    return ret;
                ret = loadObject(key);
                if (ret != null)
                    _cache.put(key, ret);
            }

            return ret;
        }
    }

    private static class FCSHeaderCache extends AbstractCache<URI, FCSHeader>
    {
        private static final int CACHE_SIZE = 100;

        private FCSHeaderCache()
        {
            super(CACHE_SIZE, "FCS header cache");
        }

        protected FCSHeader loadObject(URI uri) throws IOException
        {
            return new FCSHeader(new File(uri));
        }
    }

    private static class FCSCacheMap extends AbstractCache<URI, FCS>
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
