package com.tinyroute.security;

import com.tinyroute.exception.LinkNotFoundException;
import com.tinyroute.model.Link;
import com.tinyroute.repository.LinkRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OwnershipGuard {
    private final LinkRepository links;

    public OwnershipGuard(LinkRepository links) {
        this.links = links;
    }

    public Link requireOwned(String id, UUID ownerId) {
        final UUID parsed;
        try {
            parsed = UUID.fromString(id);
            if (!parsed.toString().equalsIgnoreCase(id)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException invalid) {
            throw new LinkNotFoundException();
        }
        return links.findByIdAndOwnerId(parsed, ownerId).orElseThrow(LinkNotFoundException::new);
    }
}
