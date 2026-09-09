package build.base.transport.json.example;

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

import build.base.marshalling.Marshal;
import build.base.marshalling.Marshalling;
import build.base.marshalling.Out;
import build.base.marshalling.Unmarshal;

import java.lang.invoke.MethodHandles;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * A marshallable class exercising the natively-supported {@code short}, {@link java.time} and {@link UUID} codecs,
 * including a {@code null} counterpart for every reference-typed value.
 *
 * @author reed.vonredwitz
 * @since Sep-2026
 */
public class NativeTypes {

    private final short aShort;
    private final Short aShortWrapper;
    private final Short aNullShortWrapper;

    private final OffsetDateTime anOffsetDateTime;
    private final OffsetDateTime aNullOffsetDateTime;

    private final OffsetTime anOffsetTime;
    private final OffsetTime aNullOffsetTime;

    private final Year aYear;
    private final Year aNullYear;

    private final YearMonth aYearMonth;
    private final YearMonth aNullYearMonth;

    private final MonthDay aMonthDay;
    private final MonthDay aNullMonthDay;

    private final ZoneId aZoneId;
    private final ZoneId aNullZoneId;

    private final ZoneOffset aZoneOffset;
    private final ZoneOffset aNullZoneOffset;

    private final UUID aUuid;
    private final UUID aNullUuid;

    public NativeTypes() {
        this(Short.MIN_VALUE, Short.MAX_VALUE, null,
            OffsetDateTime.of(2026, 9, 9, 14, 31, 7, 0, ZoneOffset.ofHours(2)), null,
            OffsetTime.of(14, 31, 7, 0, ZoneOffset.ofHours(-5)), null,
            Year.of(2026), null,
            YearMonth.of(2026, 9), null,
            MonthDay.of(9, 9), null,
            ZoneId.of("Europe/Berlin"), null,
            ZoneOffset.ofHoursMinutes(5, 30), null,
            UUID.fromString("f81d4fae-7dec-11d0-a765-00a0c91e6bf6"), null);
    }

    @Unmarshal
    public NativeTypes(final short aShort,
                       final Short aShortWrapper,
                       final Short aNullShortWrapper,
                       final OffsetDateTime anOffsetDateTime,
                       final OffsetDateTime aNullOffsetDateTime,
                       final OffsetTime anOffsetTime,
                       final OffsetTime aNullOffsetTime,
                       final Year aYear,
                       final Year aNullYear,
                       final YearMonth aYearMonth,
                       final YearMonth aNullYearMonth,
                       final MonthDay aMonthDay,
                       final MonthDay aNullMonthDay,
                       final ZoneId aZoneId,
                       final ZoneId aNullZoneId,
                       final ZoneOffset aZoneOffset,
                       final ZoneOffset aNullZoneOffset,
                       final UUID aUuid,
                       final UUID aNullUuid) {

        this.aShort = aShort;
        this.aShortWrapper = aShortWrapper;
        this.aNullShortWrapper = aNullShortWrapper;
        this.anOffsetDateTime = anOffsetDateTime;
        this.aNullOffsetDateTime = aNullOffsetDateTime;
        this.anOffsetTime = anOffsetTime;
        this.aNullOffsetTime = aNullOffsetTime;
        this.aYear = aYear;
        this.aNullYear = aNullYear;
        this.aYearMonth = aYearMonth;
        this.aNullYearMonth = aNullYearMonth;
        this.aMonthDay = aMonthDay;
        this.aNullMonthDay = aNullMonthDay;
        this.aZoneId = aZoneId;
        this.aNullZoneId = aNullZoneId;
        this.aZoneOffset = aZoneOffset;
        this.aNullZoneOffset = aNullZoneOffset;
        this.aUuid = aUuid;
        this.aNullUuid = aNullUuid;
    }

