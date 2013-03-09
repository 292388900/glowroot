/**
 * Copyright 2013 the original author or authors.
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
package io.informant.config;

import static io.informant.util.ObjectMappers.checkRequiredProperty;
import io.informant.api.weaving.MethodModifier;
import io.informant.util.Hashing2;

import java.util.List;

import checkers.igj.quals.Immutable;
import checkers.igj.quals.ReadOnly;
import checkers.nullness.quals.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;

/**
 * Immutable structure to hold an adhoc span/metric pointcut.
 * 
 * @author Trask Stalnaker
 * @since 0.5
 */
@Immutable
public class PointcutConfig {

    private final ImmutableList<CaptureItem> captureItems;
    private final String typeName;
    private final String methodName;
    private final ImmutableList<String> methodArgTypeNames;
    private final String methodReturnTypeName;
    private final ImmutableList<MethodModifier> methodModifiers;
    @Nullable
    private final String metricName;
    @Nullable
    private final String spanTemplate;
    private final String version;

    @VisibleForTesting
    public PointcutConfig(@ReadOnly List<CaptureItem> captureItems, String typeName,
            String methodName, @ReadOnly List<String> methodArgTypeNames,
            String methodReturnTypeName, @ReadOnly List<MethodModifier> methodModifiers,
            @Nullable String metricName, @Nullable String spanTemplate) {
        this.captureItems = ImmutableList.copyOf(captureItems);
        this.typeName = typeName;
        this.methodName = methodName;
        this.methodArgTypeNames = ImmutableList.copyOf(methodArgTypeNames);
        this.methodReturnTypeName = methodReturnTypeName;
        this.methodModifiers = ImmutableList.copyOf(methodModifiers);
        this.metricName = metricName;
        this.spanTemplate = spanTemplate;
        version = Hashing2.sha1(captureItems, typeName, methodName, methodArgTypeNames,
                methodReturnTypeName, methodModifiers, metricName, spanTemplate);
    }

    @Immutable
    public List<CaptureItem> getCaptureItems() {
        return captureItems;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getMethodName() {
        return methodName;
    }

    @Immutable
    public List<String> getMethodArgTypeNames() {
        return methodArgTypeNames;
    }

    public String getMethodReturnTypeName() {
        return methodReturnTypeName;
    }

    @Immutable
    public List<MethodModifier> getMethodModifiers() {
        return methodModifiers;
    }

    @Nullable
    public String getMetricName() {
        return metricName;
    }

    @Nullable
    public String getSpanTemplate() {
        return spanTemplate;
    }

    @JsonView(WithVersionJsonView.class)
    public String getVersion() {
        return version;
    }

    @JsonCreator
    static PointcutConfig readValue(
            @JsonProperty("captureItems") @Nullable List<CaptureItem> captureItems,
            @JsonProperty("typeName") @Nullable String typeName,
            @JsonProperty("methodName") @Nullable String methodName,
            @JsonProperty("methodArgTypeNames") @Nullable List<String> methodArgTypeNames,
            @JsonProperty("methodReturnTypeName") @Nullable String methodReturnTypeName,
            @JsonProperty("methodModifiers") @Nullable List<MethodModifier> methodModifiers,
            @JsonProperty("metricName") @Nullable String metricName,
            @JsonProperty("spanTemplate") @Nullable String spanTemplate)
            throws JsonMappingException {
        checkRequiredProperty(typeName, "typeName");
        checkRequiredProperty(methodName, "methodName");
        checkRequiredProperty(methodReturnTypeName, "methodReturnTypeName");
        return new PointcutConfig(orEmpty(captureItems), typeName, methodName,
                orEmpty(methodArgTypeNames), methodReturnTypeName, orEmpty(methodModifiers),
                metricName, spanTemplate);
    }

    @ReadOnly
    private static <T> List<T> orEmpty(@ReadOnly @Nullable List<T> list) {
        if (list == null) {
            return ImmutableList.of();
        }
        return list;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("captureItems", captureItems)
                .add("typeName", typeName)
                .add("methodName", methodName)
                .add("methodArgTypeNames", methodArgTypeNames)
                .add("methodReturnTypeName", methodReturnTypeName)
                .add("methodModifiers", methodModifiers)
                .add("metricName", metricName)
                .add("spanTemplate", spanTemplate)
                .add("version", version)
                .toString();
    }

    @Immutable
    public enum CaptureItem {
        METRIC, SPAN, TRACE;
    }
}
