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

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.entity.struct.Barricade;
import de.dakror.villagedefense.game.entity.struct.Struct;
import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.settings.Attributes.Attribute;
import de.dakror.villagedefense.util.Vector;
import de.dakror.villagedefense.util.path.AStar;
import de.dakror.villagedefense.util.path.Path;

/**
 * @author Dakror
 */
public abstract class Creature extends Entity {
    protected Image image;
    protected Vector target;
    protected Entity targetEntity;
    protected boolean targetByUser;
    protected boolean frozen;
    private boolean hostile;
    /**
     * 0 = down<br>
     * 1 = left<br>
     * 2 = right<br>
     * 3 = up
     */
    protected int dir;
    protected int frame;
    protected Point spawnPoint;
    public Path path;
    protected Entity origin;

    public Creature(int x, int y, String img) {
        super(x, y, Game.getImage("creature/" + img + ".png").getWidth() / 4, Game.getImage("creature/" + img + ".png").getHeight() / 4);

        spawnPoint = new Point(x, y);

        image = Game.getImage("creature/" + img + ".png");

        setBump(new Rectangle((int) (width * 0.25f), (int) (height * 0.75f), (int) (width * 0.5f), (int) (height * 0.25f)));
        frozen = false;

        dir = 0;
        frame = 0;
    }

    public Entity setOrigin(Entity e) {
        origin = e;
        return this;
    }

    public Entity getOrigin() {
        return origin;
    }

    @Override
    public void draw(Graphics2D g) {
        drawBump(g, false);

        g.drawImage(image, (int) x, (int) y, (int) x + width, (int) y + height, frame * width, dir * height, frame * width + width, dir * height + height, Game.w);

        drawBump(g, true);
    }

    @Override
    public void tick(int tick) {
        if (targetEntity != null && !Game.world.entities.contains(targetEntity)) {
            frame = 0;
            alpha = 1;
            targetEntity = null;
        }

        if (path != null) {
            if (path.isPathComplete()) path = null;
            else {
                if (target == null) {
                    path.setNodeReached();

                    if (path.isPathComplete()) {
                        path = null;
                        target = null;
                    } else target = path.getNode().clone();
                } else if (target.x == -1337) // custom trigger
                {
                    target = path.getNode().clone();
                }
            }
        }

        move(tick);

        // -- attacks -- //
        if (targetEntity != null && target == null && (path == null || path.isPathComplete())) {
            if (hostile) {
                if ((tick + randomOffset) % attributes.get(Attribute.ATTACK_SPEED) == 0) {
                    if (frame % 2 == 0) targetEntity.dealDamage((int) (targetEntity instanceof Struct ? attributes.get(Attribute.DAMAGE_STRUCT) : attributes.get(Attribute.DAMAGE_CREATURE)), this);
                    frame++;
                }
            } else if (!onArrivalAtEntity(tick)) frame = 0;
        }

        if (targetEntity == null && target == null) // killed everything
        {
            lookupTargetEntity();
        }

        frame = frame % 4;
    }

    public void move(int tick) {
        if (!frozen && attributes.get(Attribute.SPEED) > 0 && target != null) {
            Vector dif = getVelocityVector();
            if (dif.getLength() > 0) {

                float angle = Math.round(dif.getAngleOnXAxis());
                if (angle <= 135 && angle >= 45) dir = 0;
                else if (angle <= 45 && angle >= -45) dir = 2;
                else if (angle <= -45 && angle >= -135) dir = 3;
                else dir = 1;
            }
            if ((tick + randomOffset) % 10 == 0) frame++;

            setPos(getPos().add(dif));
        }
    }

    public Vector getVelocityVector() {
        try {
            Vector pos = getPos();

            Vector dif = target.clone().sub(pos);

            if (dif.getLength() < attributes.get(Attribute.SPEED)) {
                target = null;
                frame = 0;
            } else dif.setLength(attributes.get(Attribute.SPEED));

            return dif;
        } catch (NullPointerException e) {
            return new Vector(0, 0);
        }
    }

    public void setTarget(int x, int y, boolean user) {
        setTarget(new Vector(x, y), user);
    }

