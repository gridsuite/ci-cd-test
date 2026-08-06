/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.gridsuite.actions.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.list.IdentifierContingencyList;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.identifiers.IdBasedNetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifier;
import com.powsybl.iidm.network.identifiers.NetworkElementIdentifierContingencyList;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import org.gridsuite.actions.ContingencyListEvaluator;
import org.gridsuite.actions.dto.*;
import org.gridsuite.actions.dto.contingency.AbstractContingencyList;
import org.gridsuite.actions.dto.contingency.FilterBasedContingencyList;
import org.gridsuite.actions.dto.contingency.IdBasedContingencyList;
import org.gridsuite.actions.dto.contingency.PersistentContingencyList;
import org.gridsuite.actions.dto.evaluation.ContingencyIdsByGroup;
import org.gridsuite.actions.dto.evaluation.ContingencyInfos;
import org.gridsuite.actions.dto.evaluation.ContingencyListExportResult;
import org.gridsuite.actions.server.dto.ContingencyCount;
import org.gridsuite.actions.server.dto.ContingencyCountByContingencyList;
import org.gridsuite.actions.server.dto.CountWithMissingUuids;
import org.gridsuite.actions.server.entities.*;
import org.gridsuite.actions.server.repositories.FilterBasedContingencyListRepository;
import org.gridsuite.actions.server.repositories.IdBasedContingencyListRepository;
import org.gridsuite.actions.server.service.FilterService;
import org.gridsuite.actions.utils.ContingencyListType;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
public class ContingencyListService {

    private final IdBasedContingencyListRepository idBasedContingencyListRepository;

    private final FilterBasedContingencyListRepository filterBasedContingencyListRepository;

    private final NetworkStoreService networkStoreService;

    private final NotificationService notificationService;

    private final FilterService filterService;

    private final ContingencyListEvaluator contingencyListEvaluator;

    public ContingencyListService(IdBasedContingencyListRepository idBasedContingencyListRepository,
                                  FilterBasedContingencyListRepository filterBasedContingencyListRepository,
                                  NetworkStoreService networkStoreService,
                                  NotificationService notificationService,
                                  FilterService filterService,
                                  ContingencyListEvaluator contingencyListEvaluator) {
        this.idBasedContingencyListRepository = idBasedContingencyListRepository;
        this.filterBasedContingencyListRepository = filterBasedContingencyListRepository;
        this.networkStoreService = networkStoreService;
        this.notificationService = notificationService;
        this.filterService = filterService;
        this.contingencyListEvaluator = contingencyListEvaluator;
    }

    ContingencyListMetadata fromContingencyListEntity(AbstractContingencyEntity entity, ContingencyListType type) {
        return new ContingencyListMetadataImpl(entity.getId(), type, entity.getModificationDate());
    }

    List<ContingencyListMetadata> getContingencyListsMetadata() {
        return Stream.of(
            idBasedContingencyListRepository.findAll().stream().map(idBasedContingencyListEntity ->
                    fromContingencyListEntity(idBasedContingencyListEntity, ContingencyListType.IDENTIFIERS)),
            filterBasedContingencyListRepository.findAll().stream().map(filterBasedContingencyListEntity ->
                    fromContingencyListEntity(filterBasedContingencyListEntity, ContingencyListType.FILTERS))
        ).flatMap(Function.identity()).collect(Collectors.toList());
    }

