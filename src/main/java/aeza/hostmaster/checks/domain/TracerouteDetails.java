package aeza.hostmaster.checks.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import java.util.ArrayList;
import java.util.List;

@Embeddable
public class TracerouteDetails {

    @ElementCollection
    @CollectionTable(name = "traceroute_hops", joinColumns = @JoinColumn(name = "execution_result_id"))
    @OrderColumn(name = "hop_order")
    private List<TracerouteHop> hops = new ArrayList<>();

    public List<TracerouteHop> getHops() {
        return hops;
    }

    public void setHops(List<TracerouteHop> hops) {
        this.hops = hops;
    }
}
