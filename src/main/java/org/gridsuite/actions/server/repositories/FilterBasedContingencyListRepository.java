/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.repositories;

import org.gridsuite.actions.server.entities.FilterBasedContingencyListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * @author Bassel El Cheikh <bassel.el-cheikh at rte-france.com>
 */

@Repository
public interface FilterBasedContingencyListRepository extends JpaRepository<FilterBasedContingencyListEntity, UUID> {
    Integer deleteFilterBasedContingencyListEntityById(UUID id);
}
