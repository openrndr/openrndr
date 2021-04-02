package org.openrndr

import org.openrndr.exceptions.stackRootClassName

actual fun rootClassName() :String{
    return stackRootClassName()
}
