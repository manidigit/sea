package com.app.flashlearn.domain.model

data class Content(
    val id: Long,
    val conceptId: Long,
    val languageCode: String,
    val text: String,
    val pronunciation: String?,
    val definition: String?,
    val example: String?,
    val grammarNote: String?,
    val usageNote: String?
)
