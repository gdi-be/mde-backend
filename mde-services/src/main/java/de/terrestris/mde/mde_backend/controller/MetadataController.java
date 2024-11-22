package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.ClientMetadata;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Log4j2
@Controller
@RequestMapping("/metadata")
public class MetadataController {

        @GetMapping
        public ResponseEntity<List<ClientMetadata>> getAll() {
            List<ClientMetadata> clientMetadataList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                ClientMetadata clientMetadata = new ClientMetadata();
                clientMetadata.setId(BigInteger.valueOf(i));
                clientMetadata.setTitle("Metadata Title " + i);
                clientMetadata.setMetadataId(UUID.randomUUID().toString());
                clientMetadata.setData(null);
                clientMetadataList.add(clientMetadata);
            }

            return ResponseEntity.ok(clientMetadataList);
        }

        @GetMapping("/{id}")
        public ResponseEntity<ClientMetadata> getById(JwtAuthenticationToken auth, @PathVariable BigInteger id) {

            String username = auth.getToken().getClaimAsString(StandardClaimNames.PREFERRED_USERNAME);
            List<String> authorities = auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

            log.info("User {} with authorities {} requested metadata with id {}", username, authorities, id);

            ClientMetadata clientMetadata = new ClientMetadata();
            clientMetadata.setId(id);
            clientMetadata.setTitle("Metadata Title");
            clientMetadata.setMetadataId(UUID.randomUUID().toString());
            clientMetadata.setData(null);

            return ResponseEntity.ok(clientMetadata);
        }

}
