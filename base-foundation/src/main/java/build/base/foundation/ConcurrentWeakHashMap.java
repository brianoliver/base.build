package build.base.foundation;

/*-
 * #%L
 * base.build Foundation
 * %%
 * Copyright (C) 2026 The base.build Authors and Contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A hash based {@link ConcurrentMap} which weakly references its keys.
 * <p>
 * As with {@link WeakHashMap} this map will eventually remove an entry once its key is no longer reachable.
 * <p>
 * This map does not support {@code null} keys or values.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author mark.falco
 * @since March-2022
 */
public class ConcurrentWeakHashMap<K, V>
    extends AbstractWeakHasherMap<K, V>
    implements ConcurrentMap<K, V> {

    /**
     * Atomic updater for {@link #gcLock}.
     */
    @SuppressWarnings("rawtypes")
    private static final AtomicIntegerFieldUpdater<ConcurrentWeakHashMap> GC_LOCK
        = AtomicIntegerFieldUpdater.newUpdater(ConcurrentWeakHashMap.class, "gcLock");

    /**
     * The frequency at which we should attempt a {@link #gc(boolean)} on writes.
     */
    private static final int WRITE_GC_FREQUENCY = Integer.highestOneBit(Runtime.getRuntime().availableProcessors() << 1);

    /**
     * The frequency at which we should attempt a {@link #gc(boolean)} on reads.
     */
    private static final int READ_GC_FREQUENCY = WRITE_GC_FREQUENCY << 2;

    /**
     * A "try-lock" for ensuring we don't block to *become* the victim "gc" thread.
     */
    private volatile int gcLock;

    /**
     * Construct a {@link ConcurrentWeakHashMap}.
     */
    public ConcurrentWeakHashMap() {
        this(ObjectHasher.instance());
    }

    /**
     * Construct a {@link ConcurrentWeakHashMap} with a custom {@link Hasher}.
     *
     * @param hasher the hasher.
     */
    protected ConcurrentWeakHashMap(final Hasher<K> hasher) {
        super(new ConcurrentHashMap<>(), hasher);
    }

    @Override
    protected void gc(final boolean preWrite) {
        // for most operations we can just do periodic GCs
        if (ThreadLocalRandom.current().nextInt(preWrite ? WRITE_GC_FREQUENCY : READ_GC_FREQUENCY) == 0) {
            super.gc(preWrite);
        }
    }

    @Override
    protected void gc() {
        // We're a concurrent map and can expect concurrent calls here we want to try to avoid needless contention.
        // We use a try-lock to allow other threads to progress when a victim thread is performing "gc".
        if (this.gcLock == 0 && GC_LOCK.weakCompareAndSet(this, 0, 1)) {
            try {
                super.gc();
            } finally {
                this.gcLock = 0; // unlock
            }
        }
    }
}
