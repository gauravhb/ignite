/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.cache.database.db.file;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheRebalanceMode;
import org.apache.ignite.cache.affinity.rendezvous.RendezvousAffinityFunction;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.BinaryConfiguration;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.MemoryConfiguration;
import org.apache.ignite.configuration.PersistenceConfiguration;
import org.apache.ignite.internal.IgniteEx;
import org.apache.ignite.internal.processors.cache.database.GridCacheDatabaseSharedManager;
import org.apache.ignite.internal.processors.cache.database.wal.FileWriteAheadLogManager;
import org.apache.ignite.internal.util.typedef.X;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.apache.ignite.internal.util.typedef.internal.U;
import org.apache.ignite.testframework.junits.common.GridCommonAbstractTest;

/**
 *
 */
public class IgniteNoActualWalHistorySelfTest extends GridCommonAbstractTest {
    /** {@inheritDoc} */
    @Override protected IgniteConfiguration getConfiguration(String gridName) throws Exception {
        IgniteConfiguration cfg = super.getConfiguration(gridName);

        CacheConfiguration<Integer, IndexedObject> ccfg = new CacheConfiguration<>();

        ccfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);
        ccfg.setRebalanceMode(CacheRebalanceMode.SYNC);
        ccfg.setAffinity(new RendezvousAffinityFunction(false, 32));

        cfg.setCacheConfiguration(ccfg);

        MemoryConfiguration dbCfg = new MemoryConfiguration();

        dbCfg.setPageSize(4 * 1024);

        cfg.setMemoryConfiguration(dbCfg);

        PersistenceConfiguration pCfg = new PersistenceConfiguration();

        pCfg.setWalSegmentSize(4 * 1024 * 1024);
        pCfg.setWalHistorySize(2);
        pCfg.setWalSegments(10);

        cfg.setPersistenceConfiguration(pCfg);

        cfg.setMarshaller(null);

        BinaryConfiguration binCfg = new BinaryConfiguration();

        binCfg.setCompactFooter(false);

        cfg.setBinaryConfiguration(binCfg);

        return cfg;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        stopAllGrids();

        deleteRecursively(U.resolveWorkDirectory(U.defaultWorkDirectory(), "db", false));

        System.setProperty(FileWriteAheadLogManager.IGNITE_PDS_WAL_MODE, "LOG_ONLY");
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        stopAllGrids();

        deleteRecursively(U.resolveWorkDirectory(U.defaultWorkDirectory(), "db", false));

        System.clearProperty(FileWriteAheadLogManager.IGNITE_PDS_WAL_MODE);
    }

    /**
     * @throws Exception if failed.
     */
    public void testWalBig() throws Exception {
        try {
            IgniteEx ignite = startGrid(1);

            IgniteCache<Object, Object> cache = ignite.cache(null);

            Random rnd = new Random();

            Map<Integer, IndexedObject> map = new HashMap<>();

            for (int i = 0; i < 40_000; i++) {
                if (i % 1000 == 0)
                    X.println(" >> " + i);

                int k = rnd.nextInt(300_000);
                IndexedObject v = new IndexedObject(rnd.nextInt(10_000));

                cache.put(k, v);
                map.put(k, v);
            }

            GridCacheDatabaseSharedManager dbMgr = (GridCacheDatabaseSharedManager)ignite.context().cache().context()
                .database();

            // Create many checkpoints to clean up the history.
            dbMgr.wakeupForCheckpoint("test").get();
            dbMgr.wakeupForCheckpoint("test").get();
            dbMgr.wakeupForCheckpoint("test").get();
            dbMgr.wakeupForCheckpoint("test").get();
            dbMgr.wakeupForCheckpoint("test").get();
            dbMgr.wakeupForCheckpoint("test").get();
            dbMgr.wakeupForCheckpoint("test").get();

            dbMgr.enableCheckpoints(false).get();

            for (int i = 0; i < 50; i++) {
                int k = rnd.nextInt(300_000);
                IndexedObject v = new IndexedObject(rnd.nextInt(10_000));

                cache.put(k, v);
                map.put(k, v);
            }

            stopGrid(1);

            ignite = startGrid(1);

            cache = ignite.cache(null);

            // Check.
            for (Integer k : map.keySet())
                assertEquals(map.get(k), cache.get(k));
        }
        finally {
            stopAllGrids();
        }
    }

    /**
     *
     */
    private static class IndexedObject {
        /** */
        @QuerySqlField(index = true)
        private int iVal;

        /** */
        private byte[] payload = new byte[1024];

        /**
         * @param iVal Integer value.
         */
        private IndexedObject(int iVal) {
            this.iVal = iVal;
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object o) {
            if (this == o)
                return true;

            if (!(o instanceof IndexedObject))
                return false;

            IndexedObject that = (IndexedObject)o;

            return iVal == that.iVal;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return iVal;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(IndexedObject.class, this);
        }
    }

    /**
     *
     */
    private enum EnumVal {
        /** */
        VAL1,

        /** */
        VAL2,

        /** */
        VAL3
    }
}