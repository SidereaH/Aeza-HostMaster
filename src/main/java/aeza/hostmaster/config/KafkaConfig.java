package aeza.hostmaster.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public AdminClient adminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.CLIENT_DNS_LOOKUP_CONFIG, "use_all_dns_ips");
        return AdminClient.create(configs);
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.CLIENT_DNS_LOOKUP_CONFIG, "use_all_dns_ips");
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic agentTasksTopic() {
        return TopicBuilder.name("agent-tasks")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic checkResultsTopic() {
        return TopicBuilder.name("check-results")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic agentLogsTopic() {
        return TopicBuilder.name("agent-logs")
                .partitions(3)
                .replicas(1)
                .build();
    }
}