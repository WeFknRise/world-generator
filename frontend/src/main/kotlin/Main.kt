import constants.Defaults.restoreDefaultEvents
import constants.Defaults.restoreDefaultSVGAttributes
import constants.Defaults.restoreDefaultStyles
import constants.Defaults.restoreGlobalVariables
import constants.RESTConstants
import constants.RESTConstants.densityParam
import constants.RESTConstants.generatorUrl
import constants.RESTConstants.heightParam
import constants.RESTConstants.multiplierParam
import constants.RESTConstants.widthParam
import globals.WorldGlobals.density
import globals.WorldGlobals.innerHeight
import globals.WorldGlobals.innerWidth
import globals.WorldGlobals.worldMultiplier
import kotlinx.coroutines.await
import org.w3c.xhr.XMLHttpRequest
import utils.ZoomUtils.resetZoom
import visuals.visualizer.CellVisualizer
import kotlin.browser.window
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun main() {
    restoreDefaultEvents()
    restoreGlobalVariables()
    restoreDefaultStyles()

    window.onload = {
        generateWorld()
        Unit // Ugly workaround for https://youtrack.jetbrains.com/issue/KT-22635
    }
}

fun generateWorld() {
    val xhr = XMLHttpRequest()
    val url = RESTConstants.urlBuilder(generatorUrl, Pair(widthParam, innerWidth), Pair(heightParam, innerHeight),
            Pair(densityParam, density), Pair(multiplierParam, worldMultiplier))
    xhr.onreadystatechange = {
        if (xhr.readyState == XMLHttpRequest.DONE) {
            launch {
                val cellVisualizer = CellVisualizer()
                cellVisualizer.drawPromise().await()

                resetZoom(1000)
                restoreDefaultSVGAttributes()
            }
        }
    }
    xhr.open("GET", url)
    xhr.send()
}

fun launch(block: suspend () -> Unit) {
    block.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext get() = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) { result.getOrThrow() }
    })
}