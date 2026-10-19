package build.base.transport.json.example;

/*-
 * #%L
 * base.build Transport (JSON)
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

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * A marshallable class with a bunch of things to marshal.
 *
 * @author brian.oliver
 * @since Nov-2024
 */
public class Uber {

    private final int anInteger;
    private final Integer anIntegerWrapper;
    private final Integer aNullIntegerWrapper;

    private final long aLong;
    private final Long aLongWrapper;
    private final Long aNullLongWrapper;

    private final byte aByte;
    private final Byte aByteWrapper;
    private final Byte aNullByteWrapper;

    private final boolean aBoolean;
    private final Boolean aBooleanWrapper;
    private final Boolean aNullBooleanWrapper;

    private final String aString;
    private final String aNullString;

    private final char aCharacter;
    private final Character aCharacterWrapper;
    private final Character aNullCharacterWrapper;

    private final float aFloat;
    private final Float aFloatWrapper;
    private final Float aNullFloatWrapper;

    private final double aDouble;
    private final Double aDoubleWrapper;
    private final Double aNullDoubleWrapper;

    private final BigDecimal aBigDecimal;
    private final BigDecimal aNullBigDecimal;

    private final BigInteger aBigInteger;
    private final BigInteger aNullBigInteger;

    private final Instant anInstant;
    private final Instant aNullInstant;

    private final LocalDate aLocalDate;
    private final LocalDate aNullLocalDate;

    private final LocalTime aLocalTime;
    private final LocalTime aNullLocalTime;

    private final LocalDateTime aLocalDateTime;
    private final LocalDateTime aNullLocalDateTime;

    private final ZonedDateTime aZonedDateTime;
    private final ZonedDateTime aNullZonedDateTime;

    private final Duration aDuration;
    private final Duration aNullDuration;

    private final Period aPeriod;
    private final Period aNullPeriod;

    private final Date aDate;
    private final Date aNullDate;

    private final Instant anotherInstant;
    private final Instant aNullAnotherInstant;

    public Uber() {
        this.anInteger = 1;
        this.anIntegerWrapper = Integer.valueOf(2);
        this.aNullIntegerWrapper = null;

        this.aLong = 3L;
        this.aLongWrapper = 4L;
        this.aNullLongWrapper = null;

        this.aByte = (byte) 5;
        this.aByteWrapper = (byte) 6;
        this.aNullByteWrapper = null;

        this.aBoolean = true;
        this.aBooleanWrapper = Boolean.TRUE;
        this.aNullBooleanWrapper = null;

        this.aString = "G'day Mate!";
        this.aNullString = null;

        this.aCharacter = 'x';
        this.aCharacterWrapper = 'y';
        this.aNullCharacterWrapper = null;

        this.aFloat = 3.14f;
        this.aFloatWrapper = 3.14f;
        this.aNullFloatWrapper = null;

        this.aDouble = 3.141572d;
        this.aDoubleWrapper = 3.141572d;
        this.aNullDoubleWrapper = null;

        this.aBigDecimal = BigDecimal.valueOf(Math.PI);
        this.aNullBigDecimal = null;

        this.aBigInteger = BigInteger.valueOf(Long.MAX_VALUE);
        this.aNullBigInteger = null;

        this.anInstant = Instant.ofEpochSecond(1000);
        this.aNullInstant = null;

        this.aLocalDate = LocalDate.of(2024, 11, 1);
        this.aNullLocalDate = null;

        this.aLocalTime = LocalTime.of(14, 31, 7);
        this.aNullLocalTime = null;

        this.aLocalDateTime = LocalDateTime.of(2024, 11, 1, 14, 31, 7);
        this.aNullLocalDateTime = null;

        this.aZonedDateTime = ZonedDateTime.of(2024, 11, 1, 14, 31, 7, 0, ZoneId.of("UTC"));
        this.aNullZonedDateTime = null;

        this.aDuration = Duration.ofSeconds(1000);
        this.aNullDuration = null;

        this.aPeriod = Period.of(1, 2, 3);
        this.aNullPeriod = null;

        this.aDate = new Date(2024, Calendar.NOVEMBER, 1);
        this.aNullDate = null;

        this.anotherInstant = Instant.ofEpochSecond(2000);
        this.aNullAnotherInstant = null;
    }

