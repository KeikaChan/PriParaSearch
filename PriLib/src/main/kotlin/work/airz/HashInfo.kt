package work.airz

import java.io.Serializable
import java.util.*

/**
 * データ保存用のクラスです
 * 値の範囲について
 * @param titleId :-128~127
 * @param storyId :-32768~32767
 * @param frame   :-2147483648~2147483647
 */
class HashInfo(val titleId: Byte, val storyId: Short, val frame: Int) : Comparable<HashInfo>, Serializable {
    override fun compareTo(other: HashInfo): Int {
        return if (titleId - other.titleId < 0 || titleId - other.titleId > 0) {
            titleId - other.titleId
        } else if (storyId - other.storyId < 0 || storyId - other.storyId > 0) {
            storyId - other.storyId
        } else if (frame - other.frame < 0 || frame - other.frame > 0) {
            frame - other.frame
        } else {
            0
        }
    }

    override fun equals(other: Any?): Boolean {
        return if (other is HashInfo) {
            "${titleId}_${storyId}_${frame}".equals("${other.titleId}_${other.storyId}_${other.frame}")
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return Objects.hashCode(arrayOf(titleId, storyId, frame))
    }

    override fun toString(): String {
        return "${titleId}_${storyId}_${String.format("%06d", frame)}"
    }
}