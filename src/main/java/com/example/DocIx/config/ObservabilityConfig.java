package com.example.DocIx.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurasi untuk observability (metrics dan tracing)
 */
@Configuration
public class ObservabilityConfig {

    @Value("${app.tracing.jaeger.endpoint:http://localhost:14250}")
    private String jaegerEndpoint;

    @Value("${spring.application.name:docix}")
    private String applicationName;

    /**
     * Konfigurasi MeterRegistry untuk Prometheus metrics
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags("application", applicationName);
    }

    /**
     * Konfigurasi OpenTelemetry untuk distributed tracing
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        JaegerGrpcSpanExporter jaegerExporter = JaegerGrpcSpanExporter.builder()
            .setEndpoint(jaegerEndpoint)
            .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(BatchSpanProcessor.builder(jaegerExporter).build())
            .build();

        return OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .build();
    }

    /**
     * Bean untuk Tracer yang akan digunakan dalam aplikasi
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(applicationName);
    }
}
