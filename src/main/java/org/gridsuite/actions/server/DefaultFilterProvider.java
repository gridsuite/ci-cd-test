/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import org.gridsuite.actions.FilterProvider;
import org.gridsuite.actions.server.service.FilterService;
import org.gridsuite.filter.AbstractFilter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * @author Kevin Le Saulnier <kevin.le-saulnier at rte-france.com>
 */
@Component
public class DefaultFilterProvider implements FilterProvider {

    private final FilterService filterService;

    public DefaultFilterProvider(FilterService filterService) {
        this.filterService = filterService;
    }

    @Override
    public List<AbstractFilter> getFilters(List<UUID> filtersUuids) {
        return filterService.getFilters(filtersUuids);
    }
}
