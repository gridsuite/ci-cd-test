/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.Contingency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * @author Jamal KHEYYAD <jamal.kheyyad at rte-international.com>
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ContingencyListExportResult {
    List<Contingency> contingenciesFound;
    List<UUID> contingenciesNotFound;
}
