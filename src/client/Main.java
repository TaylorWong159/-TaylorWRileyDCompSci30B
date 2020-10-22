package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jdk.incubator.http.HttpClient;
import jdk.incubator.http.HttpRequest;
import jdk.incubator.http.HttpResponse;

public class Main extends Application {
	
	public static final HttpClient client = HttpClient.newHttpClient();
	private static String username = "";
	private static ChatController chatController;
	public static final double tileSize = 32;
	public static final int tilesX = 23, tilesY = 17;
	public static double height = tilesY * tileSize, width = tilesX * tileSize + 300 + 2 * tileSize;
	public static Map map;
	public static Player player;
	public static int moves = 7;

	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage window) throws ParseException, FileNotFoundException, IOException, InterruptedException, URISyntaxException {
		Object[] settings = LoginWindow.display();
		username = (String) settings[0];
		GameType gameType = (GameType) settings[1];
		
		
		switch(gameType) {
			case LOCAL:
				map = new Map((JSONObject) new JSONParser().parse(new FileReader(new File("src/Assets/MapData.json").getAbsolutePath())), LocalMenu.display());
				break;
			case ONLINE:
				break;
			default:
				return;	
		}
		

		
		// Canvas
		Canvas canvas = new Canvas(32 * 23, 32 * 17);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		
		// Object Setup
		LinkedList<Player> players = new LinkedList<Player>();
		player = new Player((JSONObject) new JSONParser().parse(HttpClient.newHttpClient().send(HttpRequest.newBuilder()
				.uri(new URI(HttpSettings.uri + "/join"))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublisher.fromString(map.spawns.toJSONString()))
				.build(),
				HttpResponse.BodyHandler.asString()).body()));
		players.add(player);
		
		// HUD
		TurnBox turns = new TurnBox();
		turns.setPadding(new Insets(0, 0, 0, 8));
		
		HBox hud = new HBox();
		hud.getChildren().addAll(canvas, turns);
		hud.setMinSize(canvas.getWidth() + 2 * tileSize, canvas.getHeight());
		hud.setAlignment(Pos.CENTER_LEFT);
		hud.setPadding(new Insets(-8, 0, 0, 0));
		
		
		// Chat Control
		chatController = new ChatController();
		new Thread(chatController).start();

		Chat chat = new Chat(chatController, username);
		
		// Animation Timer
		AnimationTimer timer = new AnimationTimer() {
			private boolean editing = false;
			
			@Override
			public void handle(long time) {
				// Update Messages
				if (!editing) {
					editing = true;
					LinkedList<Message> msgList = chatController.getMessages();
					chat.messages.getChildren().clear();
					for (int i = 0; i < msgList.size(); i++) {
						if (i % 2 == 1) msgList.get(i).setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(0), new Insets(0))));
						else msgList.get(i).setBackground(new Background(new BackgroundFill(Color.web("EEE"), new CornerRadii(0), new Insets(0))));
						chat.messages.getChildren().add(msgList.get(i));
					}
					editing = false;
				}
				
				// Update Canvas
				map.draw(gc);
				for (Player play: players) play.draw(gc);
			}
		};
		timer.start();

		HBox container = new HBox();
		container.getChildren().addAll(hud, chat);
		container.setAlignment(Pos.CENTER_RIGHT);
		container.setBackground(new Background(new BackgroundFill(Color.web("#333"), new CornerRadii(0), new Insets(0))));
		
	

		Scene scene = new Scene(container, width, height);
		
		scene.setOnMouseClicked(new ClickHandler(map));
		
		window.widthProperty().addListener((obs, old, nw) -> width = (double) nw);
		
		window.heightProperty().addListener((obs, old, nw) -> height = (double) nw);
		
		window.setScene(scene);
		window.setTitle("Game");
		window.setResizable(false);
		window.show();
		
		
		window.setOnCloseRequest(e -> {
			try {
				HttpClient.newHttpClient().send(HttpRequest.newBuilder()
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublisher.fromString(player.toJSON().toJSONString()))
						.uri(new URI(HttpSettings.uri + "/leave"))
						.build(),
						HttpResponse.BodyHandler.asString());
			} catch (Exception e1) {}
			chatController.stop();
		});
	}
}