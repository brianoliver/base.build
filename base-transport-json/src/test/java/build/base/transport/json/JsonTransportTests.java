package build.base.transport.json;

import build.base.json.JsonAssertionException;
import build.base.marshalling.Bound;
import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalled;
import build.base.marshalling.Marshalling;
import build.base.transport.json.example.Address;
import build.base.transport.json.example.AddressWithCountry;
import build.base.transport.json.example.ClassWithArrayOfClassWithObjectProperty;
import build.base.transport.json.example.ClassWithObjectProperty;
import build.base.transport.json.example.Company;
import build.base.transport.json.example.Country;
import build.base.transport.json.example.Expense;
import build.base.transport.json.example.ExpenseLine;
import build.base.transport.json.example.MultipleUnmarshalls;
import build.base.transport.json.example.NativeTypes;
import build.base.transport.json.example.Person;
import build.base.transport.json.example.PersonWithFirstName;
import build.base.transport.json.example.PersonWithOptionalLastName;
import build.base.transport.json.example.StreamsAndOptionals;
import build.base.transport.json.example.TemporalPerson;
import build.base.transport.json.example.Uber;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JsonTransport}.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class JsonTransportTests {

    /**
     * Ensure a {@link Marshal}lable {@link Object} containing a {@link String} can be transported.
     */
    @Test
    void shouldWriteAndReadMarshalledString() {

        final var person = new PersonWithFirstName("Gustav");

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(person);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<PersonWithFirstName> transported = transport.read(new StringReader(writer.toString()));

        final PersonWithFirstName unmarshalledPerson = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotSameAs(unmarshalledPerson);

        assertThat(person)
            .isEqualTo(unmarshalledPerson);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} containing a {@code null} {@link String} can be transported.
     */
    @Test
    void shouldWriteAndReadMarshalledNullString() {

        final var person = new PersonWithFirstName(null);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(person);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<PersonWithFirstName> transported = transport.read(new StringReader(writer.toString()));

        final PersonWithFirstName unmarshalledPerson = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotSameAs(unmarshalledPerson);

        assertThat(person)
            .isEqualTo(unmarshalledPerson);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} containing an empty {@link String} can be transported.
     */
    @Test
    void shouldWriteAndReadMarshalledEmptyString() {

        final var person = new PersonWithFirstName("");

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(person);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<PersonWithFirstName> transported = transport.read(new StringReader(writer.toString()));

        final PersonWithFirstName unmarshalledPerson = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotSameAs(unmarshalledPerson);

        assertThat(person)
            .isEqualTo(unmarshalledPerson);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} containing an {@link Optional} {@link String} can be transported.
     */
    @Test
    void shouldWriteAndReadMarshalledOptionalString() {

        final var person = new PersonWithOptionalLastName("Gustav", Optional.of("von Redwitz"));

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(person);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<PersonWithOptionalLastName> transported = transport.read(new StringReader(writer.toString()));

        final PersonWithOptionalLastName unmarshalledPerson = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotSameAs(unmarshalledPerson);

        assertThat(person)
            .isEqualTo(unmarshalledPerson);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} containing an {@link Optional#empty()} {@link String} can be
     * transported.
     */
    @Test
    void shouldWriteAndReadMarshalledOptionalEmptyString() {

        final var person = new PersonWithOptionalLastName("Gustav", Optional.empty());

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(person);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<PersonWithOptionalLastName> transported = transport.read(new StringReader(writer.toString()));

        final PersonWithOptionalLastName unmarshalledPerson = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotSameAs(unmarshalledPerson);

        assertThat(person)
            .isEqualTo(unmarshalledPerson);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} containing a {@code null} {@link Optional} {@link String} can be
     * transported.
     */
    @Test
    void shouldWriteAndReadMarshalledNullOptionalString() {

        final var person = new PersonWithOptionalLastName("Gustav", null);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(person);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<PersonWithOptionalLastName> transported = transport.read(new StringReader(writer.toString()));

        final PersonWithOptionalLastName unmarshalledPerson = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotSameAs(unmarshalledPerson);

        assertThat(person)
            .isEqualTo(unmarshalledPerson);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} containing a {@code null} {@link Optional} can be decoded
     * from a raw JSON string.
     */
    @Test
    void shouldReadMarshalledNullOptionalString() {

        final var marshaller = Marshalling.newMarshaller();
        final var transport = new JsonTransport();

        final var json = """
            {"@type":"build.base.transport.json.example.PersonWithOptionalLastName","firstName":"Gustav","lastName":null}
            """;

        final Marshalled<PersonWithOptionalLastName> transported = transport.read(new StringReader(json));

        final PersonWithOptionalLastName person = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotNull();
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} composed of another {@link Marshal}lable {@link Object} can be
     * transported.
     */
    @Test
    void shouldWriteAndReadComposedObjectsUsingMarshalleds() {

        final var person = new Person(new Address("Big Street", "Munich"));

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(person);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<Person> transported = transport.read(new StringReader(writer.toString()));

        final Person unmarshalledPerson = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotSameAs(unmarshalledPerson);

        assertThat(person)
            .isEqualTo(unmarshalledPerson);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} composed of another {@link Marshal}lable {@link Object} can be
     * transported.
     */
    @Test
    void shouldWriteAndReadComposedStreamableObjects() {

        final var company = new Company()
            .add(new Address("Big Street", "Munich"))
            .add(new Address("Small Street", "Boston"));

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(company);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<Company> transported = transport.read(new StringReader(writer.toString()));

        final Company unmarshalledCompany = marshaller.unmarshal(transported);

        assertThat(company)
            .isNotSameAs(unmarshalledCompany);

        assertThat(company)
            .isEqualTo(unmarshalledCompany);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} composed of another {@link Marshal}lable {@link Object}, but without
     * any {@link Object}s, can be transported.
     */
    @Test
    void shouldWriteAndReadComposedEmptyStreamableObjects() {

        final var company = new Company();

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(company);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<Company> transported = transport.read(new StringReader(writer.toString()));

        final Company unmarshalledCompany = marshaller.unmarshal(transported);

        assertThat(company)
            .isNotSameAs(unmarshalledCompany);

        assertThat(company)
            .isEqualTo(unmarshalledCompany);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} using a {@code null} {@link java.util.stream.Stream} can be decoded.
     */
    @Test
    void shouldReadComposedNullStream() {

        final var marshaller = Marshalling.newMarshaller();
        final var transport = new JsonTransport();

        final var json = """
            {"@type":"build.base.transport.json.example.Company","addresses":null}
            """;

        final Marshalled<Company> transported = transport.read(new StringReader(json));

        final Company company = marshaller.unmarshal(transported);

        assertThat(company)
            .isNotNull();
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} composed of another {@link Marshal}lable {@code null}s can be
     * transported.
     */
    @Test
    void shouldWriteAndReadComposedStreamableNulls() {

        final var company = new Company()
            .add(null)
            .add(null);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(company);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<Company> transported = transport.read(new StringReader(writer.toString()));

        final Company unmarshalledCompany = marshaller.unmarshal(transported);

        assertThat(company)
            .isNotSameAs(unmarshalledCompany);

        assertThat(company)
            .isEqualTo(unmarshalledCompany);
    }

    /**
     * Ensure a registered {@code enum} can be marshalled and unmarshalled directly via the marshaller,
     * as required when iterating a {@link java.util.stream.Stream} and marshalling each element.
     */
    @Test
    void shouldMarshalAndUnmarshalRegisteredEnumDirectly() {

        final var marshaller = Marshalling.newMarshaller();

        final var marshalled = marshaller.marshal(Country.Australia);

        assertThat(marshalled.schema().owner())
            .isEqualTo(Country.class);

        assertThat(marshalled.values().stream())
            .containsExactly("Australia");

        final Country unmarshalled = marshaller.unmarshal(marshalled);

        assertThat(unmarshalled)
            .isEqualTo(Country.Australia);
    }

    /**
     * Ensure {@link Marshal}lable with an {@code enum} can be transported.
     */
    @Test
    void shouldWriteAndReadClassWithEnum() {

        final var address = new AddressWithCountry("Queen Street", "Brisbane", Country.Australia);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(address);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<AddressWithCountry> transported = transport.read(new StringReader(writer.toString()));

        final var unmarshalled = marshaller.unmarshal(transported);

        assertThat(address)
            .isNotSameAs(unmarshalled);

        assertThat(address)
            .isEqualTo(unmarshalled);
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} containing {@code short}, {@link java.time} and
     * {@link java.util.UUID} values (including {@code null}s) can be transported.
     */
    @Test
    void shouldWriteAndReadClassWithNativeTypes() {

        final var original = new NativeTypes();

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(original);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        assertThat(writer.toString())
            .contains("\"aShort\":-32768")
            .contains("\"aShortWrapper\":32767")
            .contains("\"aNullShortWrapper\":null")
            .contains("\"anOffsetDateTime\":\"2026-09-09T14:31:07+02:00\"")
            .contains("\"anOffsetTime\":\"14:31:07-05:00\"")
            .contains("\"aYear\":\"2026\"")
            .contains("\"aYearMonth\":\"2026-09\"")
            .contains("\"aMonthDay\":\"--09-09\"")
            .contains("\"aZoneId\":\"Europe/Berlin\"")
            .contains("\"aZoneOffset\":\"+05:30\"")
            .contains("\"aUuid\":\"f81d4fae-7dec-11d0-a765-00a0c91e6bf6\"")
            .contains("\"aNullOffsetDateTime\":null")
            .contains("\"aNullOffsetTime\":null")
            .contains("\"aNullYear\":null")
            .contains("\"aNullYearMonth\":null")
            .contains("\"aNullMonthDay\":null")
            .contains("\"aNullZoneId\":null")
            .contains("\"aNullZoneOffset\":null")
            .contains("\"aNullUuid\":null");

        final Marshalled<NativeTypes> transported = transport.read(new StringReader(writer.toString()));

        final var unmarshalled = marshaller.unmarshal(transported);

        assertThat(original)
            .isNotSameAs(unmarshalled);

        assertThat(original)
            .isEqualTo(unmarshalled);
    }

    /**
     * Ensure {@link Marshal}lable {@link Uber} can be transported.
     */
    @Test
    void shouldWriteAndReadUberClass() {

        final var uber = new Uber();

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(uber);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<Uber> transported = transport.read(new StringReader(writer.toString()));

        final Uber unmarshalledUber = marshaller.unmarshal(transported);

        assertThat(uber)
            .isNotSameAs(unmarshalledUber);

        assertThat(uber)
            .isEqualTo(unmarshalledUber);
    }

    /**
     * Ensure {@link Marshal}lable object that contains a null or empty {@link java.util.stream.Stream} or
     * {@link Optional} can be transported.
     * <p>
     * Note that null and empty {@link java.util.stream.Stream} and {@link Optional} should not be written to the JSON,
     * and unmarshalling should cope with this. This can not be tested directly without confirming the
     * written JSON. It is confirmed indirectly by the unmarshalled object being slightly different
     * to the original.
     */
    @Test
    void shouldWriteAndReadNullAndEmptySteamOrNullAndEmptyOptional() {

        final var original = new StreamsAndOptionals();

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(original);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<StreamsAndOptionals> transported = transport.read(new StringReader(writer.toString()));

        final var unmarshalled = marshaller.unmarshal(transported);

        assertThat(unmarshalled)
            .isNotSameAs(original);

        assertThat(unmarshalled)
            .isEqualTo(original);
    }

    /**
     * Ensure {@link Marshal}lable object that contains a mix of null and populated {@link java.util.stream.Stream} or
     * {@link Optional} can be transported.
     * <p>
     * Note that null and empty {@link java.util.stream.Stream} and {@link Optional} should not be written to the JSON,
     * and unmarshalling should cope with this. This can not be tested directly without confirming the
     * written JSON. It is confirmed indirectly by the unmarshalled object being slightly different
     * to the original.
     */
    @Test
    void shouldWriteAndReadObjectWithNullStreamAndOptional() {

        final var original = new StreamsAndOptionals(null,
            Arrays.stream(new String[]{"One", "Two"}),
            null,
            Optional.of("Optional"));

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(original);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<StreamsAndOptionals> transported = transport.read(new StringReader(writer.toString()));

        final var unmarshalled = marshaller.unmarshal(transported);

        assertThat(unmarshalled)
            .isNotSameAs(original);

        assertThat(unmarshalled)
            .isEqualTo(original);
    }

    /**
     * Ensure {@link Marshal}lable object that contains multiple unmarshalls will prefer one that has all provided values.
     */
    @Test
    void shouldWriteAndReadObjectPreferringCompleteUnmarshall() {

        final var original = new MultipleUnmarshalls("MyValue", Optional.of("Optional"));

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(original);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<MultipleUnmarshalls> transported = transport.read(new StringReader(writer.toString()));

        final var unmarshalled = marshaller.unmarshal(transported);

        assertThat(original)
            .isNotSameAs(unmarshalled);

        assertThat(original)
            .isEqualTo(unmarshalled);
    }

    /**
     * Ensure {@link Marshal}lable object that contains multiple unmarshalls will defer to one that is missing
     * parameters that should be ignored.
     * <p>
     * Note that null and empty {@link Optional}s should not be written to the JSON, and
     * unmarshalling should cope with this. This can not be tested directly without confirming the
     * written JSON. It is confirmed indirectly by the unmarshalled object being slightly different
     * to the original.
     */
    @Test
    void shouldWriteAndReadObjectUsingIncompleteUnmarshall() {

        final var original = new MultipleUnmarshalls("MyValue", null);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(original);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<MultipleUnmarshalls> transported = transport.read(new StringReader(writer.toString()));

        final var unmarshalled = marshaller.unmarshal(transported);

        assertThat(original)
            .isNotSameAs(unmarshalled);

        assertThat(original)
            .isEqualTo(unmarshalled);
    }

    /**
     * Ensure {@link Marshal}lable object that contains an Object can be transported.
     * <p>
     * Note that the type contained within the object must be transportable. If not, then
     * an exception will be thrown.
     */
    @Test
    void shouldWriteAndReadMarshalledClassWithObject() {

        final var original = new ClassWithObjectProperty(16);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(original);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<ClassWithObjectProperty> transported = transport.read(new StringReader(writer.toString()));

        final ClassWithObjectProperty unmarshalled = marshaller.unmarshal(transported);

        assertThat(original)
            .isNotSameAs(unmarshalled);

        assertThat(original)
            .isEqualTo(unmarshalled);
    }

    @Test
    void shouldWriteAndReadMarshalledClassWithArrayOfClassesWithObject() {

        final var original = new ClassWithArrayOfClassWithObjectProperty(new ArrayList<>());
        original.values().add(new ClassWithObjectProperty(16));
        original.values().add(new ClassWithObjectProperty(32));

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(original);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<ClassWithArrayOfClassWithObjectProperty> transported =
            transport.read(new StringReader(writer.toString()));

        final ClassWithArrayOfClassWithObjectProperty unmarshalled = marshaller.unmarshal(transported);

        assertThat(original)
            .isNotSameAs(unmarshalled);

        assertThat(original)
            .isEqualTo(unmarshalled);
    }

    /**
     * Ensure {@link Marshal}lable object that contains an Object with a null value can be transported.
     */
    @Test
    void shouldWriteAndReadMarshalledClassWithNullObject() {

        final var original = new ClassWithObjectProperty(null);

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(original);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<ClassWithObjectProperty> transported = transport.read(new StringReader(writer.toString()));

        final ClassWithObjectProperty unmarshalled = marshaller.unmarshal(transported);

        assertThat(original)
            .isNotSameAs(unmarshalled);

        assertThat(original)
            .isEqualTo(unmarshalled);
    }

    /**
     * Ensure that an exception is thrown when attempting to read a class that contains an Object whose type
     * is not transportable.
     */
    @Test
    void shouldThrowOnReadMarshalledClassWithObjectThatIsNotTransportable() {

        final var serialized =
            """
                {"@type":"build.base.transport.json.example.ClassWithObjectProperty","object":{"@type":"java.lang.StringBuilder","value":"16"}}
                """;
        final var transport = new JsonTransport();

        final var ex = assertThrows(IllegalStateException.class,
            () -> transport.read(new StringReader(serialized)));
        assertTrue(
            ex.getMessage().startsWith("No Transformer, Codec, or @Marshal-able found for parameter [object]"));
    }

    /**
     * Ensure that an exception is thrown when attempting to read a class that contains an Object whose
     * wrapper JSON is missing the 'value' field.
     */
    @Test
    void shouldThrowOnReadMarshalledClassWithObjectThatWasSerializedWithMissingValue() {

        final var serialized =
            """
                {"@type":"build.base.transport.json.example.ClassWithObjectProperty","object":{"@type":"java.lang.StringBuilder"}}
                """;
        final var transport = new JsonTransport();

        final var ex = assertThrows(JsonAssertionException.class,
            () -> transport.read(new StringReader(serialized)));
        assertTrue(ex.getMessage().startsWith("No member named 'value'"));
    }

    /**
     * Ensure that an exception is thrown when attempting to read a class that contains an Object whose
     * field value is a JSON primitive rather than a nested JSON object.
     */
    @Test
    void shouldThrowOnReadMarshalledClassWithObjectThatIsNotNested() {

        final var serialized =
            """
                {"@type":"build.base.transport.json.example.ClassWithObjectProperty","object":16}
                """;
        final var transport = new JsonTransport();

        final var ex = assertThrows(JsonAssertionException.class,
            () -> transport.read(new StringReader(serialized)));
        assertTrue(ex.getMessage().startsWith("Expected a JSON object but got:"));
    }

    /**
     * Ensure a {@link Marshal}lable {@link Object} requiring a {@link Bound} property can be transported.
     */
    @Test
    void shouldWriteAndReadMarshalledWithBoundObject() {

        final var now = Instant.now();
        final var person = new TemporalPerson(now, "Sebastian");

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(person);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        marshaller.bind(now).universally();

        final Marshalled<TemporalPerson> transported = transport.read(new StringReader(writer.toString()), marshaller);

        final TemporalPerson unmarshalledPerson = marshaller.unmarshal(transported);

        assertThat(person)
            .isNotSameAs(unmarshalledPerson);

        assertThat(person)
            .isEqualTo(unmarshalledPerson);
    }

    /**
     * Ensure a {@link Marshal}lable composed of another {@link Marshal}lable can be transported.
     */
    @Test
    void shouldWriteAndReadComposedObject() {

        final var expense = new Expense();
        expense.add(new ExpenseLine(expense, "Nasi Gorang", 100.0));
        expense.add(new ExpenseLine(expense, "Bintang", 200.0));

        final var marshaller = Marshalling.newMarshaller();
        final var marshalled = marshaller.marshal(expense);

        final var transport = new JsonTransport();
        final var writer = new StringWriter();

        transport.write(marshalled, writer);

        final Marshalled<Expense> transported = transport.read(new StringReader(writer.toString()));

        final Expense unmarshalledExpense = marshaller.unmarshal(transported);

        assertThat(expense)
            .isNotSameAs(unmarshalledExpense);

        assertThat(expense)
            .isEqualTo(unmarshalledExpense);
    }
}
