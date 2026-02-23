package onlyfilms.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class NavigationBar extends HBox {
    private Runnable onHome, onMovies, onGenres;

    public NavigationBar() {
        setPadding(new Insets(10));
        setSpacing(20);
        getStyleClass().add("navbar");

        Label logo = new Label("OnlyFilms");
        logo.getStyleClass().add("logo");

        Button homeBtn = new Button("Home");
        Button moviesBtn = new Button("Films");
        Button genresBtn = new Button("Genres");

        homeBtn.setOnAction(e -> { if (onHome != null) onHome.run(); });
        moviesBtn.setOnAction(e -> { if (onMovies != null) onMovies.run(); });
        genresBtn.setOnAction(e -> { if (onGenres != null) onGenres.run(); });

        TextField searchField = new TextField();
        searchField.setPromptText("Search films...");
        searchField.setMaxWidth(200);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button signInBtn = new Button("Sign In");
        Button registerBtn = new Button("Create Account");
        // TODO: Add auth/profile logic

        getChildren().addAll(logo, homeBtn, moviesBtn, genresBtn, searchField, spacer, signInBtn, registerBtn);
    }

    public void setOnHome(Runnable handler) { this.onHome = handler; }
    public void setOnMovies(Runnable handler) { this.onMovies = handler; }
    public void setOnGenres(Runnable handler) { this.onGenres = handler; }
}
