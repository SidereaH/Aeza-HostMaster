package aeza.hostmaster.agents.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "agents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Agent implements UserDetails {
    public enum Status {
        ACTIVE,
        INACTIVE,
        BANNED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String agentName;

    @Column(nullable = false)
    private String ipAddress;

    @Column(nullable = false, unique = true)
    private String agentToken;

    private String agentCountry;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    private OffsetDateTime updatedAt;

    /**
     * Last time the agent reported a heartbeat (used to see online/offline)
     */
    private OffsetDateTime lastHeartbeat;

    /**
     * Optimistic lock for concurrent updates (optional)
     */
    @Version
    private Long version;

    // --- UserDetails implementations ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_AGENT"));
    }

    @Override
    public String getPassword() {
        return agentToken;
    }

    @Override
    public String getUsername() {
        return agentName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.status == Status.ACTIVE;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != Status.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.status == Status.ACTIVE;
    }

    @Override
    public boolean isEnabled() {
        return this.status == Status.ACTIVE;
    }

    // equals/hashCode using unique id and name
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Agent agent = (Agent) o;
        return Objects.equals(id, agent.id) && Objects.equals(agentName, agent.agentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, agentName);
    }
}
