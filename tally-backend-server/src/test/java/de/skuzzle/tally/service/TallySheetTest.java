package de.skuzzle.tally.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TallySheetTest {

    @Test
    void testIsNotAssignableToAnonymous() throws Exception {
        final UserId anonymous = UserId.unknown("foo");
        final UserId anonymous2 = UserId.unknown("foo2");
        final TallySheet sheet = TallySheet.newTallySheet(anonymous, "name", "adminKey", "publicKey");
        assertThat(sheet.isAssignableTo(anonymous)).isFalse();
        assertThatExceptionOfType(UserAssignmentException.class).isThrownBy(() -> sheet.assignToUser(anonymous2));
    }

    @Test
    void testIsAlreadyAssigned() throws Exception {
        final UserId authenticated = UserId.wellKnown("google", "foo@gmail.com");
        final UserId authenticated2 = UserId.wellKnown("google", "bar@gmail.com");
        final TallySheet sheet = TallySheet.newTallySheet(authenticated, "name", "adminKey", "publicKey");
        assertThat(sheet.isAssignableTo(authenticated2)).isFalse();
        assertThatExceptionOfType(UserAssignmentException.class).isThrownBy(() -> sheet.assignToUser(authenticated2));
    }

    @Test
    void testIsNotAssignableToAuthenticated() throws Exception {
        final UserId anonymous = UserId.unknown("foo");
        final UserId authenticated = UserId.wellKnown("google", "foo@gmail.com");
        final TallySheet sheet = TallySheet.newTallySheet(anonymous, "name", "adminKey", "publicKey");
        assertThat(sheet.isAssignableTo(authenticated)).isTrue();
    }

    @Test
    void testQueryAll() throws Exception {
        final TallySheet sheet = createWithIncrements(25,
                LocalDate.of(2019, 01, 01).atStartOfDay(),
                LocalDate.of(2019, 02, 01).atStartOfDay());

        final IncrementQueryResult queryResult = sheet.selectIncrements(IncrementQuery.all());
        assertThat(queryResult.getTotal()).isEqualTo(25);
        assertThat(queryResult.getIncrements().size()).isEqualTo(25);
    }

    @Test
    void testQueryFirstPage() throws Exception {
        final TallySheet sheet = createWithIncrements(25,
                LocalDate.of(2019, 01, 01).atStartOfDay(),
                LocalDate.of(2019, 02, 01).atStartOfDay());

        final IncrementQueryResult queryResult = sheet.selectIncrements(IncrementQuery.all()
                .start(0)
                .maxResults(10));
        assertThat(queryResult.getIncrements()).first().extracting(TallyIncrement::getId).isEqualTo("0");
        assertThat(queryResult.getIncrements()).last().extracting(TallyIncrement::getId).isEqualTo("9");
    }

    @Test
    void testQuerySecondPage() throws Exception {
        final TallySheet sheet = createWithIncrements(25,
                LocalDate.of(2019, 01, 01).atStartOfDay(),
                LocalDate.of(2019, 02, 01).atStartOfDay());

        final IncrementQueryResult queryResult = sheet.selectIncrements(IncrementQuery.all()
                .start(10)
                .maxResults(10));
        assertThat(queryResult.getIncrements()).first().extracting(TallyIncrement::getId).isEqualTo("10");
        assertThat(queryResult.getIncrements()).last().extracting(TallyIncrement::getId).isEqualTo("19");
    }

    @Test
    void testQueryFilter() throws Exception {
        final TallySheet sheet = createWithIncrements(25,
                LocalDate.of(2019, 01, 01).atStartOfDay(),
                LocalDate.of(2019, 02, 01).atStartOfDay());

        final IncrementQueryResult queryResult = sheet.selectIncrements(IncrementQuery.all()
                .from(LocalDate.of(2019, 01, 05).atStartOfDay())
                .until(LocalDate.of(2019, 01, 11).atStartOfDay()));
        assertThat(queryResult.getIncrements()).hasSize(5);
    }

    private TallySheet createWithIncrements(int count, LocalDateTime from, LocalDateTime until) {
        final TallySheet sheet = TallySheet.newTallySheet(UserId.wellKnown("google", "foo@gmail.com"), "name",
                "adminKey", "publicKey");
        final long days = ChronoUnit.DAYS.between(from, until);
        final long avgBetween = days / count;
        for (int i = 0; i < count; i++) {
            final LocalDateTime incrementDateUTC = from.plusDays(i * avgBetween);

            sheet.incrementWith(TallyIncrement.newIncrementWithId("" + i, "description", incrementDateUTC, Set.of()));
        }
        return sheet;
    }
}
