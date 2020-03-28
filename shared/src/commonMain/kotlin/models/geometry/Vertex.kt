package models.geometry

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
class Vertex(var x: Double = 0.0, var y: Double = 0.0) {
    override fun toString(): String {
        return "Vertex($x, $y)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        val o = other as Vertex?
        if (x.toBits() != o!!.x.toBits()) return false
        return y.toBits() == o.y.toBits()
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        var temp: Long = x.toBits()
        result = prime * result + (temp xor temp.ushr(32)).toInt()
        temp = y.toBits()
        result = prime * result + (temp xor temp.ushr(32)).toInt()
        return result
    }
}