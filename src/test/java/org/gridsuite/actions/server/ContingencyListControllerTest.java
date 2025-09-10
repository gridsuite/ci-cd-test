/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
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
import com.powsybl.contingency.*;
import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import com.powsybl.contingency.json.ContingencyJsonModule;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.iidm.network.identifiers.IdBasedNetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifierContingencyList;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.iidm.network.test.ShuntTestCaseFactory;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import jakarta.servlet.ServletException;
import org.gridsuite.actions.server.dto.*;
import org.gridsuite.actions.server.entities.FormContingencyListEntity;
import org.gridsuite.actions.server.entities.NumericalFilterEntity;
import org.gridsuite.actions.server.repositories.FormContingencyListRepository;
import org.gridsuite.actions.server.repositories.IdBasedContingencyListRepository;
import org.gridsuite.actions.server.utils.EquipmentType;
import org.gridsuite.actions.server.utils.MatcherJson;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.Message;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.network.store.model.NetworkStoreApi.VERSION;
import static org.apache.commons.lang3.StringUtils.join;
import static org.gridsuite.actions.server.utils.NumericalFilterOperator.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@SpringBootTest(classes = {ActionsApplication.class, TestChannelBinderConfiguration.class})
@AutoConfigureMockMvc
class ContingencyListControllerTest {

    private static final long TIMEOUT = 1000;

    private static final UUID NETWORK_UUID = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
    private static final UUID NETWORK_UUID_2 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e5");
    private static final UUID NETWORK_UUID_3 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e6");
    private static final UUID NETWORK_UUID_4 = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e7");
    private static final UUID NETWORK_UUID_5 = UUID.fromString("0313daa6-9419-4d4f-8ed1-af555998665f");
    private static final String VARIANT_ID_1 = "variant_1";
    private static final String VARIANT_ID_2 = "variant_2";
    private static final String USER_ID_HEADER = "userId";

    private final String elementUpdateDestination = "element.update";

    private Network network;

    @Autowired
    private FormContingencyListRepository formContingencyListRepository;

    @Autowired
    private IdBasedContingencyListRepository idBasedContingencyListRepository;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private NetworkStoreService networkStoreService;

    @Autowired
    private OutputDestination output;

    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        formContingencyListRepository.deleteAll();
        idBasedContingencyListRepository.deleteAll();

