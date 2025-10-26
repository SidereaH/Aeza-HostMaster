package aeza.hostmaster.checks.entity;

import jakarta.persistence.*;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "http_details")
public class HttpDetailsEntity {
    @Id
    private UUID id;

    private String method;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time_millis")
    private Long responseTimeMillis;

    @ElementCollection
    @CollectionTable(name = "http_headers", joinColumns = @JoinColumn(name = "http_details_id"))
    @MapKeyColumn(name = "header_name")
    @Column(name = "header_value", columnDefinition = "TEXT")
    private Map<String, String> headers;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "check_execution_id")
    private CheckExecutionEntity checkExecution;

    public HttpDetailsEntity() {}

    public HttpDetailsEntity(UUID id, String method, Integer statusCode, Long responseTimeMillis, Map<String, String> headers) {
        this.id = id;
        this.method = method;
        this.statusCode = statusCode;
        this.responseTimeMillis = responseTimeMillis;
        this.headers = headers;
    }

    // Геттеры и сеттеры
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public Long getResponseTimeMillis() { return responseTimeMillis; }
    public void setResponseTimeMillis(Long responseTimeMillis) { this.responseTimeMillis = responseTimeMillis; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public CheckExecutionEntity getCheckExecution() { return checkExecution; }
    public void setCheckExecution(CheckExecutionEntity checkExecution) { this.checkExecution = checkExecution; }
}