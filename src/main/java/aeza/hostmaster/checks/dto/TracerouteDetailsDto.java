package aeza.hostmaster.checks.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TracerouteDetailsDto(
        @JsonAlias({"hops", "path"})
        List<TracerouteHopDto> hops
) {
}
