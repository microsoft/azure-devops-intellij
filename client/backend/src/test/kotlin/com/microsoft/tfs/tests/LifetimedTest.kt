// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.tfs.tests

import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.junit.After
import org.junit.Before

abstract class LifetimedTest {
    private lateinit var testLifetimeDefinition: LifetimeDefinition
    protected val testLifetime: Lifetime
        get() = testLifetimeDefinition

    @Before
    open fun setUp() {
        testLifetimeDefinition = LifetimeDefinition()
    }

    @After
    fun tearDown() {
        testLifetimeDefinition.terminate()
    }
}