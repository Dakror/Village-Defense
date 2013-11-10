package de.dakror.villagedefense.ui.button;

import java.awt.Graphics2D;

import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.ui.ClickEvent;
import de.dakror.villagedefense.util.Assistant;

/**
 * @author Dakror
 */
public class TextButton extends Button
{
	static final int ty = 124, tx = 12, tw = 288, th = 58;
	
	String text;
	int size;
	
	public TextButton(int x, int y, int width, String text, int size, ClickEvent event)
	{
		super(x, y, width, Math.round(th / (float) tw * width), event);
		this.text = text;
		this.size = size;
	}
	
	@Override
	public void draw(Graphics2D g)
	{
		Assistant.drawImage(Game.getImage("gui/gui.png"), x, y, width, height, tx, ty + (78) * state, tw, th, g);
		Assistant.drawHorizontallyCenteredString(text, x, width, y + size, g, size);
	}
	
	@Override
	public void update(int tick)
	{}
}