package de.dakror.villagedefense.game.entity.struct.tower;

import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.settings.Attributes.Attribute;
import de.dakror.villagedefense.settings.Resources.Resource;

/**
 * @author Dakror
 */
public class ArrowTower extends Tower
{
	public ArrowTower(int x, int y)
	{
		super(x, y);
		tx = 5;
		ty = 7;
		name = "Pfeil-Turm";
		attributes.set(Attribute.ATTACK_RANGE, Tile.SIZE * 5);
		attributes.set(Attribute.HEALTH, 50);
		attributes.set(Attribute.HEALTH_MAX, 50);
		attributes.set(Attribute.ATTACK_SPEED, 25);
		attributes.set(Attribute.DAMAGE_CREATURE, 3);
		
		buildingCosts.set(Resource.GOLD, 75);
		buildingCosts.set(Resource.WOOD, 15);
	}
}
