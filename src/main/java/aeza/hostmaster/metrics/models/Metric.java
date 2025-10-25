package aeza.hostmaster.metrics.models;

import aeza.hostmaster.metrics.dto.MetricDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Data
public class Metric {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long metricId;
    private String agentName;
    private MetricType metricType;
    private double value;
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    @JsonSerialize(using = LocalDateTimeSerializer.class)
//    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @CreationTimestamp
    private Instant timestamp;

    @ManyToOne
    private AgentRating agentRating;


    public enum MetricType {
        AGENT_AVAILABILITY,
        REQUEST_COUNT,
        RESPONSE_COUNT
    }

    public MetricDTO toDTO() {
        MetricDTO dto = new MetricDTO();
        dto.setAgentName(agentName);
        dto.setMetricType(metricType);
        dto.setValue(value);
        dto.setTimestamp(timestamp);
        return dto;
    }

}
