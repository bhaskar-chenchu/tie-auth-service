package com.vcc.tie.auth.claims;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toSet;


@Service
public class TieClaimsProvider {
    private Map<String, InMemoryRole> userToRoles = new ConcurrentHashMap<>();
    private final InMemoryRole technicianRole;
    private final InMemoryRole marketAdmin;


    public TieClaimsProvider() {
        this.technicianRole = InMemoryRole.builder()
                .name("TECHNICIAN")
                .privilege(new InMemoryPrivilage("some-techincal-action"))
                .build();
        this.marketAdmin = InMemoryRole.builder()
                .name("MARKET_ADMIN")
                .privilege(new InMemoryPrivilage("add-workshop"))
                .privilege(new InMemoryPrivilage("remove-workshop"))
                .build();
    }


    public Optional<String> getUserRoles(String userId){
        return Optional.ofNullable(userToRoles.get(userId))
                .map(r -> r.name);
    }

    public Set<String> getUserPrivileges(String userId){
        return Optional.ofNullable(userToRoles.get(userId))
                .map(r -> r.privileges.stream().map(p -> p.name).collect(toSet()))
                .orElse(new HashSet<>());
    }

    public void registerAsTechnician(String username) {
        userToRoles.put(username, technicianRole);
    }

    public void registerAsMarketAdmin(String username){
        userToRoles.put(username, marketAdmin);
    }


    @Builder
    @AllArgsConstructor
    private static class InMemoryRole{
        private String name;
        @Singular
        private List<InMemoryPrivilage> privileges;
    }

    @AllArgsConstructor
    private static class InMemoryPrivilage{
        private String name;
    }

}