    @Marshal
    public void destructor(final Out<Short> aShort,
                           final Out<Short> aShortWrapper,
                           final Out<Short> aNullShortWrapper,
                           final Out<OffsetDateTime> anOffsetDateTime,
                           final Out<OffsetDateTime> aNullOffsetDateTime,
                           final Out<OffsetTime> anOffsetTime,
                           final Out<OffsetTime> aNullOffsetTime,
                           final Out<Year> aYear,
                           final Out<Year> aNullYear,
                           final Out<YearMonth> aYearMonth,
                           final Out<YearMonth> aNullYearMonth,
                           final Out<MonthDay> aMonthDay,
                           final Out<MonthDay> aNullMonthDay,
                           final Out<ZoneId> aZoneId,
                           final Out<ZoneId> aNullZoneId,
                           final Out<ZoneOffset> aZoneOffset,
                           final Out<ZoneOffset> aNullZoneOffset,
                           final Out<UUID> aUuid,
                           final Out<UUID> aNullUuid) {

        aShort.set(this.aShort);
        aShortWrapper.set(this.aShortWrapper);
        aNullShortWrapper.set(this.aNullShortWrapper);
        anOffsetDateTime.set(this.anOffsetDateTime);
        aNullOffsetDateTime.set(this.aNullOffsetDateTime);
        anOffsetTime.set(this.anOffsetTime);
        aNullOffsetTime.set(this.aNullOffsetTime);
        aYear.set(this.aYear);
        aNullYear.set(this.aNullYear);
        aYearMonth.set(this.aYearMonth);
        aNullYearMonth.set(this.aNullYearMonth);
        aMonthDay.set(this.aMonthDay);
        aNullMonthDay.set(this.aNullMonthDay);
        aZoneId.set(this.aZoneId);
        aNullZoneId.set(this.aNullZoneId);
        aZoneOffset.set(this.aZoneOffset);
        aNullZoneOffset.set(this.aNullZoneOffset);
        aUuid.set(this.aUuid);
        aNullUuid.set(this.aNullUuid);
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final NativeTypes that)) {
            return false;
        }
        return this.aShort == that.aShort
            && Objects.equals(this.aShortWrapper, that.aShortWrapper)
            && Objects.equals(this.aNullShortWrapper, that.aNullShortWrapper)
            && Objects.equals(this.anOffsetDateTime, that.anOffsetDateTime)
            && Objects.equals(this.aNullOffsetDateTime, that.aNullOffsetDateTime)
            && Objects.equals(this.anOffsetTime, that.anOffsetTime)
            && Objects.equals(this.aNullOffsetTime, that.aNullOffsetTime)
            && Objects.equals(this.aYear, that.aYear)
            && Objects.equals(this.aNullYear, that.aNullYear)
            && Objects.equals(this.aYearMonth, that.aYearMonth)
            && Objects.equals(this.aNullYearMonth, that.aNullYearMonth)
            && Objects.equals(this.aMonthDay, that.aMonthDay)
            && Objects.equals(this.aNullMonthDay, that.aNullMonthDay)
            && Objects.equals(this.aZoneId, that.aZoneId)
            && Objects.equals(this.aNullZoneId, that.aNullZoneId)
            && Objects.equals(this.aZoneOffset, that.aZoneOffset)
            && Objects.equals(this.aNullZoneOffset, that.aNullZoneOffset)
            && Objects.equals(this.aUuid, that.aUuid)
            && Objects.equals(this.aNullUuid, that.aNullUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.aShort, this.aShortWrapper, this.aNullShortWrapper,
            this.anOffsetDateTime, this.aNullOffsetDateTime, this.anOffsetTime, this.aNullOffsetTime,
            this.aYear, this.aNullYear, this.aYearMonth, this.aNullYearMonth, this.aMonthDay, this.aNullMonthDay,
            this.aZoneId, this.aNullZoneId, this.aZoneOffset, this.aNullZoneOffset, this.aUuid, this.aNullUuid);
    }

    static {
        Marshalling.register(NativeTypes.class, MethodHandles.lookup());
    }
}
