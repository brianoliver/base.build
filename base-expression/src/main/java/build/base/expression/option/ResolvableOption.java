package build.base.expression.option;

/*-
 * #%L
 * base.build Expression
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

import build.base.configuration.Option;
import build.base.expression.compat.Processor;

/**
 * An {@link Option} that is capable of being resolved into a new {@link Option} when using
 * {@link Processor#resolve(Option)}.
 *
 * @param <T> the type of {@link Option} to which this {@link Option} will be resolved
 * @author graeme.campbell
 * @since Jan-2019
 */
@FunctionalInterface
public interface ResolvableOption<T extends Option>
    extends Option {

    /**
     * Resolves this {@link ResolvableOption} to an {@link Option} of type {@code T} using the provided
     * {@link Processor}.
     *
     * @param processor the {@link Processor} with which to resolve this {@link ResolvableOption}
     * @return the resolved {@link Option} of type {@code T}
     */
    T resolve(Processor processor);
}
