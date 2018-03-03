package work.airz

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import java.util.*


fun main(args: Array<String>) {
    Application.launch(Main::class.java, *args)
}


class Main : Application() {
    private val MIN_SCREEN_WIDTH = 480.0
    private val MIN_SCREEN_HEIGHT = 640.0

    override fun start(primaryStage: Stage?) {
        var fxmlLoader = FXMLLoader(javaClass.getResource("/layout.fxml"))
        fxmlLoader.resources = ResourceBundle.getBundle("bundle/Controller")

        var root: Parent = fxmlLoader.load()
        primaryStage!!.title = "PriParaCreator 1.1"
        var scene = Scene(root, MIN_SCREEN_WIDTH, MIN_SCREEN_HEIGHT)
        primaryStage.scene = scene
        primaryStage.minWidth = MIN_SCREEN_WIDTH
        primaryStage.minHeight = MIN_SCREEN_HEIGHT
        primaryStage.maxWidth = MIN_SCREEN_WIDTH
        primaryStage.maxHeight = MIN_SCREEN_HEIGHT
        var controller: Controller = fxmlLoader.getController()
        controller.init(primaryStage)
        primaryStage.show()
    }


}