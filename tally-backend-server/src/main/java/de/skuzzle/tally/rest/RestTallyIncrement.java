package de.skuzzle.tally.rest;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import de.skuzzle.tally.service.TallyIncrement;

public class RestTallyIncrement {

    private final String description;
    private final Set<String> tags;
    private final LocalDateTime incrementDateUTC;

    public RestTallyIncrement(String description, Collection<String> tags, LocalDateTime incrementDateUTC) {
        this.description = description;
        this.tags = Set.copyOf(tags);
        this.incrementDateUTC = incrementDateUTC;
    }

    public static List<RestTallyIncrement> fromDomainObjects(List<TallyIncrement> increments) {
        return increments.stream()
                .map(RestTallyIncrement::fromDomainObject)
                .collect(Collectors.toList());
    }

    public static RestTallyIncrement fromDomainObject(TallyIncrement tallyIncrement) {
        Preconditions.checkArgument(tallyIncrement != null, "tallyIncrement must not be null");
        return new RestTallyIncrement(
                tallyIncrement.getDescription(),
                tallyIncrement.getTags(),
                tallyIncrement.getIncrementDateUTC());
    }

    public TallyIncrement toDomainObject() {
        return TallyIncrement.newIncrement(description, incrementDateUTC, tags);
    }

    public String getDescription() {
        return this.description;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public LocalDateTime getIncrementDateUTC() {
        return this.incrementDateUTC;
    }

}
