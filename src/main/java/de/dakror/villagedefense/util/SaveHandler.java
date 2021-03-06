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

package de.dakror.villagedefense.util;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JOptionPane;

import org.json.JSONArray;
import org.json.JSONObject;

import de.dakror.gamesetup.util.Helper;
import de.dakror.villagedefense.game.Game;
import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.entity.creature.Creature;
import de.dakror.villagedefense.game.entity.creature.Forester;
import de.dakror.villagedefense.game.entity.creature.Woodsman;
import de.dakror.villagedefense.game.entity.struct.CoreHouse;
import de.dakror.villagedefense.game.entity.struct.Struct;
import de.dakror.villagedefense.game.world.Chunk;
import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.settings.Attributes;
import de.dakror.villagedefense.settings.CFG;
import de.dakror.villagedefense.settings.Researches;
import de.dakror.villagedefense.settings.Resources;
import de.dakror.villagedefense.settings.WaveManager;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * @author Dakror
 */
public class SaveHandler {
    public static void saveGame() {
        try {
            File save = new File(CFG.DIR, "saves/" + new SimpleDateFormat("'Savegame' dd.MM.yyyy HH-mm-ss").format(new Date()) + ".save");
            save.createNewFile();

            JSONObject o = new JSONObject();

            o.put("version", CFG.VERSION);
            o.put("created", Game.currentGame.worldCreated);
            o.put("width", Game.world.width);
            o.put("height", Game.world.height);
            o.put("tile", new BASE64Encoder().encode(Compressor.compressRow(Game.world.getData())));
            o.put("resources", Game.currentGame.resources.getData());
            o.put("researches", Game.currentGame.researches);
            o.put("wave", WaveManager.wave);
            o.put("time", WaveManager.nextWave);

            JSONArray entities = new JSONArray();
            for (Entity e : Game.world.entities) {
                if ((e instanceof Forester) || (e instanceof Woodsman)) continue; // don't save them, because they get spawned by the house upgrades

                entities.put(e.getData());
            }
            o.put("entities", entities);

            Compressor.compressFile(save, o.toString());
            //			Helper.setFileContent(new File(save.getPath() + ".debug"), o.toString());
            Game.currentGame.state = 3;
            JOptionPane.showMessageDialog(Game.w, "Saved successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadSave(File f) {
        try {
            JSONObject o = new JSONObject(Compressor.decompressFile(f));
            Game.world.init(o.getInt("width"), o.getInt("height"));
            Game.world.setData((int) Math.ceil(o.getInt("width") / (float) (Chunk.SIZE * Tile.SIZE)), (int) Math.ceil(o.getInt("height") / (float) (Chunk.SIZE * Tile.SIZE)), Compressor.decompressRow(new BASE64Decoder().decodeBuffer(o.getString("tile"))));
            Game.currentGame.resources = new Resources(o.getJSONObject("resources"));

            if (o.has("created")) Game.currentGame.worldCreated = o.getInt("created");

            JSONArray researches = o.getJSONArray("researches");
            Game.currentGame.researches = new ArrayList<>();
            for (int i = 0; i < researches.length(); i++)
                Game.currentGame.researches.add(Researches.valueOf(researches.getString(i)));

            WaveManager.wave = o.getInt("wave") - 1;
            WaveManager.nextWave = o.getInt("time");

            WaveManager.init();

            JSONArray entities = o.getJSONArray("entities");

            HashMap<Integer, Creature> creaturesWithCustomData = new HashMap<>();
            for (int i = 0; i < entities.length(); i++) {
                JSONObject e = entities.getJSONObject(i);
                Entity entity = (Entity) Class.forName(e.getString("class")).getConstructor(int.class, int.class).newInstance(e.getInt("x"), e.getInt("y"));
                entity.setAttributes(new Attributes(e.getJSONObject("attributes")));
                entity.setResources(new Resources(e.getJSONObject("resources")));

                if (entity instanceof Creature) {
                    Creature c = (Creature) entity;
                    c.alpha = (float) e.getDouble("alpha");
                    c.setSpawnPoint(new Point(e.getInt("spawnX"), e.getInt("spawnY")));

                    if (!e.isNull("targetX") || !e.isNull("targetEntity") || !e.isNull("origin")) {
                        creaturesWithCustomData.put(i, c);
                        continue;
                    }
                } else if (entity instanceof Struct) {
                    JSONArray researches2 = e.getJSONArray("researches");

                    ((Struct) entity).clearResearches();
                    for (int j = 0; j < researches2.length(); j++)
                        ((Struct) entity).add(Researches.valueOf(researches2.getString(j)));

                    ((Struct) entity).tx = e.getInt("tx");
                    ((Struct) entity).ty = e.getInt("ty");
                }

                if (entity instanceof CoreHouse) {
                    Game.world.core = (Struct) entity;
                }

                Game.world.addEntity2(entity, true);
            }

            // -- set creatures' custom data
            for (Iterator<Integer> iterator = creaturesWithCustomData.keySet().iterator(); iterator.hasNext();) {
                int index = iterator.next();
                JSONObject e = entities.getJSONObject(index);

                Entity entity = creaturesWithCustomData.get(index);

                if (!e.isNull("targetEntity")) {
                    JSONObject tE = e.getJSONObject("targetEntity");
                    for (Entity e1 : Game.world.entities) {
                        int x = (int) (e1 instanceof Creature ? e1.getX() : e1.getX() / Tile.SIZE);
                        int y = (int) (e1 instanceof Creature ? e1.getY() : e1.getY() / Tile.SIZE);
                        if (e1.getClass().getName().equals(tE.getString("class")) && tE.getInt("x") == x && tE.getInt("y") == y) {
                            ((Creature) entity).setTarget(e1, false);
                            continue;
                        }
                    }
                }
                if (!e.isNull("targetX")) {
                    ((Creature) entity).setTarget(e.getInt("targetX"), e.getInt("targetY"), false);
                }
                if (!e.isNull("origin")) {
                    JSONObject tE = e.getJSONObject("origin");
                    for (Entity e1 : Game.world.entities) {
                        int x = (int) (e1 instanceof Creature ? e1.getX() : e1.getX() / Tile.SIZE);
                        int y = (int) (e1 instanceof Creature ? e1.getY() : e1.getY() / Tile.SIZE);
                        if (e1.getClass().getName().equals(tE.getString("class")) && tE.getInt("x") == x && tE.getInt("y") == y) {
                            ((Creature) entity).setOrigin(e1);
                            continue;
                        }
                    }
                }

                Game.world.addEntity2(entity, true);
            }

            Game.currentGame.state = 3;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File[] getSaves() {
        return new File(CFG.DIR, "saves").listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".save");
            }
        });
    }

