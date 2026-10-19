package build.base.expression.compat;

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

import jakarta.el.ELContext;
import jakarta.el.ELResolver;

import java.util.HashMap;
import java.util.Map;

/**
 * An {@link ELResolver} for {@link Processor}s allowing the resolution of chained methods and properties without
 * requiring backing beans.
 *
 * @author graeme.campbell
 * @since Jan-2019
 */
class DefaultResolver
    extends ELResolver {

    private final Map<String, Object> storage = new HashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public Object getValue(final ELContext context, final Object base, final Object property) {

        final Object result;
        if (base instanceof PropertyResolver && property instanceof String) {
            result = ((PropertyResolver<Object>) base).apply((String) property);
        }
        else if (property instanceof String && base == null) {
            result = this.storage.get(property);
        }
        else if (property instanceof String && base instanceof String) {
            result = this.storage.get(base + "." + property);
        }
        else {
            return null;
        }
        context.setPropertyResolved(true);
        return result;
    }

    @Override
    public Class<?> getType(final ELContext context, final Object base, final Object property) {

        if (base instanceof String && property instanceof String) {
            context.setPropertyResolved(true);
        }
        return Object.class;
    }

    @Override
    public void setValue(final ELContext context, final Object base, final Object property, final Object value) {

        if (property instanceof String && base == null) {
            this.storage.put((String) property, value);
            context.setPropertyResolved(true);
        }
        else if (property instanceof String && base instanceof String) {
            this.storage.put(base + "." + property, value);
            context.setPropertyResolved(true);
        }
    }

    @Override
    public boolean isReadOnly(final ELContext context, final Object base, final Object property) {
        return false;
    }

    @Override
    public Class<?> getCommonPropertyType(final ELContext context, final Object base) {
        return null;
    }
}
