package aeza.hostmaster.checks.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import java.util.LinkedHashMap;
import java.util.Map;

@Embeddable
public class HttpCheckDetails {

    @Column(name = "http_method")
    private String method;

    @Column(name = "http_status_code")
    private Integer statusCode;

    @Column(name = "http_response_time_ms")
    private Long responseTimeMillis;

    @ElementCollection
    @CollectionTable(name = "http_check_headers", joinColumns = @JoinColumn(name = "execution_result_id"))
    @jakarta.persistence.MapKeyColumn(name = "header_name")
    @Column(name = "header_value")
    private Map<String, String> headers = new LinkedHashMap<>();

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getResponseTimeMillis() {
        return responseTimeMillis;
    }

    public void setResponseTimeMillis(Long responseTimeMillis) {
        this.responseTimeMillis = responseTimeMillis;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
