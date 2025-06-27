package com.dataplatform.sources.zippedjson;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;

/**
 * The point of this class is to track our progress of assigning splits to
 * readers
 */
public class ZippedJsonCheckpoint {

    private static final Logger LOG = LoggerFactory.getLogger(ZippedJsonCheckpoint.class);
    private final Queue<ZippedJsonSplit> splitsToReassign;

    public ZippedJsonCheckpoint() {
        this.splitsToReassign = new ArrayDeque<>();
    }

    public ZippedJsonCheckpoint(
            Collection<ZippedJsonSplit> splitsToReassign) {
        this.splitsToReassign = new ArrayDeque<>(splitsToReassign);
    }

    public ZippedJsonCheckpoint(
            Queue<ZippedJsonSplit> splitsToReassign) {
        this.splitsToReassign = splitsToReassign;
    }

    public void addSplits(Collection<ZippedJsonSplit> splits) {
        splitsToReassign.addAll(splits);
    }

    public @Nullable ZippedJsonSplit getNextSplit() {

        final ZippedJsonSplit splitToReassign = splitsToReassign.poll();

        if (splitToReassign != null) {
            return splitToReassign;
        }

        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(splitsToReassign);
    }

    @Override
    public String toString() {
        return "ZippedJsonEnumState{"
                + "splitsToReassign="
                + splitsToReassign
                + '}';
    }
}
