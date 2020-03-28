package models.shapes.curves

import models.shapes.path.Context
import models.shapes.curves.basis.BasisClosedCurve
import models.shapes.curves.basis.BasisCurve

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
fun curveBasis(): (Context) -> Curve = ::BasisCurve
fun curveBasisClosed(): (Context) -> Curve = ::BasisClosedCurve

fun curveLinear(): (Context) -> Curve = ::LinearCurve