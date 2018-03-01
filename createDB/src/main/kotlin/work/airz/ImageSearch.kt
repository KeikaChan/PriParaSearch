package work.airz


import java.awt.image.BufferedImage
import java.util.HashMap

/**
 * 画像の探索関連をまとめています
 * @author khrom
 */
class ImageSearch {

    /**
     * ベクトル変換部分
     * @param bmp 画像データ
     */
    fun getVector(bmp: BufferedImage): Long {
        val data = getSmallImageData(bmp, 9, 8) //最終的に畳み込みして64bitになる
        var gray = IntArray(data.size / 4)
        for (i in 0 until gray.size) {
            gray[i] = 29 * data[i * 4] + 150 * data[i * 4 + 1] + 77 * data[i * 4 + 2]
        }
        var result = 0L
        var p = 0
        for (y in 0 until 8) { //64bitの画像に丸め込む
            for (x in 0 until 8) {
                result = result.shl(1).or(if (gray[p] > gray[p + 1]) 1L else 0L)
                p++
            }
            p++
        }
        return result
    }

    /**
     * 類似画像を取ってくる
     * @param hash ハッシュデータ
     * @param level 検索レベル
     */
    fun getSimilarImage(hash: Long, level: Int, videoHash: HashMap<Long, MutableList<String>>): List<String> {
        var result: List<String> = if (level <= 3) {
            getSimilarHash(hash, level, videoHash)
        } else {
            getSimilarHashB(hash, level, videoHash)
        }
        return groupByScene(result)
    }

    /**
     * 類似位置の画像だと普通に場所が被るのでそれを排除します
     * @param sceneList 対象のシーン
     */
    private fun groupByScene(sceneList: List<String>): List<String> {
        if (sceneList == null || sceneList.isEmpty()) return listOf() //list の中身　titleId_storyId_frame
        //実装的にはタイトルIDやストーリーIDは0づめで数字があることが好ましいが、近接フレームを排除するだけなので気にしなくていい
        val sortedList = sceneList.sorted()
        var old = sortedList.first()
        return sortedList.filter {
            val oldSplitText = old.split("_")
            val oldTitleId = oldSplitText[0].toInt()
            val oldStoryId = oldSplitText[1].toInt()
            val oldFrame = oldSplitText[1].toLong()

            val newSplitText = it.split("_")
            val newTitleId = newSplitText[0].toInt()
            val newStoryId = newSplitText[1].toInt()
            val newFrame = newSplitText[1].toLong()

            newTitleId != oldTitleId || newStoryId != oldStoryId || newFrame - oldFrame > 100
        }
    }

    /**
     * 全探索　Brute force
     * @param hash 対象のハッシュ値
     * @param level 検索レベル
     * @param videoHash データベースのデータ
     */
    private fun getSimilarHashB(hash: Long, level: Int, videoHash: HashMap<Long, MutableList<String>>): List<String> {
        var result = mutableListOf<String>()
        videoHash.filter { populationCount(hash.xor(it.key)) <= level }.forEach { _, value ->
            result.addAll(value)
        }
        return result.toList()
    }

    /**
     * 1になっているビット数のカウント　ハミング距離のこと
     * @param hash 対象のハッシュ値
     */
    private fun populationCount(hash: Long): Int {
        var res: Long = hash.and(6148914691236517205L) + hash.shr(1).and(6148914691236517205L)
        res = res.and(3689348814741910323L) + res.shr(2).and(3689348814741910323L)
        res = res.and(1085102592571150095L) + res.shr(4).and(1085102592571150095L)
        res = res.and(71777214294589695L) + res.shr(8).and(71777214294589695L)
        res = res.and(281470681808895L) + res.shr(16).and(281470681808895L)
        res = res.and(4294967295L) + res.shr(32).and(4294967295L)
        return res.toInt()
    }

    /**
     * 類似画像をハミング距離毎に検索を掛けます
     * @param hash 対象のハッシュ値
     * @param level 検索レベル
     * @param videoHash データベースのデータ
     */
    private fun getSimilarHash(hash: Long, level: Int, videoHash: HashMap<Long, MutableList<String>>): List<String> {
        var p: MutableList<String>?
        var result = mutableListOf<String>()

        if (level >= 0) { //完全一致
            p = videoHash[hash]
            if (p != null && p.size > 0) {
                result.addAll(p)
            }
        }

        if (level >= 1) {
            for (i in 0 until 64) {
                p = videoHash[hash.xor(1L.shl(i))]
                if (p != null && p.size > 0) {
                    result.addAll(p)
                }
            }
        }

        if (level >= 2) {
            for (i in 0 until 63) {
                for (j in i + 1 until 64) {
                    p = videoHash[hash.xor(1L.shl(i)).xor(1L.shl(j))]
                    if (p != null && p.size > 0) {
                        result.addAll(p)
                    }
                }
            }
        }

        if (level >= 3) {
            for (i in 0 until 62) {
                for (j in i + 1 until 63) {
                    for (k in j + 1 until 64) {
                        p = videoHash[hash.xor(1L.shl(i)).xor(1L.shl(j)).xor(1L.shl(k))]
                        if (p != null && p.size > 0) {
                            result.addAll(p)
                        }
                    }
                }
            }
        }

        if (level >= 4) {
            for (i in 0 until 61) {
                for (j in i + 1 until 62) {
                    for (k in j + 1 until 63) {
                        for (l in k + 1 until 64) {
                            p = videoHash[hash.xor(1L.shl(i)).xor(1L.shl(j)).xor(1L.shl(k)).xor(1L.shl(l))]
                            if (p != null && p.size > 0) {
                                result.addAll(p)
                            }
                        }
                    }
                }
            }
        }
        return result.toList()
    }

    /**
     * 画像縮小
     * @param bmp 画像データ
     * @param width 画像の幅
     * @param height 画像の高さ
     */
    private fun getSmallImageData(bmp: BufferedImage, width: Int, height: Int): IntArray {
        val bmp32 = bmp.getRGB(0, 0, bmp.width, bmp.height, null, 0, bmp.width)
        var bmp32Data = IntArray(bmp32.size * 4)
        for (i in 0 until bmp32.size) {
            bmp32Data[i * 4] = bmp32[i].and(255)
            bmp32Data[i * 4 + 1] = bmp32[i].shr(8).and(255)
            bmp32Data[i * 4 + 2] = bmp32[i].shr(16).and(255)
            bmp32Data[i * 4 + 3] = bmp32[i].shr(24).and(255)
        }
        var result = IntArray(width * height * 4)
        val s = 12
        var pos = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val srcX0 = x * bmp.width / width
                val srcY0 = y * bmp.height / height
                var a = 0
                var b = 0
                var g = 0
                var r = 0
                for (yy in 0 until s) {
                    for (xx in 0 until s) {
                        val dx = xx * bmp.width / width / s
                        val dy = yy * bmp.height / height / s
                        val p = (srcX0 + dx + (srcY0 + dy) * bmp.width) * 4
                        b += bmp32Data[p]
                        g += bmp32Data[p + 1]
                        r += bmp32Data[p + 2]
                        a += bmp32Data[p + 3]
                    }
                }
                result[pos++] = b / s / s
                result[pos++] = g / s / s
                result[pos++] = r / s / s
                result[pos++] = a / s / s
            }
        }
        return result
    }

}