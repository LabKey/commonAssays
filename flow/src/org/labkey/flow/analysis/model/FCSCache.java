package org.labkey.flow.analysis.model;

import org.labkey.api.util.TTLCacheMap;

import java.net.URI;
import java.io.File;
import java.io.IOException;

public class FCSCache
    {
    FCSHeaderCacheMap _fcsHeaderCache = new FCSHeaderCacheMap();
    FCSCacheMap _fcsCache = new FCSCacheMap();

    abstract static class AbstractCacheMap<K,V>
        {
        private Object READING_LOCK = new Object();
        TTLCacheMap _map;
        public AbstractCacheMap(int size)
            {
            _map = new TTLCacheMap(size);
            }
        abstract protected V loadObject(K key) throws IOException;

        public V get(K key) throws IOException
            {
            V ret = (V) _map.get(key);
            if (ret != null)
                return ret;
            synchronized(READING_LOCK)
                {
                ret = (V) _map.get(key);
                if (ret != null)
                    return ret;
                ret = loadObject(key);
                if (ret != null)
                    _map.put(key, ret);
                }
            return ret;
            }
        }

    static class FCSHeaderCacheMap extends AbstractCacheMap<URI, FCSHeader>
        {
        static final int CACHE_SIZE = 100;
        public FCSHeaderCacheMap()
            {
            super(CACHE_SIZE);
            }

        public FCSHeader loadObject(URI uri) throws IOException
            {
            return new FCSHeader(new File(uri));
            }
        }

    static class FCSCacheMap extends AbstractCacheMap<URI, FCS>
        {
        static final int CACHE_SIZE = 5;
        public FCSCacheMap()
            {
            super(CACHE_SIZE);
            }

        public FCS loadObject(URI uri) throws IOException
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
