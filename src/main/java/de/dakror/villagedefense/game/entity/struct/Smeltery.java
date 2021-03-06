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

package de.dakror.villagedefense.game.entity.struct;

import java.awt.geom.Rectangle2D;

import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.settings.Attributes.Attribute;
import de.dakror.villagedefense.settings.Researches;
import de.dakror.villagedefense.settings.Resources;
import de.dakror.villagedefense.settings.Resources.Resource;

/**
 * @author Dakror
 */
public class Smeltery extends Struct {
    public Smeltery(int x, int y) {
        super(x, y, 7, 7);
        tx = 0;
        ty = 16;
        name = "Smelter";
        placeGround = true;
        setBump(new Rectangle2D.Float(0, 2, 6, 4.7f));

        attributes.set(Attribute.MINE_SPEED, 300);
        attributes.set(Attribute.MINE_AMOUNT, 2); // use 3 ore + 4 coal -> 2 ingot

        buildingCosts.set(Resource.GOLD, 450);
        buildingCosts.set(Resource.WOOD, 200);
        buildingCosts.set(Resource.STONE, 275);
        buildingCosts.set(Resource.COAL, 25);
        buildingCosts.set(Resource.PEOPLE, 2);

        description = "smelts metal ore into ingots using coal.";
        canHunger = true;
    }

    @Override
    protected void tick(int tick) {
        super.tick(tick);

        if (tick % attributes.get(Attribute.MINE_SPEED) == 0 && Game.currentGame.resources.get(Resource.IRONORE) >= 3 && Game.currentGame.resources.get(Resource.COAL) >= 4 && working) {
            Game.currentGame.resources.add(Resource.IRONORE, -3);
            Game.currentGame.resources.add(Resource.COAL, -4);
            resources.add(Resource.IRONINGOT, (int) attributes.get(Attribute.MINE_AMOUNT));
        }
    }

    @Override
    public Resources getResourcesPerSecond() {
        Resources res = new Resources();

        if (!working) return res;

        res.set(Resource.IRONORE, Game.currentGame.getUPS2() / attributes.get(Attribute.MINE_SPEED) * -3);
        res.set(Resource.COAL, Game.currentGame.getUPS2() / attributes.get(Attribute.MINE_SPEED) * -4);
        res.set(Resource.IRONINGOT, Game.currentGame.getUPS2() / attributes.get(Attribute.MINE_SPEED) * attributes.get(Attribute.MINE_AMOUNT));

        return res;
    }

    @Override
    public void initGUI() {}

    @Override
    protected void onMinedUp() {}

    @Override
    public void onUpgrade(Researches research, boolean inititial) {}

    @Override
    public Entity clone() {
        return new Smeltery((int) x / Tile.SIZE, (int) y / Tile.SIZE);
    }

    @Override
    protected void onDeath() {}

}
