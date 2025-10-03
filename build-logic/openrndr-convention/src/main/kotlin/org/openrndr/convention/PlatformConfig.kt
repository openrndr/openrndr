package org.openrndr.convention

import org.gradle.api.Project
import javax.inject.Inject

abstract class PlatformConfiguration(
    @Inject val project: Project
) {
    val android: Boolean by lazy { project.property("openrndr.platform.android") == "true" }
}