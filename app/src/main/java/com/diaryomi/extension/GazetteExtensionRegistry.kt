package com.diaryomi.extension

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registro central de extensões de Diários Oficiais.
 * Mantém um mapa de extensões por ID, permitindo lookup rápido.
 * Novas extensões são registradas via injeção de dependência.
 */
@Singleton
class GazetteExtensionRegistry @Inject constructor(
    extensions: Set<@JvmSuppressWildcards GazetteExtension>
) {
    private val extensionMap: Map<String, GazetteExtension> =
        extensions.associateBy { it.id }

    fun getById(id: String): GazetteExtension? = extensionMap[id]

    fun getAll(): List<GazetteExtension> = extensionMap.values.toList()
}
