package com.mina.legadostudio.domain

import com.mina.legadostudio.data.db.ProjectEntity
import com.mina.legadostudio.verification.DomainKey

data class SourceGroup(
    val domain: String,
    val items: List<ProjectEntity>,
) {
    val latest: ProjectEntity get() = items.first()
}

object SourceCatalog {
    const val UNKNOWN_DOMAIN = "未识别域名"

    fun domainOf(siteUrl: String): String {
        val raw = siteUrl.trim()
        if (raw.isBlank()) return UNKNOWN_DOMAIN
        val withScheme = if (':' in raw) raw else "https://$raw"
        return DomainKey.fromUrl(withScheme).ifBlank { raw }
    }

    fun groupByDomain(projects: List<ProjectEntity>): List<SourceGroup> =
        projects.groupBy { domainOf(it.siteUrl) }
            .map { (domain, items) -> SourceGroup(domain, items.sortedByDescending { it.updatedAt }) }
            .sortedByDescending { it.latest.updatedAt }
}