    public static boolean isWorldScorePosted(int worldCreated) throws Exception {
        File f = new File(CFG.DIR, "scores");
        try {
            if (!f.exists()) return false;

            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = "";
            while ((line = br.readLine()) != null) {
                if (Integer.parseInt(line) == worldCreated) {
                    br.close();
                    return true;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addWorldScorePosted(int worldCreated) {
        File f = new File(CFG.DIR, "scores");
        try {
            f.createNewFile();
            Helper.setFileContent(f, Helper.getFileContent(f) + worldCreated + "\r\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendScore() {
        try {
            if (isWorldScorePosted(Game.currentGame.worldCreated)) {
                JOptionPane.showMessageDialog(null, "You already placed your score on the leaderboard for this world!", "Already placed!", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String response = Helper.getURLContent(new URL("https://dakror.de/villagedefense/api/scores.php?USERNAME=" + urlencode(CFG.USERNAME) + "&SCORE=" + Game.currentGame.getPlayerScore()));
            System.out.println(response);
            if (!response.equals("false")) {
                JOptionPane.showMessageDialog(null, "Your score has been placed on the leaderboard successfully.", "Placement successful!", JOptionPane.INFORMATION_MESSAGE);
                addWorldScorePosted(Game.currentGame.worldCreated);
                Game.currentGame.scoreSent = true;
            } else JOptionPane.showMessageDialog(null, "Your score could not be placed on the leaderboard!", "Placement failed!", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "Your score could not be placed on the leaderboard!\nMaybe you're not connected to the internet.", "Placement failed!", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static String urlencode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
