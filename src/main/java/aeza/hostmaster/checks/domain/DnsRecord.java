package aeza.hostmaster.checks.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class DnsRecord {

    @Enumerated(EnumType.STRING)
    @Column(name = "dns_record_type", nullable = false)
    private DnsRecordType type;

    @Column(name = "dns_record_value", nullable = false, length = 2048)
    private String value;

    @Column(name = "dns_record_ttl")
    private Long ttl;

    public DnsRecordType getType() {
        return type;
    }

    public void setType(DnsRecordType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getTtl() {
        return ttl;
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
