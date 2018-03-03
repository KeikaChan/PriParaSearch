package work.airz

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.image.BufferedImage
import java.io.*
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.imageio.ImageIO
import kotlin.collections.ArrayList

/**
 * 画像/動画/hashmap保存系の処理を行います
 * @author khrom
 */
abstract class MediaIO {
    val VIDEO_HASH_PATH = System.getProperty("user.dir") + File.separator + "index.db"

    /**
     * 引数のディレクトリから全ての動画ファイルを読み出して処理します
     * @param rootDir 動画ファイルのrootディレクトリ
     * @return 辞書データ
     */
    fun videoProcessing(rootDir: File): HashMap<Long, MutableList<HashInfo>> {
        if (!rootDir.isDirectory) return hashMapOf()
        var videoHash = HashMap<Long, MutableList<HashInfo>>(4000000, 1.0F) //100話分くらいだったらこれくらいかと(要検証)

        var fileList = recursiveSearch(rootDir).filter { file -> nameCheck(file, "mp4") }

        updateStatus("${fileList.size} video files are found.")
        fileList.forEach {
            var miniVideoHash = video2Hash(it)
            miniVideoHash.forEach { key, value ->
                //key:Long, value:MutableList<HashInfo>
                val hashListExists = videoHash[key] //あるか確認
                if (hashListExists != null && hashListExists.isNotEmpty()) {
                    hashListExists.addAll(value)
                    videoHash[key] = hashListExists
                } else { //新たなやつだった場合
                    var hashList = mutableListOf<HashInfo>()
                    hashList.addAll(value)
                    videoHash[key] = hashList //新たに追加
                }
            }
        }
        return videoHash
    }


    /**
     * 動画からフレームを抽出してhashを求める
     * ネイティブベースの方がjavaネイティブより3倍くらい高速なのでjavacppベースのものを用いています
     * @param input input video file
     * @param 辞書データ
     */
    private fun video2Hash(input: File): HashMap<Long, MutableList<HashInfo>> {
        var frameGrabber = FFmpegFrameGrabber(input)
        var converter = Java2DFrameConverter()
        var videoHash = HashMap<Long, MutableList<HashInfo>>(400000, 1.0F) //大体30分アニメだと全てかぶらなくてもこれくらい
        updateStatus("processing:${input.name}")
        frameGrabber.audioChannels = 0
        frameGrabber.start()
        var count = 1
        while (frameGrabber.frameNumber < frameGrabber.lengthInFrames) { //全フレーム抽出
            /**
             * 注意：動画とビデオのフレームを同時に扱っているのでフレーム数が同じことがある(動画は異なる)
             * →動画はカウンターを使って分ける
             */
            var bmp = converter.convert(frameGrabber.grabImage())

            var splittedText = input.nameWithoutExtension.split("_") //ファイル名が titleId_storyIdのため
            var hashInfo = HashInfo(splittedText[0].toByte(), splittedText[1].toShort(), count)

            val hashListExists = videoHash[ImageSearch().getVector(bmp)] //あるか確認 参照の値を渡している
            if (hashListExists != null && hashListExists.isNotEmpty()) {
                hashListExists.add(hashInfo)  //
            } else { //新たなやつだった場合
                var hashList = mutableListOf<HashInfo>()
                hashList.add(hashInfo)
                videoHash[ImageSearch().getVector(bmp)] = hashList //新たに追加
            }
            if (count % 1000 == 0) {
                updateStatus("now ${frameGrabber.frameNumber}/${frameGrabber.lengthInFrames - 1}")
            }
            count++
        }
        return videoHash
    }

    /**
     * JPGインポート用です
     * @param rootDir JPGデータのrootディレクトリ
     * @return 辞書データ
     */
    fun importJPG(rootDir: File): HashMap<Long, MutableList<HashInfo>> {
        var jpgFiles = recursiveSearch(rootDir).filter { file -> nameCheck(file, "jpg") }
        updateStatus("${jpgFiles.size} jpg files are found.")
        var videoHash = HashMap<Long, MutableList<HashInfo>>(4000000, 1.0F)
        var count = 1
        jpgFiles.forEach { jpgFile ->
            val jpgData = ImageIO.read(jpgFile) ?: return@forEach
            val key = ImageSearch().getVector(jpgData)

            var splittedText = jpgFile.nameWithoutExtension.split("_")  //titleId_storyId_frame
            var hashInfo = HashInfo(splittedText[0].toByte(), splittedText[1].toShort(), splittedText[1].toInt())

            val hashListExists = videoHash[key] //あるか確認
            if (hashListExists != null && hashListExists.isNotEmpty()) {
                hashListExists.add(hashInfo)
            } else { //新たなやつだった場合
                var hashList = mutableListOf<HashInfo>()
                hashList.add(hashInfo)
                videoHash[key] = hashList //新たに追加
            }
            if (count % 1000 == 0) {
                updateStatus("now ${count}/${jpgFiles.size}")
            }
            count++
        }
        removeDuplication(videoHash)
        return videoHash
    }


