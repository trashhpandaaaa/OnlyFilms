package onlyfilms.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;

public class MoviesSection extends StackPane {
    public MoviesSection() {
        Label label = new Label("Movies Section (Coming Soon)");
        label.setStyle("-fx-font-size: 2em; -fx-text-fill: #fff;");
        getChildren().add(label);
    }
}
