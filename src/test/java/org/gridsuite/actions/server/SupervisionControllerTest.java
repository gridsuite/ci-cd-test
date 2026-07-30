/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.powsybl.contingency.json.ContingencyJsonModule;
import com.powsybl.contingency.list.IdentifierContingencyList;
import com.powsybl.iidm.network.identifiers.IdBasedNetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifierContingencyList;
import org.gridsuite.actions.dto.contingency.IdBasedContingencyList;
import org.gridsuite.actions.server.repositories.IdBasedContingencyListRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Radouane KHOUADRI {@literal <redouane.khouadri_externe at rte-france.com>}
 */
@SpringBootTest(classes = {ActionsApplication.class, TestChannelBinderConfiguration.class})
@AutoConfigureMockMvc
class SupervisionControllerTest {

    @Autowired
    private ContingencyListService contingencyListService;

    @Autowired
    private IdBasedContingencyListRepository idBasedContingencyListRepository;

    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModule(new ContingencyJsonModule());

    }

    @AfterEach
    void tearDown() {
        idBasedContingencyListRepository.deleteAll();
    }

    @Test
    void testContingencyLists() throws Exception {
        int count = getContingencyListsCount();
        assertEquals(0, count);

        createIdBasedContingencyList(null, Instant.now(), "NHV1_NHV2_1", "Test");

        count = getContingencyListsCount();
        assertEquals(1, count);
    }

    private int getContingencyListsCount() throws Exception {
        String res = mvc.perform(get("/" + VERSION + "/supervision/contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        List<IdBasedContingencyList> contingencyListAttributes = objectMapper.readValue(res, new TypeReference<>() {
        });
        return contingencyListAttributes.size();
    }

    private void createIdBasedContingencyList(UUID listId, Instant modificationDate, String... identifiers) {
        List<NetworkElementIdentifier> networkElementIdentifiers = Arrays.stream(identifiers).map(id -> new NetworkElementIdentifierContingencyList(List.of(new IdBasedNetworkElementIdentifier(id)),
                id)).collect(Collectors.toList());
        IdBasedContingencyList idBasedContingencyList = new IdBasedContingencyList(listId, modificationDate, new IdentifierContingencyList(listId != null ? listId.toString() : "defaultName",
                networkElementIdentifiers));

        contingencyListService.createIdBasedContingencyList(null, idBasedContingencyList);
    }
}