    @Unmarshal
    public Uber(final int anInteger,
                final Integer anIntegerWrapper,
                final Integer aNullIntegerWrapper,
                final long aLong,
                final Long aLongWrapper,
                final Long aNullLongWrapper,
                final byte aByte,
                final Byte aByteWrapper,
                final Byte aNullByteWrapper,
                final boolean aBoolean,
                final Boolean aBooleanWrapper,
                final Boolean aNullBooleanWrapper,
                final String aString,
                final String aNullString,
                final char aCharacter,
                final Character aCharacterWrapper,
                final Character aNullCharacterWrapper,
                final float aFloat,
                final Float aFloatWrapper,
                final Float aNullFloatWrapper,
                final double aDouble,
                final Double aDoubleWrapper,
                final Double aNullDoubleWrapper,
                final BigDecimal aBigDecimal,
                final BigDecimal aNullBigDecimal,
                final BigInteger aBigInteger,
                final BigInteger aNullBigInteger,
                final Instant anInstant,
                final Instant aNullInstant,
                final LocalDate aLocalDate,
                final LocalDate aNullLocalDate,
                final LocalTime aLocalTime,
                final LocalTime aNullLocalTime,
                final LocalDateTime aLocalDateTime,
                final LocalDateTime aNullLocalDateTime,
                final ZonedDateTime aZonedDateTime,
                final ZonedDateTime aNullZonedDateTime,
                final Duration aDuration,
                final Duration aNullDuration,
                final Period aPeriod,
                final Period aNullPeriod,
                final Date aDate,
                final Date aNullDate,
                final Instant anotherInstant,
                final Instant aNullAnotherInstant) {

        this.anInteger = anInteger;
        this.anIntegerWrapper = anIntegerWrapper;
        this.aNullIntegerWrapper = aNullIntegerWrapper;

        this.aLong = aLong;
        this.aLongWrapper = aLongWrapper;
        this.aNullLongWrapper = aNullLongWrapper;

        this.aByte = aByte;
        this.aByteWrapper = aByteWrapper;
        this.aNullByteWrapper = aNullByteWrapper;

        this.aBoolean = aBoolean;
        this.aBooleanWrapper = aBooleanWrapper;
        this.aNullBooleanWrapper = aNullBooleanWrapper;

        this.aString = aString;
        this.aNullString = aNullString;

        this.aCharacter = aCharacter;
        this.aCharacterWrapper = aCharacterWrapper;
        this.aNullCharacterWrapper = aNullCharacterWrapper;

        this.aFloat = aFloat;
        this.aFloatWrapper = aFloatWrapper;
        this.aNullFloatWrapper = aNullFloatWrapper;

        this.aDouble = aDouble;
        this.aDoubleWrapper = aDoubleWrapper;
        this.aNullDoubleWrapper = aNullDoubleWrapper;

        this.aBigDecimal = aBigDecimal;
        this.aNullBigDecimal = aNullBigDecimal;

        this.aBigInteger = aBigInteger;
        this.aNullBigInteger = aNullBigInteger;

        this.anInstant = anInstant;
        this.aNullInstant = aNullInstant;

        this.aLocalDate = aLocalDate;
        this.aNullLocalDate = aNullLocalDate;

        this.aLocalTime = aLocalTime;
        this.aNullLocalTime = aNullLocalTime;

        this.aLocalDateTime = aLocalDateTime;
        this.aNullLocalDateTime = aNullLocalDateTime;

        this.aZonedDateTime = aZonedDateTime;
        this.aNullZonedDateTime = aNullZonedDateTime;

        this.aDuration = aDuration;
        this.aNullDuration = aNullDuration;

        this.aPeriod = aPeriod;
        this.aNullPeriod = aNullPeriod;

        this.aDate = aDate;
        this.aNullDate = aNullDate;

        this.anotherInstant = anotherInstant;
        this.aNullAnotherInstant = aNullAnotherInstant;
    }