    List<ContingencyListMetadata> getContingencyListsMetadata(List<UUID> ids) {
        return Stream.of(
            idBasedContingencyListRepository.findAllById(ids).stream().map(idBasedContingencyListEntity ->
                    fromContingencyListEntity(idBasedContingencyListEntity, ContingencyListType.IDENTIFIERS)),
            filterBasedContingencyListRepository.findAllById(ids).stream().map(filterBasedContingencyListEntity ->
                fromContingencyListEntity(filterBasedContingencyListEntity, ContingencyListType.FILTERS))
        ).flatMap(Function.identity()).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PersistentContingencyList> getIdBasedContingencyList(UUID id, Network network) {
        return doGetIdBasedContingencyList(id, network);
    }

    private Optional<PersistentContingencyList> doGetIdBasedContingencyList(UUID id, Network network) {
        Objects.requireNonNull(id);
        return idBasedContingencyListRepository.findById(id).map(idBasedContingencyListEntity -> fromIdBasedContingencyListEntity(idBasedContingencyListEntity, network));
    }

    @Transactional
    public Optional<FilterBasedContingencyList> getFilterBasedContingencyList(UUID id) {
        Optional<FilterBasedContingencyListEntity> entity = doGetFilterBasedContingencyListEntity(id);
        if (entity.isEmpty()) {
            return Optional.empty();
        } else {
            List<UUID> filterIds = entity.get().getFiltersIds();
            List<EquipmentTypesByFilter> selectedEquipmentTypesByFilter = entity.get().getSelectedEquipmentTypesByFilter()
                .stream()
                .map(EquipmentTypesByFilterEntity::toDto)
                .toList();
            //get information from filterServer
            List<FilterAttributes> attributes = filterService.getFiltersAttributes(filterIds);
            return Optional.of(new FilterBasedContingencyList(entity.get().getId(), entity.get().getModificationDate(), attributes, selectedEquipmentTypesByFilter));
        }
    }

    private Optional<PersistentContingencyList> doGetFilterBasedContingencyList(UUID id) {
        Objects.requireNonNull(id);
        return filterBasedContingencyListRepository.findById(id).map(ContingencyListService::fromFilterBasedContingencyListEntity);
    }

    private Optional<FilterBasedContingencyListEntity> doGetFilterBasedContingencyListEntity(UUID id) {
        Objects.requireNonNull(id);
        return filterBasedContingencyListRepository.findById(id);
    }

    private ContingencyCount getContingencyCount(Network network, List<UUID> ids) {
        Map<UUID, ContingencyCountByContingencyList> contingenciesCountByContingencyList = new HashMap<>();
        for (UUID uuid : ids) {
            try {
                Optional<PersistentContingencyList> contingencyList = getAnyContingencyList(uuid, network);
                contingencyList.ifPresent(l -> contingenciesCountByContingencyList.put(uuid,
                        new ContingencyCountByContingencyList(getContingencies(l, network).size(), l.getNotFoundElements(network), null)));
            } catch (PowsyblException e) {
                contingenciesCountByContingencyList.put(uuid, new ContingencyCountByContingencyList(0, null, e.getMessage()));
            }
        }
        return new ContingencyCount(contingenciesCountByContingencyList);
    }

    @Transactional(readOnly = true)
    public Map<String, CountWithMissingUuids> getContingencyCountByGroup(ContingencyIdsByGroup contingencyIdsByGroup, UUID networkUuid, String variantId) {
        Network network = getNetworkFromUuid(networkUuid, variantId);
        return contingencyIdsByGroup.getIds().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> getContingencyCountByGroup(network, e.getValue()))
        );
    }

    private CountWithMissingUuids getContingencyCountByGroup(Network network, List<UUID> ids) {
        long nbContingencies = 0;
        List<UUID> missingContingencyListIds = new ArrayList<>();

        for (UUID uuid : ids) {
            Optional<PersistentContingencyList> contingencyList = getAnyContingencyList(uuid, network);
            if (contingencyList.isPresent()) {
                nbContingencies += getContingencies(contingencyList.get(), network).size();
            } else {
                missingContingencyListIds.add(uuid);
            }
        }
        return new CountWithMissingUuids(nbContingencies, missingContingencyListIds);
    }

    @Transactional(readOnly = true)
    public ContingencyCount getContingencyCount(List<UUID> ids, UUID networkUuid, String variantId) {
        Network network = getNetworkFromUuid(networkUuid, variantId);
        return getContingencyCount(network, ids);
    }

    @Transactional(readOnly = true)
    public ContingencyListExportResult exportContingencyList(List<UUID> contingencyListIds, UUID networkUuid, String variantId) {
        Network network = getNetworkFromUuid(networkUuid, variantId);
        List<Contingency> contingencies = new ArrayList<>();
        List<UUID> notFoundIds = new ArrayList<>();

        contingencyListIds.forEach(contingencyListId -> {
            Optional<PersistentContingencyList> contingencyList = getAnyContingencyList(contingencyListId, network);
            contingencyList.ifPresentOrElse(
                    list -> contingencies.addAll(getContingencies(list, network)),
                    () -> notFoundIds.add(contingencyListId)
            );
        });
        return new ContingencyListExportResult(contingencies, notFoundIds);
    }

    private List<Contingency> getContingencies(PersistentContingencyList persistentContingencyList, Network network) {
        return contingencyListEvaluator.evaluateContingencyList(persistentContingencyList, network)
                .stream()
                .map(ContingencyInfos::getContingency)
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContingencyInfos> exportContingencyInfosList(List<UUID> ids, UUID networkUuid, String variantId) {
        Network network = getNetworkFromUuid(networkUuid, variantId);
        return ids.stream().map(id -> contingencyListEvaluator.evaluateContingencyList(findContingencyList(id, network), network)).flatMap(Collection::stream).toList();
    }

    private PersistentContingencyList findContingencyList(UUID id, Network network) {
        Objects.requireNonNull(id);
        return getAnyContingencyList(id, network)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contingency list " + id + " not found"));
    }

    private Optional<PersistentContingencyList> getAnyContingencyList(UUID id, Network network) {
        return doGetIdBasedContingencyList(id, network)
                .or(() -> doGetFilterBasedContingencyList(id));
    }

    private Network getNetworkFromUuid(UUID networkUuid, String variantId) {
        Network network;
        if (networkUuid == null) {
            // use an empty network, script might not have need to network
            network = new NetworkFactoryImpl().createNetwork("empty", "empty");
        } else {
            network = networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION);
            if (network == null) {
                throw new PowsyblException("Network '" + networkUuid + "' not found");
            }
            if (variantId != null) {
                network.getVariantManager().setWorkingVariant(variantId);
            }
        }

        return network;
    }

    @Transactional
    public Optional<UUID> duplicateFilterBasedContingencyList(UUID sourceListId) {
        Optional<PersistentContingencyList> contingencyList = doGetFilterBasedContingencyList(sourceListId);
        if (contingencyList.isEmpty()) {
            throw createNotFoundException(sourceListId.toString(), "Form contingency list");
        } else {
            FilterBasedContingencyList filterContingencyList = createFilterBasedContingencyList(null, (FilterBasedContingencyList) contingencyList.get());
            return Optional.of(filterContingencyList.getId());
        }
    }

    @Transactional
    public Optional<UUID> duplicateIdentifierContingencyList(UUID sourceListId) {
        Optional<IdBasedContingencyList> idBasedContingencyList = doGetIdBasedContingencyList(sourceListId, null).map(s -> createIdBasedContingencyList(null, (IdBasedContingencyList) s));
        if (!idBasedContingencyList.isPresent()) {
            throw createNotFoundException(sourceListId.toString(), "Identifier contingency list");
        } else {
            return Optional.of(idBasedContingencyList.get().getId());
        }
    }

    @Transactional
    public void modifyIdBasedContingencyList(UUID id, IdBasedContingencyList idBasedContingencyList, String userId) {
        // throw if not found
        idBasedContingencyListRepository.save(idBasedContingencyListRepository.getReferenceById(id).update(idBasedContingencyList));
        notificationService.emitElementUpdated(id, userId);
    }

    @Transactional
    public void modifyFilterBasedContingencyList(UUID id, FilterBasedContingencyList contingencyList, String userId) {
        // throw if not found
        filterBasedContingencyListRepository.save(filterBasedContingencyListRepository.getReferenceById(id).update(contingencyList));
        notificationService.emitElementUpdated(id, userId);
    }

    @Transactional
    public void deleteContingencyList(UUID id) throws EmptyResultDataAccessException {
        Objects.requireNonNull(id);
        // if there is no contingency list by this Id, deleted count == 0
        if (idBasedContingencyListRepository.deleteIdBasedContingencyListEntityById(id) == 0
            && filterBasedContingencyListRepository.deleteFilterBasedContingencyListEntityById(id) == 0) {
            throw new EmptyResultDataAccessException("No element found", 1);
        }
    }

    private static IdBasedContingencyList fromIdBasedContingencyListEntity(IdBasedContingencyListEntity entity, Network network) {
        List<NetworkElementIdentifier> listOfNetworkElementIdentifierList = new ArrayList<>();
        Map<String, Set<String>> notFoundElements = new HashMap<>();
        entity.getIdentifiersListEntities().forEach(identifierList -> {
            List<NetworkElementIdentifier> networkElementIdentifiers = new ArrayList<>();
            identifierList.getEquipmentIds().forEach(equipmentId -> {
                if (network != null && network.getIdentifiable(equipmentId) == null) {
                    Set<String> ids = notFoundElements.computeIfAbsent(identifierList.getName(), k -> new HashSet<>());
                    ids.add(equipmentId);
                }
                networkElementIdentifiers.add(new IdBasedNetworkElementIdentifier(equipmentId));
            });
            listOfNetworkElementIdentifierList.add(new NetworkElementIdentifierContingencyList(networkElementIdentifiers, identifierList.getName()));
        });
        return new IdBasedContingencyList(entity.getId(),
                entity.getModificationDate(),
                new IdentifierContingencyList(entity.getId().toString(), listOfNetworkElementIdentifierList),
                notFoundElements);
    }

    private static FilterBasedContingencyList fromFilterBasedContingencyListEntity(FilterBasedContingencyListEntity entity) {
        return new FilterBasedContingencyList(entity.getId(), entity.getModificationDate(),
            entity.getFiltersIds().stream().map(uuid -> new FilterAttributes(uuid, null)).toList(),
            entity.getSelectedEquipmentTypesByFilter().stream().map(EquipmentTypesByFilterEntity::toDto).toList());
    }

    public IdBasedContingencyList createIdBasedContingencyList(UUID id, IdBasedContingencyList idBasedContingencyList) {
        IdBasedContingencyListEntity entity = new IdBasedContingencyListEntity(idBasedContingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromIdBasedContingencyListEntity(idBasedContingencyListRepository.save(entity), null);
    }

    public FilterBasedContingencyList createFilterBasedContingencyList(UUID id, FilterBasedContingencyList contingencyList) {
        FilterBasedContingencyListEntity entity = new FilterBasedContingencyListEntity(contingencyList);
        entity.setId(id == null ? UUID.randomUUID() : id);
        return fromFilterBasedContingencyListEntity(filterBasedContingencyListRepository.save(entity));
    }

    public ResponseStatusException createNotFoundException(String resourceId, String resourceType) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("%s %s not found", resourceType, resourceId));
    }

    @Transactional(readOnly = true)
    public List<AbstractContingencyList> getPersistentContingencyLists(List<UUID> ids) {
        Objects.requireNonNull(ids);

        List<AbstractContingencyList> result = new ArrayList<>();

        // Get all id based contingency lists
        List<AbstractContingencyList> idBasedLists = idBasedContingencyListRepository.findAllById(ids)
                .stream()
                .map(entity -> fromIdBasedContingencyListEntity(entity, null))
                .collect(Collectors.toList());
        result.addAll(idBasedLists);

        // Get all filter based contingency lists
        List<AbstractContingencyList> filterBasedLists = filterBasedContingencyListRepository.findAllById(ids)
                .stream()
                .map(ContingencyListService::fromFilterBasedContingencyListEntity)
                .collect(Collectors.toList());
        result.addAll(filterBasedLists);

        return result;
    }
}
