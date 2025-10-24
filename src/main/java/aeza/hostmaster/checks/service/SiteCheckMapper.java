package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckExecutionResult;
import aeza.hostmaster.checks.domain.CheckMetric;
import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.domain.DnsLookupDetails;
import aeza.hostmaster.checks.domain.DnsRecord;
import aeza.hostmaster.checks.domain.HttpCheckDetails;
import aeza.hostmaster.checks.domain.PingCheckDetails;
import aeza.hostmaster.checks.domain.SiteCheckResult;
import aeza.hostmaster.checks.domain.TcpCheckDetails;
import aeza.hostmaster.checks.domain.TracerouteDetails;
import aeza.hostmaster.checks.domain.TracerouteHop;
import aeza.hostmaster.checks.dto.CheckExecutionRequest;
import aeza.hostmaster.checks.dto.CheckExecutionResponse;
import aeza.hostmaster.checks.dto.CheckMetricDto;
import aeza.hostmaster.checks.dto.DnsLookupDetailsDto;
import aeza.hostmaster.checks.dto.DnsRecordDto;
import aeza.hostmaster.checks.dto.HttpCheckDetailsDto;
import aeza.hostmaster.checks.dto.PingCheckDetailsDto;
import aeza.hostmaster.checks.dto.SiteCheckRequest;
import aeza.hostmaster.checks.dto.SiteCheckResponse;
import aeza.hostmaster.checks.dto.TcpCheckDetailsDto;
import aeza.hostmaster.checks.dto.TracerouteDetailsDto;
import aeza.hostmaster.checks.dto.TracerouteHopDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SiteCheckMapper {

    public SiteCheckResult toEntity(SiteCheckRequest request) {
        SiteCheckResult result = new SiteCheckResult();
        result.setTarget(request.target());
        result.setExecutedAt(request.executedAt());
        result.setStatus(request.status());
        result.setTotalDurationMillis(request.totalDurationMillis());

        request.checks().stream()
                .map(this::toExecutionEntity)
                .forEach(result::addCheck);
        return result;
    }

    private CheckExecutionResult toExecutionEntity(CheckExecutionRequest request) {
        CheckExecutionResult executionResult = new CheckExecutionResult();
        executionResult.setType(request.type());
        executionResult.setStatus(request.status());
        executionResult.setDurationMillis(request.durationMillis());
        executionResult.setMessage(request.message());
        executionResult.setMetrics(toMetricEntities(request.metrics()));

        switch (request.type()) {
            case HTTP -> executionResult.setHttpDetails(toHttpDetails(request.httpDetails()));
            case PING -> executionResult.setPingDetails(toPingDetails(request.pingDetails()));
            case TCP_CONNECT -> executionResult.setTcpDetails(toTcpDetails(request.tcpDetails()));
            case TRACEROUTE -> executionResult.setTracerouteDetails(toTracerouteDetails(request.tracerouteDetails()));
            case DNS_LOOKUP -> executionResult.setDnsLookupDetails(toDnsLookupDetails(request.dnsLookupDetails()));
        }
        return executionResult;
    }

    private HttpCheckDetails toHttpDetails(HttpCheckDetailsDto dto) {
        if (dto == null) {
            throw new InvalidCheckDetailsException("HTTP check requires response details");
        }
        if (dto.statusCode() == null) {
            throw new InvalidCheckDetailsException("HTTP status code is required");
        }
        if (dto.responseTimeMillis() == null || dto.responseTimeMillis() < 0) {
            throw new InvalidCheckDetailsException("HTTP response time must be zero or positive");
        }
        HttpCheckDetails details = new HttpCheckDetails();
        details.setMethod(dto.method());
        details.setStatusCode(dto.statusCode());
        details.setResponseTimeMillis(dto.responseTimeMillis());
        Map<String, String> headers = dto.headers() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dto.headers());
        details.setHeaders(headers);
        return details;
    }

    private PingCheckDetails toPingDetails(PingCheckDetailsDto dto) {
        if (dto == null) {
            throw new InvalidCheckDetailsException("Ping check requires metrics");
        }
        PingCheckDetails details = new PingCheckDetails();
        details.setPacketsTransmitted(dto.packetsTransmitted());
        details.setPacketsReceived(dto.packetsReceived());
        details.setPacketLossPercentage(dto.packetLossPercentage());
        details.setMinimumRttMillis(dto.minimumRttMillis());
        details.setAverageRttMillis(dto.averageRttMillis());
        details.setMaximumRttMillis(dto.maximumRttMillis());
        details.setStandardDeviationRttMillis(dto.standardDeviationRttMillis());
        return details;
    }

    private TcpCheckDetails toTcpDetails(TcpCheckDetailsDto dto) {
        if (dto == null) {
            throw new InvalidCheckDetailsException("TCP connect check requires port information");
        }
        if (dto.port() == null || dto.port() <= 0) {
            throw new InvalidCheckDetailsException("TCP port must be positive");
        }
        if (dto.connectionTimeMillis() == null || dto.connectionTimeMillis() < 0) {
            throw new InvalidCheckDetailsException("TCP connection time must be zero or positive");
        }
        TcpCheckDetails details = new TcpCheckDetails();
        details.setPort(dto.port());
        details.setConnectionTimeMillis(dto.connectionTimeMillis());
        details.setAddress(dto.address());
        return details;
    }

    private TracerouteDetails toTracerouteDetails(TracerouteDetailsDto dto) {
        if (dto == null) {
            throw new InvalidCheckDetailsException("Traceroute check requires hop data");
        }
        if (dto.hops() == null || dto.hops().isEmpty()) {
            throw new InvalidCheckDetailsException("Traceroute check requires at least one hop");
        }
        TracerouteDetails details = new TracerouteDetails();
        List<TracerouteHop> hops = dto.hops().stream()
                .map(this::toTracerouteHop)
                .collect(Collectors.toCollection(ArrayList::new));
        details.setHops(hops);
        return details;
    }

    private TracerouteHop toTracerouteHop(TracerouteHopDto dto) {
        if (dto.hopIndex() == null || dto.hopIndex() <= 0) {
            throw new InvalidCheckDetailsException("Traceroute hop index must be positive");
        }
        TracerouteHop hop = new TracerouteHop();
        hop.setHopIndex(dto.hopIndex());
        hop.setIpAddress(dto.ipAddress());
        hop.setHostname(dto.hostname());
        hop.setLatencyMillis(dto.latencyMillis());
        hop.setLatitude(dto.latitude());
        hop.setLongitude(dto.longitude());
        hop.setCountry(dto.country());
        hop.setCity(dto.city());
        return hop;
    }

    private DnsLookupDetails toDnsLookupDetails(DnsLookupDetailsDto dto) {
        if (dto == null) {
            throw new InvalidCheckDetailsException("DNS lookup check requires record data");
        }
        if (dto.records() == null || dto.records().isEmpty()) {
            throw new InvalidCheckDetailsException("DNS lookup check requires at least one record");
        }
        DnsLookupDetails details = new DnsLookupDetails();
        List<DnsRecord> records = dto.records().stream()
                .map(this::toDnsRecord)
                .collect(Collectors.toCollection(ArrayList::new));
        details.setRecords(records);
        return details;
    }

    private DnsRecord toDnsRecord(DnsRecordDto dto) {
        if (dto.type() == null) {
            throw new InvalidCheckDetailsException("DNS record type is required");
        }
        if (dto.value() == null || dto.value().isBlank()) {
            throw new InvalidCheckDetailsException("DNS record value is required");
        }
        DnsRecord record = new DnsRecord();
        record.setType(dto.type());
        record.setValue(dto.value());
        record.setTtl(dto.ttl());
        return record;
    }

    private List<CheckMetric> toMetricEntities(List<CheckMetricDto> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return new ArrayList<>();
        }
        return metrics.stream().map(this::toMetricEntity).collect(Collectors.toCollection(ArrayList::new));
    }

    private CheckMetric toMetricEntity(CheckMetricDto dto) {
        if (dto == null || dto.name() == null || dto.name().isBlank()) {
            throw new InvalidCheckDetailsException("Metric name is required");
        }
        CheckMetric metric = new CheckMetric();
        metric.setName(dto.name());
        metric.setValue(dto.value());
        metric.setUnit(dto.unit());
        metric.setDescription(dto.description());
        return metric;
    }

    public SiteCheckResponse toResponse(SiteCheckResult entity) {
        List<CheckExecutionResponse> checks = entity.getChecks().stream()
                .map(this::toExecutionResponse)
                .toList();
        return new SiteCheckResponse(
                entity.getId(),
                entity.getTarget(),
                entity.getExecutedAt(),
                entity.getStatus(),
                entity.getTotalDurationMillis(),
                checks);
    }

    private CheckExecutionResponse toExecutionResponse(CheckExecutionResult entity) {
        return new CheckExecutionResponse(
                entity.getId(),
                entity.getType(),
                entity.getStatus(),
                entity.getDurationMillis(),
                entity.getMessage(),
                entity.getType() == CheckType.HTTP ? toHttpDto(entity.getHttpDetails()) : null,
                entity.getType() == CheckType.PING ? toPingDto(entity.getPingDetails()) : null,
                entity.getType() == CheckType.TCP_CONNECT ? toTcpDto(entity.getTcpDetails()) : null,
                entity.getType() == CheckType.TRACEROUTE ? toTracerouteDto(entity.getTracerouteDetails()) : null,
                entity.getType() == CheckType.DNS_LOOKUP ? toDnsLookupDto(entity.getDnsLookupDetails()) : null,
                toMetricDtos(entity.getMetrics()));
    }

    private HttpCheckDetailsDto toHttpDto(HttpCheckDetails details) {
        if (details == null) {
            return null;
        }
        Map<String, String> headers = details.getHeaders() == null ? null : new LinkedHashMap<>(details.getHeaders());
        return new HttpCheckDetailsDto(details.getMethod(), details.getStatusCode(), details.getResponseTimeMillis(), headers);
    }

    private PingCheckDetailsDto toPingDto(PingCheckDetails details) {
        if (details == null) {
            return null;
        }
        return new PingCheckDetailsDto(
                details.getPacketsTransmitted(),
                details.getPacketsReceived(),
                details.getPacketLossPercentage(),
                details.getMinimumRttMillis(),
                details.getAverageRttMillis(),
                details.getMaximumRttMillis(),
                details.getStandardDeviationRttMillis());
    }

    private TcpCheckDetailsDto toTcpDto(TcpCheckDetails details) {
        if (details == null) {
            return null;
        }
        return new TcpCheckDetailsDto(details.getPort(), details.getConnectionTimeMillis(), details.getAddress());
    }

    private TracerouteDetailsDto toTracerouteDto(TracerouteDetails details) {
        if (details == null) {
            return null;
        }
        List<TracerouteHopDto> hops = details.getHops() == null ? List.of() : details.getHops().stream()
                .filter(Objects::nonNull)
                .map(this::toTracerouteHopDto)
                .toList();
        if (hops.isEmpty()) {
            return null;
        }
        return new TracerouteDetailsDto(hops);
    }

    private TracerouteHopDto toTracerouteHopDto(TracerouteHop hop) {
        return new TracerouteHopDto(
                hop.getHopIndex(),
                hop.getIpAddress(),
                hop.getHostname(),
                hop.getLatencyMillis(),
                hop.getLatitude(),
                hop.getLongitude(),
                hop.getCountry(),
                hop.getCity());
    }

    private DnsLookupDetailsDto toDnsLookupDto(DnsLookupDetails details) {
        if (details == null) {
            return null;
        }
        List<DnsRecordDto> records = details.getRecords() == null ? List.of() : details.getRecords().stream()
                .map(record -> new DnsRecordDto(record.getType(), record.getValue(), record.getTtl()))
                .toList();
        if (records.isEmpty()) {
            return null;
        }
        return new DnsLookupDetailsDto(records);
    }

    private List<CheckMetricDto> toMetricDtos(List<CheckMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return List.of();
        }
        return metrics.stream()
                .map(metric -> new CheckMetricDto(metric.getName(), metric.getValue(), metric.getUnit(), metric.getDescription()))
                .toList();
    }
}
