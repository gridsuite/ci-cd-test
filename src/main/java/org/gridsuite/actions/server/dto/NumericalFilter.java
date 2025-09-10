/**
 *  Copyright (c) 2022, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import com.powsybl.iidm.criteria.SingleNominalVoltageCriterion;
import com.powsybl.iidm.criteria.VoltageInterval;
import lombok.*;
import org.gridsuite.actions.server.utils.NumericalFilterOperator;

/**
 * @author David Braquart <david.braquart at rte-france.com>
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NumericalFilter {
    NumericalFilterOperator type;
    Double value1;
    Double value2;

    public String operator() {
        switch (type) {
            case EQUALITY:
                return "==";
            case LESS_THAN:
                return "<";
            case LESS_OR_EQUAL:
                return "<=";
            case GREATER_THAN:
                return ">";
            case GREATER_OR_EQUAL:
                return ">=";
            default:
                return "";
        }
    }

    public static SingleNominalVoltageCriterion toNominalVoltageCriterion(NumericalFilter numericalFilter) {
        if (numericalFilter == null) {
            return null;
        }
        switch (numericalFilter.getType()) {
            case EQUALITY:
                return new SingleNominalVoltageCriterion(VoltageInterval.builder()
                        .setLowBound(numericalFilter.getValue1(), true)
                        .setHighBound(numericalFilter.getValue1(), true)
                        .build());
            case LESS_THAN:
                return new SingleNominalVoltageCriterion(VoltageInterval.builder()
                        .setLowBound(Double.MIN_VALUE, true)
                        .setHighBound(numericalFilter.getValue1(), false)
                        .build());
            case LESS_OR_EQUAL:
                return new SingleNominalVoltageCriterion(VoltageInterval.builder()
                        .setLowBound(Double.MIN_VALUE, true)
                        .setHighBound(numericalFilter.getValue1(), true)
                        .build());
            case GREATER_THAN:
                return new SingleNominalVoltageCriterion(VoltageInterval.builder()
                        .setLowBound(numericalFilter.getValue1(), false)
                        .setHighBound(Double.MAX_VALUE, true)
                        .build());
            case GREATER_OR_EQUAL:
                return new SingleNominalVoltageCriterion(VoltageInterval.builder()
                        .setLowBound(numericalFilter.getValue1(), true)
                        .setHighBound(Double.MAX_VALUE, true)
                        .build());
            case RANGE:
                return new SingleNominalVoltageCriterion(VoltageInterval.builder()
                        .setLowBound(numericalFilter.getValue1(), true)
                        .setHighBound(numericalFilter.getValue2(), true)
                        .build());
            default:
                return new SingleNominalVoltageCriterion(VoltageInterval.builder().build());
        }
    }
}