    /**
     * csvインポート用です
     * @param rootDir CSVデータのrootディレクトリ
     * @return 辞書データ
     */
    fun importCSV(rootDir: File): HashMap<Long, MutableList<HashInfo>> {
        var csvFiles = recursiveSearch(rootDir).filter { file -> nameCheck(file, "csv") }
        var videoHash = HashMap<Long, MutableList<HashInfo>>(4000000, 1.0F)
        csvFiles.forEach { csvFile ->
            val tIDsID = csvFile.nameWithoutExtension.split("_") // titleId_storyId
            csvFile.readLines().forEach {
                val splittedText = it.split(",") //csvの分割 hash,frame
                //0がハッシュ値,1がフレーム
                val key = splittedText[0].toLong()
                val hashInfo = HashInfo(tIDsID[0].toByte(), tIDsID[1].toShort(), splittedText[1].toInt())
                val hashListExists = videoHash[key] //あるか確認
                if (hashListExists != null && hashListExists.isNotEmpty()) {
                    hashListExists.add(hashInfo)
                } else { //新たなやつだった場合
                    var hashList = mutableListOf<HashInfo>()
                    hashList.add(hashInfo)
                    videoHash[key] = hashList //新たに追加
                }
            }
        }
        removeDuplication(videoHash)
        return videoHash
    }

    /**
     * 辞書データ動詞をインポートしてマージします。
     * @param rootDir 辞書データのrootディレクトリ
     * @return 辞書データ
     */
    fun importDB(rootDir: File): HashMap<Long, MutableList<HashInfo>> {
        var dbFiles = recursiveSearch(rootDir).filter { file -> file.extension.equals("db") }
        var videoHash = HashMap<Long, MutableList<HashInfo>>(4000000, 1.0F)
        dbFiles.forEach {
            var oldHash = loadHashMap(it) ?: return@forEach
            mergeVideoHash2NewVideoHash(list2HashMap(oldHash), videoHash)
        }
        return videoHash
    }

    /**
     * videoのハッシュを書き出します。
     * @param rootDir 出力先rootディレクトリ
     * @param videoHash 動画の辞書データ
     * @return 辞書データ
     */
    fun exportCSV(rootDir: File, videoHash: HashMap<Long, MutableList<HashInfo>>) {
        rootDir.mkdirs()
        //出力用のHashMapを作る
        var destMap = hashMapOf<String, String>() //ファイル名(titleId_storyId) | ハッシュ値とフレーム数のペアのリストをStringにしたもの
        videoHash.forEach { key, value ->
            value.forEach {
                //it:　titleId_storyId_frame
                var dest = destMap["${it.titleId}_${it.storyId}"] ?: "" //参照の値がdestに渡されていることに注意。実際のデータではない
                dest += "${key},${it.frame}\n" //追加 一回の書き込みで済むようにまとめてる
                destMap["${it.titleId}_${it.storyId}"] = dest
            }
        }
        destMap.forEach { key, value ->
            //keyがファイル名、valueがハッシュ値とフレーム
            var outputFile = File(rootDir, "${key}.csv")
            if (!outputFile.parentFile.exists()) outputFile.parentFile.mkdirs() //出力先がない場合に作成する
            outputFile.appendText(value)
        }
    }


