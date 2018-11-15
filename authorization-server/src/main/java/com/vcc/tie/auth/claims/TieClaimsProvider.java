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
    private Map<String, List<InMemoryPrivilage>> userToPrivileges = new ConcurrentHashMap<>();



    public TieClaimsProvider() {

    }


    public Set<String> getUserPrivileges(String userId){
        return Optional.ofNullable(userToPrivileges.get(userId))
                .map(r -> r.stream().map(p -> p.name)
                .collect(toSet()))
                .orElse(new HashSet<>());
    }

    public void updateWithEditorPrivilage(String username) {
        userToPrivileges.put(username, Arrays.asList(new InMemoryPrivilage("SUPPORT_ARTICLE_READ"), new InMemoryPrivilage("SUPPORT_ARTICLE_WRITE")));
    }

    public void updateWithReaderPrivilage(String username) {
        userToPrivileges.put(username, Arrays.asList(new InMemoryPrivilage("SUPPORT_ARTICLE_READ")));
    }


    @AllArgsConstructor
    private static class InMemoryPrivilage{
        private String name;
    }

}
