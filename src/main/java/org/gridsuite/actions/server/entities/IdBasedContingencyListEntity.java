/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.entities;

import com.powsybl.contingency.contingency.list.IdentifierContingencyList;
import com.powsybl.iidm.network.identifiers.IdBasedNetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifierContingencyList;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.dto.IdBasedContingencyList;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer <etienne.homer@rte-france.com>
 */
@NoArgsConstructor
@Getter
@Entity
@Table(name = "id_based_contingency_list")
public class IdBasedContingencyListEntity extends AbstractContingencyEntity {

    @OneToMany(cascade = CascadeType.ALL)
    @OrderColumn(name = "identifier_order")
    private List<IdentifierListEntity> identifiersListEntities;

    public IdBasedContingencyListEntity(IdBasedContingencyList idBasedContingencyList) {
        super();
        init(idBasedContingencyList.getIdentifierContingencyList());
    }

    final void init(IdentifierContingencyList identifierContingencyList) {
        if (identifierContingencyList.getIdentifiants().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contingency list " + identifierContingencyList.getName() + " empty");
        }

        this.identifiersListEntities = new ArrayList<>();
        identifierContingencyList.getIdentifiants().forEach(networkElementIdentifier -> {
            List<NetworkElementIdentifier> identifierList = ((NetworkElementIdentifierContingencyList) networkElementIdentifier).getNetworkElementIdentifiers();
            String contingencyName = networkElementIdentifier.getContingencyId().isPresent() ? networkElementIdentifier.getContingencyId().get() : "";
            if (contingencyName.isEmpty() || identifierList == null || identifierList.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one contingency is partially defined for the contingency list " + identifierContingencyList.getName());
            }
            identifiersListEntities.add(new IdentifierListEntity(UUID.randomUUID(), contingencyName, identifierList.stream().map(identifier -> ((IdBasedNetworkElementIdentifier) identifier).getIdentifier()).collect(Collectors.toSet())));
            }
        );
    }

    public IdBasedContingencyListEntity update(IdBasedContingencyList idBasedContingencyList) {
        init(idBasedContingencyList.getIdentifierContingencyList());
        return this;
    }
}
