/*
 *  Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.gridsuite.actions.server.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.dto.NumericalFilter;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "numericFilter")
public class NumericalFilterEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator")
    NumericalFilterOperator operator;

    @Column(name = "value1")
    Double value1;

    @Column(name = "value2")
    Double value2;

    public static NumericalFilterEntity convert(NumericalFilter filter) {
        if (filter == null || filter.getValue1() == null
            || filter.getType() == NumericalFilterOperator.RANGE && filter.getValue2() == null) {
            return null;
        }
        return new NumericalFilterEntity(null, filter.getType(), filter.getValue1(), filter.getValue2());
    }

    public static NumericalFilter convert(NumericalFilterEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NumericalFilter(entity.getOperator(), entity.getValue1(), entity.getValue2());
    }
}

