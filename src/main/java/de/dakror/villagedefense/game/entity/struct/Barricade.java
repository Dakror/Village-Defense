package de.dakror.villagedefense.game.entity.struct;

import java.awt.geom.Rectangle2D;

import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.entity.creature.Creature;
import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.settings.Attributes.Attribute;
import de.dakror.villagedefense.settings.Researches;
import de.dakror.villagedefense.settings.Resources.Resource;
import de.dakror.villagedefense.util.Vector;

/**
 * @author Dakror
 */
public class Barricade extends Struct
{
	public Barricade(int x, int y)
	{
		super(x, y, 1, 3);
		name = "Barrikade";
		tx = 2;
		ty = 10;
		attributes.set(Attribute.HEALTH, 21);
		attributes.set(Attribute.HEALTH_MAX, 21);
		
		buildingCosts.set(Resource.STONE, 5);
		buildingCosts.set(Resource.GOLD, 10);
		canPlaceOnWay = true;
		setBump(new Rectangle2D.Float(0.2f, 1, 0.4f, 2));
		
		structPoints.addAttacks(new Vector(-0.8f, 2), new Vector(0.6f, 2));
		
		description = "Blockiert Monster bis zur Zerstörung. Geister fliegen hindurch!";
	}
	
	@Override
	protected void onMinedUp()
	{}
	
	@Override
	public Entity clone()
	{
		return new Barricade((int) x / Tile.SIZE, (int) y / Tile.SIZE);
	}
	
	@Override
	protected void onDeath()
	{
		dead = true;
	}
	
	@Override
	public void onSpawn(boolean initial)
	{
		super.onSpawn(initial);
		for (Entity e : Game.world.entities)
		{
			if (e instanceof Creature) ((Creature) e).lookupTargetEntity();
		}
	}
	
	@Override
	public void initGUI()
	{}
	
	@Override
	public void onUpgrade(Researches research, boolean inititial)
	{}
}