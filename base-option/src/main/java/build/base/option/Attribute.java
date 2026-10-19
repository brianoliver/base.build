package build.base.option;

/*-
 * #%L
 * base.build Option
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

import build.base.configuration.MappedOption;
import build.base.configuration.Option;

import java.util.Objects;

/**
 * A {@link MappedOption} {@link Option} representing a non-null key-value pair.
 *
 * @author brian.oliver
 */
public class Attribute
    implements MappedOption<String> {

    /**
     * The key for the {@link Attribute}.
     */
    private final String key;

    /**
     * The value of the {@link Attribute}.
     */
    private final String value;

    /**
     * Constructs an {@link Attribute} with a specific non-null key and non-null value.
     *
     * @param key   the key
     * @param value the value
     */
    protected Attribute(final String key, final String value) {
        this.key = Objects.requireNonNull(key, "Attribute key may not be null");
        this.value = Objects.requireNonNull(value, "Attribute value may not be null");
    }

    /**
     * Create an {@link Attribute} with a specific non-null key and non-null value.
     *
     * @param name  the key for the {@link Attribute}
     * @param value the value for the {@link Attribute}
     * @return the new {@link Attribute}
     */
    public static Attribute of(final String name, final String value) {
        return new Attribute(name, value);
    }

    /**
     * Obtains the value of the {@link Attribute}.
     *
     * @return the value
     */
    public String value() {
        return this.value;
    }

    @Override
    public String key() {
        return this.key;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final Attribute attribute = (Attribute) other;

        if (!this.key.equals(attribute.key)) {
            return false;
        }
        else {
            return this.value.equals(attribute.value);
        }
    }

    @Override
    public int hashCode() {
        int result = this.key.hashCode();
        result = 31 * result + this.value.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return this.key + "=" + this.value;
    }
}
