/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.utils;

/**
 * @author Chamseddine benhamed <chamseddine.benhamed at rte-france.com>
 */

public enum EquipmentType {
    GENERATOR,
    STATIC_VAR_COMPENSATOR,
    SHUNT_COMPENSATOR,
    LINE,
    TWO_WINDINGS_TRANSFORMER,
    HVDC_LINE,
    BUSBAR_SECTION,
    DANGLING_LINE
}
