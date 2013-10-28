package de.dakror.villagedefense.game.world;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.entity.Struct;
import de.dakror.villagedefense.game.entity.Structs;
import de.dakror.villagedefense.game.tile.Tile;
import de.dakror.villagedefense.util.Drawable;
import de.dakror.villagedefense.util.EventListener;

/**
 * @author Dakror
 */
public class World extends EventListener implements Drawable
{
	public int width, height;
	
	Chunk[][] chunks;
	
	public ArrayList<Entity> entities = new ArrayList<>();
	
	public World()
	{
		width = Game.w.getWidth();
		height = Game.w.getHeight();
		
		chunks = new Chunk[(int) Math.floor(width / (float) (Chunk.SIZE * Tile.SIZE))][(int) Math.floor(height / (float) (Chunk.SIZE * Tile.SIZE))];
		for (int i = 0; i < chunks.length; i++)
			for (int j = 0; j < chunks[0].length; j++)
				chunks[i][j] = new Chunk(i, j);
		
		for (int i = 0; i < chunks.length; i++)
			for (int j = 0; j < chunks[0].length; j++)
				chunks[i][j].render(this);
		
		generate();
	}
	
	public byte getTileId(int x, int y)
	{
		Point index = getChunk(x, y);
		
		if (index.x < 0 || index.y < 0 || index.x >= chunks.length || index.y >= chunks[index.x].length)
		{
			return Tile.emtpy.getId();
		}
		
		return chunks[index.x][index.y].getTileId(x - index.x * Chunk.SIZE, y - index.y * Chunk.SIZE);
	}
	
	/**
	 * @return <table>
	 *         <tr>
	 *         <td>(0|0)</td>
	 *         <td>(1|0)</td>
	 *         <td>(2|0)</td>
	 *         </tr>
	 *         <tr>
	 *         <td>(0|1)</td>
	 *         <td>(1|1)</td>
	 *         <td>(2|1)</td>
	 *         </tr>
	 *         <tr>
	 *         <td>(0|2)</td>
	 *         <td>(1|2)</td>
	 *         <td>(2|2)</td>
	 *         </tr>
	 *         </table>
	 */
	public byte[][] getNeighbors(int x, int y)
	{
		byte[][] data = new byte[3][3];
		for (int i = -1; i < 2; i++)
		{
			for (int j = -1; j < 2; j++)
			{
				data[i + 1][j + 1] = getTileId(x + i, y + j);
			}
		}
		return data;
	}
	
	public Point getChunk(int x, int y)
	{
		return new Point((int) Math.floor(x / (float) Chunk.SIZE), (int) Math.floor(y / (float) Chunk.SIZE));
	}
	
	@Override
	public void draw(Graphics2D g)
	{
		for (int i = 0; i < chunks.length; i++)
			for (int j = 0; j < chunks[0].length; j++)
				chunks[i][j].draw(g);
		
		@SuppressWarnings("unchecked")
		ArrayList<Entity> sorted = (ArrayList<Entity>) entities.clone();
		Collections.sort(sorted, new Comparator<Entity>()
		{
			@Override
			public int compare(Entity o1, Entity o2)
			{
				return o1.getY() - o2.getY();
			}
		});
		
		for (Entity e : sorted)
			e.draw(g);
	}
	
	public void generate()
	{
		int x = (int) Math.floor(width / 2f / Tile.SIZE) - 2;
		int y = (int) Math.floor(height / 2f / Tile.SIZE) - 3;
		
		entities.add(new Struct(x, y, Structs.CORE_HOUSE));
		entities.add(new Struct(x, y + 4, Structs.TREE));
	}
	
	@Override
	public void update()
	{}
	
	@Override
	public void mouseMoved(MouseEvent e)
	{
		for (Entity entity : entities)
			entity.setHovered(false);
		for (Entity entity : entities)
			if (entity.mouseMoved(e)) break;
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		for (Entity entity : entities)
			entity.setClicked(false);
		for (Entity entity : entities)
			if (entity.mousePressed(e)) break;
	}
}
