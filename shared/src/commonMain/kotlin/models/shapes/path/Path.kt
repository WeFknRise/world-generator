package models.shapes.path

import models.shapes.BasicShape
import models.shapes.path.elements.PathElement

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
data class Path(val elements: List<PathElement>, val pathString: String) : BasicShape()