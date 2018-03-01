package work.airz

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.imageio.ImageIO

/**
 * 画像/動画/hashmap保存系の処理を行います
 * @author khrom
 */
class MediaIO {


    /**
     * 引数のディレクトリから全ての動画ファイルを読み出して処理します
     */
    fun videoProcessing(rootDir: File) {
        if (!rootDir.isDirectory) return
        var videoHash = HashMap<Long, MutableList<String>>(4000000, 1.0F) //100話分くらいだったらこれくらいかと(要検証)

        var fileList = recursiveSearch(rootDir).filter { file -> nameCheck(file, "mp4") }
        println("${fileList.size} video files are found.")
        fileList.forEach {
            var miniVideoHash = video2Hash(it)
            miniVideoHash.forEach { key, value ->
                val hashListExists = videoHash[key] //あるか確認
                if (hashListExists != null && hashListExists.isNotEmpty()) {
                    hashListExists.addAll(value)
                    videoHash[key] = hashListExists
                } else { //新たなやつだった場合
                    var hashList = mutableListOf<String>()
                    hashList.addAll(value)
                    videoHash[key] = hashList //新たに追加
                }
            }
        }
        videoHash.forEach { key, value ->
            println("hash:${String.format("%016X", key)} value:${value}")
        }
    }


    /**
     * 動画からフレームを抽出してhashを求める
     * ネイティブベースの方がjavaネイティブより3倍くらい高速なのでjavacppベースのものを用いています
     * @param input input video file
     */
    private fun video2Hash(input: File): HashMap<Long, MutableList<String>> {
        var frameGrabber = FFmpegFrameGrabber(input)
        var converter = Java2DFrameConverter()
        var videoHash = HashMap<Long, MutableList<String>>(400000, 1.0F) //大体30分アニメだと全てかぶらなくてもこれくらい
        frameGrabber.audioChannels = 0
        frameGrabber.start()
//    var start = System.currentTimeMillis()
        var count = 1
        while (frameGrabber.frameNumber < frameGrabber.lengthInFrames) { //全フレーム抽出
            /**
             * 注意：動画とビデオのフレームを同時に扱っているのでフレーム数が同じことがある(動画は異なる)
             * →動画はカウンターを使って分ける
             */
            var bmp = converter.convert(frameGrabber.grabImage())
//        println("frame:${String.format("%06d", frameGrabber.frameNumber)} hash:${String.format("%016X", ImageSearch().getVector(bmp))}") //for debug
//        save2jpg(input, File("./jpgout"), bmp, frameGrabber.frameNumber) //savejpg
            val hashListExists = videoHash[ImageSearch().getVector(bmp)] //あるか確認
            if (hashListExists != null && hashListExists.isNotEmpty()) {
                hashListExists.add(input.nameWithoutExtension + "_" + String.format("%06d", count))
                videoHash[ImageSearch().getVector(bmp)] = hashListExists
            } else { //新たなやつだった場合
                var hashList = mutableListOf<String>()
                hashList.add(input.nameWithoutExtension + "_" + String.format("%06d", count))
                videoHash[ImageSearch().getVector(bmp)] = hashList //新たに追加
            }
            count++
//        if (count > 500) break
        }
//    var end = System.currentTimeMillis()
//    println("${end - start}[ms]")
        return videoHash
    }


    fun importJPG(rootDir: File): HashMap<Long, MutableList<String>> {
        var jpgFiles = recursiveSearch(rootDir).filter { file -> nameCheck(file, "jpg") }
        println("${jpgFiles.size} jpg files are found.")
//   jpgFiles= jpgFiles.toMutableList().addAll(recursiveSearch(rootDir).filter { file -> nameCheck(file, "jpg") })
        var videoHash = HashMap<Long, MutableList<String>>(4000000, 1.0F)
        jpgFiles.forEach { jpgFile ->
            val jpgData = ImageIO.read(jpgFile) ?: return@forEach
            val key = ImageSearch().getVector(jpgData)
            val value = jpgFile.nameWithoutExtension // titleId_storyId_frame
            val hashListExists = videoHash[key] //あるか確認
            if (hashListExists != null && hashListExists.isNotEmpty()) {
                hashListExists.add(value)
                videoHash[key] = hashListExists
            } else { //新たなやつだった場合
                var hashList = mutableListOf<String>()
                hashList.add(value)
                videoHash[key] = hashList //新たに追加
            }
        }
        removeDuplication(videoHash)
        return videoHash
    }


