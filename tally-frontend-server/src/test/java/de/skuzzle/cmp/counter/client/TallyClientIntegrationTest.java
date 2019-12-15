package de.skuzzle.cmp.counter.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import de.skuzzle.cmp.counter.client.BackendClient;
import de.skuzzle.cmp.counter.client.RestTallyIncrement;
import de.skuzzle.cmp.counter.client.RestTallySheet;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK, properties = {
        "cmp.backend.url=http://localhost:6565",
        "cmp.backend.healthUrl=http://not.used.in.this.test" })
@AutoConfigureStubRunner(ids = "de.skuzzle.tally:tally-backend:+:stubs:6565",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class TallyClientIntegrationTest {

    @Autowired
    private BackendClient tallyClient;

    @Test
    void testCreateTallySheet() {
        final var apiResponse = tallyClient.createNewTallySheet("name");

        final RestTallySheet tallySheet = apiResponse.getTallySheet();
        assertThat(tallySheet.getCreateDateUTC()).isEqualTo(LocalDateTime.of(1987, 9, 12, 11, 11, 0, 123000000));
        assertThat(tallySheet.getLastModifiedDateUTC()).isEqualTo(LocalDateTime.of(1987, 9, 12, 11, 11, 0, 123000000));
    }

    @Test
    void incrementTallySheet() {
        final var increment = RestTallyIncrement.createNew("Description",
                LocalDateTime.of(2019, 04, 12, 11, 21, 32, 123000000), Set.of("tag1", "tag2"));

        tallyClient.increment("adminKey1", increment);
    }

    @Test
    void testGetUnknownTallySheet() {
        assertThatExceptionOfType(HttpStatusCodeException.class)
                .isThrownBy(() -> tallyClient.getTallySheet("unknownPublicKey"))
                .matches(e -> e.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    void testGetExistingTallySheet() {
        final var apiResponse = tallyClient.getTallySheet("publicKey1");
        assertThat(apiResponse.getTallySheet().getPublicKey()).isEqualTo("publicKey1");
    }

    @Test
    void testDeleteUnknownTallySheet() {
        assertThatExceptionOfType(HttpStatusCodeException.class)
                .isThrownBy(() -> tallyClient.deleteTallySheet("unknownAdminKey"))
                .matches(e -> e.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    void testDeleteTallySheet() {
        tallyClient.deleteTallySheet("adminKey1");
    }

    @Test
    void testDeleteIncrement() throws Exception {
        tallyClient.deleteIncrement("adminKey2", "incrementId");
    }

    @Test
    void testDeleteUnknownIncrement() throws Exception {
        assertThatExceptionOfType(HttpStatusCodeException.class)
                .isThrownBy(() -> tallyClient.deleteIncrement("adminKey1", "unknownIncrementId"))
                .matches(e -> e.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Test
    void testAssignToCurrentUser() throws Exception {
        tallyClient.assignToCurrentUser("adminKey3");
    }

    @Test
    void testListTallySheets() throws Exception {
        final var apiResponse = tallyClient.listTallySheets();
        assertThat(apiResponse.getTallySheets()).hasSize(2);
    }

    @Test
    void testGetMetaInformation() throws Exception {
        final var apiResponse = tallyClient.getMetaInfo();
        assertThat(apiResponse.getTotalTallySheetCount()).isEqualTo(3);
    }
}
