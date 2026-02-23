package onlyfilms;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import onlyfilms.ui.NavigationBar;
import onlyfilms.ui.HomeSection;
import onlyfilms.ui.MoviesSection;
import onlyfilms.ui.GenresSection;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        NavigationBar navBar = new NavigationBar();
        root.setTop(navBar);

        // Main content area (switchable)
        HomeSection homeSection = new HomeSection();
        MoviesSection moviesSection = new MoviesSection();
        GenresSection genresSection = new GenresSection();
        root.setCenter(homeSection);

        // Navigation logic
        navBar.setOnHome(() -> root.setCenter(homeSection));
        navBar.setOnMovies(() -> root.setCenter(moviesSection));
        navBar.setOnGenres(() -> root.setCenter(genresSection));
        Scene scene = new Scene(root, 1200, 800);
        // Add JavaFX stylesheet for OnlyFilms look
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        primaryStage.setTitle("OnlyFilms - Discover, Review, Share");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
