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
import build.base.foundation.Lazy;
import build.base.foundation.stream.Streamable;
import build.base.foundation.stream.Streams;
import build.base.foundation.tuple.Pair;
import build.base.json.Json;
import build.base.json.JsonArray;
import build.base.json.JsonNull;
import build.base.json.JsonNumber;
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
import build.base.transport.json.codec.LazyCodec;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private static final String ID_FIELD = "@id";
    private static final String REF_FIELD = "@ref";

    /**
     * The identity assigned, for the current {@link #write}, to each {@link Marshalled} already encountered -
     * so that a {@link Marshalled} reached a second time (whether still being encoded, i.e. genuinely cyclic,
     * or already fully encoded, i.e. merely shared) is represented as a back-reference rather than re-walked.
     * <p>
     * Scoped to the current {@link #write} via {@link ThreadLocal}, since {@link #encode} is public API also
     * invoked (recursively) by {@link Codec}s, which cannot be given an additional context parameter. The state
     * is owned by the outermost {@link #encodeMarshalled} and cleared when it completes, even if it throws.
     */
    private static final ThreadLocal<Map<Marshalled<?>, Integer>> ENCODING_IDS =
        ThreadLocal.withInitial(IdentityHashMap::new);

    /**
     * The {@link Marshalled} decoded for each {@code @id} so far during the current outermost decode, so a
     * {@code @ref} to it can be resolved on demand. See {@link Marshalled#referent()}.
     */
    private static final ThreadLocal<Map<Integer, Marshalled<?>>> DECODED_BY_ID = new ThreadLocal<>();

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
        register(new LazyCodec<>());
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
     * Substitutes {@code targetClass} for {@code originalType}'s raw type, preserving {@code originalType}'s own
     * type arguments if it has any. Used when a {@link Transformer} swaps one {@link Class} for another (e.g.
     * {@code Stream} for {@code Streamable}) - {@link Transformer#targetClass()} only offers a raw {@link Class},
     * which would otherwise erase any type argument (e.g. the {@code X} of {@code Stream<X>}) the original
     * {@link Type} carried, exactly the same erasure a {@link Codec} would suffer without this.
     *
     * @param originalType the {@link Type} being transformed
     * @param targetClass  the {@link Transformer}'s target {@link Class}
     * @return {@code targetClass}, reparameterized with {@code originalType}'s type arguments if it had any
     */
    private static Type retarget(final Type originalType, final Class<?> targetClass) {
        if (!(originalType instanceof ParameterizedType parameterizedType)) {
            return targetClass;
        }
        final var typeArguments = parameterizedType.getActualTypeArguments();
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return typeArguments;
            }

            @Override
            public Type getRawType() {
                return targetClass;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }

    /**
     * Encodes a {@link Marshalled} object as a {@link JsonObject} and writes it to the provided {@link Writer}.
     * <p>
     * Any raw value encountered along the way is marshalled using a new {@link Marshaller}, distinct from the one
     * that produced {@code marshalled}. A raw value that refers back to the object {@code marshalled} was produced
     * from is therefore marshalled afresh, and a cycle through it only closes one level further in than the
     * outermost object. To close it onto the outermost object, use {@link #write(Marshalled, Writer, Marshaller)}
     * with the {@link Marshaller} that produced {@code marshalled}.
     *
     * @param marshalled the {@link Marshalled} object
     * @param writer     the destination
     */
    public void write(final Marshalled<?> marshalled, final Writer writer) {
        write(marshalled, writer, this.schemaFactory.newMarshaller());
    }

    /**
     * Encodes a {@link Marshalled} object as a {@link JsonObject} and writes it to the provided {@link Writer},
     * using the given {@link Marshaller} to marshal any raw values encountered along the way.
     * <p>
     * Supplying the very {@link Marshaller} that produced {@code marshalled} lets a raw value that refers back
     * to the object {@code marshalled} was produced from be recognized as that same {@link Marshalled} (a
     * {@link Marshaller} returns the same {@link Marshalled} for an object it has already marshalled), so the
     * cycle is closed with a reference to the outermost object. With a different {@link Marshaller} the raw
     * value is marshalled afresh, and the cycle only closes one level further in.
     *
     * @param marshalled the {@link Marshalled} object
     * @param writer     the destination
     * @param marshaller the {@link Marshaller}
     */
    public void write(final Marshalled<?> marshalled, final Writer writer, final Marshaller marshaller) {
        Objects.requireNonNull(marshalled, "marshalled");
        Objects.requireNonNull(writer, "writer");
        Objects.requireNonNull(marshaller, "marshaller");
        try {
            writer.write(encodeMarshalled(marshalled, marshaller).toJsonString());
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
     * <p>
     * Shared and cyclic {@link Marshalled}s are tracked in state scoped to the current thread, so a
     * {@link Codec} must encode nested values on the calling thread (not, for example, in a parallel stream),
     * otherwise a repeated {@link Marshalled} is encoded afresh instead of as an {@code @ref}.
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
            return encode(parameter, retarget(valueType, transformer.targetClass()), transformed, marshaller);
        }

        final var optionalCodec = getCodec(valueType);
        if (optionalCodec.isPresent()) {
            return optionalCodec.orElseThrow().encode(this, parameter, valueType, value, marshaller);
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
            final var read = decode(parameter, retarget(type, transformer.targetClass()), value, marshaller);
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

        // a Lazy<T> parameter is willing to receive its value once it becomes available, rather than
        // requiring it up front - so, for a marshallable T, this decodes only as far as the structural
        // Marshalled<T> (exactly as the Marshalled.class branch below does), leaving the actual unmarshal()
        // deferred. It can't be deferred here: the Marshaller in scope at this point is whichever one is
        // parsing the JSON (e.g. via JsonTransport.read()), which is not necessarily the same Marshaller
        // that will eventually construct objects from the result (e.g. via a later, separate
        // Marshaller.unmarshal() call) - so ConcurrentSchemaFactory.UnmarshallingSchema.unmarshal() is what
        // actually wraps this in a Lazy, using the Marshaller that is genuinely doing the constructing. This
        // is what allows a genuine cycle to be reconstructed at all: by the time something actually calls
        // Lazy.get(), the rest of the decode this value participates in - including, for a self-reference,
        // the very object whose constructor is receiving this Lazy - has finished and is resolvable.
        // <p>
        // This only applies when the value is a raw structural object (i.e. the corresponding @Marshal side
        // exposed the value as a plain T, not as a Lazy<T> itself) - a symmetric Lazy<T> field is encoded by
        // LazyCodec instead, as the array-wrapped [] / [element] shape (mirroring OptionalCodec), and must be
        // decoded that same way (eagerly, since there is no separate raw/lazy asymmetry to defer across).
        if (Lazy.class.isAssignableFrom(readableClass) && value instanceof JsonObject) {
            final var elementType = Introspection.getParameterType(type)
                .orElseThrow(() -> new IllegalStateException(
                    "Failed to determine Lazy<T> element type for parameter [" + parameter.name() + "]"));
            final var elementClass = Introspection.getClassFromType(elementType).orElse(Object.class);
            if (Marshalled.class.isAssignableFrom(elementClass) || marshaller.isMarshallable(elementClass)) {
                return (T) decodeMarshalled(value.asObject(), marshaller);
            }
            if (elementClass == Object.class
                || elementClass.isInterface()
                || Modifier.isAbstract(elementClass.getModifiers())) {

                // mirrors the abstract/interface dispatch below (unwrap the @type/value wrapper to find the
                // concrete type) but, as above, stops at the structural Marshalled<T> rather than resolving
                // it - the wrapped value is that concrete type's own self-describing (@type/@id) object, so
                // decodeMarshalled can resolve it without needing the concrete type looked up separately here
                final var wrapper = value.asObject();
                if (!wrapper.has(VALUE_FIELD)) {
                    throw new IllegalStateException("Failed to decode Lazy<T> parameter [" + parameter.name()
                        + "]: expected a [" + VALUE_FIELD + "] member in the wrapper for type [" + elementClass + "]");
                }
                return (T) decodeMarshalled(wrapper.get(VALUE_FIELD).asObject(), marshaller);
            }
            // not a deferred-cycle case: a plain-valued Lazy<T> (e.g. Lazy<Integer>) is encoded via LazyCodec
            // (array-wrapped, mirroring OptionalCodec/StreamableCodec), not as a raw value - so fall through
            // to the getCodec(type) lookup below instead of assuming raw passthrough.
        }

        final var optionalCodec = getCodec(type);
        if (optionalCodec.isPresent()) {
            return (T) optionalCodec.orElseThrow().decode(this, parameter, type, value, marshaller);
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

    private JsonObject encodeMarshalled(final Marshalled<?> marshalled, final Marshaller marshaller) {

        // the outermost encode (empty ids) owns the identity state and clears it when finished, so it never
        // outlives the encode - whether it was started by write() or by a direct call to encode()
        final var outermost = ENCODING_IDS.get().isEmpty();
        try {
            final var encoded = walkMarshalled(marshalled, marshaller);
            return outermost ? withoutUnreferencedIds(encoded) : encoded;
        }
        finally {
            if (outermost) {
                ENCODING_IDS.remove();
            }
        }
    }

    /**
     * Removes every {@code @id} from an encoded tree that no {@code @ref} within it refers to. An identity is
     * assigned to every {@link Marshalled} as it is encoded, as whether it is referenced again can only be
     * known once everything has been encoded - by which point its (immutable) {@link JsonObject} has already
     * been built. Pruning afterwards keeps the output free of identities nothing needs.
     */
    private static JsonObject withoutUnreferencedIds(final JsonObject encoded) {
        final var referenced = new HashSet<Integer>();
        collectReferences(encoded, referenced);
        return (JsonObject) prune(encoded, referenced);
    }

    private static void collectReferences(final JsonValue value, final Set<Integer> referenced) {
        if (value instanceof JsonObject object) {
            if (object.has(REF_FIELD)) {
                referenced.add(object.get(REF_FIELD).asNumber().toNumber().intValue());
            }
            object.members().values().forEach(member -> collectReferences(member, referenced));
        }
        else if (value instanceof JsonArray array) {
            array.values().forEach(element -> collectReferences(element, referenced));
        }
    }

    private static JsonValue prune(final JsonValue value, final Set<Integer> referenced) {
        if (value instanceof JsonObject object) {
            final var members = new LinkedHashMap<String, JsonValue>();
            for (final var entry : object.members().entrySet()) {
                if (ID_FIELD.equals(entry.getKey())
                    && !referenced.contains(entry.getValue().asNumber().toNumber().intValue())) {
                    continue;
                }
                members.put(entry.getKey(), prune(entry.getValue(), referenced));
            }
            return JsonObject.of(members);
        }
        if (value instanceof JsonArray array) {
            return JsonArray.of(array.values().stream()
                .<JsonValue>map(element -> prune(element, referenced))
                .toList());
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private JsonObject walkMarshalled(final Marshalled<?> marshalled, final Marshaller marshaller) {

        final var ids = ENCODING_IDS.get();
        final var existingId = ids.get(marshalled);
        if (existingId != null) {
            // already encoded (or still being encoded, i.e. a genuine cycle) - reference it rather than
            // re-walking it, which for a genuine cycle would otherwise recurse forever
            return JsonObject.of(Map.of(REF_FIELD, JsonNumber.of(existingId)));
        }

        // assign the identity before walking this Marshalled's own values, so a cycle discovered while doing
        // so (directly, or transitively) can already reference it
        final var id = ids.size() + 1;
        ids.put(marshalled, id);

        final var members = new LinkedHashMap<String, JsonValue>();
        members.put(TYPE_FIELD, JsonString.of(marshalled.schema().owner().getName()));
        members.put(ID_FIELD, JsonNumber.of(id));

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

    private <T> Marshalled<T> decodeMarshalled(final JsonObject json, final Marshaller marshaller) {

        // the outermost decode owns the definitions map; every reference decoded within it captures the map, so
        // it remains usable (to resolve a reference on demand) long after this thread-local has been cleared
        final var outermost = DECODED_BY_ID.get() == null;
        if (outermost) {
            DECODED_BY_ID.set(new HashMap<>());
        }
        try {
            return walkDecodedMarshalled(json, marshaller);
        }
        finally {
            if (outermost) {
                DECODED_BY_ID.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Marshalled<T> walkDecodedMarshalled(final JsonObject json, final Marshaller marshaller) {

        final var definitions = DECODED_BY_ID.get();

        if (json.has(REF_FIELD)) {
            final var referencedId = json.get(REF_FIELD).asNumber().toNumber().intValue();
            return new Marshalled<T>() {
                @Override
                public Schema<T> schema() {
                    throw new UnsupportedOperationException("a reference Marshalled has no schema");
                }

                @Override
                public Streamable<Object> values() {
                    throw new UnsupportedOperationException("a reference Marshalled has no values");
                }

                @Override
                public Optional<Integer> reference() {
                    return Optional.of(referencedId);
                }

                @Override
                public Optional<Marshalled<T>> referent() {
                    return Optional.ofNullable((Marshalled<T>) definitions.get(referencedId));
                }
            };
        }

        final var id = json.has(ID_FIELD)
            ? Optional.of(json.get(ID_FIELD).asNumber().toNumber().intValue())
            : Optional.<Integer>empty();

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
            if (TYPE_FIELD.equals(fieldName) || ID_FIELD.equals(fieldName)) {
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

        final var definition = new Marshalled<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public Schema<T> schema() {
                return (Schema<T>) match.first();
            }

            @Override
            public Streamable<Object> values() {
                return values;
            }

            @Override
            public Optional<Integer> id() {
                return id;
            }
        };

        id.ifPresent(identity -> definitions.put(identity, definition));
        return definition;
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
