package com.dataplatform.sources.http.impl;

import com.dataplatform.sources.http.*;
import org.apache.flink.connector.base.source.reader.RecordsBySplits;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.splitreader.SplitReader;
import org.apache.flink.connector.base.source.reader.splitreader.SplitsChange;
import org.apache.flink.types.Row;

import java.util.Queue;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

public class HttpSplitReader implements SplitReader<Row, HttpSplit> {

    private static final Logger LOG = LoggerFactory.getLogger(HttpSplitReader.class);
    private final AtomicBoolean wakeup = new AtomicBoolean(false);
    private final Queue<HttpSplit> splits;
    private final HttpClient httpClient;
    @Nullable
    private String currentSplitId;

    public HttpSplitReader() {
        this.splits = new ArrayDeque<>();
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
    }

    @Override
    public RecordsWithSplitIds<Row> fetch() {
        Map<String, Collection<Row>> recordsBySplit = new HashMap<>();
        Set<String> finishedSplits = new HashSet<>();

        wakeup.compareAndSet(true, false);

        for (HttpSplit split : splits) {
            try {
                // Create HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(split.url()))
                        .GET()
                        .build();

                // Send request and get response
                LOG.info("Sending HTTP request to: {}", split.url());
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                LOG.info("Received response with status code: {} for URL: {}", response.statusCode(), split.url());

                if (response.statusCode() == 200) {
                    HttpRecordParser parser = HttpRecordParserFactory.fromConfig(split.config());
                    byte[] respBody = response.body();
                    recordsBySplit.put(
                            split.splitId(),
                            parser.parse(respBody));
                } else {
                    LOG.error("HTTP request failed with status code: {} for URL: {}", response.statusCode(),
                            split.url());
                    throw new RuntimeException("HTTP request failed with status code: " + response.statusCode());
                }

                finishedSplits.add(split.splitId());

            } catch (Exception e) {
                LOG.error("Failed to process split {} for URL: {}", split.splitId(), split.url(), e);
                throw new RuntimeException("Failed to process split " + split.splitId(), e);
            }
        }

        return new RecordsBySplits<>(recordsBySplit, finishedSplits);
    }

    @Override
    public void wakeUp() {
        wakeup.compareAndSet(false, true);
    }

    @Override
    public void handleSplitsChanges(SplitsChange<HttpSplit> splitsChanges) {
        splits.addAll(splitsChanges.splits());
    }

    @Override
    public void close() throws Exception {
    }
}