    /**
     * 入力動画名をベースにフレームを保存します。
     *
     * @param input 動画ファイル名
     * @param rootDir 出力先rootディレクトリ
     * @param bufferedImage 画像データ
     * @param frameNumber 画像のフレーム番号
     * @return 保存がうまくいったか　true:success false:error
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
    fun saveHashMap(destFile: File, videoHash: HashMap<Long, MutableList<HashInfo>>) {
        destFile.parentFile.mkdirs()
        ObjectOutputStream(GZIPOutputStream(FileOutputStream(File(destFile.absolutePath)))).use {
            it.writeObject(hashMap2List(videoHash))
        }
    }

    /**
     * 辞書データの読み込み
     * @param destFile 辞書ファイルの場所
     * @param 辞書データ
     */
    fun loadHashMap(destFile: File): List<Pair<Long, MutableList<HashInfo>>>? {
        if (!destFile.exists() || destFile.isDirectory || !destFile.isFile) return null
        var videoHash = listOf<Pair<Long, MutableList<HashInfo>>>() //初期化用。後で書き換わる
        ObjectInputStream(GZIPInputStream(FileInputStream(destFile))).use {
            videoHash = it.readObject() as List<Pair<Long, MutableList<HashInfo>>>
        }
        return videoHash
    }

    /**
     * 再帰的ファイル検索
     * @param rootDir ルートディレクトリ
     * @param ファイルの一覧
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
     * @return 正しい拡張子か　true:ok false:NOT ok
     */
    private fun nameCheck(inputFile: File, ext: String): Boolean {
        if (!inputFile.exists() || inputFile.isDirectory) return false //存在確認
        if (!inputFile.extension.toLowerCase().equals(ext)) return false //拡張子確認
        if (inputFile.nameWithoutExtension.split("_").size != 2 && inputFile.nameWithoutExtension.split("_").size != 3) return false //ファイル名は titleId_storyId_frame (.mp4/jpg) という制限
        if (inputFile.nameWithoutExtension.split("_").any { it.equals("") }) return false //titleId/storyIdに空があってはならない
        return true
    }

    /**
     * リスト内の重複排除をします。
     * @param videoHash 辞書データ
     */
    private fun removeDuplication(videoHash: HashMap<Long, MutableList<HashInfo>>) {
        videoHash.forEach { key, list -> videoHash[key] = list.toHashSet().toMutableList() }
    }

    /**
     * VideoHashをマージします
     * @param oldVideoHash マージ元　こちらは読み込まれるだけで編集されません
     * @param newVideoHash マージを行う辞書　こちらにマージ済みデータが入ります
     */
    fun mergeVideoHash2NewVideoHash(oldVideoHash: HashMap<Long, MutableList<HashInfo>>, newVideoHash: HashMap<Long, MutableList<HashInfo>>) {
        oldVideoHash.forEach { oldKey, oldValue ->
            newVideoHash[oldKey] = if (newVideoHash[oldKey] == null) { //新しいデータにもし無かったら
                oldValue
            } else {//既にあったらまとめて返す
                var allList = mutableListOf<HashInfo>()
                allList.addAll(oldValue)
                var newList = newVideoHash[oldKey] ?: mutableListOf() //nullは無いはずだが一応
                allList.addAll(newList)
                allList
            }
        }
        removeDuplication(newVideoHash)
        return
    }


    /**
     * 再生時間を計算し、MM:SSの形式で返します。
     * @param frame 対象のフレーム
     * @param frameRate 対象動画のフレームレート
     * @return 再生時間
     */
    fun getTimeString(frame: Int, frameRate: Double): String {
        val sec = 1.0 / frameRate * frame
        return "${String.format("%02d", (sec / 60).toInt())}:${String.format("%02d", (sec % 60).toInt())}"
    }

    /**
     * 保存用。
     * 重複のない辞書データ(HashMap)をリストに変換します。
     * @param hashMap 辞書データ
     */
    fun hashMap2List(hashMap: HashMap<Long, MutableList<HashInfo>>): List<Pair<Long, MutableList<HashInfo>>> {
        var arrayList = ArrayList<Pair<Long, MutableList<HashInfo>>>() //順序つきリストに変更してみる
        hashMap.forEach { key, value ->
            arrayList.add(Pair(key, value))
        }
        var sortedList = arrayList.sortedWith(compareBy({ it.first })) //どうせ後からソートされるから。
        return sortedList
    }

    /**
     * データマージ用。
     * リストをHashMapに変換します。
     * @param hashList list型の辞書データ
     */
    fun list2HashMap(hashList: List<Pair<Long, MutableList<HashInfo>>>): HashMap<Long, MutableList<HashInfo>> {
        var videoHash = HashMap<Long, MutableList<HashInfo>>(4000000, 1.0F)
        hashList.forEach { (long, list) ->
            videoHash[long] = list
        }
        return videoHash
    }

    /**
     * ログ出力用です
     * @param log 出力したい内容
     */
    abstract fun updateStatus(log: String)

}