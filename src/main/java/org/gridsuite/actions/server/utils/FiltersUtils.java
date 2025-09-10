/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.utils;

import com.powsybl.iidm.network.*;
import org.apache.commons.collections4.CollectionUtils;
import java.util.List;
import java.util.Optional;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
public final class FiltersUtils {
    private FiltersUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    // single country match
    public static boolean injectionMatch(Terminal terminal, List<String> countries) {
        Optional<Country> country = terminal.getVoltageLevel().getSubstation().flatMap(Substation::getCountry);
        return CollectionUtils.isEmpty(countries) || country.map(c -> countries.contains(c.name())).orElse(false);
    }

    // crossed country match with 2 filters (countries and countries2)
    private static boolean filterByCountries(Terminal terminal1, Terminal terminal2, List<String> countries, List<String> countries2) {
        return
            // terminal 1 matches filter 1 and terminal 2 matches filter 2
            injectionMatch(terminal1, countries) &&
            injectionMatch(terminal2, countries2)
            || // or the opposite
            injectionMatch(terminal1, countries2) &&
            injectionMatch(terminal2, countries);
    }

    public static boolean hvdcLineMatch(HvdcLine line, List<String> countries, List<String> countries2) {
        return filterByCountries(line.getConverterStation1().getTerminal(), line.getConverterStation2().getTerminal(), countries, countries2);
    }

    public static boolean lineMatch(Line line, List<String> countries, List<String> countries2) {
        return filterByCountries(line.getTerminal1(), line.getTerminal2(), countries, countries2);
    }

    public static boolean transfoMatch(TwoWindingsTransformer transfo, List<String> countries) {
        return filterByCountries(transfo.getTerminal1(), transfo.getTerminal2(), countries, List.of());
    }
}