    public void setTarget(Vector target, boolean user) {
        targetByUser = user;
        if (!hostile) {
            path = AStar.getPath(getTile(), Game.world.getTile(target));

            if (path == null) return;

            this.target = new Vector(-1337, 0);
            path.mul(Tile.SIZE);
            path.translate(0, -bump.y + bump.height);
        } else this.target = target;
    }

    public void setTarget(Entity entity, boolean user) {
        targetEntity = null;
        if (frozen || attributes.get(Attribute.SPEED) == 0) return;

        if (hostile) targetEntity = entity;
        targetByUser = user;

        if (entity instanceof Creature) {
            // TODO: target on creatures
            // Creature c = (Creature) entity;
            // if (hostile != c.isHostile())
            // {
            //
            // }
        } else if (entity instanceof Struct) {
            Struct s = (Struct) entity;

            targetEntity = entity;

            setTarget(getTargetForStruct(s), user);
        }
    }

    public Vector getTargetForStruct(Struct s) {
        Vector nearestPoint = null;

        ArrayList<Vector> points = hostile ? s.getStructPoints().attacks : s.getStructPoints().entries;

        if (points.size() == 0) points = s.getSurroundingTiles(false);

        Vector pos = getPos();
        for (Vector p : points) {
            Vector v = p.clone();
            v.mul(Tile.SIZE);
            v.add(s.getPos());
            if (nearestPoint == null || v.getDistance(pos) < nearestPoint.getDistance(pos)) nearestPoint = v;
        }

        nearestPoint.setLength(nearestPoint.getLength() - attributes.get(Attribute.ATTACK_RANGE));

        if (hostile) nearestPoint.y -= height * 0.6f;

        return nearestPoint;
    }

    public Vector getTarget() {
        return target;
    }

    public Vector getTarget2() {
        return target == null ? getPos() : target;
    }

    public void setSpawnPoint(Point point) {
        spawnPoint = point;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isHostile() {
        return hostile;
    }

    public void setTargetEntity(Entity target) {
        targetEntity = target;
    }

    public Entity getTargetEntity() {
        return targetEntity;
    }

    public void setHostile(boolean hostile) {
        this.hostile = hostile;
        lookupTargetEntity();
    }

    public void lookupTargetEntity() {
        if (!hostile) return;

        Barricade closestBarricade = null;

        for (Entity e : Game.world.entities) {
            if (e instanceof Barricade) {
                boolean nearer = (closestBarricade == null) ? true : e.getPos().getDistance(getPos()) < closestBarricade.getPos().getDistance(getPos());

                boolean inDirection = (closestBarricade == null) ? true : spawnPoint.x < Game.world.width / 2 ? /* left side */e.getX() > x : /* right side */e.getX() < x;

                if (closestBarricade == null || (nearer && inDirection)) {
                    closestBarricade = (Barricade) e;
                }
            }
        }

        if (closestBarricade == null || closestBarricade.getPos().getDistance(getPos()) > Game.world.core.getPos().getDistance(getPos()))
            setTarget(Game.world.core, false);
        else
            setTarget(closestBarricade, false);
    }

    @Override
    public void onDeath() {
        if (hostile) Game.currentGame.resources.add(resources);
        dead = true;
    }

    @Override
    public void onSpawn(boolean initial) {}

    public boolean isTargetByUser() {
        return targetByUser;
    }

    @Override
    public JSONObject getData() {
        JSONObject o = new JSONObject();
        try {
            o.put("x", x);
            o.put("y", y);
            o.put("spawnX", spawnPoint.x);
            o.put("spawnY", spawnPoint.y);
            o.put("targetX", target != null ? target.x : JSONObject.NULL);
            o.put("targetY", target != null ? target.y : JSONObject.NULL);
            o.put("targetEntity", targetEntity != null ? targetEntity.getData() : JSONObject.NULL);
            o.put("origin", origin != null ? origin.getData() : JSONObject.NULL);
            o.put("class", getClass().getName());
            o.put("attributes", attributes.getData());
            o.put("resources", resources.getData());
            o.put("alpha", alpha);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return o;
    }

    protected abstract boolean onArrivalAtEntity(int tick);
}
