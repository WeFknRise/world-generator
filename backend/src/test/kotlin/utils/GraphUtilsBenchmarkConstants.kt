package utils

import models.geometry.Vertex

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object GraphUtilsBenchmarkConstants {
    var density: Double = 1.0
    var cellsDesired: Double = 0.0
    var width: Double = 0.0
    var height: Double = 0.0
    var spacing: Double = 0.0

    var densityMedium: Double = 10.0
    var cellsDesiredMedium: Double = 0.0
    var widthMedium: Double = 0.0
    var heightMedium: Double = 0.0
    var spacingMedium: Double = 0.0

    var densityLarge: Double = 25.0
    var cellsDesiredLarge: Double = 0.0
    var widthLarge: Double = 0.0
    var heightLarge: Double = 0.0
    var spacingLarge: Double = 0.0

    lateinit var boundaryVertices: Array<Array<MutableList<Vertex>>>
    lateinit var graphVertices: Array<Array<MutableList<Vertex>>>
    lateinit var boundaryVerticesMedium: Array<Array<MutableList<Vertex>>>
    lateinit var graphVerticesMedium: Array<Array<MutableList<Vertex>>>
    lateinit var boundaryVerticesLarge: Array<Array<MutableList<Vertex>>>
    lateinit var graphVerticesLarge: Array<Array<MutableList<Vertex>>>
}