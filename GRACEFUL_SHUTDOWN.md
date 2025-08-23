server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s

# Actuator Endpoints
management.endpoints.web.exposure.include=health,info,metrics,shutdown
management.endpoint.shutdown.enabled=true

# Thread Pool Shutdown
spring.task.execution.shutdown.await-termination=true
spring.task.execution.shutdown.await-termination-period=30s
```

## API Endpoints

### System Status
- **GET** `/api/system/status` - Status aplikasi dan active tasks
- **GET** `/api/system/health/ready` - Readiness probe
- **GET** `/api/system/health/live` - Liveness probe

### Shutdown Control
- **POST** `/actuator/shutdown` - Trigger graceful shutdown programmatically
# DocIx Graceful Shutdown Implementation
## Testing Graceful Shutdown

### Manual Testing
1. Start aplikasi: `./gradlew bootRun`
2. Trigger shutdown: `Ctrl+C` atau POST ke `/actuator/shutdown`
3. Monitor logs untuk urutan shutdown

### Automated Testing
- **Linux/Mac**: `./test-graceful-shutdown.sh`
- **Windows**: `test-graceful-shutdown.bat`

## Status Responses

### Normal Operation
```json
{
  "status": "RUNNING",
  "message": "Application is running normally",
  "activeProcessingTasks": 2
}
```

### During Shutdown
```json
{
  "status": "SHUTTING_DOWN",
  "message": "Application is shutting down gracefully",
  "activeProcessingTasks": 1
}
```

### After Shutdown
```json
{
  "status": "SHUTDOWN_COMPLETED",
  "message": "Application has completed graceful shutdown"
}
```

## Docker & Kubernetes Support

### Dockerfile
```dockerfile
# Proper signal handling
STOPSIGNAL SIGTERM

# Health checks
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8081/api/system/health/live || exit 1
```

### Kubernetes Deployment
```yaml
spec:
  containers:
  - name: docix
    livenessProbe:
      httpGet:
        path: /api/system/health/live
        port: 8081
    readinessProbe:
      httpGet:
        path: /api/system/health/ready
        port: 8081
    lifecycle:
      preStop:
        exec:
          command: ["/bin/sh", "-c", "sleep 10"]
```

## Monitoring

### Logs to Watch
```
🛑 Graceful shutdown initiated for DocIx application
📋 Starting graceful shutdown sequence...
⏳ Waiting for document processing completion...
🗄️ Closing MinIO client connections...
✅ Graceful shutdown completed successfully
```

### Metrics
- Active processing tasks: `GET /api/system/status`
- Health status: `GET /actuator/health`
- Application metrics: `GET /actuator/metrics`

## Troubleshooting

### Common Issues

1. **Shutdown Timeout**
   - Increase `spring.lifecycle.timeout-per-shutdown-phase`
   - Check for long-running tasks

2. **Tasks Not Completing**
   - Review document processing logic
   - Check external service connectivity

3. **Health Checks Failing**
   - Verify endpoint accessibility
   - Check application logs

### Debug Commands
```bash
# Check application status
curl http://localhost:8081/api/system/status

# Check health
curl http://localhost:8081/actuator/health

# Trigger shutdown
curl -X POST http://localhost:8081/actuator/shutdown
```

## Best Practices

1. **Always test graceful shutdown** dalam development
2. **Monitor active tasks** sebelum shutdown
3. **Set proper timeouts** untuk environment Anda
4. **Use health checks** untuk load balancer integration
5. **Log shutdown sequence** untuk debugging

## Integration dengan Infrastructure

Graceful shutdown terintegrasi dengan:
- **Load Balancers**: Health checks untuk traffic routing
- **Message Queues**: Proper message acknowledgment
- **Databases**: Connection cleanup
- **File Storage**: Active operation completion
- **Monitoring**: Status reporting

## Overview

Graceful shutdown memungkinkan aplikasi DocIx untuk menyelesaikan proses yang sedang berjalan sebelum benar-benar shutdown, memastikan tidak ada data yang hilang atau proses yang terputus secara tiba-tiba.

## Komponen yang Diimplementasikan

### 1. **GracefulShutdownManager**
- Central coordinator untuk proses shutdown
- Mengelola status shutdown (initiated, completed)
- Mengkoordinasi urutan shutdown yang benar

### 2. **DocumentProcessingMessageHandler** (Updated)
- Tracking active processing tasks
- Menolak task baru saat shutdown initiated
- Memberikan feedback tentang jumlah task yang masih aktif

### 3. **ShutdownAwareHealthIndicator**
- Health check yang aware terhadap status shutdown
- Memberikan status yang akurat selama proses shutdown

### 4. **SystemStatusController**
- Endpoint untuk monitoring status aplikasi
- Real-time information tentang active tasks
- Readiness dan liveness probes

### 5. **ShutdownHookManager**
- Menangani system signals (SIGTERM, SIGINT)
- Cleanup final sebelum aplikasi benar-benar berhenti

## Urutan Graceful Shutdown

1. **🛑 Shutdown Initiated**
   - Trigger: SIGTERM, Ctrl+C, atau Actuator endpoint
   - Status berubah ke "SHUTTING_DOWN"

2. **📋 Stop Accepting New Requests**
   - Health check return OUT_OF_SERVICE
   - Load balancer akan stop routing traffic

3. **⏳ Wait for Active Tasks**
   - Menunggu document processing selesai (max 25 detik)
   - Monitoring active processing tasks

4. **🔌 Close External Connections**
   - MinIO connections
   - Elasticsearch connections
   - RabbitMQ connections

5. **🧹 Resource Cleanup**
   - Temporary files
   - Memory cleanup
   - Final logging

6. **✅ Shutdown Complete**
   - Application context closed
   - Process terminated

## Configuration

### Application Properties
```properties
# Graceful Shutdown

