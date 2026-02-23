package onlyfilms.ui;

import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;

public class GenresSection extends StackPane {
    public GenresSection() {
        Label label = new Label("Genres Section (Coming Soon)");
        label.setStyle("-fx-font-size: 2em; -fx-text-fill: #fff;");
        getChildren().add(label);
    }
}
