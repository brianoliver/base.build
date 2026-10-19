package build.base.foundation.iterator.matching;

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

import build.base.foundation.Capture;
import build.base.foundation.iterator.Iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

/**
 * The basis of a {@link Matcher} for a {@link build.base.foundation.iterator.matching.Repeated}.
 *
 * @param <T> the type of elements evaluate
 * @param <C> the type of elements that were matched
 */
abstract class AbstractRepeated<T, C>
    extends AbstractSequence<T>
    implements Repeated<T, C>,
    Collected<T, C>,
    CapturedCollected<T, C> {

    /**
     * The additional {@link Stage}s of processing to occur after matching zero or more elements
     */
    private final ArrayList<Stage<?, ?>> stages;

    /**
     * Constructs a {@link AbstractRepeated} pattern.
     *
     * @param matcher the {@link Matcher} being repeated
     */
    AbstractRepeated(final AbstractMatcher<T> matcher) {
        // the previous pattern for this pattern will be the one before
        // the one being repeated
        super(matcher.previous);

        // exclude the pattern to be repeated from the sequence.
        // it will be a nested with in the qualified pattern.
        matcher.previous = null;
        matcher.next = null;

        this.stages = new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Collectable<T, R> map(final Function<? super C, R> function) {
        if (function != null) {
            this.stages.add(new Stage<>(null, null, function, null, null, null));
        }

        return (Collectable<T, R>) this;
    }

    @Override
    public Collectable<T, C> filter(final Predicate<? super C> predicate) {
        if (predicate != null) {
            this.stages.add(new Stage<>(null, predicate, null, null, null, null));
        }

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A, R> Collected<T, R> collect(final Collector<C, A, R> collector) {
        if (collector != null) {
            this.stages.add(new Stage<>(null, null, null, collector, null, null));
        }
        return (Collected<T, R>) this;
    }

    @Override
    public CapturedCollected<T, C> capture(final Capture<? super C> capture) {
        if (capture != null) {
            this.stages.add(new Stage<C, C>(capture::clear, null, null, null, capture::set, null));
        }

        return this;
    }

    @Override
    public Sequence<T> match(final Predicate<? super C> predicate) {
        if (predicate != null) {
            this.stages.add(new Stage<>(null, null, null, null, null, predicate));
        }

        return this;
    }

    /**
     * Determines if the {@link AbstractRepeated} has any processing stages defined.
     *
     * @return {@code true} when there are one ore more stages defined, {@code false} otherwise
     */
    protected boolean hasStages() {
        return !this.stages.isEmpty();
    }

    /**
     * Initializes the {@link AbstractRepeated}
     */
    protected void initialize() {
        this.stages.stream()
            .filter(stage -> stage.initializer != null)
            .forEach(stage -> stage.initializer.run());
    }

    /**
     * Attempts to collect the elements provided by the specified {@link Iterator}, applying the configured
     * filters, mappings and collector.
     *
     * @param iterator the {@link Iterator}
     * @return {@code true} if the collection was successful, {@code false} otherwise
     */
    @SuppressWarnings("unchecked")
    protected boolean collect(final Iterator<T> iterator) {

        // initially we assume we're iterating over all of the matched elements as they are
        Iterator<Object> elements = (Iterator<Object>) iterator;

        // apply the filtering and mapping iterators in the order defined by the stages
        final Iterator<Stage<?, ?>> stages = this.stages.iterator();
        final Capture<Collector> collector = Capture.empty();

        while (stages.hasNext() && !collector.isPresent()) {
            final Stage stage = stages.next();

            if (stage.filter != null) {
                elements = Iterators.filter(elements, stage.filter);
            }

            if (stage.map != null) {
                elements = Iterators.map(elements, stage.map);
            }

            if (stage.collector != null) {
                collector.set(stage.collector);
            }
        }

        // apply the collector (there will be one)
        final Iterator<Object> collectables = elements;
        final Capture<Object> result = collector.map(c -> {
            final Object container = c.supplier().get();

            collectables.forEachRemaining(element -> {
                c.accumulator().accept(container, element);
            });

            return c.finisher().apply(container);
        });

        // apply captures and matching (where present)
        while (stages.hasNext() && result.isPresent()) {
            final Stage stage = stages.next();

            if (stage.consumer != null) {
                stage.consumer.accept(result.get());
            }

            if (stage.predicate != null && !stage.predicate.test(result.get())) {
                result.clear();
            }
        }

        // re-initialize stages when the match fails
        if (!result.isPresent()) {
            initialize();
        }

        return result.isPresent();
    }

    /**
     * Encapsulates information to process zero or more matched and/or collected elements.
     *
     * @param <T> the type of the matched and/or collected elements
     * @param <R> the type of the {@link Stage} result
     */
    protected static class Stage<T, R> {

        /**
         * A {@link Runnable} to initialize processing of the {@link Stage}.
         */
        final Runnable initializer;

        /**
         * A {@link Predicate} to filter elements from being processed in the {@link Stage}.
         */
        final Predicate<? super T> filter;

        /**
         * A {@link Function} to map elements being processed in the {@link Stage}.
         */
        final Function<? super T, R> map;

        /**
         * A {@link Consumer} of collected elements being processed in the {@link Stage}.
         */
        final Consumer<? super R> consumer;

        /**
         * A {@link Collector} of filtered elements being processed in the {@link Stage}.
         */
        final Collector<T, ?, R> collector;

        /**
         * A {@link Predicate} to be applied to the collected result of the {@link Stage}.
         */
        final Predicate<? super R> predicate;

        Stage(final Runnable initializer,
              final Predicate<? super T> filter,
              final Function<? super T, R> map,
              final Collector<T, ?, R> collector,
              final Consumer<? super R> consumer,
              final Predicate<? super R> predicate) {

            this.initializer = initializer;
            this.filter = filter;
            this.map = map;
            this.collector = collector;
            this.consumer = consumer;
            this.predicate = predicate;
        }
    }
}
