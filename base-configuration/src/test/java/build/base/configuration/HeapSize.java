package build.base.configuration;

/*-
 * #%L
 * base.build Configuration
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

import java.util.Optional;

/**
 * A representation of heapsize.
 *
 * @author brian.oliver
 * @since Nov-2017
 */
public class HeapSize
    implements ComposedOption<HeapSize> {

    /**
     * The minimum {@link HeapSize}.
     */
    private final Optional<Integer> minimum;

    /**
     * The maximum {@link HeapSize}.
     */
    private final Optional<Integer> maximum;

    /**
     * Constructs a {@link HeapSize} with {@link Optional} minimum and maximums.
     *
     * @param minimum the minimum
     * @param maximum the maximum
     */
    HeapSize(final Optional<Integer> minimum, final Optional<Integer> maximum) {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    /**
     * Creates a {@link HeapSize} with a specified minimum size.
     *
     * @param size the minimum size
     * @return a new {@link HeapSize}
     */
    public static HeapSize minimum(final int size) {
        return new HeapSize(Optional.of(size), Optional.empty());
    }

    /**
     * Creates a {@link HeapSize} with a specified maximum size.
     *
     * @param size the maximum size
     * @return a new {@link HeapSize}
     */
    public static HeapSize maximum(final int size) {
        return new HeapSize(Optional.empty(), Optional.of(size));
    }

    /**
     * Creates a {@link HeapSize} with a specified minimum and maximum size.
     *
     * @param minimum the minimum size
     * @param maximum the maximum size
     * @return a new {@link HeapSize}
     */
    public static HeapSize of(final int minimum, final int maximum) {
        return new HeapSize(Optional.of(minimum), Optional.of(maximum));
    }

    /**
     * Creates a default {@link HeapSize}.
     *
     * @return a new default {@link HeapSize}
     */
    @Default
    public static HeapSize automatic() {
        return new HeapSize(Optional.empty(), Optional.of(1000));
    }

    /**
     * Obtain the {@link Optional} minimum {@link HeapSize}.
     *
     * @return the {@link Optional} minimum
     */
    public Optional<Integer> getMinimum() {
        return this.minimum;
    }

    /**
     * Obtain the {@link Optional} maximum {@link HeapSize}.
     *
     * @return the {@link Optional} maximum
     */
    public Optional<Integer> getMaximum() {
        return this.maximum;
    }

    @Override
    public HeapSize compose(final HeapSize other) {

        return new HeapSize(other.minimum.isPresent() ? other.minimum : this.minimum,
            other.maximum.isPresent() ? other.maximum : this.maximum);
    }
}
