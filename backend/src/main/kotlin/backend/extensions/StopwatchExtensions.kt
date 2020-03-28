package backend.extensions

import org.springframework.util.StopWatch
import java.text.NumberFormat.getNumberInstance
import java.text.NumberFormat.getPercentInstance

/**
 * Class:
 * Explanation:
 *
 * @author Jarno Michiels
 */
object StopwatchExtensions {
    fun StopWatch.prettyPrintMS(): String {
        val sb = StringBuilder(shortSummaryMS())
        sb.append('\n')
        sb.append("---------------------------------------------\n")
        sb.append("ms         %     Task name\n")
        sb.append("---------------------------------------------\n")
        val nf = getNumberInstance()
        nf.minimumIntegerDigits = 9
        nf.isGroupingUsed = false
        val pf = getPercentInstance()
        pf.minimumIntegerDigits = 3
        pf.isGroupingUsed = false
        for (task in taskInfo) {
            sb.append(nf.format(task.timeMillis)).append("  ")
            sb.append(pf.format(task.timeMillis.toDouble() / totalTimeMillis)).append("  ")
            sb.append(task.taskName).append("\n")
        }

        return sb.toString()
    }

    private fun StopWatch.shortSummaryMS(): String {
        return "StopWatch '$id': running time = $totalTimeMillis ms"
    }

    fun StopWatch.averageTimeNS(itemsToIgnore: Int = 0): String {
        val items = taskInfo.slice(itemsToIgnore until taskInfo.size)
        val totalTime = items.map { it.timeNanos }.sum()

        var text = "Average time for ${taskInfo.size} tasks"
        if (itemsToIgnore > 0) text += ", excluding the first $itemsToIgnore tasks"
        return "$text: ${totalTime / items.size} ns"
    }

    fun StopWatch.averageTimeMS(itemsToIgnore: Int = 0): String {
        val items = taskInfo.slice(itemsToIgnore until taskInfo.size)
        val totalTime = items.map { it.timeMillis }.sum()

        var text = "Average time for ${taskInfo.size} tasks"
        if (itemsToIgnore > 0) text += ", excluding the first $itemsToIgnore tasks"
        return "$text: ${totalTime / items.size} ms"
    }
}