package onlyfilms.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;

public class HomeSection extends StackPane {
    public HomeSection() {
        Label label = new Label("Welcome to OnlyFilms!");
        label.setStyle("-fx-font-size: 2em; -fx-text-fill: #fff;");
        getChildren().add(label);
    }
}
