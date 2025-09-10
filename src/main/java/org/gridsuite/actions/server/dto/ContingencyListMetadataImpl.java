/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.time.Instant;
import java.util.UUID;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Contingency list infos")
public class ContingencyListMetadataImpl implements ContingencyListMetadata {

    @Schema(description = "List id")
    private UUID id;

    @Schema(description = "List type")
    private ContingencyListType type;

    @Schema(description = "Modification Date")
    Instant modificationDate;

}
