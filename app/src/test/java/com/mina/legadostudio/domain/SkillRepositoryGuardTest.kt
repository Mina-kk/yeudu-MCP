package com.mina.legadostudio.domain

import com.mina.legadostudio.skills.SkillRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SkillRepositoryGuardTest {
    private val builtIn = setOf("legado-book-source", "dandan-tamer")

    @Test
    fun acceptsCustomIds() {
        SkillRepository.requireMutable("my-custom-skill", builtIn)
        assertTrue(SkillRepository.validId("my-custom-skill"))
    }

    @Test
    fun rejectsBuiltInOverwrite() {
        val error = runCatching { SkillRepository.requireMutable("legado-book-source", builtIn) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error!!.message.orEmpty().contains("内置"))
    }

    @Test
    fun rejectsInvalidIds() {
        assertFalse(SkillRepository.validId(""))
        assertFalse(SkillRepository.validId(".."))
        assertFalse(SkillRepository.validId("a/b"))
        val error = runCatching { SkillRepository.requireMutable("../etc", builtIn) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}