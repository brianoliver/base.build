package build.base.naming;

/*-
 * #%L
 * base.build Naming
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

import build.base.foundation.Primes;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An {@link Iterator} that randomly generates unique and descriptive names consisting of an adjective drawn from the
 * {@link Adjectives}, a first name drawn from the {@link FirstNames}s, and a last name drawn from the
 * {@link LastNames}, by default, each separated by a white space, for example "Vibrant Salva Maiorano".
 *
 * @author brian.oliver
 * @see Adjectives
 * @see FirstNames
 * @see LastNames
 * @since Aug-2021
 */
public class UniqueNameGenerator
    implements Iterator<String> {

    /**
     * The maximum number of names we can generate.
     */
    private final int maximumCombinations;

    /**
     * The separator to use between adjectives and a names.
     */
    private final String separator;

    /**
     * The maximum {@link Adjectives} index we can use for choosing an adjective.
     */
    private final int maximumAdjectiveIndex;

    /**
     * The random prime to step through {@link Adjectives}.
     */
    private final int adjectivePrimeStep;

    /**
     * The index of the next adjective to choose from the {@link Adjectives}.
     */
    private int nextAdjectiveIndex;

    /**
     * The maximum {@link FirstNames} index we can use for choosing a first name.
     */
    private final int maximumFirstNameIndex;

    /**
     * The random prime to step through {@link FirstNames}.
     */
    private final int firstNamePrimeStep;

    /**
     * The index of the next name to choose from the {@link FirstNames}.
     */
    private int nextFirstNameIndex;

    /**
     * The maximum {@link LastNames} index we can use for choosing a last name.
     */
    private final int maximumLastNameIndex;

    /**
     * The random prime to step through {@link LastNames}.
     */
    private final int lastNamePrimeStep;

    /**
     * The index of the next last name to choose from the {@link LastNames}.
     */
    private int nextLastNameIndex;

    /**
     * The number of currently generated unique names.
     */
    private int generated;

    /**
     * Constructs a {@link UniqueNameGenerator}.
     *
     * @param separator the separator between adjectives and names
     */
    public UniqueNameGenerator(final String separator) {
        this.maximumAdjectiveIndex = Primes.before(Adjectives.count());
        this.adjectivePrimeStep = Primes.randomAfter(this.maximumAdjectiveIndex);
        this.nextAdjectiveIndex = this.adjectivePrimeStep % this.maximumAdjectiveIndex;

        this.maximumFirstNameIndex = Primes.before(FirstNames.count());
        this.firstNamePrimeStep = Primes.randomAfter(this.maximumFirstNameIndex);
        this.nextFirstNameIndex = this.firstNamePrimeStep % this.maximumFirstNameIndex;

        this.maximumLastNameIndex = Primes.before(LastNames.count());
        this.lastNamePrimeStep = Primes.randomAfter(this.maximumLastNameIndex);
        this.nextLastNameIndex = this.lastNamePrimeStep % this.maximumLastNameIndex;

        this.maximumCombinations = this.maximumAdjectiveIndex * this.maximumFirstNameIndex * this.maximumLastNameIndex;
        this.generated = 0;

        this.separator = separator;
    }

    /**
     * Constructs a {@link UniqueNameGenerator} using a single white-space as a separator.
     */
    public UniqueNameGenerator() {
        this(" ");
    }

    /**
     * Obtains the number of combinations of unique names that can be generated.
     *
     * @return the number of unique names that can be generated
     */
    public long size() {
        return this.maximumCombinations;
    }

    @Override
    public boolean hasNext() {
        return this.generated < this.maximumCombinations;
    }

    @Override
    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException("Exhausted all possible names for generation");
        }

        final String unique = Adjectives.get(this.nextAdjectiveIndex)
            + this.separator
            + FirstNames.get(this.nextFirstNameIndex)
            + this.separator
            + LastNames.get(this.nextLastNameIndex);

        this.nextAdjectiveIndex = (this.nextAdjectiveIndex + this.adjectivePrimeStep) % this.maximumAdjectiveIndex;
        this.nextFirstNameIndex = (this.nextFirstNameIndex + this.firstNamePrimeStep) % this.maximumFirstNameIndex;
        this.nextLastNameIndex = (this.nextLastNameIndex + this.lastNamePrimeStep) % this.maximumLastNameIndex;
        this.generated++;

        return unique;
    }
}
