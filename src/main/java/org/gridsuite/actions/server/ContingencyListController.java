/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gridsuite.actions.server.dto.*;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + ActionsApi.API_VERSION)
@Tag(name = "Actions server")
@ComponentScan(basePackageClasses = ContingencyListService.class)
public class ContingencyListController {

    private final ContingencyListService service;

    public ContingencyListController(ContingencyListService service) {
        this.service = service;
    }

    @GetMapping(value = "/form-contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all form contingency lists")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All form contingency lists")})
    public ResponseEntity<List<PersistentContingencyList>> getFormContingencyLists() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getFormContingencyLists());
    }

    @GetMapping(value = "/contingency-lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all contingency lists metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "All contingency lists metadata")})
    public ResponseEntity<List<ContingencyListMetadata>> getContingencyListsMetadata() {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyListsMetadata());
    }

    @GetMapping(value = "/form-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get form contingency list by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list"),
        @ApiResponse(responseCode = "404", description = "The form contingency list does not exists")})
    public ResponseEntity<PersistentContingencyList> getFormContingencyList(@PathVariable("id") UUID id) {
        return service.getFormContingencyList(id).map(contingencyList -> ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(contingencyList))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/contingency-lists/count", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Evaluate all contingency lists passed and return the global count")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The total contingency count")})
    public ResponseEntity<Integer> getContingencyCount(@Parameter(description = "Contingency list ids") @RequestParam(name = "ids") List<UUID> ids,
                                                       @RequestParam(value = "networkUuid", required = false) UUID networkUuid,
                                                       @RequestParam(value = "variantId", required = false) String variantId) {
        return ResponseEntity.ok().body(service.getContingencyCount(ids, networkUuid, variantId));
    }

    @GetMapping(value = "/contingency-lists/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Evaluate and export a contingency list to PowSyBl JSON format")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The contingency list in PowSyBl JSON format"),
                           @ApiResponse(responseCode = "404", description = "The contingency list does not exists")})
    public ResponseEntity<ContingencyListExportResult> exportContingencyList(@RequestParam(value = "networkUuid", required = false) UUID networkUuid,
                                                                             @RequestParam(value = "variantId", required = false) String variantId,
                                                                             @RequestParam(value = "contingencyListIds") List<UUID> contingencyListIds) {
        return ResponseEntity.ok().body(service.exportContingencyList(contingencyListIds, networkUuid, variantId));
    }

    @GetMapping(value = "/contingency-lists/contingency-infos/export", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Evaluate and export a contingency infos list to JSON format")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The contingency list in JSON format"),
                           @ApiResponse(responseCode = "404", description = "The contingency list does not exists")})
    public ResponseEntity<List<ContingencyInfos>> exportContingencyInfosList(@RequestParam(value = "networkUuid", required = false) UUID networkUuid,
                                                                             @RequestParam(value = "variantId", required = false) String variantId,
                                                                             @RequestParam(value = "ids") List<UUID> ids) {
        return ResponseEntity.ok().body(service.exportContingencyInfosList(ids, networkUuid, variantId));
    }

    @PostMapping(value = "/form-contingency-lists", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list have been created successfully")})
    public ResponseEntity<PersistentContingencyList> createFormContingencyList(@RequestParam(required = false, value = "id") UUID id,
                                                                               @RequestBody FormContingencyList formContingencyList) {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(service.createFormContingencyList(id, formContingencyList));
    }

    @PostMapping(value = "/identifier-contingency-lists", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create an identifier contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The identifier contingency list have been created successfully")})
    public ResponseEntity<PersistentContingencyList> createIdentifierContingencyList(@RequestParam(required = false, value = "id") UUID id,
                                                                                     @RequestBody IdBasedContingencyList idBasedContingencyList) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.createIdBasedContingencyList(id, idBasedContingencyList));
    }

    @GetMapping(value = "/identifier-contingency-lists/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get identifier contingency list by id")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The identifier contingency list"),
        @ApiResponse(responseCode = "404", description = "The identifier contingency list does not exists")})
    public ResponseEntity<PersistentContingencyList> getIdentifierContingencyList(@PathVariable("id") UUID id) {
        return service.getIdBasedContingencyList(id, null).map(contingencyList -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/identifier-contingency-lists")
    @Operation(summary = "Create a identifier contingency list from another existing one")
        @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The identifier contingency list have been duplicated successfully"),
                               @ApiResponse(responseCode = "404", description = "Source script contingency list not found")})
    public ResponseEntity<UUID> duplicateIdentifierContingencyList(@RequestParam("duplicateFrom") UUID identifierContingencyListsId) {
        return service.duplicateIdentifierContingencyList(identifierContingencyListsId).map(contingencyList -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/identifier-contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a identifier contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The identifier contingency list have been modified successfully")})
    public ResponseEntity<Void> modifyIdentifierContingencyList(
            @PathVariable UUID id,
            @RequestBody IdBasedContingencyList idBasedContingencyList,
            @RequestHeader("userId") String userId) {
        try {
            service.modifyIdBasedContingencyList(id, idBasedContingencyList, userId);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping(value = "/form-contingency-lists", params = "duplicateFrom")
    @Operation(summary = "Create a form contingency list from another existing one")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list have been duplicated successfully"),
                           @ApiResponse(responseCode = "404", description = "Source form contingency list not found")})
    public ResponseEntity<UUID> duplicateFormContingencyList(@RequestParam("duplicateFrom") UUID formContingencyListsId) {
        return service.duplicateFormContingencyList(formContingencyListsId).map(contingencyList -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(contingencyList))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/form-contingency-lists/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify a form contingency list")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The form contingency list have been modified successfully")})
    public ResponseEntity<Void> modifyFormContingencyList(
            @PathVariable UUID id,
            @RequestBody FormContingencyList formContingencyList,
            @RequestHeader("userId") String userId) {
        try {
            service.modifyFormContingencyList(id, formContingencyList, userId);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping(value = "/contingency-lists/{id}")
    @Operation(summary = "delete the contingency list")
    @ApiResponse(responseCode = "200", description = "The contingency list has been deleted")
    public ResponseEntity<Void> deleteContingencyList(@PathVariable("id") UUID id) {
        try {
            service.deleteContingencyList(id);
            return ResponseEntity.ok().build();
        } catch (EmptyResultDataAccessException ignored) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping(value = "/contingency-lists/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get contingency lists metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "contingency lists metadata"),
        @ApiResponse(responseCode = "404", description = "The contingency list does not exists")})
    public ResponseEntity<List<ContingencyListMetadata>> getContingencyListsMetadata(@RequestParam("ids") List<UUID> ids) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(service.getContingencyListsMetadata(ids));
    }
}
