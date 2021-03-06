/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.material

import androidx.compose.runtime.Composable
import androidx.ui.core.AndroidOwner
import androidx.ui.core.ExperimentalLayoutNodeApi
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.compose.foundation.layout.DpConstraints
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.ui.test.ComposeTestRule
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onRoot
import androidx.ui.test.getAlignmentLinePosition
import androidx.ui.test.runOnIdle
import androidx.compose.foundation.text.FirstBaseline
import androidx.compose.foundation.text.LastBaseline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width

fun ComposeTestRule.setMaterialContent(
    modifier: Modifier = Modifier,
    composable: @Composable () -> Unit
) {
    setContent {
        MaterialTheme {
            Surface(modifier = modifier, content = composable)
        }
    }
}

fun <T> ComposeTestRule.runOnIdleWithDensity(action: Density.() -> T): T {
    return runOnIdle {
        density.action()
    }
}

fun SemanticsNodeInteraction.getFirstBaselinePosition() = getAlignmentLinePosition(FirstBaseline)

fun SemanticsNodeInteraction.getLastBaselinePosition() = getAlignmentLinePosition(LastBaseline)

fun SemanticsNodeInteraction.assertIsSquareWithSize(expectedSize: Dp) =
    assertWidthIsEqualTo(expectedSize).assertHeightIsEqualTo(expectedSize)

fun SemanticsNodeInteraction.assertWidthFillsRoot(): SemanticsNodeInteraction {
    val node = fetchSemanticsNode("Failed to assertWidthFillsScreen")
    @OptIn(ExperimentalLayoutNodeApi::class)
    val owner = node.componentNode.owner as AndroidOwner
    val rootViewWidth = owner.view.width

    with(owner.density) {
        node.boundsInRoot.width.toDp().assertIsEqualTo(rootViewWidth.toDp())
    }
    return this
}

fun rootWidth(): Dp {
    val nodeInteraction = onRoot()
    val node = nodeInteraction.fetchSemanticsNode("Failed to get screen width")
    @OptIn(ExperimentalLayoutNodeApi::class)
    val owner = node.componentNode.owner as AndroidOwner

    return with(owner.density) {
        owner.view.width.toDp()
    }
}

fun rootHeight(): Dp {
    val nodeInteraction = onRoot()
    val node = nodeInteraction.fetchSemanticsNode("Failed to get screen height")
    @OptIn(ExperimentalLayoutNodeApi::class)
    val owner = node.componentNode.owner as AndroidOwner

    return with(owner.density) {
        owner.view.height.toDp()
    }
}

/**
 * Constant to emulate very big but finite constraints
 */
val BigTestConstraints = DpConstraints(maxWidth = 5000.dp, maxHeight = 5000.dp)

fun ComposeTestRule.setMaterialContentForSizeAssertions(
    parentConstraints: DpConstraints = BigTestConstraints,
    // TODO : figure out better way to make it flexible
    children: @Composable () -> Unit
): SemanticsNodeInteraction {
    setContent {
        MaterialTheme {
            Surface {
                Stack {
                    Stack(Modifier.preferredSizeIn(parentConstraints)
                        .testTag("containerForSizeAssertion")) {
                        children()
                    }
                }
            }
        }
    }

    return onNodeWithTag("containerForSizeAssertion")
}
