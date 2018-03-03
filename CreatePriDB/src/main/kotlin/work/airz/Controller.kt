package work.airz

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.input.DragEvent
import javafx.scene.input.TransferMode
import javafx.stage.Stage
import kotlinx.coroutines.experimental.*
import java.io.File
import javafx.stage.DirectoryChooser


class Controller : MediaIO() {
    @FXML
    lateinit var process: ChoiceBox<String>
    @FXML
    lateinit var select: Button
    @FXML
    lateinit var start: Button
    @FXML
    lateinit var cancel: Button
    @FXML
    lateinit var clear: Button
    @FXML
    lateinit var logArea: TextArea
    @FXML
    lateinit var pathField: TextField
    @FXML
    lateinit var exportCSV: CheckBox
    @FXML
    lateinit var mergeData: CheckBox

    @FXML
    fun handleDragOver(event: DragEvent) {
        if (event.dragboard.hasFiles()) event.acceptTransferModes(TransferMode.LINK)
    }

    @FXML
    fun handleDropped(event: DragEvent) {
        event.isDropCompleted = if (event.dragboard.hasFiles() && event.dragboard.files.first().isDirectory) {
            pathField.text = event.dragboard.files.first().absolutePath
            true
        } else {
            false
        }

    }


    private lateinit var stage: Stage
    fun init(primaryStage: Stage) {
        stage = primaryStage
        process.selectionModel.selectFirst()

    }

    @FXML
    fun buttonHandler(event: ActionEvent) {
        when ((event.source as Button).id) {
            select.id -> {
                val directoryChooser = DirectoryChooser()
                directoryChooser.title = "Open Root Directory"

                val selectedFolder = directoryChooser.showDialog(stage.scene.window) ?: return
                pathField.text = selectedFolder.absolutePath
            }
            start.id -> {
                start.isDisable = true
                launchThread()

            }
            clear.id -> {
                logArea.clear()
            }
            else -> {
                println("button not found!")
            }
        }
    }

    private fun launchThread() = launch {
        println(File(pathField.text).absolutePath)
        var newVideoHash = when (process.value) {
            "From MP4" -> {
                updateStatus("from MP4")
                videoProcessing(File(pathField.text))
            }
            "From CSV" -> {
                updateStatus("from CSV")
                importCSV(File(pathField.text))
            }
            "From JPG" -> {
                updateStatus("from JPG")
                importJPG(File(pathField.text))
            }
            "From DB" -> {
                updateStatus("from DB")
                importDB(File(pathField.text))
            }
            else -> {
                hashMapOf()
            }
        }
        if (!newVideoHash.any()) {
            start.isDisable = false //ボタンを有効に戻す
            return@launch
        }
        updateStatus("start processing")

        if (mergeData.isSelected) {//データのマージ
            updateStatus("merging...")
            runBlocking {
                var oldVideoHash = loadHashList(File(VIDEO_HASH_PATH)) ?: listOf()
                if (oldVideoHash.isEmpty()) {
                    updateStatus("old db file does not exist!")
                    return@runBlocking
                }
                mergeVideoHash2NewVideoHash(list2HashMap(oldVideoHash), newVideoHash)
            }
            updateStatus("merge finished!")
        }

        if (exportCSV.isSelected) { //csv出力にチェックが入っていたら
            runBlocking {
                updateStatus("csv exporting...")
                exportCSV(File(System.getProperty("user.dir"), "csv"), newVideoHash)
                updateStatus("csv export finished!")
            }
        }
        runBlocking {
            updateStatus("saving...")
            if (File(VIDEO_HASH_PATH).exists()) {
                File(VIDEO_HASH_PATH).renameTo(File("${VIDEO_HASH_PATH}.old"))
            }
            saveHashMap(File(VIDEO_HASH_PATH), newVideoHash)
            updateStatus("saved!!")
        }

        start.isDisable = false //ボタンを有効に戻す
    }

    override fun updateStatus(log: String) {
        val MAX_LENGTH = 100000000
        logArea.text = "$log\n${logArea.text}"
        println(log)
        if (logArea.text.length >= MAX_LENGTH) {
            logArea.text = logArea.text.substring(0, MAX_LENGTH)
        }
    }

}