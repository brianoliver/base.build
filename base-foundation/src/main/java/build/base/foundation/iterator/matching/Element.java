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
import build.base.foundation.iterator.ResettableIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static build.base.foundation.predicate.Predicates.always;

/**
 * A {@link Matcher} to evaluate an element using a {@link Predicate}.
 *
 * @param <T> the type of elements evaluate
 * @param <C> the type of elements to capture
 */
class Element<T, C>
    extends AbstractComposition<T, C>
    implements Matched<T, C>, Capturable<T, C>, Captured<T, C> {

    /**
     * The {@link Predicate} for matching the {@link build.base.foundation.iterator.matching.Element}.
     */
    private final Predicate<? super T> predicate;

    /**
     * The additional {@link Stage}s of processing to occur after matching an element.
     */
    private final ArrayList<Stage<?, ?>> stages;

    Element(final AbstractMatcher<T> previous,
            final Predicate<? super T> predicate) {

        super(previous);
        this.predicate = predicate == null ? always() : predicate;
        this.stages = new ArrayList<>(0);
    }

    Element(final Predicate<? super T> predicate) {
        this(null, predicate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean match(final ResettableIterator<T> iterator) {

        if (iterator.hasNext()) {
            final T element = iterator.next();

            if (this.predicate.test(element)) {

                if (this.stages.isEmpty()) {
                    return true;
                }

                // upon successfully matching, we apply each of the stages in order
                // (starting with the matched value)
                Optional<Object> value = Optional.of(element);

                final Iterator<Stage<?, ?>> stages = this.stages.iterator();
                while (stages.hasNext() && value.isPresent()) {

                    final Stage stage = stages.next();

                    stage.initializer.run();

                    final Object mapped = stage.map.apply(value.get());

                    stage.consumer.accept(mapped);

                    value = stage.predicate.test(mapped)
                        ? Optional.of(mapped)
                        : Optional.empty();
                }

                if (value.isPresent()) {
                    return true;
                }
            }
        }

        // we a match did not occur, we must re-initialize each of the Stages
        this.stages.forEach(stage -> stage.initializer.run());

        return false;
    }

    @Override
    public String describe() {
        return this.predicate.equals(always())
            ? "matchesAny()"
            : "matches(" + this.predicate + ")";
    }

    @Override
    public Captured<T, C> capture(final Capture<? super C> capture) {
        if (capture != null) {
            this.stages.add(new Stage<C, C>(capture::clear, element -> element, capture::set, __ -> true));
        }

        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> Capturable<T, R> map(final Function<? super C, R> function) {
        if (function != null) {
            this.stages.add(new Stage<C, R>(() -> {
            }, function, __ -> {
            }, always()));
        }

        return (Capturable<T, R>) this;
    }

    @Override
    public Sequence<T> match(final Predicate<? super C> predicate) {
        if (predicate != null) {
            this.stages.add(new Stage<C, C>(() -> {
            }, element -> element, __ -> {
            }, predicate));
        }

        return this;
    }

    /**
     * Encapsulates information to process a matched element.
     *
     * @param <T> the type of the {@link IteratorPatternMatcher} element
     * @param <C> the type of the matched element
     */
    static class Stage<T, C> {

        final Runnable initializer;

        final Function<? super T, C> map;

        final Consumer<? super C> consumer;

        final Predicate<? super C> predicate;

        Stage(final Runnable initializer,
              final Function<? super T, C> map,
              final Consumer<? super C> consumer,
              final Predicate<? super C> predicate) {

            this.initializer = initializer;
            this.map = map;
            this.consumer = consumer;
            this.predicate = predicate;
        }
    }
}
