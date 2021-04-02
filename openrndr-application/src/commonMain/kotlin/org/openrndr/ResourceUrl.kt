package org.openrndr

import kotlin.reflect.KClass


/**
 * Resolves resource named [name] relative to [class] as a [String] based URL.
 */
expect fun resourceUrl(name: String, `class`: KClass<*> = Application::class): String