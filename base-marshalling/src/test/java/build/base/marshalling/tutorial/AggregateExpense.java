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

import build.base.foundation.stream.Streams;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An expense consists of zero or more {@link ExpenseLine}s.
 *
 * @author brian.oliver
 * @since Jan-2025
 */
public class AggregateExpense {

    private final ArrayList<AggregateExpenseLine> expenseLines;

    public AggregateExpense() {
        this.expenseLines = new ArrayList<>();
    }

    @Unmarshal
    public AggregateExpense(
        final Marshaller marshaller,
        final Stream<Marshalled<AggregateExpenseLine>> expenseLines) {

        this();

        // allow "this" AggregatedExpense to be used for unmarshalling its AggregatedExpenseLines
        marshaller.bind(this).universally();

        expenseLines.forEach(marshalled -> add(marshaller.unmarshal(marshalled)));
    }

    @Marshal
    public void destructor(final Marshaller marshaller,
                           final Out<Stream<Marshalled<AggregateExpenseLine>>> expenseLines) {

        expenseLines.set(this.expenseLines.stream()
            .map(marshaller::marshal));
    }

    public void add(final AggregateExpenseLine expenseLine) {
        if (expenseLine != null) {
            if (expenseLine.expense() != this) {
                throw new IllegalArgumentException("Attempted to add an ExpenseLine from a different Expense");
            }

            this.expenseLines.add(expenseLine);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final AggregateExpense that)) {
            return false;
        }
        return Streams.equals(this.expenseLines.stream(), that.expenseLines.stream());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.expenseLines);
    }

    static {
        // self-register this type when it's initialized
        Marshalling.register(AggregateExpense.class, MethodHandles.lookup());
    }
}
