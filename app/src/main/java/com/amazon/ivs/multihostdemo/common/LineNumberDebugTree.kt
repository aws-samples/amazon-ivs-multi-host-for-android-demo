package com.amazon.ivs.multihostdemo.common

import timber.log.Timber

class LineNumberDebugTree : Timber.DebugTree() {
    override fun createStackElementTag(element: StackTraceElement) =
        "$TIMBER_TAG: (${element.fileName}:${element.lineNumber}) #${element.methodName} "
}
