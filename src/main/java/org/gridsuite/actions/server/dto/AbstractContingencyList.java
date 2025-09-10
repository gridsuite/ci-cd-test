/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.gridsuite.actions.server.utils.ContingencyListType;

import java.time.Instant;

import java.util.UUID;

/**
 * @author Etienne Homer <etienne.homer at rte-france.com>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractContingencyList implements PersistentContingencyList {

    private ContingencyListMetadataImpl metadata;

    @Override public ContingencyListMetadata getMetadata() {
        return metadata;
    }

    public UUID getId() {
        return metadata.getId();
    }

    public Instant getModificationDate() {
        return metadata.getModificationDate();
    }

    public ContingencyListType getType() {
        return metadata.getType();
    }
}
