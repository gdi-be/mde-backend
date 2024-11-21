package de.terrestris.mde.mde_backend.controller;

import de.terrestris.mde.mde_backend.model.ClientMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        public ResponseEntity<ClientMetadata> getById(@PathVariable BigInteger id) {
            ClientMetadata clientMetadata = new ClientMetadata();
            clientMetadata.setId(id);
            clientMetadata.setTitle("Metadata Title");
            clientMetadata.setMetadataId(UUID.randomUUID().toString());
            clientMetadata.setData(null);

            return ResponseEntity.ok(clientMetadata);
        }

}
