package build.base.marshalling.tutorial;

/*-
 * #%L
 * base.build Marshalling
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

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;
import build.base.marshalling.example.Expense;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * A part of an {@link Expense}.
 *
 * @author brian.oliver
 * @since Jan-2025
 */
public class ExpenseLine {

    private final String description;
    private final double amount;

    @Unmarshal
    public ExpenseLine(final String description,
                       final double amount) {

        this.description = description;
        this.amount = amount;
    }

    @Marshal
    public void destructor(final Out<String> description,
                           final Out<Double> amount) {

        description.set(this.description);
        amount.set(this.amount);
    }

    public String description() {
        return this.description;
    }

    public double amount() {
        return this.amount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final ExpenseLine that)) {
            return false;
        }
        return Double.compare(this.amount, that.amount) == 0
            && Objects.equals(this.description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.description, this.amount);
    }

    static {
        // self-register this type when it's initialized
        Marshalling.register(ExpenseLine.class, MethodHandles.lookup());
    }
}
