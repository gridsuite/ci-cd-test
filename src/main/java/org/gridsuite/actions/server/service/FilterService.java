/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.actions.server.service;

import lombok.Getter;
import org.gridsuite.actions.dto.FilterAttributes;
import org.gridsuite.filter.AbstractFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FilterService {
    public static final String FILTER_END_POINT_INFOS_IDS = "/filters/infos";
    public static final String FILTER_END_POINT_METADATA = "/filters/metadata";
    public static final String DELIMITER = "/";
    public static final String FILTER_API_VERSION = "v1";

    @Getter
    private final String baseUri;
    private final RestTemplate restTemplate;

    @Autowired
    public FilterService(@Value("${gridsuite.services.filter-server.base-uri:http://filter-server/}") String baseUri,
                         RestTemplateBuilder restTemplateBuilder) {
        this.baseUri = baseUri;
        this.restTemplate = restTemplateBuilder.build();
    }

    public List<FilterAttributes> getFiltersAttributes(List<UUID> filtersUuid) {
        if (filtersUuid.isEmpty()) {
            return new ArrayList<>();
        }
        String endPointUrl = getBaseUri() + DELIMITER + FILTER_API_VERSION + FILTER_END_POINT_INFOS_IDS;
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(endPointUrl);
        uriComponentsBuilder.queryParam("filterUuids", filtersUuid);
        var uriComponent = uriComponentsBuilder.buildAndExpand();

        HttpHeaders headers = new HttpHeaders();

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<List<FilterAttributes>> response = restTemplate.exchange(uriComponent.toUriString(),
            HttpMethod.GET, entity, new ParameterizedTypeReference<>() { });
        return response.getBody() != null ? response.getBody() : new ArrayList<>();
    }

    public List<AbstractFilter> getFilters(List<UUID> filtersUuids) {
        var ids = !filtersUuids.isEmpty() ? "?ids=" + filtersUuids.stream().map(UUID::toString).collect(Collectors.joining(",")) : "";
        String path = UriComponentsBuilder.fromPath(DELIMITER + FILTER_API_VERSION + FILTER_END_POINT_METADATA + ids)
            .buildAndExpand()
            .toUriString();
        ResponseEntity<List<AbstractFilter>> response = restTemplate.exchange(getBaseUri() + path, HttpMethod.GET, null, new ParameterizedTypeReference<>() { });
        return response.getBody() != null ? response.getBody() : new ArrayList<>();
    }
}
