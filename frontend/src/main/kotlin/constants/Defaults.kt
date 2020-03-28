package constants

import constants.WorldElements.cells
import constants.WorldElements.coastline
import constants.WorldElements.ocean
import constants.WorldElements.svg
import constants.WorldElements.viewbox
import globals.WorldGlobals.innerHeight
import globals.WorldGlobals.innerWidth
import globals.WorldGlobals.worldHeight
import globals.WorldGlobals.worldMultiplier
import globals.WorldGlobals.worldWidth
import utils.ZoomUtils.initZoom
import utils.ZoomUtils.minScale
import utils.ZoomUtils.zoom
import kotlin.browser.window
import kotlin.math.min

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object Defaults {
    val d3 = js("d3")

    fun restoreDefaultEvents() {
        initZoom()
        svg.call(zoom)
        viewbox.style("cursor", "default").on(".drag", null)
    }

    fun restoreGlobalVariables() {
        innerWidth = window.innerWidth.toDouble()
        innerHeight = window.innerHeight.toDouble()
        worldWidth = innerWidth * worldMultiplier
        worldHeight = innerHeight * worldMultiplier
        minScale = 1.0 / worldMultiplier
    }

    fun restoreDefaultStyles() {
        // SVG default
        svg.attr("background-color", "#000000").attr("data-filter", null).attr("filter", null)

        // Cells default
        cells.attr("opacity", null).attr("stroke", "#808080").attr("stroke-width", .5).attr("filter", null).attr("mask", null)

        // Ocean style
        ocean.attr("opacity", null);
        ocean.select("rect").attr("fill", "#53679f")
        ocean.attr("filter", null).attr("layers", "-1")//.attr("layers", "-6,-3,-1")
        ocean.attr("filter", null).attr("layer-opacity", "0.13")

        // Coastline style
        coastline.select("#sea_island").attr("opacity", .5).attr("stroke", "#1f3846").attr("stroke-width", .7).attr("auto-filter", 1).attr("filter", "url(#dropShadow)")
        coastline.select("#lake_island").attr("opacity", 1).attr("stroke", "#7c8eaf").attr("stroke-width", .35).attr("filter", null)
    }

    fun restoreDefaultSVGAttributes() {
        svg.attr("width", innerWidth).attr("height", innerHeight)

        val translateExtent = arrayOf(arrayOf(0, 0), arrayOf(worldWidth, worldHeight))
        val scaleExtent = arrayOf(minScale, 20)
        zoom.translateExtent(translateExtent).scaleExtent(scaleExtent).scaleTo(svg, 1)
    }
}