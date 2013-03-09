/**
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.informant.local.store;

import io.informant.util.CharStreams2;
import io.informant.util.OnlyUsedByTests;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import checkers.igj.quals.ReadOnly;
import checkers.nullness.quals.Nullable;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import com.google.common.io.CharStreams;

/**
 * @author Trask Stalnaker
 * @since 0.5
 */
public class TraceSnapshotWriter {

    private final StringBuilder sb = new StringBuilder();
    private final List</*@ReadOnly*/CharSource> charSources = Lists.newArrayList();

    @ReadOnly
    public static CharSource toCharSource(TraceSnapshot snapshot, boolean active)
            throws UnsupportedEncodingException {
        return new TraceSnapshotWriter().toCharSourceInternal(snapshot, active);
    }

    private TraceSnapshotWriter() {}

    @ReadOnly
    private CharSource toCharSourceInternal(TraceSnapshot snapshot, boolean active)
            throws UnsupportedEncodingException {
        sb.append("{\"id\":\"");
        sb.append(snapshot.getId());
        sb.append("\",\"start\":");
        sb.append(snapshot.getStart());
        sb.append(",\"duration\":");
        sb.append(snapshot.getDuration());
        sb.append(",\"active\":");
        sb.append(active);
        sb.append(",\"stuck\":");
        sb.append(snapshot.isStuck());
        sb.append(",\"completed\":");
        sb.append(snapshot.isCompleted());
        sb.append(",\"background\":");
        sb.append(snapshot.isBackground());
        sb.append(",\"headline\":\"");
        sb.append(JsonStringEncoder.getInstance().quoteAsString(snapshot.getHeadline()));
        sb.append("\"");
        writeAttributes(snapshot);
        writeUserId(snapshot);
        writeError(snapshot);
        writeMetrics(snapshot);
        writeCharSource("spans", snapshot.getSpans());
        writeCharSource("coarseMergedStackTree", snapshot.getCoarseMergedStackTree());
        writeCharSource("fineMergedStackTree", snapshot.getFineMergedStackTree());
        sb.append("}");
        flushStringBuilder();
        return CharStreams2.join(charSources);
    }

    private void writeAttributes(TraceSnapshot snapshot) {
        String attributes = snapshot.getAttributes();
        if (attributes != null) {
            sb.append(",\"attributes\":");
            sb.append(attributes);
        }
    }

    private void writeUserId(TraceSnapshot snapshot) {
        String userId = snapshot.getUserId();
        if (userId != null) {
            sb.append(",\"userId\":\"");
            sb.append(JsonStringEncoder.getInstance().quoteAsString(userId));
            sb.append("\"");
        }
    }

    private void writeError(TraceSnapshot snapshot) {
        String errorText = snapshot.getErrorText();
        if (errorText != null) {
            sb.append(",\"error\":{\"text\":\"");
            sb.append(JsonStringEncoder.getInstance().quoteAsString(errorText));
            sb.append("\"");
            if (snapshot.getErrorDetail() != null) {
                sb.append(",\"detail\":");
                sb.append(snapshot.getErrorDetail());
            }
            if (snapshot.getException() != null) {
                sb.append(",\"exception\":");
                sb.append(snapshot.getException());
            }
            sb.append("}");
        }
    }

    private void writeMetrics(TraceSnapshot snapshot) {
        String metrics = snapshot.getMetrics();
        if (metrics != null) {
            sb.append(",\"metrics\":");
            sb.append(metrics);
        }
    }

    private void writeCharSource(String attributeName, @ReadOnly @Nullable CharSource charSource)
            throws UnsupportedEncodingException {
        if (charSource != null) {
            sb.append(",\"");
            sb.append(attributeName);
            sb.append("\":");
            flushStringBuilder();
            charSources.add(charSource);
        }
    }

    // flush current StringBuilder as its own chunk and reset StringBuilder
    private void flushStringBuilder() throws UnsupportedEncodingException {
        charSources.add(CharStreams.asCharSource(sb.toString()));
        sb.setLength(0);
    }

    // this method exists because tests cannot use (sometimes) shaded guava CharSource
    @OnlyUsedByTests
    public static String toString(TraceSnapshot snapshot, boolean active) throws IOException {
        return toCharSource(snapshot, active).read();
    }
}
