/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.configs;

import org.gridsuite.actions.ContingencyListEvaluator;
import org.gridsuite.actions.FilterProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Kevin Le Saulnier <kevin.le-saulnier at rte-france.com>
 */
@Configuration
public class ActionsConfig {
    @Bean
    public ContingencyListEvaluator contingencyListEvaluator(
        FilterProvider filterProvider
    ) {
        return new ContingencyListEvaluator(filterProvider);
    }
}
