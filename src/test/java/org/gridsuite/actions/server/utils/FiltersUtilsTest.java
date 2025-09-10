/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.utils;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.iidm.network.test.HvdcTestNetwork;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
class FiltersUtilsTest {
    @Test
    void testInjection() {
        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators(new NetworkFactoryImpl());
        // just test Generator for common injection equipment
        // Injection eqpt can have 1 single country filter
        Generator generator = network.getGenerator("GEN");
        assertNotNull(generator);
        assertTrue(FiltersUtils.injectionMatch(generator.getTerminal(), List.of("FR", "BE")));
        assertFalse(FiltersUtils.injectionMatch(generator.getTerminal(), List.of("DE")));
        assertTrue(FiltersUtils.injectionMatch(generator.getTerminal(), List.of()));
    }

    @Test
    void testHvdcLine() {
        Network network = HvdcTestNetwork.createVsc(new NetworkFactoryImpl());
        network.getSubstation("S2").setCountry(Country.IT);
        // S1 = FR, S2 = IT
        HvdcLine hvdc = network.getHvdcLine("L");
        assertNotNull(hvdc);
        // HVDC can have 2 country filters
        assertTrue(FiltersUtils.hvdcLineMatch(hvdc, List.of(), List.of()));
        assertTrue(FiltersUtils.hvdcLineMatch(hvdc, List.of("FR"), List.of()));
        assertTrue(FiltersUtils.hvdcLineMatch(hvdc, List.of(), List.of("FR")));
        assertTrue(FiltersUtils.hvdcLineMatch(hvdc, List.of("IT"), List.of()));
        assertTrue(FiltersUtils.hvdcLineMatch(hvdc, List.of(), List.of("IT")));
        assertTrue(FiltersUtils.hvdcLineMatch(hvdc, List.of("FR"), List.of("IT")));
        assertTrue(FiltersUtils.hvdcLineMatch(hvdc, List.of("ES", "FR"), List.of("ZA", "IT")));

        assertFalse(FiltersUtils.hvdcLineMatch(hvdc, List.of("ES"), List.of("ZA")));
        assertFalse(FiltersUtils.hvdcLineMatch(hvdc, List.of(), List.of("ZA")));
        assertFalse(FiltersUtils.hvdcLineMatch(hvdc, List.of("FR", "IT"), List.of("ZA")));
    }

    @Test
    void testLine() {
        Network network = EurostagTutorialExample1Factory.create(new NetworkFactoryImpl());
        network.getSubstation("P2").setCountry(Country.PT);
        Line line1 = network.getLine("NHV1_NHV2_1");
        assertNotNull(line1);
        // Line can have 2 country filters
        assertTrue(FiltersUtils.lineMatch(line1, List.of(), List.of()));
        assertTrue(FiltersUtils.lineMatch(line1, List.of("FR"), List.of()));
        assertTrue(FiltersUtils.lineMatch(line1, List.of(), List.of("FR")));
        assertTrue(FiltersUtils.lineMatch(line1, List.of("PT"), List.of()));
        assertTrue(FiltersUtils.lineMatch(line1, List.of(), List.of("PT")));
        assertTrue(FiltersUtils.lineMatch(line1, List.of("FR"), List.of("PT")));
        assertTrue(FiltersUtils.lineMatch(line1, List.of("ES", "FR"), List.of("ZA", "PT")));

        assertFalse(FiltersUtils.lineMatch(line1, List.of("ES"), List.of("ZA")));
        assertFalse(FiltersUtils.lineMatch(line1, List.of(), List.of("ZA")));
        assertFalse(FiltersUtils.lineMatch(line1, List.of("DE"), List.of()));
        assertFalse(FiltersUtils.lineMatch(line1, List.of("FR", "PT"), List.of("ZA")));
    }

    @Test
    void test2WTransfo() {
        Network network = EurostagTutorialExample1Factory.create(new NetworkFactoryImpl());
        network.getSubstation("P2").setCountry(Country.PT);
        TwoWindingsTransformer transfo1 = network.getTwoWindingsTransformer("NGEN_NHV1"); // belongs to FR
        TwoWindingsTransformer transfo2 = network.getTwoWindingsTransformer("NHV2_NLOAD"); // belongs to PT
        // Transfo can have 1 single country filter
        assertNotNull(transfo1);
        assertTrue(FiltersUtils.transfoMatch(transfo1, List.of()));
        assertTrue(FiltersUtils.transfoMatch(transfo1, List.of("FR")));
        assertFalse(FiltersUtils.transfoMatch(transfo1, List.of("PT")));
        assertFalse(FiltersUtils.transfoMatch(transfo1, List.of("ES", "ZA", "DE", "PT")));

        assertNotNull(transfo2);
        assertTrue(FiltersUtils.transfoMatch(transfo2, List.of()));
        assertTrue(FiltersUtils.transfoMatch(transfo2, List.of("PT")));
        assertFalse(FiltersUtils.transfoMatch(transfo2, List.of("FR")));
        assertFalse(FiltersUtils.transfoMatch(transfo2, List.of("ES", "ZA", "DE", "FR")));
    }
}