    /**
     * TODO: 重複を取り除く処理をひっそり入れる
     * List(mutableList)は省メモリかつ高速なので変えない
     */
    fun importCSV(rootDir: File): HashMap<Long, MutableList<String>> {
        var csvFiles = recursiveSearch(rootDir).filter { file -> nameCheck(file, "csv") }
        var videoHash = HashMap<Long, MutableList<String>>(4000000, 1.0F)
        csvFiles.forEach { csvFile ->
            val tIDsID = csvFile.nameWithoutExtension // titleId_storyId
            csvFile.readLines().forEach {
                val splittedText = it.split(",") //csvの分割
                //0がハッシュ値,1がフレーム
                val key = splittedText[0].toLong()
                val value = "${tIDsID}_${splittedText[1]}"

                val hashListExists = videoHash[key] //あるか確認
                if (hashListExists != null && hashListExists.isNotEmpty()) {
                    hashListExists.add(value)
                    videoHash[key] = hashListExists
                } else { //新たなやつだった場合
                    var hashList = mutableListOf<String>()
                    hashList.add(value)
                    videoHash[key] = hashList //新たに追加
                }
            }
        }
        removeDuplication(videoHash)
        return videoHash
    }

    /**
     * videoのハッシュを書き出します。
     */
    fun exportCSV(rootDir: File, videoHash: HashMap<Long, MutableList<String>>) {
        rootDir.mkdirs()
        videoHash.forEach { key, value ->
            //ファイル形式 value:　titleId_storyId_frame
            value.forEach {
                var splittedValue = it.split("_")
                var outputFile = File(rootDir, "${splittedValue[0]}_${splittedValue[1]}.csv") //出力先の作成
                if (!outputFile.parentFile.exists()) outputFile.parentFile.mkdirs() //出力先がない場合に作成する
                outputFile.appendText("${key},${splittedValue[2]}\n")
            }
        }
    }


    /**
     * 入力動画名をベースにフレームを保存します
     * @param input 動画ファイル名
     * @param rootDir 出力先rootディレクトリ
     * @param bufferedImage 画像データ
     * @param frameNumber 画像のフレーム番号
     */
    fun save2jpg(input: File, rootDir: File, bufferedImage: BufferedImage, frameNumber: Int): Boolean {
        if (!(rootDir.exists() && rootDir.isDirectory)) rootDir.mkdirs() //ディレクトリ作成
        var outputFolder = File(rootDir, input.nameWithoutExtension)
        outputFolder.mkdirs()
        return try {
            ImageIO.write(bufferedImage, "jpg", File(outputFolder, input.nameWithoutExtension + "_" + String.format("%06d", frameNumber) + ".jpg"))
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }


    /**
     * 辞書データの書き込み
     * @param destFile 対象
     * @param videoHash 辞書データ
     */
    fun saveHashMap(destFile: File, videoHash: HashMap<Long, MutableList<String>>) {
        destFile.parentFile.mkdirs()
        ObjectOutputStream(GZIPOutputStream(FileOutputStream(destFile))).use {
            it.writeObject(videoHash)
        }
    }

    /**
     * 辞書データの読み込み
     * @param destFile 辞書ファイルの場所
     */
    fun loadHashMap(destFile: File): HashMap<Long, MutableList<String>>? {
        if (!destFile.exists() || destFile.isDirectory || !destFile.isFile) return null
        var videoHash = HashMap<Long, MutableList<String>>(4000000, 1.0F) //初期化用。後で書き換わる
        ObjectInputStream(GZIPInputStream(FileInputStream(destFile))).use {
            videoHash = it.readObject() as HashMap<Long, MutableList<String>>
        }
        removeDuplication(videoHash)
        return videoHash
    }

    /**
     * 再帰的ファイル検索
     * @param rootDir ルートディレクトリ
     */
    fun recursiveSearch(rootDir: File): MutableList<File> {
        var files = rootDir.listFiles()
        var fileList = mutableListOf<File>()
        if (files.isEmpty()) return mutableListOf() //無ければ空のリストを返す
        files.filter { file -> file.exists() }.forEach {
            if (it.isDirectory) fileList.addAll(recursiveSearch(it))
            else if (it.isFile) fileList.add(it)
        }
        return fileList
    }

    /**
     * ファイル名が正しいか確認
     * @param inputFile 確認する対象
     * @param ext 拡張子
     */
    fun nameCheck(inputFile: File, ext: String): Boolean {
        if (!inputFile.exists() || inputFile.isDirectory) return false //存在確認
        if (!inputFile.extension.equals(ext)) return false //拡張子確認
        if (inputFile.nameWithoutExtension.split("_").size != 2 && inputFile.nameWithoutExtension.split("_").size != 3) return false //ファイル名は titleId_storyId_frame (.mp4/jpg) という制限
        if (inputFile.nameWithoutExtension.split("_").any { it.equals("") }) return false //titleId/storyIdに空があってはならない
        return true
    }

    /**
     * リスト内の重複排除をします。
     * @param videoHash 辞書データ
     */
    fun removeDuplication(videoHash: HashMap<Long, MutableList<String>>) {
        videoHash.forEach { key, list -> videoHash[key] = list.toHashSet().toMutableList() }
    }

}