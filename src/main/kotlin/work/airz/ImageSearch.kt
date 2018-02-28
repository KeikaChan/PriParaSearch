package work.airz


import java.awt.image.BufferedImage

class ImageSearch {
    /**
     * ベクトル変換部分
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
     * 画像縮小
     * TODO: ND4Jを使うかどうかの検討。入れると容量が激増する
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