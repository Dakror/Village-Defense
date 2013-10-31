package de.dakror.villagedefense.game.entity.struct;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.settings.Resources;
import de.dakror.villagedefense.settings.Resources.Resource;
import de.dakror.villagedefense.settings.StructPoints;
import de.dakror.villagedefense.util.Vector;

/**
 * @author Dakror
 */
public abstract class Struct extends Entity
{
	protected int tx, ty;
	protected boolean placeGround;
	protected StructPoints structPoints;
	protected Resources buildingCosts;
	protected Image image;
	
	public Struct(int x, int y, int width, int height)
	{
		super(x * Tile.SIZE, y * Tile.SIZE, width * Tile.SIZE, height * Tile.SIZE);
		
		structPoints = new StructPoints();
		buildingCosts = new Resources();
	}
	
	@Override
	public void draw(Graphics2D g)
	{
		drawBump(g, false);
		
		g.drawImage(getImage(), (int) x, (int) y, Game.w);
		
		drawBump(g, true);
	}
	
	public void setBump(Rectangle2D r)
	{
		super.setBump(new Rectangle((int) Math.round(r.getX() * Tile.SIZE), (int) Math.round(r.getY() * Tile.SIZE), (int) Math.round(r.getWidth() * Tile.SIZE), (int) Math.round(r.getHeight() * Tile.SIZE)));
	}
	
	public void placeGround()
	{
		if (!placeGround) return;
		
		int x = (int) Math.round(bump.getX() / Tile.SIZE) - 1;
		int y = (int) Math.round(bump.getY() / Tile.SIZE) - 1;
		int width = (int) Math.round(bump.getWidth() / Tile.SIZE) + 2;
		int height = (int) Math.round(bump.getHeight() / Tile.SIZE) + 2;
		
		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{
				Game.world.setTileId((int) this.x / Tile.SIZE + x + i, (int) this.y / Tile.SIZE + y + j, Tile.ground.getId());
			}
		}
	}
	
	public boolean isPlaceGround()
	{
		return placeGround;
	}
	
	public StructPoints getStructPoints()
	{
		return structPoints;
	}
	
	@Override
	public void onSpawn()
	{
		placeGround();
	}
	
	public void mineAllResources(int amount)
	{
		if (resources.size() == 0) return;
		
		ArrayList<Resource> filled = resources.getFilled();
		for (Resource r : filled)
		{
			if (!r.isUsable()) continue;
			
			int get = resources.get(r);
			int newVal = get - amount > -1 ? get - amount : 0;
			Game.currentGame.resources.add(r, get - newVal);
			resources.set(r, newVal);
		}
		
		if (resources.size() == 0) onMinedUp();
	}
	
	public ArrayList<Vector> getSurroundingTiles()
	{
		ArrayList<Vector> tiles = new ArrayList<>();
		
		// -- bump rectangle -- //
		int x = (int) Math.round(bump.getX() / Tile.SIZE);
		int y = (int) Math.round(bump.getY() / Tile.SIZE);
		int w = (int) Math.round(bump.getWidth() / Tile.SIZE);
		int h = (int) Math.round(bump.getHeight() / Tile.SIZE);
		
		for (int i = 0; i < w + 2; i++)
		{
			for (int j = 0; j < h + 2; j++)
			{
				if ((i == 0 || i == w + 1) && (j == 0 || j == h + 1)) continue;
				if (i > 0 && j > 0 && i < w + 1 && j < h + 1) continue;
				
				tiles.add(new Vector(x + i - 1, y + j - 1));
			}
		}
		
		return tiles;
	}
	
	public Resources getBuildingCosts()
	{
		return buildingCosts;
	}
	
	public Image getImage()
	{
		if (image != null) return image;
		image = Game.getImage("structs.png").getSubimage(tx * Tile.SIZE, ty * Tile.SIZE, width, height);
		return image;
	}
	
	protected abstract void onMinedUp();
}