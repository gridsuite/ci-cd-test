/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.actions.dto.ContingencyListMetadata;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Radouane KHOUADRI {@literal <redouane.khouadri_externe at rte-france.com>}
 */
@RestController
@RequestMapping(value = "/" + ActionsApi.API_VERSION + "/supervision")
@Tag(name = "Actions server - Supervision")
@ComponentScan(basePackageClasses = ContingencyListService.class)
public class SupervisionController {

    private final ContingencyListService service;

    public SupervisionController(ContingencyListService service) {
        this.service = service;
    }

    @GetMapping(value = "/contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all contingency lists metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All contingency lists metadata")})
    public ResponseEntity<List<ContingencyListMetadata>> getContingencyListsMetadata() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyListsMetadata());
    }
}