        List<String> destinations = List.of(elementUpdateDestination);
        assertQueuesEmptyThenClear(destinations, output);
    }

    private static void assertQueuesEmptyThenClear(List<String> destinations, OutputDestination output) {
        try {
            destinations.forEach(destination -> assertNull(output.receive(TIMEOUT, destination), "Should not be any messages in queue " + destination + " : "));
        } catch (NullPointerException e) {
            // Ignoring
        } finally {
            output.clear(); // purge in order to not fail the other tests
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MockitoAnnotations.initMocks(this);

        network = EurostagTutorialExample1Factory.createWithMoreGenerators(new NetworkFactoryImpl());
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_ID_1);
        network.getVariantManager().setWorkingVariant(VARIANT_ID_1);
        // remove generator 'GEN2' from network in variant VARIANT_ID_1
        network.getGenerator("GEN2").remove();
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_ID_2);
        network.getVariantManager().setWorkingVariant(VARIANT_ID_2);
        //disconnect a line NHV1_NHV2_1
        network.getConnectable("NHV1_NHV2_1").getTerminals().forEach(Terminal::disconnect);
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        Network network2 = HvdcTestNetwork.createVsc(new NetworkFactoryImpl());
        Network network3 = SvcTestCaseFactory.createWithMoreSVCs(new NetworkFactoryImpl());
        Network network4 = ShuntTestCaseFactory.create(new NetworkFactoryImpl());
        Network network5 = EurostagTutorialExample1Factory.createWithFixedCurrentLimits(new NetworkFactoryImpl());
        given(networkStoreService.getNetwork(NETWORK_UUID, PreloadingStrategy.COLLECTION)).willReturn(network);
        given(networkStoreService.getNetwork(NETWORK_UUID_2, PreloadingStrategy.COLLECTION)).willReturn(network2);
        given(networkStoreService.getNetwork(NETWORK_UUID_3, PreloadingStrategy.COLLECTION)).willReturn(network3);
        given(networkStoreService.getNetwork(NETWORK_UUID_4, PreloadingStrategy.COLLECTION)).willReturn(network4);
        given(networkStoreService.getNetwork(NETWORK_UUID_5, PreloadingStrategy.COLLECTION)).willReturn(network5);

        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.registerModule(new ContingencyJsonModule());
    }

    @Test
    void test() throws Exception {
        UUID notFoundId = UUID.fromString("abcdef01-1234-5678-abcd-e123456789aa");

        String formContingencyList = "{\n" +
                "  \"equipmentType\": \"GENERATOR\"," +
                "  \"nominalVoltage\": {" +
                "    \"type\": \"GREATER_THAN\"," +
                "    \"value1\": \"100\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": [\"FR\", \"BE\"]" +
                "}";

        String formContingencyList2 = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"LESS_OR_EQUAL\"," +
                "    \"value1\": \"225\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries1\": [\"FR\", \"IT\", \"NL\"]" +
                "}";

        // LOAD is not a supported type => error case
        String formContingencyListError = "{\n" +
                "  \"equipmentType\": \"LOAD\"," +
                "  \"nominalVoltage\": {" +
                "    \"type\": \"EQUALITY\"," +
                "    \"value1\": \"380\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries\": []" +
                "}";

        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(formContingencyList)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID ticId = objectMapper.readValue(res, FormContingencyList.class).getId();

        // check first form insert
        mvc.perform(get("/" + VERSION + "/form-contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries1\":[], \"countries2\":[],\"metadata\":{\"type\":\"FORM\"}}]", false));

        mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(formContingencyList2)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Throwable e = null;
        try {
            mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                            .content(formContingencyListError)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk());
        } catch (Throwable ex) {
            e = ex;
        }
        assertInstanceOf(ServletException.class, e);

        // Check data
        mvc.perform(get("/" + VERSION + "/contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"type\":\"FORM\"},{\"type\":\"FORM\"}]", false));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries1\":[],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}},{" +
                        "\"equipmentType\":\"LINE\",\"nominalVoltage\":null,\"nominalVoltage1\":{\"type\":\"LESS_OR_EQUAL\",\"value1\":225.0,\"value2\":null},\"nominalVoltage2\":null,\"countries\":[],\"countries1\":[\"IT\",\"FR\",\"NL\"],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}}]", false));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + ticId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("{\"equipmentType\":\"GENERATOR\",\"nominalVoltage\":{\"type\":\"GREATER_THAN\",\"value1\":100.0,\"value2\":null},\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[\"BE\",\"FR\"],\"countries1\":[],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}}", false));

        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + notFoundId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        mvc.perform(get("/" + VERSION + "/contingency-lists/export")
                        .queryParam("contingencyListIds", ticId.toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), true)); // there is no network so all contingencies are invalid

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + ticId))
                .andExpect(status().isOk());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + ticId))
                .andExpect(status().isNotFound());
    }

    private static StringBuilder jsonVal(String id, String val, boolean trailingComma) {
        return new StringBuilder("\"").append(id).append("\": \"").append(val).append("\"").append(trailingComma ? ", " : "");
    }

    private static StringBuilder jsonVal(String id, Double val, boolean trailingComma) {
        return new StringBuilder("\"").append(id).append("\": ").append(val).append(trailingComma ? ", " : "");
    }

    private static String genFormContingencyList(EquipmentType type, Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        // single nominalVoltage => no range allowed
        assertNotEquals(RANGE, nominalVoltageOperator);

        return switch (type) {
            case LINE -> genFormContingencyListForLine(nominalVoltage, nominalVoltageOperator, countries);
            case HVDC_LINE -> genFormContingencyListForHVDC(nominalVoltage, nominalVoltageOperator, countries);
            case TWO_WINDINGS_TRANSFORMER ->
                    genFormContingencyListFor2WT(nominalVoltage, nominalVoltageOperator, countries);
            default -> genFormContingencyListForOthers(type, nominalVoltage, nominalVoltageOperator, countries);
        };
    }

    private static String genFormContingencyListForLine(Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        return genFormContingencyList(EquipmentType.LINE, -1.0, -1.0, EQUALITY, nominalVoltage, -1.0, nominalVoltageOperator, -1.0, -1.0, EQUALITY, Collections.emptySet(), countries, Collections.emptySet());
    }

    private static String genFormContingencyListForHVDC(Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        return genFormContingencyList(EquipmentType.HVDC_LINE, nominalVoltage, -1.0, nominalVoltageOperator, -1.0, -1.0, EQUALITY, -1.0, -1.0, EQUALITY, Collections.emptySet(), countries, Collections.emptySet());
    }

    private static String genFormContingencyListFor2WT(Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator, Set<String> countries) {
        return genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, nominalVoltage, -1.0, nominalVoltageOperator, -1.0, -1.0, EQUALITY, countries, Collections.emptySet(), Collections.emptySet());
    }

    private static String genFormContingencyListForOthers(EquipmentType type, Double nominalVoltage, NumericalFilterOperator nominalVoltageOperator,
                                                          Set<String> countries) {
        return genFormContingencyList(type, nominalVoltage, -1.0, nominalVoltageOperator, -1.0, -1.0, EQUALITY, -1.0, -1.0, EQUALITY, countries, Collections.emptySet(), Collections.emptySet());
    }

    private static String genFormContingencyList(EquipmentType type,
                                                 Double value01, Double value02, NumericalFilterOperator operator0,
                                                 Double value11, Double value12, NumericalFilterOperator operator1,
                                                 Double value21, Double value22, NumericalFilterOperator operator2,
                                                 Set<String> countries, Set<String> countries1, Set<String> countries2) {
        String jsonData = "{" + jsonVal("equipmentType", type.name(), true);
        // value01 == -1 => no filter on voltage-level (for all equipments except lines)
        if (value01 == -1.) {
            jsonData += "\"nominalVoltage\": null,";
        } else {
            jsonData += "\"nominalVoltage\": {"
                    + jsonVal("type", operator0.name(), true)
                    + jsonVal("value1", value01, operator0 == RANGE);
            if (operator0 == RANGE) {
                jsonData += jsonVal("value2", value02, false);
            }
            jsonData += "},";
        }
        // value11 == -1 => no first filter on voltage-level (lines)
        if (value11 == -1.) {
            jsonData += "\"nominalVoltage1\": null,";
        } else {
            jsonData += "\"nominalVoltage1\": {"
                    + jsonVal("type", operator1.name(), true)
                    + jsonVal("value1", value11, operator1 == RANGE);
            if (operator1 == RANGE) {
                jsonData += jsonVal("value2", value12, false);
            }
            jsonData += "},";
        }
        // value21 == -1 => no second filter on voltage-level (lines)
        if (value21 == -1.) {
            jsonData += "\"nominalVoltage2\": null,";
        } else {
            jsonData += "\"nominalVoltage2\": {"
                    + jsonVal("type", operator2.name(), true)
                    + jsonVal("value1", value21, operator2 == RANGE);
            if (operator2 == RANGE) {
                jsonData += jsonVal("value2", value22, false);
            }
            jsonData += "},";
        }
        jsonData += "\"countries\": [" + (!countries.isEmpty() ? "\"" + join(countries, "\",\"") + "\"" : "") + "], "; // all equipments except lines
        jsonData += "\"countries1\": [" + (!countries1.isEmpty() ? "\"" + join(countries1, "\",\"") + "\"" : "") + "], "; // lines
        jsonData += "\"countries2\": [" + (!countries2.isEmpty() ? "\"" + join(countries2, "\",\"") + "\"" : "") + "]}"; // lines
        return jsonData;
    }

    @Test
    void testDateFormContingencyList() throws Exception {
        String userId = "userId";
        String list = genFormContingencyList(EquipmentType.LINE, 11., EQUALITY, Set.of());
        UUID id = addNewFormContingencyList(list);
        ContingencyListMetadataImpl attributes = getMetadata(id);

        assertEquals(id, attributes.getId());
        Instant baseModificationDate = attributes.getModificationDate();

        mvc.perform(put("/" + VERSION + "/form-contingency-lists/" + id)
                        .content(list)
                        .contentType(APPLICATION_JSON)
                        .header(USER_ID_HEADER, "userId"))
                .andExpect(status().isOk());

        Message<byte[]> message = output.receive(TIMEOUT, elementUpdateDestination);
        assertEquals(id, message.getHeaders().get(NotificationService.HEADER_ELEMENT_UUID));
        assertEquals(userId, message.getHeaders().get(NotificationService.HEADER_MODIFIED_BY));

        attributes = getMetadata(id);
        assertTrue(baseModificationDate.toEpochMilli() < attributes.getModificationDate().toEpochMilli());
    }

    private UUID addNewFormContingencyList(String form) throws Exception {

        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(form)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        FormContingencyList list = objectMapper.readValue(res, FormContingencyList.class);
        FormContingencyList original = objectMapper.readValue(form, FormContingencyList.class);
        compareFormContingencyList(original, list);
        return list.getId();
    }

    @Test
    void testExportContingenciesLine() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");

        String lineForm = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, noCountries);
        String lineForm1 = genFormContingencyList(EquipmentType.LINE, 100., LESS_THAN, noCountries);
        String lineForm2 = genFormContingencyList(EquipmentType.LINE, 380., EQUALITY, noCountries);
        String lineForm3 = genFormContingencyList(EquipmentType.LINE, 390., GREATER_OR_EQUAL, noCountries);
        String lineForm4 = genFormContingencyList(EquipmentType.LINE, 390., LESS_OR_EQUAL, noCountries);
        String lineForm5 = genFormContingencyList(EquipmentType.LINE, 100., GREATER_THAN, noCountries);
        String lineForm6 = genFormContingencyList(EquipmentType.LINE, -1., GREATER_THAN, france);

        Contingency nhv1nload = new Contingency("NHV1_NHV2_1", List.of(new LineContingency("NHV1_NHV2_1")));
        Contingency nhv2nload = new Contingency("NHV1_NHV2_2", List.of(new LineContingency("NHV1_NHV2_2")));

        testExportContingencies(lineForm, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nhv1nload, nhv2nload), List.of())), NETWORK_UUID);
        testExportContingencies(lineForm1, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID);
        testExportContingencies(lineForm2, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nhv1nload, nhv2nload), List.of())), NETWORK_UUID);
        testExportContingencies(lineForm3, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID);
        testExportContingencies(lineForm4, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nhv1nload, nhv2nload), List.of())), NETWORK_UUID);
        testExportContingencies(lineForm5, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nhv1nload, nhv2nload), List.of())), NETWORK_UUID);
        testExportContingencies(lineForm6, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nhv1nload, nhv2nload), List.of())), NETWORK_UUID);
    }

    @Test
    void testExportContingencies2WTransfoWith1NumFilter() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        // with this network (EurostagTutorialExample1Factory::create), we have 2 FR substations and 2 2WT Transfos:
        // - NGEN_NHV1  term1: 24 kV term2: 380 kV
        // - NHV2_NLOAD term1: 380 kV term2: 150 kV
        var ngennhv1 = new Contingency("NGEN_NHV1", List.of(new TwoWindingsTransformerContingency("NGEN_NHV1")));
        var nvh2nload = new Contingency("NHV2_NLOAD", List.of(new TwoWindingsTransformerContingency("NHV2_NLOAD")));
        final String bothMatch = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(ngennhv1, nvh2nload), List.of()));
        final String matchLOAD = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nvh2nload), List.of()));
        final String matchGEN = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(ngennhv1), List.of()));
        final String noMatch = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of()));

        // single voltage filter
        String twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., EQUALITY, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 10., GREATER_THAN, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 10., EQUALITY, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 24., EQUALITY, noCountries);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 150., LESS_THAN, noCountries);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 150., LESS_OR_EQUAL, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 150., EQUALITY, noCountries);
        testExportContingencies(twtForm, matchLOAD, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 380., GREATER_THAN, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 380., GREATER_OR_EQUAL, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, 400., GREATER_OR_EQUAL, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
        // range
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 0., 24., RANGE, -1., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 24., 150., RANGE, -1., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 225., 380., RANGE, -1., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 400., 500., RANGE, -1., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
    }

    @Test
    void testExportContingencies2WTransfoWith2NumFilter() throws Exception {
        Set<String> noCountries = Collections.emptySet();

        final String matchLOAD = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(new Contingency("NHV2_NLOAD", List.of(new TwoWindingsTransformerContingency("NHV2_NLOAD")))), List.of()));
        final String matchGEN = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(new Contingency("NGEN_NHV1", List.of(new TwoWindingsTransformerContingency("NGEN_NHV1")))), List.of()));
        final String noMatch = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of()));

        // 2 voltage filters
        String twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 400., 500., RANGE, 24., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 150., 400., RANGE, 24., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 150., 400., RANGE, 150., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, matchLOAD, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1.0, -1.0, EQUALITY, 150., 400., RANGE, 33., -1., EQUALITY, noCountries, noCountries, noCountries);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);
    }

    @Test
    void testExportContingencies2WTransfoWithCountryFilter() throws Exception {
        Set<String> france = Collections.singleton("FR");
        Set<String> franceAndMore = Set.of("FR", "ZA", "ES");
        Set<String> belgium = Collections.singleton("BE");
        Set<String> italy = Collections.singleton("IT");
        Set<String> belgiumAndFrance = Set.of("FR", "BE");

        Contingency nhv1nload = new Contingency("NGEN_NHV1", List.of(new TwoWindingsTransformerContingency("NGEN_NHV1")));
        Contingency nhv2nload = new Contingency("NHV2_NLOAD", List.of(new TwoWindingsTransformerContingency("NHV2_NLOAD")));

        final String bothMatch = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nhv1nload, nhv2nload), List.of()));
        final String matchLOAD = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nhv2nload), List.of()));
        final String matchGEN = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(nhv1nload), List.of()));
        final String noMatch = objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of()));

        String twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, france);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, franceAndMore);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, belgium);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID);

        // NETWORK_UUID5 : a 2-country network (one substation FR, one BE)
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, belgium);
        testExportContingencies(twtForm, matchLOAD, NETWORK_UUID_5);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, france);
        testExportContingencies(twtForm, matchGEN, NETWORK_UUID_5);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, belgiumAndFrance);
        testExportContingencies(twtForm, bothMatch, NETWORK_UUID_5);
        twtForm = genFormContingencyList(EquipmentType.TWO_WINDINGS_TRANSFORMER, -1., GREATER_THAN, italy);
        testExportContingencies(twtForm, noMatch, NETWORK_UUID_5);
    }

    @Test
    void testExportContingenciesGenerator() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");
        Set<String> belgium = Collections.singleton("BE");

        String generatorForm1 = genFormContingencyList(EquipmentType.GENERATOR, -1., EQUALITY, france);
        String generatorForm4 = genFormContingencyList(EquipmentType.GENERATOR, 10., LESS_THAN, noCountries);
        String generatorForm5 = genFormContingencyList(EquipmentType.GENERATOR, -1., GREATER_THAN, france);
        String generatorForm6 = genFormContingencyList(EquipmentType.GENERATOR, -1., GREATER_THAN, belgium);
        Contingency gen = new Contingency("GEN", List.of(new GeneratorContingency("GEN")));
        Contingency gen2 = new Contingency("GEN2", List.of(new GeneratorContingency("GEN2")));

        testExportContingencies(generatorForm1, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(gen, gen2), List.of())), NETWORK_UUID);

        // test export on specific variant where generator 'GEN2' has been removed
        testExportContingencies(generatorForm1, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(gen), List.of())), NETWORK_UUID, VARIANT_ID_1);
        network.getVariantManager().setWorkingVariant(VariantManagerConstants.INITIAL_VARIANT_ID);

        testExportContingencies(generatorForm4, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID);
        testExportContingencies(generatorForm5, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(gen, gen2), List.of())), NETWORK_UUID);
        testExportContingencies(generatorForm6, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID);
    }

    @Test
    void testExportContingenciesSVC() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        Set<String> france = Collections.singleton("FR");
        Set<String> belgium = Collections.singleton("BE");

        String svcForm1 = genFormContingencyList(EquipmentType.STATIC_VAR_COMPENSATOR, -1., EQUALITY, noCountries);
        String svcForm4 = genFormContingencyList(EquipmentType.STATIC_VAR_COMPENSATOR, 100., LESS_THAN, noCountries);
        String svcForm5 = genFormContingencyList(EquipmentType.STATIC_VAR_COMPENSATOR, -1., LESS_THAN, france);
        String svcForm6 = genFormContingencyList(EquipmentType.STATIC_VAR_COMPENSATOR, -1., LESS_THAN, belgium);
        Contingency svc2 = new Contingency("SVC2", List.of(new StaticVarCompensatorContingency("SVC2")));
        Contingency svc3 = new Contingency("SVC3", List.of(new StaticVarCompensatorContingency("SVC3")));
        testExportContingencies(svcForm1, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(svc3, svc2), List.of())), NETWORK_UUID_3);
        testExportContingencies(svcForm4, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID_3);
        testExportContingencies(svcForm5, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(svc3, svc2), List.of())), NETWORK_UUID_3);
        testExportContingencies(svcForm6, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID_3);
    }

    @Test
    void testExportContingenciesShuntCompensator() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        String scForm1 = genFormContingencyList(EquipmentType.SHUNT_COMPENSATOR, -1., EQUALITY, noCountries);
        String scForm4 = genFormContingencyList(EquipmentType.SHUNT_COMPENSATOR, 300., EQUALITY, noCountries);
        testExportContingencies(scForm1, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(new Contingency("SHUNT", List.of(new ShuntCompensatorContingency("SHUNT")))), List.of())), NETWORK_UUID_4);
        testExportContingencies(scForm4, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID_4);
    }

    @Test
    void testExportContingenciesHVDC() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        String hvdcForm1 = genFormContingencyList(EquipmentType.HVDC_LINE, -1., EQUALITY, noCountries);
        String hvdcForm4 = genFormContingencyList(EquipmentType.HVDC_LINE, 400., EQUALITY, noCountries);
        String hvdcForm5 = genFormContingencyList(EquipmentType.HVDC_LINE, 300., LESS_THAN, noCountries);
        testExportContingencies(hvdcForm1, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(new Contingency("L", List.of(new HvdcLineContingency("L")))), List.of())), NETWORK_UUID_2);
        testExportContingencies(hvdcForm4, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(new Contingency("L", List.of(new HvdcLineContingency("L")))), List.of())), NETWORK_UUID_2);
        testExportContingencies(hvdcForm5, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID_2);
    }

    @Test
    void testExportContingenciesBusBar() throws Exception {
        Set<String> noCountries = Collections.emptySet();

        String bbsForm = genFormContingencyList(EquipmentType.BUSBAR_SECTION, -1., EQUALITY, noCountries);
        testExportContingencies(bbsForm, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID);
    }

    @Test
    void testExportContingenciesDanglingLine() throws Exception {
        Set<String> noCountries = Collections.emptySet();

        String dlForm = genFormContingencyList(EquipmentType.DANGLING_LINE, -1., EQUALITY, noCountries);
        testExportContingencies(dlForm, objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(), List.of())), NETWORK_UUID);
    }

    private static void compareFormContingencyList(FormContingencyList expected, FormContingencyList current) {
        // Ideally we shouldn't do that... it's because in the app null <=> [] for country list
        Set<String> expectedCountries = expected.getCountries();
        if (expectedCountries == null) {
            expectedCountries = Collections.emptySet();
        }
        Set<String> currentCountries = current.getCountries();
        if (currentCountries == null) {
            currentCountries = Collections.emptySet();
        }
        Set<String> expectedCountries1 = expected.getCountries1();
        if (expectedCountries1 == null) {
            expectedCountries1 = Collections.emptySet();
        }
        Set<String> currentCountries1 = current.getCountries1();
        if (currentCountries1 == null) {
            currentCountries1 = Collections.emptySet();
        }
        assertEquals(expectedCountries, currentCountries);
        assertEquals(expectedCountries1, currentCountries1);
        assertEquals(expected.getEquipmentType(), current.getEquipmentType());
        if (expected.getNominalVoltage() == null || current.getNominalVoltage() == null) {
            assertNull(expected.getNominalVoltage());
            assertNull(current.getNominalVoltage());
        } else {
            assertEquals(expected.getNominalVoltage().getValue1(), current.getNominalVoltage().getValue1());
        }
        if (expected.getNominalVoltage1() == null || current.getNominalVoltage1() == null) {
            assertNull(expected.getNominalVoltage1());
            assertNull(current.getNominalVoltage1());
        } else {
            assertEquals(expected.getNominalVoltage1().getValue1(), current.getNominalVoltage1().getValue1());
        }
    }

    private void testExportContingencies(String content, String expectedContent, UUID networkId) throws Exception {
        testExportContingencies(content, expectedContent, networkId, null);
    }

    private void testExportContingencies(String content, String expectedContent, UUID networkId, String variantId) throws Exception {
        // put the data
        UUID formContingencyListId = addNewFormContingencyList(content);
        // search matching equipments
        mvc.perform(get("/" + VERSION + "/contingency-lists/export?networkUuid=" + networkId + (variantId != null ? "&variantId=" + variantId : "") + "&contingencyListIds=" + formContingencyListId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(expectedContent));
        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + formContingencyListId))
                .andExpect(status().isOk());
    }

    @Test
    void testExportMultiContingencies() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        String lineForm = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, noCountries);
        String genForm = genFormContingencyList(EquipmentType.GENERATOR, 100., LESS_THAN, noCountries);

        List<UUID> contingencies = new ArrayList<>();
        contingencies.add(addNewFormContingencyList(lineForm));
        contingencies.add(addNewFormContingencyList(genForm));

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("/").append(VERSION).append("/contingency-lists/export");
        urlBuilder.append("?networkUuid=").append(NETWORK_UUID);
        urlBuilder.append("&variantId=").append(VARIANT_ID_1);

        contingencies.forEach(id -> urlBuilder.append("&").append("contingencyListIds").append("=").append(id));

        Contingency expectedContingency1 = new Contingency("NHV1_NHV2_1", List.of(new LineContingency("NHV1_NHV2_1")));
        Contingency expectedContingency2 = new Contingency("NHV1_NHV2_2", List.of(new LineContingency("NHV1_NHV2_2")));
        Contingency expectedContingency3 = new Contingency("GEN", List.of(new GeneratorContingency("GEN")));

        mvc.perform(get(urlBuilder.toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(expectedContingency1, expectedContingency2, expectedContingency3), List.of()))));

        // not found contingency
        UUID wrongUuid = UUID.randomUUID();
        urlBuilder.append("&contingencyListIds=").append(wrongUuid);
        mvc.perform(get(urlBuilder.toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(new ContingencyListExportResult(List.of(expectedContingency1, expectedContingency2, expectedContingency3), List.of(wrongUuid)))));
        // delete data
        contingencies.forEach(id -> {
            try {
                mvc.perform(delete("/" + VERSION + "/contingency-lists/" + id.toString()))
                                .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    @Test
    void testExportMultiContingenciesInfo() throws Exception {
        Set<String> noCountries = Collections.emptySet();
        String lineForm = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, noCountries);
        String genForm = genFormContingencyList(EquipmentType.GENERATOR, 100., LESS_THAN, noCountries);

        List<UUID> contingencies = new ArrayList<>();
        contingencies.add(addNewFormContingencyList(lineForm));
        contingencies.add(addNewFormContingencyList(genForm));

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("/").append(VERSION).append("/contingency-lists/contingency-infos/export");
        urlBuilder.append("?networkUuid=").append(NETWORK_UUID);
        urlBuilder.append("&variantId=").append(VARIANT_ID_1);

        contingencies.forEach(id -> urlBuilder.append("&").append("ids").append("=").append(id));

        ContingencyInfos expectedContingency1 = new ContingencyInfos("NHV1_NHV2_1", new Contingency("NHV1_NHV2_1", null, List.of(new LineContingency("NHV1_NHV2_1"))), null, Set.of());
        ContingencyInfos expectedContingency2 = new ContingencyInfos("NHV1_NHV2_2", new Contingency("NHV1_NHV2_2", null, List.of(new LineContingency("NHV1_NHV2_2"))), null, Set.of());
        ContingencyInfos expectedContingency3 = new ContingencyInfos("GEN", new Contingency("GEN", null, List.of(new GeneratorContingency("GEN"))), null, Set.of());

        mvc.perform(get(urlBuilder.toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(expectedContingency1, expectedContingency2, expectedContingency3))));
        // delete data
        contingencies.forEach(id -> {
            try {
                mvc.perform(delete("/" + VERSION + "/contingency-lists/" + id.toString()))
                    .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );
    }

    @Test
    void testExportContingenciesInfos() throws Exception {
        Instant date = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, date, "NHV1_NHV2_1", "Test");
        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        mvc.perform(get("/" + VERSION + "/contingency-lists/contingency-infos/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1 + "&ids=" + contingencyListId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json("[{\"id\":\"NHV1_NHV2_1\",\"contingency\":{\"id\":\"NHV1_NHV2_1\",\"elements\":[{\"id\":\"NHV1_NHV2_1\",\"type\":\"LINE\"}]},\"notFoundElements\":null},{\"id\":\"Test\",\"contingency\":null,\"notFoundElements\":[\"Test\"]}]"));
        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());
    }

    @Test
    void testExportContingenciesNotConnectedAndNotFoundElements() throws Exception {
        NetworkElementIdentifierContingencyList networkElementIdentifierContingencyList = new NetworkElementIdentifierContingencyList(List.of(new IdBasedNetworkElementIdentifier("NHV1_NHV2_1"), new IdBasedNetworkElementIdentifier("NHV1_NHV2_2"), new IdBasedNetworkElementIdentifier("TEST1")), "default");
        IdBasedContingencyList idBasedContingencyList = new IdBasedContingencyList(null, Instant.now(), new IdentifierContingencyList("defaultName", List.of(networkElementIdentifierContingencyList)));

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        Contingency contingency = new Contingency("default", null, List.of(new LineContingency("NHV1_NHV2_2"), new LineContingency("NHV1_NHV2_1")));
        ContingencyInfos contingencyExpectedResult = new ContingencyInfos("default", contingency, Set.of("TEST1"), Set.of("NHV1_NHV2_1"));

        mvc.perform(get("/" + VERSION + "/contingency-lists/contingency-infos/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_2 + "&ids=" + contingencyListId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(contingencyExpectedResult))))
                .andReturn();

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());
    }

    private ContingencyListMetadataImpl getMetadata(UUID id) throws Exception {
        var res = mvc.perform(get("/" + VERSION + "/contingency-lists/metadata?ids=" + id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<ContingencyListMetadataImpl> contingencyListAttributes = objectMapper.readValue(res, new TypeReference<>() {
        });
        assertEquals(1, contingencyListAttributes.size());
        return contingencyListAttributes.get(0);
    }

    @Test
    void testExportContingencies3() {
        Throwable e = null;
        UUID id = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e8");
        String lineFilters = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, Collections.emptySet());
        try {
            testExportContingencies(lineFilters, "", id);
        } catch (Throwable ex) {
            e = ex;
        }
        assertInstanceOf(ServletException.class, e);
        assertEquals("Request processing failed: com.powsybl.commons.PowsyblException: Network '7928181c-7977-4592-ba19-88027e4254e8' not found", e.getMessage());
    }

    @Test
    void testCreateContingencyBadOperator() throws Exception {
        String lineFilters = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"BAD_OP\"," +
                "    \"value1\": \"100\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries1\": [\"FR\", \"BE\"]" +
                "}";

        mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(lineFilters)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testCreateContingencyNoValue1() throws Exception {
        String formContingencyList = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"EQUALITY\"," +
                "    \"value1\": \"null\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"nominalVoltage2\": {" +
                "    \"type\": \"LESS_OR_EQUAL\"," +
                "    \"value1\": \"null\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries1\": [\"FR\", \"BE\"]" +
                "}";
        // creation
        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(formContingencyList)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID clId = objectMapper.readValue(res, FormContingencyList.class).getId();
        // retrieve it, no numeric filters created (because of null values)
        String noNominalFilter1Response = "{\"equipmentType\":\"LINE\",\"nominalVoltage\":null,\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[],\"countries1\":[\"BE\",\"FR\"],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}}";
        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + clId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(noNominalFilter1Response, false));

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + clId)).andExpect(status().isOk());
    }

    @Test
    void testCreateContingencyNoValue2ForRange() throws Exception {
        String formContingencyList = "{\n" +
                "  \"equipmentType\": \"LINE\"," +
                "  \"nominalVoltage1\": {" +
                "    \"type\": \"RANGE\"," +
                "    \"value1\": \"63.\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"nominalVoltage2\": {" +
                "    \"type\": \"RANGE\"," +
                "    \"value1\": \"44.\"," +
                "    \"value2\": \"null\"" +
                "  }," +
                "  \"countries1\": [\"FR\", \"BE\"]" +
                "}";
        // creation
        String res = mvc.perform(post("/" + VERSION + "/form-contingency-lists")
                        .content(formContingencyList)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID clId = objectMapper.readValue(res, FormContingencyList.class).getId();
        // retrieve it, no numeric filters created (because of null values)
        String noNominalFilter1Response = "{\"equipmentType\":\"LINE\",\"nominalVoltage\":null,\"nominalVoltage1\":null,\"nominalVoltage2\":null,\"countries\":[],\"countries1\":[\"BE\",\"FR\"],\"countries2\":[], \"metadata\":{\"type\":\"FORM\"}}";
        mvc.perform(get("/" + VERSION + "/form-contingency-lists/" + clId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(content().json(noNominalFilter1Response, false));

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + clId)).andExpect(status().isOk());
    }

    @Test
    void formContingencyListEntityTest() {
        FormContingencyListEntity entity = new FormContingencyListEntity();
        entity.setEquipmentType("LINE");
        entity.setNominalVoltage1(new NumericalFilterEntity(null, EQUALITY, 225., null));
        entity.setCountries1(Set.of("FRANCE", "ITALY"));

        assertEquals("LINE", entity.getEquipmentType());
        assertEquals(225., entity.getNominalVoltage1().getValue1(), 0.1);
        assertEquals(EQUALITY, entity.getNominalVoltage1().getOperator());
        assertTrue(entity.getCountries1().contains("FRANCE"));
        assertTrue(entity.getCountries1().contains("ITALY"));
    }

    @Test
    void duplicateFormContingencyList() throws Exception {
        String list = genFormContingencyList(EquipmentType.LINE, 11., EQUALITY, Set.of());
        UUID id = addNewFormContingencyList(list);

        String newUuid = mvc.perform(post("/" + VERSION + "/form-contingency-lists?duplicateFrom=" + id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNotNull(newUuid);
        mvc.perform(post("/" + VERSION + "/form-contingency-lists?duplicateFrom=" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private static IdBasedContingencyList createIdBasedContingencyList(UUID listId, Instant modificationDate, String... identifiers) {
        List<NetworkElementIdentifier> networkElementIdentifiers = Arrays.stream(identifiers).map(id -> new NetworkElementIdentifierContingencyList(List.of(new IdBasedNetworkElementIdentifier(id)), id)).collect(Collectors.toList());
        return new IdBasedContingencyList(listId, modificationDate, new IdentifierContingencyList(listId != null ? listId.toString() : "defaultName", networkElementIdentifiers));
    }

    private int getContingencyListsCount() throws Exception {
        String res = mvc.perform(get("/" + VERSION + "/contingency-lists")
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        List<IdBasedContingencyList> contingencyListAttributes = objectMapper.readValue(res, new TypeReference<>() {
        });
        return contingencyListAttributes.size();
    }

    private static void matchContingencyListMetadata(ContingencyListMetadata metadata1, ContingencyListMetadata metadata2) {
        assertEquals(metadata1.getId(), metadata2.getId());
        assertEquals(metadata1.getType(), metadata2.getType());
        assertTrue((metadata1.getModificationDate().toEpochMilli() - metadata2.getModificationDate().toEpochMilli()) < 2000);
    }

    private void matchIdBasedContingencyList(IdBasedContingencyList cl1, IdBasedContingencyList cl2) {
        matchContingencyListMetadata(cl1.getMetadata(), cl2.getMetadata());
        assertTrue(new MatcherJson<>(objectMapper, cl1.getIdentifierContingencyList()).matchesSafely(cl2.getIdentifierContingencyList()));
    }

    @Test
    void createIdBasedContingencyList() throws Exception {
        Instant modificationDate = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "NHV1_NHV2_1");

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        IdBasedContingencyList resultList = createIdBasedContingencyList(contingencyListId, modificationDate, "NHV1_NHV2_1");

        res = mvc.perform(get("/" + VERSION + "/identifier-contingency-lists/" + contingencyListId)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        matchIdBasedContingencyList(objectMapper.readValue(res, IdBasedContingencyList.class), resultList);

        mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ContingencyListMetadataImpl attributes = getMetadata(contingencyListId);
        assertEquals(attributes.getId(), contingencyListId);

        assertEquals(2, getContingencyListsCount());

        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());

        assertEquals(1, getContingencyListsCount());
    }

    @Test
    void createIdBasedContingencyListError() throws Exception {
        Instant modificationDate = Instant.now();

        IdBasedContingencyList idBasedContingencyList1 = createIdBasedContingencyList(null, modificationDate, "");
        mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList1))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        IdBasedContingencyList idBasedContingencyList2 = createIdBasedContingencyList(null, modificationDate, new String[0]);
        mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList2))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateBasedContingencyList() throws Exception {
        Instant modificationDate = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "id1");
        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID id = objectMapper.readValue(res, IdBasedContingencyList.class).getId();

        String newUuid = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists?duplicateFrom=" + id))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertNotNull(newUuid);

        mvc.perform(post("/" + VERSION + "/identifier-contingency-lists?duplicateFrom=" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void exportIdBasedContingencyList() throws Exception {
        Instant modificationDate = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "NHV1_NHV2_1");

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();

        mvc.perform(get("/" + VERSION + "/contingency-lists/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .queryParam("contingencyListIds", contingencyListId.toString())
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // delete data
        mvc.perform(delete("/" + VERSION + "/contingency-lists/" + contingencyListId))
                .andExpect(status().isOk());
    }

    @Test
    void testExportUnknownContingencyList() throws Exception {
        mvc.perform(get("/" + VERSION + "/contingency-lists/" + UUID.randomUUID() + "/export?networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    void testCountUnknownContingencyList() throws Exception {
        String res = mvc.perform(get("/" + VERSION + "/contingency-lists/count?ids=" + UUID.randomUUID() + "&networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(0, Integer.parseInt(res));
    }

    @Test
    void testCountContingencyList() throws Exception {
        // insert 3 contingency lists
        Set<String> noCountries = Collections.emptySet();
        String lineForm1 = genFormContingencyList(EquipmentType.LINE, -1., EQUALITY, noCountries); // 2 defaults
        String lineForm2 = genFormContingencyList(EquipmentType.LINE, 100., LESS_THAN, noCountries); // none
        String lineForm3 = genFormContingencyList(EquipmentType.LINE, 380., EQUALITY, noCountries); // 2 defaults
        UUID cl1 = addNewFormContingencyList(lineForm1);
        UUID cl2 = addNewFormContingencyList(lineForm2);
        UUID cl3 = addNewFormContingencyList(lineForm3);

        // count them (incl a wrong uuid)
        String res = mvc.perform(get("/" + VERSION + "/contingency-lists/count?ids=" + UUID.randomUUID() + "&ids=" + cl1 + "&ids=" + cl2 + "&ids=" + cl3 + "&networkUuid=" + NETWORK_UUID + "&variantId=" + VARIANT_ID_1)
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(4, Integer.parseInt(res));
    }

    @Test
    void modifyIdBasedContingencyList() throws Exception {
        Instant modificationDate = Instant.now();
        IdBasedContingencyList idBasedContingencyList = createIdBasedContingencyList(null, modificationDate, "LINE1");

        String res = mvc.perform(post("/" + VERSION + "/identifier-contingency-lists")
                        .content(objectMapper.writeValueAsString(idBasedContingencyList))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        UUID contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();

        IdBasedContingencyList newList = createIdBasedContingencyList(contingencyListId, modificationDate, "LINE2");

        mvc.perform(put("/" + VERSION + "/identifier-contingency-lists/" + newList.getId())
                        .content(objectMapper.writeValueAsString(newList))
                        .contentType(APPLICATION_JSON)
                        .header(USER_ID_HEADER, USER_ID_HEADER))
                .andExpect(status().isOk());

        Message<byte[]> message = output.receive(TIMEOUT, elementUpdateDestination);
        assertEquals(contingencyListId, message.getHeaders().get(NotificationService.HEADER_ELEMENT_UUID));
        assertEquals(USER_ID_HEADER, message.getHeaders().get(NotificationService.HEADER_MODIFIED_BY));

        contingencyListId = objectMapper.readValue(res, IdBasedContingencyList.class).getId();
        IdBasedContingencyList resultList = createIdBasedContingencyList(contingencyListId, modificationDate, "LINE2");
        res = mvc.perform(get("/" + VERSION + "/identifier-contingency-lists/" + contingencyListId)
                        .contentType(APPLICATION_JSON))
                .andReturn().getResponse().getContentAsString();

        matchIdBasedContingencyList(objectMapper.readValue(res, IdBasedContingencyList.class), resultList);

        mvc.perform(put("/" + VERSION + "/identifier-contingency-lists/" + UUID.randomUUID())
                        .content(objectMapper.writeValueAsString(newList))
                        .contentType(APPLICATION_JSON)
                        .header(USER_ID_HEADER, USER_ID_HEADER))
                .andExpect(status().isNotFound());
    }
}
