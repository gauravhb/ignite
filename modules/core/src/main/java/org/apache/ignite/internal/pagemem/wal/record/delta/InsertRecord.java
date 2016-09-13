/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.pagemem.wal.record.delta;

import java.nio.ByteBuffer;
import org.apache.ignite.IgniteCheckedException;
import org.apache.ignite.internal.pagemem.PageMemory;
import org.apache.ignite.internal.processors.cache.database.tree.io.BPlusIO;

/**
 * Insert into inner or leaf page.
 */
public class InsertRecord<L> extends PageDeltaRecord {
    /** */
    private int idx;

    /** */
    private L row;

    /** */
    private byte[] rowBytes;

    /** */
    private long rightId;

    /** */
    private BPlusIO<L> io;

    /**
     * @param cacheId Cache ID.
     * @param pageId Page ID.
     * @param io IO.
     * @param idx Index.
     * @param row Row.
     * @param rowBytes Row bytes.
     * @param rightId Right ID.
     */
    public InsertRecord(
        int cacheId,
        long pageId,
        BPlusIO<L> io,
        int idx,
        L row,
        byte[] rowBytes,
        long rightId
    ) {
        super(cacheId, pageId);

        this.io = io;
        this.idx = idx;
        this.row = row;
        this.rowBytes = rowBytes;
        this.rightId = rightId;
    }

    /** {@inheritDoc} */
    @Override public void applyDelta(PageMemory pageMem, ByteBuffer buf) throws IgniteCheckedException {
        io.insert(buf, idx, row, rowBytes, rightId);
    }

    /** {@inheritDoc} */
    @Override public RecordType type() {
        return RecordType.BTREE_PAGE_INSERT;
    }

    public BPlusIO<L> io() {
        return io;
    }

    public int index() {
        return idx;
    }

    public long rightId() {
        return rightId;
    }

    public L row() {
        return row;
    }
}
