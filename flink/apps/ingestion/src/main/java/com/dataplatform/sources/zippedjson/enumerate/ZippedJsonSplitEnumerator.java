package com.dataplatform.sources.zippedjson.enumerate;

import com.dataplatform.sources.zippedjson.*;

import java.io.LineNumberReader;
import java.io.InputStreamReader;

import java.util.zip.ZipEntry;
import org.apache.flink.api.connector.source.SourceEvent;
import org.apache.flink.api.connector.source.SplitEnumerator;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.flink.core.fs.Path;

import java.lang.RuntimeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class ZippedJsonSplitEnumerator implements SplitEnumerator<ZippedJsonSplit, ZippedJsonCheckpoint> {

    private static final Logger LOG = LoggerFactory.getLogger(ZippedJsonSplitEnumerator.class);

    private final SplitEnumeratorContext<ZippedJsonSplit> context;
    private final LinkedHashMap<Integer, String> readersAwaitingSplit;
    private ZippedJsonCheckpoint state;
    private long batchSize;

    Path zipPath;

    public ZippedJsonSplitEnumerator(
            SplitEnumeratorContext<ZippedJsonSplit> context,
            Path zipPath,
            long batchSize) {
        this.context = context;
        this.state = new ZippedJsonCheckpoint();
        this.zipPath = zipPath;
        this.batchSize = batchSize;
        this.readersAwaitingSplit = new LinkedHashMap<>();
    }

    @Override
    public void start() {
        // lookup all splits (each json file in zip is one split)
        final ArrayList<ZippedJsonSplit> splits = new ArrayList<>();

        try (InputStream s3InputStream = zipPath.getFileSystem().open(zipPath);
                ZipInputStream zis = new ZipInputStream(s3InputStream)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                // Check that the entry is a file and ends with ".json" (case-insensitive)
                if (entry.isDirectory()) {
                    continue;
                }

                if (!entry.getName().toLowerCase().endsWith(".json")) {
                    continue;
                }

                // if file is too large, we need to chunk it into multiple splits
                // we'll use the offset field to determine where to start the split

                int lineCount = 0;
                LineNumberReader reader = new LineNumberReader(new InputStreamReader(zis));
                while ((reader.readLine()) != null)
                    ;
                lineCount = reader.getLineNumber();

                if (lineCount > batchSize) {

                    int numSplits = (int) Math.ceil((double) lineCount / batchSize);
                    for (int i = 0; i < numSplits; i++) {
                        splits.add(new ZippedJsonSplit(
                                zipPath,
                                new Path(entry.getName()),
                                i * batchSize));
                    }

                } else {
                    // if offset is negative, then that means the split represents the entire file
                    splits.add(new ZippedJsonSplit(
                            zipPath,
                            new Path(entry.getName()),
                            -1));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error processing ZIP file", e);
        }

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
    public void addSplitsBack(List<ZippedJsonSplit> splits, int subtaskId) {
        state = new ZippedJsonCheckpoint(splits);
    }

    @Override
    public void addReader(int subtaskId) {
        // don't assign splits to readers pro-actively, do it on request (via
        // handleSplitRequest) could do it that route, but shouldn't do both
        // approaches at the same time
    }

    @Override
    public ZippedJsonCheckpoint snapshotState(long checkpointId) {
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
            final ZippedJsonSplit nextSplit = state.getNextSplit();

            if (nextSplit == null) {
                context.signalNoMoreSplits(awaitingSubtask);
                break;
            }

            context.assignSplit(nextSplit, awaitingSubtask);
            awaitingReader.remove();

        }
    }

}
