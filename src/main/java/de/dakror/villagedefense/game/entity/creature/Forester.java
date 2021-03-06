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

package de.dakror.villagedefense.game.entity.creature;

import de.dakror.gamesetup.util.Helper;
import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.entity.struct.Tree;
import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.settings.Attributes.Attribute;
import de.dakror.villagedefense.settings.Resources.Resource;
import de.dakror.villagedefense.util.Vector;

/**
 * @author Dakror
 */
public class Forester extends Creature {
    public Forester(int x, int y) {
        super(x, y, "forester");
        setHostile(false);
        name = "Ranger";

        attributes.set(Attribute.ATTACK_SPEED, 30 * 45); // 45 second plant cooldown
        attributes.set(Attribute.ATTACK_RANGE, 6 * Tile.SIZE); // 6 fields plant radius

        description = "Plants tree saplings around his hut.";
        canHunger = true;
    }

    @Override
    public void onSpawn(boolean initial) {
        Game.currentGame.resources.add(Resource.PEOPLE, 1);
    }

    @Override
    public void onDeath() {
        super.onDeath();
        Game.currentGame.resources.add(Resource.PEOPLE, -1);
    }

    @Override
    public void tick(int tick) {
        super.tick(tick);

        if (target == null && path == null) {
            if ((tick + randomOffset) % attributes.get(Attribute.ATTACK_SPEED) == 0) {
                if (!getPos().equals(new Vector(spawnPoint))) Game.world.addEntity2(new Tree(Helper.round(Math.round(getPos().x), Tile.SIZE) / Tile.SIZE, Helper.round(Math.round(getPos().y), Tile.SIZE) / Tile.SIZE, true), false);
                setTarget(lookupPlantTarget(), false);
            }
        }
    }

    public Vector lookupPlantTarget() {
        float rad = (float) Math.toRadians(Math.random() * 360);
        float hyp = (float) Math.random() * attributes.get(Attribute.ATTACK_RANGE);

        int x = Helper.round(Math.round(spawnPoint.x + (float) Math.cos(rad) * hyp), Tile.SIZE);
        int y = Helper.round(Math.round(spawnPoint.y + (float) Math.sin(rad) * hyp), Tile.SIZE);
        int my = Helper.round(Game.world.height / 2, Tile.SIZE);

        if (x < 0 || y < 100 || x >= Game.world.width || y >= Game.world.height - 120 || y == my || y == my + Tile.SIZE) return lookupPlantTarget();

        if (Game.world.isFreeTile(x, y)) return new Vector(x, y);
        else return lookupPlantTarget();
    }

    @Override
    protected boolean onArrivalAtEntity(int tick) {
        return false;
    }

    @Override
    public Entity clone() {
        return new Forester((int) x, (int) y);
    }
}
