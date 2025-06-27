package com.dataplatform.sources.http.enumerate;

import com.dataplatform.sources.http.*;

import org.apache.flink.api.connector.source.SourceEvent;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;

import java.util.Set;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpSplitEnumerator implements SplitEnumerator<HttpSplit, HttpCheckpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSplitEnumerator.class);

    private final LinkedHashMap<Integer, String> readersAwaitingSplit;
    private final SplitEnumeratorContext<HttpSplit> context;
    private HttpCheckpoint state;
    private HttpSourceConfig config;

    public HttpSplitEnumerator(
            SplitEnumeratorContext<HttpSplit> context,
            HttpSourceConfig config,
            @Nullable HttpCheckpoint checkpoint) {
        this.context = context;

        if (checkpoint != null) {
            this.state = checkpoint;
        } else {
            this.state = new HttpCheckpoint();
        }
        this.config = config;
        this.readersAwaitingSplit = new LinkedHashMap<>();
    }

    @Override
    public void start() {

        // compile list of URL's we need to fetch based off url configuration
        final ArrayList<HttpSplit> splits = new ArrayList<>();
        Set<String> generatedUrls = state.getGeneratedUrls();

        if (config.getIterationMechanism() == null || config.getIterationMechanism().equals("none")) {
            splits.add(new HttpSplit(config.getUrl(), config));

        } else if (config.getIterationMechanism().equals("date")) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate start = LocalDate.parse(config.startDate(), formatter);
            LocalDate end = LocalDate.now().plusDays(config.endDateOffset());

            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                String parsedUrl = config.getUrl()
                        .replace("__YEAR__", String.valueOf(date.getYear()))
                        .replace("__MONTH__", String.format("%02d", date.getMonthValue()))
                        .replace("__DAY__", String.format("%02d", date.getDayOfMonth()));

                if (!generatedUrls.contains(parsedUrl)) {
                    splits.add(new HttpSplit(parsedUrl, config));
                }
            }
        } else {
            LOG.error("Unsupported URL iteration mechanism provided: " + config.getIterationMechanism());
            return;
        }
        // TODO: add support for other query result iteration mechanism

        state.addSplits(splits);
    }

    @Override
    public void handleSplitRequest(int subtaskId, String requesterHostname) {
        readersAwaitingSplit.put(subtaskId, requesterHostname);
        assignSplits();
    }

    @Override
    public void handleSourceEvent(int subtaskId, SourceEvent sourceEvent) {
        LOG.error("Received unrecognized event: {}", sourceEvent);
    }

    @Override
    public void addSplitsBack(List<HttpSplit> splits, int subtaskId) {
        state = new HttpCheckpoint(splits);
    }

    @Override
    public void addReader(int subtaskId) {
        // don't assign splits to readers pro-actively, do it on request (via
        // handleSplitRequest) could do it that route, but shouldn't do both
        // approaches at the same time
    }

    @Override
    public HttpCheckpoint snapshotState(long checkpointId) {
        return state;
    }

    @Override
    public void close() {
    }

    private void assignSplits() {

        final Iterator<Map.Entry<Integer, String>> awaitingReader = readersAwaitingSplit.entrySet().iterator();

        while (awaitingReader.hasNext()) {
            final Map.Entry<Integer, String> nextAwaiting = awaitingReader.next();

            // if the reader that requested another split has failed in the meantime, remove
            // it from the list of waiting readers
            if (!context.registeredReaders().containsKey(nextAwaiting.getKey())) {
                awaitingReader.remove();
                continue;
            }

            final int awaitingSubtask = nextAwaiting.getKey();
            final HttpSplit nextSplit = state.getNextSplit();

            if (nextSplit == null) {
                context.signalNoMoreSplits(awaitingSubtask);
                break;
            }

            context.assignSplit(nextSplit, awaitingSubtask);
            awaitingReader.remove();

        }
    }

}
