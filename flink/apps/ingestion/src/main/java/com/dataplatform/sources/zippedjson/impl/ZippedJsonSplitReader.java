package com.dataplatform.sources.zippedjson.impl;

import com.dataplatform.sources.zippedjson.*;
import org.apache.flink.connector.base.source.reader.RecordsBySplits;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.splitreader.SplitReader;
import org.apache.flink.connector.base.source.reader.splitreader.SplitsChange;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;
import java.util.zip.ZipInputStream;

import java.io.InputStream;
import java.util.zip.ZipEntry;

import java.util.stream.Stream;

public class ZippedJsonSplitReader implements SplitReader<String, ZippedJsonSplit> {

    private final AtomicBoolean wakeup = new AtomicBoolean(false);
    private final Queue<ZippedJsonSplit> splits;
    @Nullable
    private String currentSplitId;
    private long batchSize;

    public ZippedJsonSplitReader(long batchSize) {
        this.splits = new ArrayDeque<>();
        this.batchSize = batchSize;
    }

    @Override
    public RecordsWithSplitIds<String> fetch() {

        Map<String, Collection<String>> recordsBySplit = new HashMap<>();
        Set<String> finishedSplits = new HashSet<>();

        wakeup.compareAndSet(true, false);
        for (ZippedJsonSplit split : splits) {

            try (InputStream in = split.zipPath().getFileSystem().open(split.zipPath());
                    ZipInputStream zis = new ZipInputStream(in)) {

                long offset = split.offset();
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {

                    if (entry.isDirectory() || !entry.getName().equals(split.jsonPath().toString())) {
                        continue;
                    }

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(zis))) {

                        Stream<String> linesStream = reader.lines();

                        // If offset is set (>= 0), skip lines
                        if (offset >= 0) {
                            linesStream = linesStream.skip(offset).limit(batchSize);
                        }

                        linesStream.forEach(line -> recordsBySplit
                                .computeIfAbsent(split.splitId(), id -> new ArrayList<>()).add(line));
                    }

                    break;
                }

                finishedSplits.add(split.splitId());

            } catch (Exception e) {
                throw new RuntimeException("Error processing ZIP file", e);
            }

            break;
        }

        return new RecordsBySplits<>(recordsBySplit, finishedSplits);
    }

    @Override
    public void wakeUp() {
        wakeup.compareAndSet(false, true);
    }

    @Override
    public void handleSplitsChanges(SplitsChange<ZippedJsonSplit> splitsChanges) {
        splits.addAll(splitsChanges.splits());
    }

    @Override
    public void close() throws Exception {
    }
}