    @Marshal
    public void destructor(final Out<Integer> anInteger,
                           final Out<Integer> anIntegerWrapper,
                           final Out<Integer> aNullIntegerWrapper,
                           final Out<Long> aLong,
                           final Out<Long> aLongWrapper,
                           final Out<Long> aNullLongWrapper,
                           final Out<Byte> aByte,
                           final Out<Byte> aByteWrapper,
                           final Out<Byte> aNullByteWrapper,
                           final Out<Boolean> aBoolean,
                           final Out<Boolean> aBooleanWrapper,
                           final Out<Boolean> aNullBooleanWrapper,
                           final Out<String> aString,
                           final Out<String> aNullString,
                           final Out<Character> aCharacter,
                           final Out<Character> aCharacterWrapper,
                           final Out<Character> aNullCharacterWrapper,
                           final Out<Float> aFloat,
                           final Out<Float> aFloatWrapper,
                           final Out<Float> aNullFloatWrapper,
                           final Out<Double> aDouble,
                           final Out<Double> aDoubleWrapper,
                           final Out<Double> aNullDoubleWrapper,
                           final Out<BigDecimal> aBigDecimal,
                           final Out<BigDecimal> aNullBigDecimal,
                           final Out<BigInteger> aBigInteger,
                           final Out<BigInteger> aNullBigInteger,
                           final Out<Instant> anInstant,
                           final Out<Instant> aNullInstant,
                           final Out<LocalDate> aLocalDate,
                           final Out<LocalDate> aNullLocalDate,
                           final Out<LocalTime> aLocalTime,
                           final Out<LocalTime> aNullLocalTime,
                           final Out<LocalDateTime> aLocalDateTime,
                           final Out<LocalDateTime> aNullLocalDateTime,
                           final Out<ZonedDateTime> aZonedDateTime,
                           final Out<ZonedDateTime> aNullZonedDateTime,
                           final Out<Duration> aDuration,
                           final Out<Duration> aNullDuration,
                           final Out<Period> aPeriod,
                           final Out<Period> aNullPeriod,
                           final Out<Date> aDate,
                           final Out<Date> aNullDate,
                           final Out<Instant> anotherInstant,
                           final Out<Instant> aNullAnotherInstant) {

        anInteger.set(this.anInteger);
        anIntegerWrapper.set(this.anIntegerWrapper);
        aNullIntegerWrapper.set(this.aNullIntegerWrapper);

        aLong.set(this.aLong);
        aLongWrapper.set(this.aLongWrapper);
        aNullLongWrapper.set(this.aNullLongWrapper);

        aByte.set(this.aByte);
        aByteWrapper.set(this.aByteWrapper);
        aNullByteWrapper.set(this.aNullByteWrapper);

        aBoolean.set(this.aBoolean);
        aBooleanWrapper.set(this.aBooleanWrapper);
        aNullBooleanWrapper.set(this.aNullBooleanWrapper);

        aString.set(this.aString);
        aNullString.set(this.aNullString);

        aCharacter.set(this.aCharacter);
        aCharacterWrapper.set(this.aCharacterWrapper);
        aNullCharacterWrapper.set(this.aNullCharacterWrapper);

        aFloat.set(this.aFloat);
        aFloatWrapper.set(this.aFloatWrapper);
        aNullFloatWrapper.set(this.aNullFloatWrapper);

        aDouble.set(this.aDouble);
        aDoubleWrapper.set(this.aDoubleWrapper);
        aNullDoubleWrapper.set(this.aNullDoubleWrapper);

        aBigDecimal.set(this.aBigDecimal);
        aNullBigDecimal.set(this.aNullBigDecimal);

        aBigInteger.set(this.aBigInteger);
        aNullBigInteger.set(this.aNullBigInteger);

        anInstant.set(this.anInstant);
        aNullInstant.set(this.aNullInstant);

        aLocalDate.set(this.aLocalDate);
        aNullLocalDate.set(this.aNullLocalDate);

        aLocalTime.set(this.aLocalTime);
        aNullLocalTime.set(this.aNullLocalTime);

        aLocalDateTime.set(this.aLocalDateTime);
        aNullLocalDateTime.set(this.aNullLocalDateTime);

        aZonedDateTime.set(this.aZonedDateTime);
        aNullZonedDateTime.set(this.aNullZonedDateTime);

        aDuration.set(this.aDuration);
        aNullDuration.set(this.aNullDuration);

        aPeriod.set(this.aPeriod);
        aNullPeriod.set(this.aNullPeriod);

        aDate.set(this.aDate);
        aNullDate.set(this.aNullDate);

        anotherInstant.set(this.anotherInstant);
        aNullAnotherInstant.set(this.aNullAnotherInstant);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final Uber uber)) {
            return false;
        }
        return anInteger == uber.anInteger && aLong == uber.aLong && aByte == uber.aByte && aBoolean == uber.aBoolean
            && aCharacter == uber.aCharacter && Float.compare(aFloat, uber.aFloat) == 0
            && Double.compare(aDouble, uber.aDouble) == 0 && Objects.equals(anIntegerWrapper,
            uber.anIntegerWrapper) && Objects.equals(aNullIntegerWrapper, uber.aNullIntegerWrapper)
            && Objects.equals(aLongWrapper, uber.aLongWrapper) && Objects.equals(aNullLongWrapper,
            uber.aNullLongWrapper) && Objects.equals(aByteWrapper, uber.aByteWrapper) && Objects.equals(
            aNullByteWrapper, uber.aNullByteWrapper) && Objects.equals(aBooleanWrapper, uber.aBooleanWrapper)
            && Objects.equals(aNullBooleanWrapper, uber.aNullBooleanWrapper) && Objects.equals(aString,
            uber.aString) && Objects.equals(aNullString, uber.aNullString) && Objects.equals(
            aCharacterWrapper, uber.aCharacterWrapper) && Objects.equals(aNullCharacterWrapper,
            uber.aNullCharacterWrapper) && Objects.equals(aFloatWrapper, uber.aFloatWrapper)
            && Objects.equals(aNullFloatWrapper, uber.aNullFloatWrapper) && Objects.equals(
            aDoubleWrapper, uber.aDoubleWrapper) && Objects.equals(aNullDoubleWrapper, uber.aNullDoubleWrapper)
            && Objects.equals(aBigDecimal, uber.aBigDecimal) && Objects.equals(aNullBigDecimal,
            uber.aNullBigDecimal) && Objects.equals(aBigInteger, uber.aBigInteger) && Objects.equals(
            aNullBigInteger, uber.aNullBigInteger) && anInstant.equals(uber.anInstant) && Objects.equals(
            aNullInstant, uber.aNullInstant) && aLocalDate.equals(uber.aLocalDate) && Objects.equals(
            aNullLocalDate, uber.aNullLocalDate) && aLocalTime.equals(uber.aLocalTime) && Objects.equals(
            aNullLocalTime, uber.aNullLocalTime) && aLocalDateTime.equals(uber.aLocalDateTime) && Objects.equals(
            aNullLocalDateTime, uber.aNullLocalDateTime) && aZonedDateTime.equals(uber.aZonedDateTime)
            && Objects.equals(aNullZonedDateTime, uber.aNullZonedDateTime) && aDuration.equals(uber.aDuration)
            && Objects.equals(aNullDuration, uber.aNullDuration) && aPeriod.equals(uber.aPeriod) &&
            Objects.equals(aNullPeriod, uber.aNullPeriod) && aDate.equals(uber.aDate) && Objects.equals(aNullDate,
            uber.aNullDate) && anotherInstant.equals(uber.anotherInstant) && Objects.equals(aNullAnotherInstant,
            uber.aNullAnotherInstant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(anInteger, anIntegerWrapper, aNullIntegerWrapper, aLong, aLongWrapper, aNullLongWrapper,
            aByte,
            aByteWrapper, aNullByteWrapper, aBoolean, aBooleanWrapper, aNullBooleanWrapper, aString, aNullString,
            aCharacter, aCharacterWrapper, aNullCharacterWrapper, aFloat, aFloatWrapper, aNullFloatWrapper, aDouble,
            aDoubleWrapper, aNullDoubleWrapper, aBigDecimal, aNullBigDecimal, aBigInteger, aNullBigInteger, anInstant,
            aNullInstant, aLocalDate, aNullLocalDate, aLocalTime, aNullLocalTime, aLocalDateTime, aNullLocalDateTime,
            aZonedDateTime, aNullZonedDateTime, aDuration, aNullDuration, aPeriod, aNullPeriod, aDate, aNullDate,
            anotherInstant, aNullAnotherInstant);
    }

    static {
        Marshalling.register(Uber.class, MethodHandles.lookup());
    }
}
