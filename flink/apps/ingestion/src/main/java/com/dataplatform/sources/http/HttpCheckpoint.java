package com.dataplatform.sources.http;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Objects;
import java.util.Queue;

/**
 * The point of this class is to track our progress of assigning splits to
 * readers
 */
public class HttpCheckpoint {

    private static final Logger LOG = LoggerFactory.getLogger(HttpCheckpoint.class);
    private final Queue<HttpSplit> splitsToReassign;

    public HttpCheckpoint() {
        this.splitsToReassign = new ArrayDeque<>();
    }

    public HttpCheckpoint(
            Collection<HttpSplit> splitsToReassign) {
        this.splitsToReassign = new ArrayDeque<>(splitsToReassign);
    }

    public HttpCheckpoint(
            Queue<HttpSplit> splitsToReassign) {
        this.splitsToReassign = splitsToReassign;
    }

    public void addSplits(Collection<HttpSplit> splits) {
        splitsToReassign.addAll(splits);
    }

    public Set<String> getGeneratedUrls() {
        Set<String> urls = new HashSet<String>();
        for (HttpSplit split : splitsToReassign) {
            urls.add(split.url());
        }
        return urls;
    }

    public @Nullable HttpSplit getNextSplit() {

        final HttpSplit splitToReassign = splitsToReassign.poll();

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
        return "HttpEnumState{"
                + "splitsToReassign="
                + splitsToReassign
                + '}';
    }
}
