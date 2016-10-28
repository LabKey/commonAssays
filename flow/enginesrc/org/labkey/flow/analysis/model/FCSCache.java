/*
 * Copyright (c) 2005-2016 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.cache.BlockingCache;
import org.labkey.api.cache.CacheLoader;
import org.labkey.api.cache.CacheManager;
import org.labkey.api.cache.Wrapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;

public class FCSCache
{
    FCSHeaderCache _fcsHeaderCache = new FCSHeaderCache();
    FCSCacheMap _fcsCache = new FCSCacheMap();


    private static class FCSHeaderCache
            extends BlockingCache<URI, FCSHeader> implements CacheLoader<URI, FCSHeader>
    {
        private static final int CACHE_SIZE = 100;

        private FCSHeaderCache()
        {
            super(CacheManager.getCache(CACHE_SIZE, CacheManager.DAY, "FCS header cache"), new CacheLoader<URI, FCSHeader>(){
                @Override
                public FCSHeader load(URI uri, Object argument)
                {
                    // should CacheLoader.load() declare throw Exception?
                    try
                    {
                        return new FCSHeader(new File(uri));
                    }
                    catch (IOException x)
                    {
                        throw new RuntimeException(x);
                    }
                }
            });
        }


        public FCSHeader load(URI uri, Object argument)
        {
            // should CacheLoader.load() declare throw Exception?
            try
            {
                return new FCSHeader(new File(uri));
            }
            catch (IOException x)
            {
                throw new RuntimeException(x);
            }
        }
    }


    private static class FCSCacheMap extends BlockingCache<URI,FCS>
    {
        private static final int CACHE_SIZE = 20;

        private FCSCacheMap()
        {
            super(CacheManager.getCache(CACHE_SIZE, CacheManager.DAY, "FCS cache"), new CacheLoader<URI, FCS>(){
                @Override
                public FCS load(URI uri, Object argument)
                {
                    // should CacheLoader.load() declare throw Exception?
                    try
                    {
                        return new FCS(new File(uri));
                    }
                    catch (IOException x)
                    {
                        throw new RuntimeException(x);
                    }
                }
            });
        }
    }


    public FCS readFCS(URI uri) throws IOException
    {
        try
        {
            return _fcsCache.get(uri);
        }
        catch (RuntimeException x)
        {
            if (x.getCause() instanceof IOException)
                throw (IOException)x.getCause();
            throw x;
        }
    }


    public FCSHeader readFCSHeader(URI uri) throws IOException
    {
        try
        {
            return _fcsHeaderCache.get(uri);
        }
        catch (RuntimeException x)
        {
            if (x.getCause() instanceof IOException)
                throw (IOException)x.getCause();
            throw x;
        }
    }

    public void clear(@Nullable URI uri)
    {
        if (uri == null)
        {
            _fcsCache.clear();
            _fcsHeaderCache.clear();
        }
        else
        {
            _fcsCache.remove(uri);
            _fcsHeaderCache.remove(uri);
        }
    }
}
