package models.geometry

/**
 * Class: Node
 * Explanation:
 *
 * @author Jarno Michiels
 */
class Node(val i: Int, val x: Double, val y: Double, var t: Int, var prev: Node?, var next: Node?, var removed: Boolean) {
    fun remove(): Node? {
        prev?.next = next
        next?.prev = prev
        removed = true
        return prev
    }
}