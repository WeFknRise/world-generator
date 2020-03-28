package constants

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object WorldElements {
    val svg: dynamic = js("d3.select(\"#map\")")

    // Mask elements
    val masks: dynamic = svg.select("#masks")
    val landMask: dynamic = masks.select("#land")
    val waterMask: dynamic = masks.select("#water")

    // Viewbox elements
    val viewbox: dynamic = svg.select("#viewbox")

    // Ocean elements
    val ocean: dynamic = viewbox.append("g").attr("id", "ocean")

    // Lake elements
    val lakes: dynamic = viewbox.append("g").attr("id", "lakes")

    // Cell elements
    val cells: dynamic = viewbox.append("g").attr("id", "cells")

    // Coastline elements
    val coastline: dynamic = viewbox.append("g").attr("id", "coastline")
}