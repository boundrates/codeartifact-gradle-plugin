package org.gradle.kotlin.dsl

import co.bound.codeartifact.CodeArtifactPluginExtension
import org.gradle.api.Action
import org.gradle.api.initialization.Settings

/**
 * Configure Maven repository for AWS CodeArtifact.
 *
 * @param action an [Action] to configure the [CodeArtifactPluginExtension]
 */
@Suppress("unused")
fun Settings.codeartifact(action: Action<CodeArtifactPluginExtension>) {
    extensions.configure(CodeArtifactPluginExtension::class.java, action)
}
