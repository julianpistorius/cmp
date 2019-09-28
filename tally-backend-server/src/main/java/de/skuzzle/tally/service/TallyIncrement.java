package de.skuzzle.tally.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.google.common.base.Preconditions;

public final class TallyIncrement {

    private final String id;

    private final Set<String> tags;
    private final String description;
    private final LocalDateTime createDateUTC;
    private final LocalDateTime incrementDateUTC;

    private TallyIncrement(String id, Set<String> tags, String description, LocalDateTime createDateUTC,
            LocalDateTime incrementDateUTC) {
        this.id = id;
        this.tags = tags;
        this.description = description;
        this.createDateUTC = createDateUTC;
        this.incrementDateUTC = incrementDateUTC;
    }

    public static TallyIncrement newIncrement(String description, LocalDateTime incrementDateUTC,
            Collection<String> tags) {
        Preconditions.checkArgument(description != null, "description must not be null");
        Preconditions.checkArgument(incrementDateUTC != null, "incrementDateUTC must not be null");
        Preconditions.checkArgument(tags != null, "tags must not be null");

        return new TallyIncrement(
                UUID.randomUUID().toString(),
                Set.copyOf(tags),
                description,
                UTCDateTimeProvider.getInstance().getNowLocal(),
                incrementDateUTC);
    }

    public String getId() {
        return id;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public String getDescription() {
        return this.description;
    }

    public LocalDateTime getCreateDateUTC() {
        return this.createDateUTC;
    }

    public LocalDateTime getIncrementDateUTC() {
        return incrementDateUTC;
    }

}
