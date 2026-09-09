package build.base.transport.json.codec;

/*-
 * #%L
 * base.build Transport (JSON)
 * %%
 * Copyright (C) 2025 Workday Inc
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

import build.base.json.JsonNull;
import build.base.json.JsonString;
import build.base.json.JsonValue;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Parameter;
import build.base.transport.json.Codec;
import build.base.transport.json.JsonTransport;

import java.time.ZoneOffset;

/**
 * A {@link Codec} for {@link ZoneOffset} values.
 *
 * @author reed.vonredwitz
 * @since Sep-2026
 */
public class ZoneOffsetCodec
    implements Codec<ZoneOffset> {

    @Override
    public Class<ZoneOffset> codecClass() {
        return ZoneOffset.class;
    }

    @Override
    public JsonValue encode(final JsonTransport transport,
                            final Parameter parameter,
                            final ZoneOffset value,
                            final Marshaller marshaller) {

        return value == null ? JsonNull.INSTANCE : JsonString.of(value.getId());
    }

    @Override
    public ZoneOffset decode(final JsonTransport transport,
                             final Parameter parameter,
                             final JsonValue value,
                             final Marshaller marshaller) {

        return value instanceof JsonNull ? null : ZoneOffset.of(value.asString().value());
    }
}
