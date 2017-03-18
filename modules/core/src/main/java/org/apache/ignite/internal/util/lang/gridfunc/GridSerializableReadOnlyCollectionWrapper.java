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

package org.apache.ignite.internal.util.lang.gridfunc;

import java.util.Collection;
import java.util.Iterator;
import org.apache.ignite.internal.util.GridSerializableCollection;
import org.apache.ignite.internal.util.GridSerializableIterator;
import org.apache.ignite.internal.util.lang.GridFunc;
import org.apache.ignite.internal.util.typedef.internal.S;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Collection wrapper.
 * A read-only view will be created over the element and given
 * collections and no copying will happen.
 *
 * @param <T> Element type.
 */
public class GridSerializableReadOnlyCollectionWrapper<T> extends GridSerializableCollection<T> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Collection. */
    private final Collection<T> collection;

    /** First element in the collection. */
    private final T firstElement;

    /**
     * @param collection Collection to wrap.
     * @param firstElement First element.
     */
    public GridSerializableReadOnlyCollectionWrapper(@NotNull Collection<T> collection, @NotNull T firstElement) {
        this.collection = collection;
        this.firstElement = firstElement;
    }

    /** {@inheritDoc} */
    @NotNull
    @Override public Iterator<T> iterator() {
        return new GridSerializableIterator<T>() {
            private Iterator<T> it;

            @Override public boolean hasNext() {
                return it == null || it.hasNext();
            }

            @Nullable @Override public T next() {
                if (it == null) {
                    it = collection.iterator();

                    return firstElement;
                }

                return it.next();
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return collection.size() + 1;
    }

    /** {@inheritDoc} */
    @Override public boolean equals(Object obj) {
        return obj instanceof Collection && GridFunc.eqNotOrdered(this, (Collection)obj);
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridSerializableReadOnlyCollectionWrapper.class, this);
    }
}
