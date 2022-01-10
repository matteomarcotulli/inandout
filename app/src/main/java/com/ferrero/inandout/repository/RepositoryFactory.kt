package com.ferrero.inandout.repository

import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntitySets
import com.sap.cloud.android.odata.entitycontainer.ElementBehaviour
import com.sap.cloud.android.odata.entitycontainer.HeaderLIL

import com.sap.cloud.mobile.odata.EntitySet
import com.sap.cloud.mobile.odata.EntityValue
import com.sap.cloud.mobile.odata.Property
import com.ferrero.inandout.service.OfflineWorkerUtil

import java.util.WeakHashMap

/*
 * Repository factory to construct repository for an entity set
 */
class RepositoryFactory
/**
 * Construct a RepositoryFactory instance. There should only be one repository factory and used
 * throughout the life of the application to avoid caching entities multiple times.
 */
{
    private val repositories: WeakHashMap<String, Repository<out EntityValue>> = WeakHashMap()

    /**
     * Construct or return an existing repository for the specified entity set
     * @param entitySet - entity set for which the repository is to be returned
     * @param orderByProperty - if specified, collection will be sorted ascending with this property
     * @return a repository for the entity set
     */
    fun getRepository(entitySet: EntitySet, orderByProperty: Property?): Repository<out EntityValue> {
        val entityContainer = OfflineWorkerUtil.entityContainer
        val key = entitySet.localName
        var repository: Repository<out EntityValue>? = repositories[key]
        if (repository == null) {
            repository = when (key) {
                EntitySets.elementBehaviour.localName -> Repository<ElementBehaviour>(entityContainer, EntitySets.elementBehaviour, orderByProperty)
                EntitySets.headerLIL.localName -> Repository<HeaderLIL>(entityContainer, EntitySets.headerLIL, orderByProperty)
                else -> throw AssertionError("Fatal error, entity set[$key] missing in generated code")
            }
            repositories[key] = repository
        }
        return repository
    }

    /**
     * Get rid of all cached repositories
     */
    fun reset() {
        repositories.clear()
    }
}
