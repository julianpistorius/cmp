package de.skuzzle.cmp.collaborativeorder.domain;

import java.util.Objects;

import com.google.common.base.Preconditions;

public final class UserId {

    private static final String UNKNOWN_PREFIX = "unknown";

    private final String source;
    private final String id;
    private final boolean anonymous;

    private UserId(String source, String id, boolean anonymous) {
        Preconditions.checkArgument(source != null, "source must not be null");
        Preconditions.checkArgument(!source.isEmpty(), "source must not be empty");

        Preconditions.checkArgument(id != null, "id must not be null");
        Preconditions.checkArgument(!id.isEmpty(), "id must not be empty");

        Preconditions.checkArgument(UNKNOWN_PREFIX.equals(source) || !anonymous,
                "source must be 'unknown' when anonymous flag is true, but was '%s'", source);
        this.source = source;
        this.id = id;
        this.anonymous = anonymous;
    }

    public static UserId of(String source, String id, boolean anonymous) {
        return new UserId(source, id, anonymous);
    }

    public static UserId wellKnown(String source, String id) {
        return new UserId(source, id, false);
    }

    public static UserId unknown(String id) {
        return new UserId(UNKNOWN_PREFIX, id, true);
    }

    public String getSource() {
        return this.source;
    }

    public String getId() {
        return this.id;
    }

    String getMetricsId() {
        return isAnonymous()
                ? UNKNOWN_PREFIX
                : toString();
    }

    public boolean isAnonymous() {
        return this.anonymous;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, id);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof UserId
                && Objects.equals(source, ((UserId) obj).source)
                && Objects.equals(id, ((UserId) obj).id);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", source, id);
    }
}
