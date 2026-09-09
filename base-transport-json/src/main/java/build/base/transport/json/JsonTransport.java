package build.base.transport.json;

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

import build.base.foundation.Introspection;
import build.base.foundation.stream.Streamable;
import build.base.foundation.stream.Streams;
import build.base.foundation.tuple.Pair;
import build.base.json.Json;
import build.base.json.JsonNull;
import build.base.json.JsonObject;
import build.base.json.JsonString;
import build.base.json.JsonValue;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshaller;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Parameter;
import build.base.marshalling.Schema;
import build.base.marshalling.SchemaFactory;
import build.base.transport.AbstractTransport;
import build.base.transport.Transport;
import build.base.transport.json.codec.BigDecimalCodec;
import build.base.transport.json.codec.BigIntegerCodec;
import build.base.transport.json.codec.BooleanCodec;
import build.base.transport.json.codec.ByteCodec;
import build.base.transport.json.codec.CharacterCodec;
import build.base.transport.json.codec.DateCodec;
import build.base.transport.json.codec.DoubleCodec;
import build.base.transport.json.codec.DurationCodec;
import build.base.transport.json.codec.FloatCodec;
import build.base.transport.json.codec.InstantCodec;
import build.base.transport.json.codec.IntegerCodec;
import build.base.transport.json.codec.LocalDateCodec;
import build.base.transport.json.codec.LocalDateTimeCodec;
import build.base.transport.json.codec.LocalTimeCodec;
import build.base.transport.json.codec.LongCodec;
import build.base.transport.json.codec.MonthDayCodec;
import build.base.transport.json.codec.OffsetDateTimeCodec;
import build.base.transport.json.codec.OffsetTimeCodec;
import build.base.transport.json.codec.OptionalCodec;
import build.base.transport.json.codec.PeriodCodec;
import build.base.transport.json.codec.ShortCodec;
import build.base.transport.json.codec.StreamableCodec;
import build.base.transport.json.codec.StringCodec;
import build.base.transport.json.codec.UUIDCodec;
import build.base.transport.json.codec.YearCodec;
import build.base.transport.json.codec.YearMonthCodec;
import build.base.transport.json.codec.ZoneIdCodec;
import build.base.transport.json.codec.ZoneOffsetCodec;
import build.base.transport.json.codec.ZonedDateTimeCodec;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A JSON-based {@link Transport} for {@link Marshalled} {@link Object}s.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class JsonTransport
    extends AbstractTransport<JsonTransport> {

    private static final String TYPE_FIELD = "@type";
    private static final String VALUE_FIELD = "value";

    private final SchemaFactory schemaFactory;
    private final ConcurrentHashMap<Class<?>, Codec<?>> codecs;

    /**
     * Constructs a {@link JsonTransport} using the specified {@link SchemaFactory}.
     *
     * @param schemaFactory the {@link SchemaFactory}
     */
    public JsonTransport(final SchemaFactory schemaFactory) {

        this.codecs = new ConcurrentHashMap<>();
        this.schemaFactory = schemaFactory == null
            ? Marshalling.globalSchemaFactory()
            : schemaFactory;

        register(new StringCodec());
        register(new OptionalCodec());
        register(new StreamableCodec());
        register(new IntegerCodec());
        register(new BooleanCodec());
        register(new LongCodec());
        register(new ByteCodec());
        register(new ShortCodec());
        register(new FloatCodec());
        register(new DoubleCodec());
        register(new CharacterCodec());
        register(new BigDecimalCodec());
        register(new BigIntegerCodec());
        register(new InstantCodec());
        register(new LocalDateCodec());
        register(new LocalTimeCodec());
        register(new LocalDateTimeCodec());
        register(new ZonedDateTimeCodec());
        register(new OffsetDateTimeCodec());
        register(new OffsetTimeCodec());
        register(new YearCodec());
        register(new YearMonthCodec());
        register(new MonthDayCodec());
        register(new ZoneIdCodec());
        register(new ZoneOffsetCodec());
        register(new DurationCodec());
        register(new PeriodCodec());
        register(new DateCodec());
        register(new UUIDCodec());
    }

    /**
     * Constructs a {@link JsonTransport} using the {@link Marshalling#globalSchemaFactory()}.
     */
    public JsonTransport() {
        this(Marshalling.globalSchemaFactory());
    }

    /**
     * Registers the specified {@link Codec} for use with the {@link JsonTransport}.
     *
     * @param codec the {@link Codec}
     * @return this {@link JsonTransport} to permit fluent-style method invocation
     */
    public JsonTransport register(final Codec<?> codec) {
        if (codec != null) {
            this.codecs.put(codec.codecClass(), codec);
        }
        return this;
    }

    /**
     * Obtains the {@link Codec} for the specified {@link Type}.
     *
     * @param type the {@link Type}
     * @return the {@link Optional} {@link Codec}, otherwise {@link Optional#empty()}
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<Codec<T>> getCodec(final Type type) {
        return Introspection.getClassFromType(type)
            .map(this.codecs::get)
            .map(codec -> (Codec<T>) codec);
    }

    /**
     * Encodes a {@link Marshalled} object as a {@link JsonObject} and writes it to the provided {@link Writer}.
     *
     * @param marshalled the {@link Marshalled} object
     * @param writer     the destination
     */
    public void write(final Marshalled<?> marshalled, final Writer writer) {
        Objects.requireNonNull(marshalled, "marshalled");
        Objects.requireNonNull(writer, "writer");
        try {
            writer.write(encodeMarshalled(marshalled, this.schemaFactory.newMarshaller()).toJsonString());
        }
        catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parses JSON from the provided {@link Reader} and decodes it as a {@link Marshalled} object.
     *
     * @param reader the source of JSON text
     * @param <T>    the type of the marshalled object
     * @return the decoded {@link Marshalled}
     */
    public <T> Marshalled<T> read(final Reader reader) {
        Objects.requireNonNull(reader, "reader");
        return decodeMarshalled(Json.parse(reader).asObject(), this.schemaFactory.newMarshaller());
    }

    /**
     * Parses JSON from the provided {@link Reader} and decodes it as a {@link Marshalled} object,
     * using the given {@link Marshaller} (e.g. one with bound values).
     *
     * @param reader     the source of JSON text
     * @param marshaller the {@link Marshaller} to use
     * @param <T>        the type of the marshalled object
     * @return the decoded {@link Marshalled}
     */
    public <T> Marshalled<T> read(final Reader reader, final Marshaller marshaller) {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(marshaller, "marshaller");
        return decodeMarshalled(Json.parse(reader).asObject(), marshaller);
    }

    /**
     * Encodes a value of the given type as a {@link JsonValue}, used by codecs for recursive encoding.
     *
     * @param parameter  the {@link Parameter}
     * @param valueType  the {@link Type} of the value
     * @param value      the value
     * @param marshaller the {@link Marshaller}
     * @return the encoded {@link JsonValue}
     */
    @SuppressWarnings("unchecked")
    public JsonValue encode(final Parameter parameter,
                            final Type valueType,
                            final Object value,
                            final Marshaller marshaller) {

        if (value == null) {
            return JsonNull.INSTANCE;
        }

        if (value instanceof Marshalled<?> marshalledValue) {
            return encodeMarshalled(marshalledValue, marshaller);
        }

        final var valueClass = Introspection.getClassFromType(valueType)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to determine class of [" + valueType + "] for parameter [" + parameter.name() + "]"));

        final var optionalTransformer = getTransformer(valueClass);
        if (optionalTransformer.isPresent()) {
            final var transformer = optionalTransformer.orElseThrow();
            final var transformed = transformer.transform(marshaller, value);
            if (Objects.equals(transformed, value)) {
                throw new IllegalStateException("Transformer produced no change for parameter ["
                    + parameter.name() + "] of type [" + valueClass + "]");
            }
            return encode(parameter, transformer.targetClass(), transformed, marshaller);
        }

        final var optionalCodec = getCodec(valueType);
        if (optionalCodec.isPresent()) {
            return optionalCodec.orElseThrow().encode(this, parameter, value, marshaller);
        }

        if (this.schemaFactory.isMarshallable(valueClass)) {
            return encodeMarshalled(marshaller.marshal(value), marshaller);
        }

        if (valueClass == Object.class
            || valueClass.isInterface()
            || Modifier.isAbstract(valueClass.getModifiers())) {

            return JsonObject.of(Map.of(
                TYPE_FIELD,  JsonString.of(value.getClass().getName()),
                VALUE_FIELD, encode(parameter, value.getClass(), value, marshaller)));
        }

        throw new IllegalStateException("No Transformer, Codec, or @Marshal-able found for parameter ["
            + parameter.name() + "] of type [" + valueClass + "]");
    }

    /**
     * Decodes a value of the given type from a {@link JsonValue}, used by codecs for recursive decoding.
     *
     * @param parameter  the {@link Parameter}
     * @param type       the {@link Type} of the expected value
     * @param value      the {@link JsonValue} to decode
     * @param marshaller the {@link Marshaller}
     * @param <T>        the type of the decoded value
     * @return the decoded value
     */
    @SuppressWarnings("unchecked")
    public <T> T decode(final Parameter parameter,
                        final Type type,
                        final JsonValue value,
                        final Marshaller marshaller) {

        if (value instanceof JsonNull) {
            return null;
        }

        final var optionalTransformer = getTransformer(type);
        if (optionalTransformer.isPresent()) {
            final var transformer = optionalTransformer.orElseThrow();
            final var read = decode(parameter, transformer.targetClass(), value, marshaller);
            final var reformed = transformer.reform(marshaller, type, read);
            if (Objects.equals(reformed, read)) {
                throw new IllegalStateException("Transformer reform produced no change for parameter ["
                    + parameter.name() + "] of type [" + type + "]");
            }
            return (T) reformed;
        }

        final var readableClass = Introspection.getClassFromType(type)
            .orElseThrow(() -> new IllegalStateException(
                "Failed to determine class for parameter [" + parameter.name() + "] of type [" + type + "]"));

        final var optionalCodec = getCodec(type);
        if (optionalCodec.isPresent()) {
            return (T) optionalCodec.orElseThrow().decode(this, parameter, value, marshaller);
        }

        if (Marshalled.class.isAssignableFrom(readableClass)) {
            return (T) decodeMarshalled(value.asObject(), marshaller);
        }

        if (marshaller.isMarshallable(readableClass)) {
            return (T) marshaller.unmarshal(decodeMarshalled(value.asObject(), marshaller));
        }

        if (readableClass == Object.class
            || readableClass.isInterface()
            || Modifier.isAbstract(readableClass.getModifiers())) {

            final var wrapper = value.asObject();
            final var typeName = wrapper.get(TYPE_FIELD).asString().value();
            final var concreteType = loadClass(typeName, parameter);
            return (T) decode(parameter, concreteType, wrapper.get(VALUE_FIELD), marshaller);
        }

        throw new IllegalStateException("No Transformer, Codec, or @Marshal-able found for parameter ["
            + parameter.name() + "] of type [" + readableClass + "]");
    }

    @SuppressWarnings("unchecked")
    private JsonObject encodeMarshalled(final Marshalled<?> marshalled, final Marshaller marshaller) {
        final var members = new LinkedHashMap<String, JsonValue>();
        members.put(TYPE_FIELD, JsonString.of(marshalled.schema().owner().getName()));

        final Iterable<Pair<Parameter, Object>> pairs = () -> Streams.zip(
                marshalled.schema().parameters().stream(),
                marshalled.values().stream())
            .iterator();

        for (final var pair : pairs) {
            final var parameter = pair.first();
            final var value = pair.second();

            final var codec = getCodec(parameter.type());

            if (codec.filter(ConditionalCodec.class::isInstance)
                .map(ConditionalCodec.class::cast)
                .filter(cc -> cc.ignore(value))
                .isPresent()) {
                continue;
            }

            members.put(parameter.name(), encode(parameter, parameter.type(), value, marshaller));
        }

        return JsonObject.of(members);
    }

    @SuppressWarnings("unchecked")
    private <T> Marshalled<T> decodeMarshalled(final JsonObject json, final Marshaller marshaller) {
        final var typeName = json.get(TYPE_FIELD).asString().value();
        final Class<?> typeClass = loadClass(typeName, null);

        final var schemas = this.schemaFactory.getUnmarshallingSchemas(typeClass)
            .map(schema -> Pair.of(
                schema,
                schema.parameters().stream()
                    .collect(Collectors.toMap(Parameter::name, p -> Pair.of(p, Out.empty())))))
            .collect(Collectors.toCollection(ArrayList::new));

        if (schemas.isEmpty()) {
            throw new IllegalStateException("No schemas defined for type: " + typeName);
        }

        for (final var entry : json.members().entrySet()) {
            final var fieldName = entry.getKey();
            if (TYPE_FIELD.equals(fieldName)) {
                continue;
            }
            final var fieldValue = entry.getValue();

            schemas.removeIf(pair -> !pair.second().containsKey(fieldName));

            if (schemas.isEmpty()) {
                throw new IllegalStateException("No schema supports field '" + fieldName + "' for type " + typeName);
            }

            final var parameter = schemas.getFirst().second().get(fieldName).first();
            final var decoded = decode(parameter, parameter.type(), fieldValue, marshaller);

            schemas.forEach(pair -> pair.second().get(fieldName).second().set(decoded));
        }

        var optionalMatch = schemas.stream()
            .filter(pair -> pair.second().values().stream().map(Pair::second).allMatch(Out::isPresent))
            .findFirst();

        if (optionalMatch.isEmpty()) {
            optionalMatch = schemas.stream()
                .filter(pair -> pair.second().values().stream()
                    .allMatch(entry -> {
                        if (entry.second().isPresent()) {
                            return true;
                        }
                        final var codec = getCodec(entry.first().type());
                        codec.filter(ConditionalCodec.class::isInstance)
                            .map(ConditionalCodec.class::cast)
                            .ifPresent(cc -> entry.second().set(cc.defaultValue()));
                        return entry.second().isPresent();
                    }))
                .findFirst();
        }

        final var match = optionalMatch
            .orElseThrow(() -> new IllegalStateException("Failed to decode required fields for type " + typeName));

        final var values = Streamable.of(match.first().parameters().stream()
            .map(p -> match.second().get(p.name()).second().orElse(null)));

        return new Marshalled<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public Schema<T> schema() {
                return (Schema<T>) match.first();
            }

            @Override
            public Streamable<Object> values() {
                return values;
            }
        };
    }

    private Class<?> loadClass(final String name, final Parameter parameter) {
        try {
            return Class.forName(name);
        }
        catch (final ClassNotFoundException e) {
            final var context = parameter == null ? "" : " for parameter [" + parameter.name() + "]";
            throw new IllegalStateException("Failed to load class [" + name + "]" + context, e);
        }
    }
}
