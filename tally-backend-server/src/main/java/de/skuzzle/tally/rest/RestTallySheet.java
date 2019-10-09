package de.skuzzle.tally.rest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import de.skuzzle.tally.service.ShallowTallySheet;

public class RestTallySheet {

    private final String name;
    private final String adminKey;
    private final String publicKey;

    // dates in UTC+0
    private final LocalDateTime createDateUTC;
    private final LocalDateTime lastModifiedDateUTC;

    private RestTallySheet(String name, String adminKey, String publicKey,
            LocalDateTime createDateUTC, LocalDateTime lastModifiedDateUTC) {
        this.name = name;
        this.adminKey = adminKey;
        this.publicKey = publicKey;
        this.createDateUTC = createDateUTC;
        this.lastModifiedDateUTC = lastModifiedDateUTC;
    }

    public static List<RestTallySheet> fromDomainObjects(List<? extends ShallowTallySheet> tallySheets) {
        Preconditions.checkArgument(tallySheets != null, "tallySheets must not be null");
        return tallySheets.stream()
                .map(RestTallySheet::fromDomainObject)
                .collect(Collectors.toList());
    }
    
    public static RestTallySheet fromDomainObject(ShallowTallySheet tallySheet) {
        Preconditions.checkArgument(tallySheet != null, "tallySheet must not be null");
        return new RestTallySheet(tallySheet.getName(),
                tallySheet.getAdminKey().orElse(null),
                tallySheet.getPublicKey(),
                tallySheet.getCreateDateUTC(),
                tallySheet.getLastModifiedDateUTC());
    }

    public String getName() {
        return this.name;
    }

    public String getAdminKey() {
        return this.adminKey;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public LocalDateTime getCreateDateUTC() {
        return this.createDateUTC;
    }

    public LocalDateTime getLastModifiedDateUTC() {
        return this.lastModifiedDateUTC;
    }

}
