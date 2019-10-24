package de.skuzzle.tally.frontend.client;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;

public class TestResponses {

    public static TallySheetResponseBuilder tallySheet() {
        return new TallySheetResponseBuilder();
    }

    public static TallySheetsResponseBuilder emptyTallySheets() {
        return new TallySheetsResponseBuilder();
    }

    public static class TallySheetsResponseBuilder {
        private final List<RestTallySheet> tallySheets = new ArrayList<>();

        public TallySheetsResponseBuilder addTallySheet(TallySheetResponseBuilder tallySheetBuilder) {
            tallySheets.add(tallySheetBuilder.toTallySheet());
            return this;
        }

        public TallyResult<RestTallySheetsReponse> toResponse() {
            final RestTallySheetsReponse tallySheetsReponse = new RestTallySheetsReponse(tallySheets);
            return TallyResult.success(HttpStatus.OK, tallySheetsReponse);
        }
    }

    public static class TallySheetResponseBuilder {
        private String name = "name";
        private String adminKey = "adminKey";
        private String publicKey = "publicKey";

        private LocalDateTime createDateUTC = LocalDateTime.of(1970, 1, 1, 0, 0);
        private LocalDateTime lastModifiedDateUTC = LocalDateTime.of(1970, 1, 1, 0, 0);
        private boolean assignableToCurrentUser = false;
        private int totalCount = 0;
        private final List<RestTallyIncrement> increments = new ArrayList<>();

        public TallySheetResponseBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public TallySheetResponseBuilder withAdminKey(String adminKey) {
            this.adminKey = adminKey;
            return this;
        }

        public TallySheetResponseBuilder withPublicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public TallySheetResponseBuilder withCreateDateUTC(LocalDateTime createDateUTC) {
            this.createDateUTC = createDateUTC;
            return this;
        }

        public TallySheetResponseBuilder withLastModifiedDateUTC(LocalDateTime lastModifiedDateUTC) {
            this.lastModifiedDateUTC = lastModifiedDateUTC;
            return this;
        }

        public TallySheetResponseBuilder withAssignableToCurrentUser(boolean assignableToCurrentUser) {
            this.assignableToCurrentUser = assignableToCurrentUser;
            return this;
        }

        public TallySheetResponseBuilder withTotalCount(int totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public TallySheetResponseBuilder addIncrement(String id, String description, LocalDateTime incrementDateUTC,
                String... tags) {
            this.increments.add(new RestTallyIncrement(id, description, Set.of(tags), incrementDateUTC));
            return this;
        }

        public RestTallySheet toTallySheet() {
            return new RestTallySheet(name,
                    adminKey,
                    publicKey,
                    createDateUTC,
                    lastModifiedDateUTC,
                    assignableToCurrentUser,
                    totalCount);
        }

        public TallyResult<RestTallyResponse> toResponse() {
            final RestTallySheet tallySheet = toTallySheet();
            final RestIncrements increments = new RestIncrements(this.increments, 0, 0);
            final RestTallyResponse restTallyResponse = new RestTallyResponse(tallySheet, increments);
            return TallyResult.success(HttpStatus.OK, restTallyResponse);
        }
    }
}
