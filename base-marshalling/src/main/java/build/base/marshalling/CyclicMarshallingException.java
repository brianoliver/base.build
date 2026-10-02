package build.base.marshalling;

/*-
 * #%L
 * base.build Marshalling
 * %%
 * Copyright (C) 2026 Workday Inc
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

/**
 * Thrown when {@link Marshaller#marshal(Object)} discovers that the {@link Object} currently being marshalled
 * is its own ancestor — that is, marshalling it required (synchronously, on the current call stack) marshalling
 * it again.
 * <p>
 * Such a cycle cannot be represented by simply returning a partially-built {@link Marshalled}, because none
 * exists yet for the ancestor — it is still being constructed. This is distinct from a {@link Marshalled} that
 * is merely <i>shared</i> (reached more than once, but not from itself): that case is resolved automatically,
 * by {@link Marshalled#reference()}, and never reaches this exception.
 * <p>
 * A destructor that marshals its own {@link Object} graph <i>lazily</i> (for example, via a
 * {@code Stream<Marshalled<X>>} produced with {@code Stream.map}) does not trigger this exception even if the
 * graph is genuinely cyclic — the cycle is instead resolved, once discovered, via {@link Marshalled#reference()}.
 * This exception is only raised for a cycle discovered synchronously, before any {@link Marshalled} exists for
 * the ancestor to be referenced by.
 * <p>
 * <b>To fix:</b> change the destructor(s) involved in the cycle to expose the relevant reference(s) lazily
 * instead of calling {@code marshaller.marshal(...)} directly - for example, destruct a collection of related
 * {@link Object}s as a {@code Stream<Marshalled<X>>} built with {@code Stream.map(marshaller::marshal)}, rather
 * than eagerly marshalling each one. That defers the recursive {@code marshal()} call until whatever is
 * actually walking the {@link Marshalled} does so (typically a {@code Transport}, during encoding) - by which
 * point a real {@link Marshalled} already exists for the ancestor, so the cycle resolves automatically via
 * {@link Marshalled#reference()} instead of throwing.
 *
 * @author Reed von Redwitz
 * @since Sep-2026
 */
public class CyclicMarshallingException
    extends RuntimeException {

    /**
     * Constructs a {@link CyclicMarshallingException} for the specified {@link Object}.
     *
     * @param object the {@link Object} that is its own ancestor
     */
    public CyclicMarshallingException(final Object object) {
        super("Cyclic reference detected while marshalling [" + object + "] - its own destructor, synchronously "
            + "(directly or transitively), required marshalling it again, before any Marshalled existed yet for "
            + "it to be referenced by. To fix: change the destructor(s) involved to expose the cyclic "
            + "relationship lazily instead - for example, marshal a Stream<Marshalled<X>> via Stream.map(marshaller::marshal) "
            + "rather than calling marshaller.marshal(...) directly - so the cycle is only discovered once a real "
            + "Marshalled already exists for it to reference back to, at which point it is resolved automatically "
            + "instead of throwing.");
    }
}
