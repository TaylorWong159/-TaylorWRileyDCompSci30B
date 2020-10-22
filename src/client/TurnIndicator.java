package client;

import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class TurnIndicator extends Button {
	public Actions action;
	public boolean set = false;
	
	public TurnIndicator() {
		super();
		this.setWidth(Main.tileSize);
		this.setHeight(Main.tileSize);
		this.setDisable(true);
		this.setMouseTransparent(true);
		this.setFocusTraversable(false);
	}
	
	public void setAction(Actions action) {
		this.action = action;
		this.set = true;
		this.setDisable(false);
		this.setGraphic(new ImageView(new Image(FileSettings.assets + "Actions/" + Actions.getString(action) + ".png")));
	}
	
	
	public void clear() {
		this.action = Actions.NONE;
		this.set = false;
		this.setDisable(true);
	}
	
	
}
