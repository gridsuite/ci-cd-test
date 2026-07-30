package org.gridsuite.actions.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gridsuite.filter.AbstractFilter;
import org.gridsuite.filter.identifierlistfilter.IdentifierListFilter;
import org.gridsuite.filter.identifierlistfilter.IdentifierListFilterEquipmentAttributes;
import org.gridsuite.filter.utils.EquipmentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gridsuite.actions.server.service.FilterService.*;

@RestClientTest(FilterService.class)
@ContextConfiguration(classes = {FilterService.class})
class FilterServiceTest {
    @Autowired
    FilterService filterService;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void getFilters() throws JsonProcessingException {
        List<UUID> filtersUuids = List.of(UUID.randomUUID(), UUID.randomUUID());
        String filtersUuidAsQueryParams = "?ids=" + filtersUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        List<IdentifierListFilterEquipmentAttributes> filterEquipmentAttributes = List.of(new IdentifierListFilterEquipmentAttributes("GEN", 30.));
        List<AbstractFilter> expectedFilterList = List.of(new IdentifierListFilter(UUID.randomUUID(), Date.from(Instant.now()), EquipmentType.GENERATOR, filterEquipmentAttributes));

        server.expect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.requestTo("http://localhost:5027" + DELIMITER + FILTER_API_VERSION + FILTER_END_POINT_METADATA + filtersUuidAsQueryParams))
            .andRespond(MockRestResponseCreators.withSuccess()
                .contentType(MediaType.APPLICATION_JSON)
                .body(objectMapper.writeValueAsString(expectedFilterList)));

        List<AbstractFilter> response = filterService.getFilters(filtersUuids);
        assertThat(response).usingRecursiveComparison().isEqualTo(expectedFilterList);
    }

    @Test
    void getFiltersWithNullResponse() {
        List<UUID> filtersUuids = List.of(UUID.randomUUID(), UUID.randomUUID());
        String filtersUuidAsQueryParams = "?ids=" + filtersUuids.stream().map(UUID::toString).collect(Collectors.joining(","));
        List<AbstractFilter> expectedFilterList = List.of();

        server.expect(MockRestRequestMatchers.method(HttpMethod.GET))
            .andExpect(MockRestRequestMatchers.requestTo("http://localhost:5027" + DELIMITER + FILTER_API_VERSION + FILTER_END_POINT_METADATA + filtersUuidAsQueryParams))
            .andRespond(MockRestResponseCreators.withSuccess()
                .contentType(MediaType.APPLICATION_JSON));

        List<AbstractFilter> response = filterService.getFilters(filtersUuids);
        assertThat(response).usingRecursiveComparison().isEqualTo(expectedFilterList);
    }
}
