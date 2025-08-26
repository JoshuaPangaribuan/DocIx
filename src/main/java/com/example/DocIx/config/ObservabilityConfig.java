package com.example.DocIx.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
// Removed deprecated JaegerGrpcSpanExporter import
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Removed Profile import - no longer needed without Jaeger fallback

import java.time.Duration;

/**
 * Konfigurasi untuk observability (metrics dan tracing)
 */
@Configuration
public class ObservabilityConfig {

    @Value("${app.tracing.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

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
     * Konfigurasi Resource untuk OpenTelemetry
     */
    @Bean
    public Resource otelResource() {
        return Resource.getDefault()
                .merge(Resource.builder()
                        .put(ResourceAttributes.SERVICE_NAME, applicationName)
                        .put(ResourceAttributes.SERVICE_VERSION, "1.0.0")
                        .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, "development")
                        .build());
    }

    /**
     * Konfigurasi OTLP gRPC Span Exporter (modern standard)
     */
    @Bean
    public SpanExporter spanExporter() {
        return OtlpGrpcSpanExporter.builder()
                .setEndpoint(otlpEndpoint)
                .setCompression("gzip")
                .build();
    }

    /**
     * Konfigurasi OpenTelemetry untuk distributed tracing
     */
    @Bean
    public OpenTelemetry openTelemetry(Resource otelResource, SpanExporter spanExporter) {
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(otelResource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter)
                        .setMaxExportBatchSize(512)
                        .setScheduleDelay(Duration.ofSeconds(1))
                        .build())
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();
    }

    /**
     * Bean untuk Tracer yang akan digunakan dalam aplikasi
     */
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(applicationName);
    }
}
