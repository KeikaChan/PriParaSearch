package work.airz

import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.stage.Stage
import java.io.File
import java.awt.image.BufferedImage
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import kotlin.system.measureTimeMillis


class Controller : MediaIO(), Initializable {
    @FXML
    lateinit var process: ChoiceBox<String>
    @FXML
    lateinit var logArea: TextArea
    @FXML
    lateinit var ddImage: ImageView

    @FXML
    fun handleDragOver(event: DragEvent) {
        if (event.dragboard.hasFiles()) event.acceptTransferModes(TransferMode.LINK)
    }

    @FXML
    fun handleDropped(event: DragEvent) {
        event.isDropCompleted = if (event.dragboard.hasFiles() && !event.dragboard.files.first().isDirectory) {

            var image = ImageIO.read(File(event.dragboard.files.first().absolutePath)) ?: return
            ddImage.image = SwingFXUtils.toFXImage(image, null)
            searchScene(image, event.dragboard.files.first().absolutePath)
            true
        } else {
            false
        }

    }


    private lateinit var stage: Stage
    private var videoHashMap = hashMapOf<Long, MutableList<String>>()
    private var titleIndex = hashMapOf<String, Pair<Double, String>>()
    private val TITLE_INDEX_PATH = System.getProperty("user.dir") + File.separator + "index.txt"
    private lateinit var resourceBundle: ResourceBundle

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        resourceBundle = resources ?: return
    }

    /**
     * 初期化用
     */
    fun init(primaryStage: Stage) {
        stage = primaryStage
        process.selectionModel.select(2)
        videoHashMap = loadHashMap(File(VIDEO_HASH_PATH)) ?: hashMapOf()
        logArea.isWrapText = true
        titleIndex = loadTitleIndex(File(TITLE_INDEX_PATH))
    }

    fun searchScene(image: BufferedImage, imageFilePath: String) {
        var result = listOf<String>()
        val time = measureTimeMillis {
            result = ImageSearch().getSimilarImage(ImageSearch().getVector(image), process.value.toInt(), videoHashMap)
        }
        var display = "${resourceBundle.getString("key.searchedImage")}: ${imageFilePath}\n${resourceBundle.getString("key.searchTime")}: ${time} ms\n\n"

        index2TitlesWithSec(result).forEach { display += "${it}${resourceBundle.getString("key.nearby")}" }
        updateStatus(display)
    }

    /**
     * 出力用の形式に変換します
     *
     */
    fun index2TitlesWithSec(indexList: List<String>): List<String> {
        //titleId_storyId_frameのリスト
        var resultList = mutableListOf<String>()
        indexList.forEach {
            val splittedText = it.split("_")
            val tIDsID = "${splittedText[0]}_${splittedText[1]}"
            val frame = splittedText[2].toInt()
            var (frameRate, title) = titleIndex[tIDsID] ?: return@forEach
            resultList.add("${title} ${getTimeString(frame, frameRate)}")
        }
        return resultList
    }

    /**
     * タイトルIndexの読み込み
     */
    fun loadTitleIndex(index: File): HashMap<String, Pair<Double, String>> { //titleId_StoryID と frameRate Titleのペア
        if (!index.exists() || index.isDirectory) return hashMapOf()
        var indexMap = hashMapOf<String, Pair<Double, String>>()
        var rawIndex = index.readLines()
        rawIndex.forEach {
            val splittedText = it.split(",") //中身csvなので
            val tIDsID = "${splittedText[0]}_${splittedText[1]}"
            val frameRate = splittedText[2].toDouble()
            val title = splittedText[3]
            indexMap[tIDsID] = Pair(frameRate, title)
        }
        return indexMap
    }

    override fun updateStatus(log: String) {
        logArea.clear()
        logArea.text = "$log\n"
        println(log)

    }

}