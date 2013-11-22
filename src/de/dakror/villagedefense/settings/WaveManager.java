package de.dakror.villagedefense.settings;

import java.util.EnumMap;

import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.entity.creature.Creature;
import de.dakror.villagedefense.game.entity.creature.Ghost;
import de.dakror.villagedefense.game.entity.creature.Golem;
import de.dakror.villagedefense.game.entity.creature.Skeleton;
import de.dakror.villagedefense.game.entity.creature.TreeMonster;
import de.dakror.villagedefense.game.entity.creature.Troll;
import de.dakror.villagedefense.game.entity.creature.Zombie;
import de.dakror.villagedefense.game.world.Tile;

/**
 * @author Dakror
 */
public class WaveManager
{
	public enum Monster
	{
		ZOMBIE("zombie", Zombie.class, "Zombie:Normalschneller Untoter. F�gt Geb�uden 5 Schaden hinzu. H�lt 20 Schaden aus"),
		GHOST("ghost", Ghost.class, "Geist:Schnelles Gespenst. F�gt Geb�uden 10 Schaden hinzu. H�lt 8 Schaden aus. Kann durch Barrikaden hindurchfliegen"),
		SKELETON("skeleton", Skeleton.class, "Skelett:Langsamerer Totenritter. F�gt Geb�uden 10 Schaden hinzu. H�lt 50 Schaden aus"),
		TROLL("troll", Troll.class, "Troll:Schlurfender Monsterboss. F�gt Geb�uden 25 Schaden hinzu. H�lt 500 Schaden aus"),
		GOLEM("golem", Golem.class, "Golem:Normalschnelle Steingestalt. F�gt Geb�uden 20 Schaden hinzu. H�lt 150 Schaden aus. Ist immun gegen Pfeile"),
		TREE("treemonster", TreeMonster.class, "Monstereiche:Langsamer Monster-Baum.F�gt Geb�uden 18 Schaden hinzu. H�lt 100 Schaden aus. Kann nur durch Feuer verletzt werden")
		
		;
		
		private final String image;
		private final Class<?> creatureClass;
		private final String description;
		
		private Monster(String image, Class<?> creatureClass, String desc)
		{
			this.image = image;
			this.creatureClass = creatureClass;
			description = desc;
		}
		
		public String getImage()
		{
			return image;
		}
		
		public String getDescription()
		{
			return description;
		}
		
		public Class<?> getCreatureClass()
		{
			return creatureClass;
		}
	}
	
	public static int wave;
	public static int nextWave; // int seconds
	
	public static final int waveTimer = 60; // in seconds
	
	public static EnumMap<Monster, Integer> monsters = new EnumMap<>(Monster.class);
	
	public static void init()
	{
		wave = 0;
		generateNextWave();
	}
	
	static void generateNextWave()
	{
		Game.world.core.dealDamage(-5, null);
		monsters.clear();
		
		wave++;
		
		/*
		 * either parabola: 0.075 * wave*wave + 3
		 * or line: 2 * wave + 3
		 */
		if (wave < 30) monsters.put(Monster.ZOMBIE, Math.round(2 * wave + 1));
		else monsters.put(Monster.GOLEM, Math.round(wave - 29));
		
		if (wave >= 5) monsters.put(Monster.SKELETON, Math.round(1.25f * wave - 5 - (wave > 30 ? 30 : 0)));
		
		if (wave % 10 == 0 && wave > 2)
		{
			monsters.clear();
			monsters.put(Monster.TROLL, wave / 10);
		}
		
		nextWave = waveTimer;
	}
	
	public static void update()
	{
		if (nextWave <= 0)
		{
			if (monsters.size() > 0)
			{
				new Thread()
				{
					@Override
					public void run()
					{
						EnumMap<Monster, Integer> monsters = WaveManager.monsters.clone();
						WaveManager.monsters.clear();
						int leftLength = 0;
						int rightLength = 0;
						
						int space = Tile.SIZE * 2 - wave;
						space = space < Tile.SIZE ? Tile.SIZE : space;
						
						for (Monster monster : monsters.keySet())
						{
							for (int i = 0; i < monsters.get(monster); i++)
							{
								try
								{
									boolean left = Math.random() < 0.5;
									
									int x = left ? -leftLength * space : Game.world.width + rightLength * space;
									Entity e = (Entity) monster.getCreatureClass().getConstructor(int.class, int.class).newInstance(x, 0);
									int y = Game.world.height / 2 - e.getBump(false).y + e.getBump(false).height;
									e.setY(y);
									
									Game.world.addEntity2(e, false);
									
									if (left) leftLength++;
									else rightLength++;
								}
								catch (Exception e1)
								{
									e1.printStackTrace();
								}
							}
							
							leftLength /= 2;
							rightLength /= 2;
						}
					}
				}.start();
			}
			else
			{
				if (stageClear()) generateNextWave();
			}
		}
	}
	
	public static boolean stageClear()
	{
		for (Entity e : Game.world.entities)
		{
			if (e instanceof Creature && ((Creature) e).isHostile()) return false;
		}
		
		return true;
	}
}
