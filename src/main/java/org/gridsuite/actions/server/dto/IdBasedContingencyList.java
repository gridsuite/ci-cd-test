/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.contingency.contingency.list.ContingencyList;
import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import com.powsybl.iidm.network.Network;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@Getter
@NoArgsConstructor
@Schema(description = "Id based contingency list")
public class IdBasedContingencyList extends AbstractContingencyList {

    @Schema(description = "Identifier list")
    private IdentifierContingencyList identifierContingencyList;
    private Map<String, Set<String>> notFoundElements;

    public IdBasedContingencyList(UUID uuid, Instant date, IdentifierContingencyList identifierContingencyList) {
        this(uuid, date, identifierContingencyList, Map.of());
    }

    public IdBasedContingencyList(UUID uuid, Instant date, IdentifierContingencyList identifierContingencyList, Map<String, Set<String>> notFoundElements) {
        super(new ContingencyListMetadataImpl(uuid, ContingencyListType.IDENTIFIERS, date));
        this.identifierContingencyList = identifierContingencyList;
        this.notFoundElements = notFoundElements;
    }

    @Override
    public ContingencyList toPowsyblContingencyList(Network network) {
        return identifierContingencyList;
    }

    //TODO this a temporary workaround to get elements not found in the network
    // this should be deleted when a fix is added to powsybl
    @Override
    public Map<String, Set<String>> getNotFoundElements(Network network) {
        return notFoundElements;
    }
}
