/*******************************************************************************
 * Copyright 2015 Maximilian Stark | Dakror <mail@dakror.de>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/


package de.dakror.villagedefense.layer;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

import de.dakror.gamesetup.layer.Layer;
import de.dakror.gamesetup.ui.Component;
import de.dakror.gamesetup.util.Helper;
import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.entity.struct.Catapult;
import de.dakror.villagedefense.game.entity.struct.Struct;
import de.dakror.villagedefense.game.entity.struct.Way;
import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.settings.Resources.Resource;
import de.dakror.villagedefense.ui.BuildBar;
import de.dakror.villagedefense.ui.button.BuildButton;

/**
 * @author Dakror
 */
public class BuildStructLayer extends Layer {
	public boolean canPlace;
	Point drag;
	
	@Override
	public void draw(Graphics2D g) {
		try {
			if (Game.currentGame.activeStruct != null) {
				Game.currentGame.activeStruct.setX(Helper.round((drag == null ? Game.currentGame.mouse.x - Tile.SIZE / 2 : drag.x - Tile.SIZE / 2) - Game.currentGame.activeStruct.getBump(false).x, Tile.SIZE) + (Game.world.x % Tile.SIZE));
				Game.currentGame.activeStruct.setY(Helper.round((drag == null ? Game.currentGame.mouse.y - Tile.SIZE / 2 * 3 : drag.y - Tile.SIZE / 2 * 3) - Game.currentGame.activeStruct.getBump(false).y, Tile.SIZE) + (Game.world.y % Tile.SIZE));
				Game.currentGame.activeStruct.setClicked(true);
				
				Rectangle bump = Game.currentGame.activeStruct.getBump(true);
				bump.translate(-Game.world.x % Tile.SIZE, -Game.world.y % Tile.SIZE);
				int malus = 5;
				
				canPlace = true;
				
				int centerY = Helper.round(Math.round(Game.world.height / 2f), Tile.SIZE);
				
				for (int i = Helper.round(bump.x, Tile.SIZE) + Game.world.x % Tile.SIZE; i < bump.x + bump.width + Game.world.x % Tile.SIZE; i += Tile.SIZE) {
					for (int j = Helper.round(bump.y, Tile.SIZE) + Game.world.y % Tile.SIZE; j < bump.y + bump.height + Game.world.y % Tile.SIZE; j += Tile.SIZE) {
						boolean blocked = false;
						
						if (Game.currentGame.activeStruct.canPlaceOnWay()) {
							blocked = true;
						}
						
						if (Game.currentGame.activeStruct instanceof Way) {
							if (Game.world.getTileId((int) Math.floor((i - Game.world.x) / Tile.SIZE), (int) Math.floor((j - Game.world.y) / Tile.SIZE)) != Tile.grass.getId()) {
								blocked = true;
							}
						}
						
						if (j == centerY + Tile.SIZE + Game.world.y || j == centerY + Game.world.y) {
							blocked = !Game.currentGame.activeStruct.canPlaceOnWay();
						}
						
						for (Entity e : Game.world.entities) {
							if (e.getBump(true).intersects(i - Game.world.x, j - Game.world.y, Tile.SIZE, Tile.SIZE)) {
								blocked = true;
								break;
							}
						}
						if (blocked) canPlace = false;
						
						g.drawImage(Game.getImage(blocked ? "tile/blockedtile.png" : "tile/freetile.png"), i - malus, j - malus, Tile.SIZE + malus * 2, Tile.SIZE + malus * 2, Game.w);
					}
				}
				
				Composite oldComposite = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
				if (Game.currentGame.activeStruct != null) Game.currentGame.activeStruct.draw(g);
				g.setComposite(oldComposite);
			}
		} catch (NullPointerException e) {}
	}
	
	@Override
	public void update(int tick) {}
	
	@Override
	public void init() {}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		super.mouseReleased(e);
		drag = null;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		if (Game.currentGame.activeStruct != null && e.getButton() == 1) {
			build(e);
		} else Game.currentGame.activeStruct = null;
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		if (Game.currentGame.activeStruct != null && e.getModifiers() == MouseEvent.BUTTON1_MASK) {
			Game.currentGame.activeStruct.setX(Helper.round(e.getX() - Tile.SIZE / 2 - Game.currentGame.activeStruct.getBump(false).x, Tile.SIZE) + (Game.world.x % Tile.SIZE));
			Game.currentGame.activeStruct.setY(Helper.round(e.getY() - Tile.SIZE / 2 * 3 - Game.currentGame.activeStruct.getBump(false).y, Tile.SIZE) + (Game.world.y % Tile.SIZE));
			drag = e.getPoint();
			
			build(e);
		}
	}
	
	public void build(MouseEvent e) {
		if (Game.currentGame.state != 0) {
			Game.currentGame.activeStruct = null;
			return;
		}
		
		if (canPlace && e.getY() > 80 && e.getY() < Game.getHeight() - 100) {
			Game.currentGame.activeStruct.setClicked(false);
			ArrayList<Resource> filled = Game.currentGame.activeStruct.getBuildingCosts().getFilled();
			for (Resource r : filled) {
				if (!r.isUsable()) continue;
				Game.currentGame.resources.add(r, -Game.currentGame.activeStruct.getBuildingCosts().get(r));
			}
			
			Game.currentGame.activeStruct.translate(-Game.world.x, -Game.world.y);
			
			Game.world.addEntity2(Game.currentGame.activeStruct.clone(), false);
			// Game.currentGame.placedStruct = true;
			
			for (Component c : ((BuildBar) HUDLayer.currentHudLayer.components.get(0)).buttons) {
				if (c instanceof BuildButton) {
					BuildButton b = (BuildButton) c;
					if (b.getStruct().getName().equals(Game.currentGame.activeStruct.getName())) {
						c.update(0);
						if (b.enabled) {
							Game.currentGame.activeStruct.setClicked(false);
							Game.currentGame.activeStruct = (Struct) b.getStruct().clone();
							
							return;
						}
					}
				}
			}
			
			Game.currentGame.activeStruct = null;
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		super.keyReleased(e);
		
		switch (e.getKeyCode()) {
			case KeyEvent.VK_ESCAPE: {
				Game.currentGame.activeStruct = null;
				break;
			}
		}
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		super.mouseWheelMoved(e);
		if (Game.currentGame.activeStruct instanceof Catapult) {
			Catapult c = (Catapult) Game.currentGame.activeStruct;
			c.setDownwards(e.getWheelRotation() == 1);
		}
	}
}
