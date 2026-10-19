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

import build.base.foundation.Introspection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * An {@link Option} type that will automatically be collected into a specified collection type {@code C}
 * thus allowing multiple instances of the same type to be managed by an {@link Configuration}.
 *
 * @param <C> the type of {@link Collection} into which the {@link CollectedOption} is to be managed
 * @author brian.oliver
 * @since Nov-2017
 */
public interface CollectedOption<C extends Collection>
    extends Option {

    /**
     * Creates a new {@link Collection} into which {@link CollectedOption} {@link Option}s of this
     * type will be collected and managed.
     *
     * @return a new {@link Collection}
     */
    @SuppressWarnings("unchecked")
    default C createCollection() {
        try {
            // determine the collection class based on the generic parameter of the collected class
            Class<C> collectionClass = (Class<C>) Introspection.getAll(getClass(), Class::getGenericInterfaces)
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(type -> type.getTypeName().contains(CollectedOption.class.getName()))
                .findFirst()
                .orElseThrow(
                    () -> new RuntimeException("Failed to create a collection for " + getClass().getName()))
                .getActualTypeArguments()[0];

            if (!Collection.class.isAssignableFrom(collectionClass)) {
                throw new IllegalArgumentException(
                    "The class " + collectionClass.getName() + " does not implement the Collection interface");
            }

            // determine a concrete class for commonly specified interface classes
            if (List.class.equals(collectionClass)) {
                collectionClass = (Class<C>) ArrayList.class;
            }
            else if (Set.class.equals(collectionClass)) {
                collectionClass = (Class<C>) HashSet.class;
            }
            else if (SortedSet.class.equals(collectionClass)) {
                collectionClass = (Class<C>) TreeSet.class;
            }
            else if (collectionClass.isInterface()) {
                throw new RuntimeException("Can't instantiate a collection for the Collected Option "
                    + getClass().getName()
                    + " as the specified type of collection is not concrete (it's an interface)."
                    + " Either specify a concrete class or use a supported built-in interface like, List, Set or SortedSet");
            }

            return collectionClass.getDeclaredConstructor().newInstance();
        }
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException |
               InvocationTargetException e) {
            throw new RuntimeException("Failed to create a collection for " + getClass().getName(), e);
        }
    }
}
