package utils

import constants.Defaults.d3
import constants.WorldElements.svg
import constants.WorldElements.viewbox
import globals.WorldGlobals.worldMultiplier
import kotlin.browser.document

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object ZoomUtils {
    var minScale = 1.0 / worldMultiplier
    var scale = 1.0
    var viewX = 0.0
    var viewY = 0.0

    var zoom: dynamic = js("d3.zoom().scaleExtent([1, 20])")

    fun initZoom() {
        zoom.on("zoom") { zoomed() }
    }

    private fun zoomed() {
        val transform: dynamic = js("d3.event.transform")
        val scaleDiff = scale - transform.k as Double
        val positionDiff = (viewX - transform.x as Double).toBits().or((viewY - transform.y as Double).toBits())
        if (positionDiff == 0L && scaleDiff == 0.0) return

        scale = transform.k as Double
        viewX = transform.x as Double
        viewY = transform.y as Double
        viewbox.attr("transform", transform);

        // zoom image converter overlay
        val canvas = document.getElementById("canvas").asDynamic()
        if (canvas != null && +canvas.style.opacity) {
            val img = document.getElementById("image")
            val ctx = canvas.getContext("2d")
            ctx.clearRect(0, 0, canvas.width, canvas.height)
            ctx.setTransform(scale, 0, 0, scale, viewX, viewY)
            ctx.drawImage(img, 0, 0, canvas.width, canvas.height)
        }
    }

    // Reset zoom to initial
    fun resetZoom(d: Int = 1000) {
        svg.transition().duration(d).call(zoom.transform, d3.zoomIdentity)
    }
}
