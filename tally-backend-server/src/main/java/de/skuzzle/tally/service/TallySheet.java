package de.skuzzle.tally.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.google.common.base.Preconditions;

@Document
public class TallySheet {

    @Id
    private String id;
    @Version
    private int version;

    @NotEmpty
    private String name;
    @NotEmpty
    @Indexed
    private String adminKey;
    @NotEmpty
    @Indexed
    private String publicKey;
    @NotNull
    private List<TallyIncrement> increments;

    // dates in UTC+0
    @CreatedDate
    private LocalDateTime createDateUTC;
    @LastModifiedDate
    private LocalDateTime lastModifiedDateUTC;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Optional<String> getAdminKey() {
        return Optional.ofNullable(this.adminKey);
    }

    public void setAdminKey(String adminKey) {
        this.adminKey = adminKey;
    }

    public String getPublicKey() {
        return this.publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public List<TallyIncrement> getIncrements() {
        return this.increments;
    }

    public void setIncrements(List<TallyIncrement> increments) {
        this.increments = increments;
    }

    public LocalDateTime getLastModifiedDateUTC() {
        return this.lastModifiedDateUTC;
    }

    void setLastModifiedDateUTC(LocalDateTime lastModifiedDate) {
        this.lastModifiedDateUTC = lastModifiedDate;
    }

    public LocalDateTime getCreateDateUTC() {
        return this.createDateUTC;
    }

    void setCreateDateUTC(LocalDateTime createDate) {
        this.createDateUTC = createDate;
    }

    public void incrementWith(TallyIncrement increment) {
        Preconditions.checkArgument(increment != null, "increment must not be null");
        this.increments.add(increment);
    }
}
