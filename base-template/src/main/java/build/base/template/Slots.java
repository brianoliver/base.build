package build.base.template;

/*-
 * #%L
 * base.build Template
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

import java.util.HashMap;
import java.util.Map;

/**
 * The named parts of the body of an include, given to the layout that is included: a layout that declares a parameter
 * of this type renders each part at the slot of its name, and the content that is in no part at the slot without one.
 * <p>
 * A part that was not given renders nothing, which makes every slot optional.
 *
 * @param <O> the type of output the parts render to
 * @author reed.vonredwitz
 * @since Oct-2026
 */
public final class Slots<O extends Out>
    implements Template<O> {

    /**
     * The name of the content that is in no part: what {@link #render(Out)} renders.
     */
    public static final String BODY = "body";

    private final Map<String, ? extends Template<O>> parts;

    /**
     * @param parts the parts by name; the content that is in no part has the name {@link #BODY}
     */
    public Slots(final Map<String, ? extends Template<O>> parts) {
        this.parts = Map.copyOf(parts);
    }

    /**
     * The parts for a layout that is rendered from Java rather than from an include: the content that is in no part,
     * to which {@link #fill(String, Template)} adds the named parts.
     *
     * @param body the content that is in no part
     * @param <O>  the type of output the parts render to
     */
    public static <O extends Out> Slots<O> of(final Template<O> body) {
        return new Slots<>(Map.of(BODY, body));
    }

    /**
     * These parts and one more, given under the name, which replaces a part of that name that there is.
     *
     * @param name the name of the part, which a layout renders with a slot of that name
     * @param part the part
     */
    public Slots<O> fill(final String name, final Template<O> part) {
        final Map<String, Template<O>> filled = new HashMap<>(parts);
        filled.put(name, part);
        return new Slots<>(filled);
    }

    /**
     * Whether a part was given under the name.
     */
    public boolean has(final String name) {
        return parts.containsKey(name);
    }

    /**
     * Renders the part given under the name, or nothing if there is none.
     */
    public void render(final String name, final O out) {
        final Template<O> part = parts.get(name);
        if (part != null) {
            part.render(out);
        }
    }

    /**
     * Renders the content that is in no part.
     */
    @Override
    public void render(final O out) {
        render(BODY, out);
    }
}
