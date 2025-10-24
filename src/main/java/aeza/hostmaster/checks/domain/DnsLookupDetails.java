package aeza.hostmaster.checks.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;

@Embeddable
public class DnsLookupDetails {

    @ElementCollection
    @CollectionTable(name = "dns_lookup_records", joinColumns = @JoinColumn(name = "execution_result_id"))
    private List<DnsRecord> records = new ArrayList<>();

    public List<DnsRecord> getRecords() {
        return records;
    }

    public void setRecords(List<DnsRecord> records) {
        this.records = records;
    }
}
