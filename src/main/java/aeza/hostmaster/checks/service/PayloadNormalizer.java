package aeza.hostmaster.checks.service;

import aeza.hostmaster.checks.domain.CheckType;
import aeza.hostmaster.checks.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class PayloadNormalizer {

    public CheckType detectType(JsonNode node) {

        if (node.has("http")) return CheckType.HTTP;
        if (node.has("ping")) return CheckType.PING;
        if (node.has("dns")) return CheckType.DNS_LOOKUP;
        if (node.has("tcp")) return CheckType.TCP;
        if (node.has("traceroute")) return CheckType.TRACEROUTE;

        // fallback to HTTP (default)
        return CheckType.HTTP;
    }


    public HttpDetailsDto normalizeHttp(JsonNode httpNode) {
        JsonNode entry = httpNode.get(0);

        return new HttpDetailsDto(
                entry.path("location").asText(),
                entry.path("country").asText(),
                extractMillisFromSeconds(entry.path("time").asText()),
                entry.path("status").asInt(),
                entry.path("ip").asText(),
                entry.path("result").asText(),
                null
        );
    }


    public PingDetailsDto normalizePing(JsonNode pingNode) {
        JsonNode entry = pingNode.get(0);

        String lossStr = entry.path("packets").path("loss").asText("0%").replace("%", "");
        double lossVal = parseDouble(lossStr);

        double min = parseMs(entry.path("roundTrip").path("min").asText());
        double avg = parseMs(entry.path("roundTrip").path("avg").asText());
        double max = parseMs(entry.path("roundTrip").path("max").asText());

        return new PingDetailsDto(
                entry.path("location").asText(),
                entry.path("country").asText(),
                entry.path("ip").asText(),
                entry.path("packets").path("transmitted").asInt(),
                entry.path("packets").path("received").asInt(),
                lossVal,
                min, avg, max
        );
    }


    public TcpDetailsDto normalizeTcp(JsonNode tcpNode) {
        JsonNode entry = tcpNode.get(0);

        long timeMillis = extractMillisFromSeconds(entry.path("connectTime").asText());

        return new TcpDetailsDto(
                entry.path("location").asText(),
                entry.path("country").asText(),
                timeMillis,
                entry.path("status").asText(),
                entry.path("ip").asText()
        );
    }


    public DnsDetailsDto normalizeDns(JsonNode dnsNode) {
        JsonNode entry = dnsNode.path("locations").get(0);

        List<String> records = List.of(entry.path("records").asText().split(", "));

        return new DnsDetailsDto(
                entry.path("location").asText(),
                entry.path("country").asText(),
                records,
                entry.path("ttl").asText()
        );
    }


    public TracerouteDetailsDto normalizeTraceroute(JsonNode trNode) {
        List<TracerouteHopDto> hops = new ArrayList<>();

        Iterator<JsonNode> it = trNode.iterator();
        while (it.hasNext()) {
            JsonNode hop = it.next();
            hops.add(
                    new TracerouteHopDto(
                            hop.path("hop").asInt(),
                            hop.path("ip").asText(),
                            hop.path("time").asText()
                    )
            );
        }

        return new TracerouteDetailsDto(hops);
    }


    // helpers

    private double parseMs(String msString) {
        if (!msString.endsWith(" ms")) return 0;
        return parseDouble(msString.replace(" ms", ""));
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return 0; }
    }

    private long extractMillisFromSeconds(String sec) {
        try {
            return (long)(Double.parseDouble(sec.replace(" s", "")) * 1000);
        } catch (Exception e) {
            return 0;
        }
    }
}
