/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.repositories;

import org.gridsuite.actions.server.entities.FormContingencyListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@Repository
public interface FormContingencyListRepository extends JpaRepository<FormContingencyListEntity, UUID> {

    @Query("SELECT DISTINCT entity FROM FormContingencyListEntity entity LEFT JOIN FETCH entity.countries1")
    List<FormContingencyListEntity> findAllWithCountries();

    /* do not use deleteById which throw when id does not exists */
    Integer deleteFormContingencyListEntityById(UUID id);
}
