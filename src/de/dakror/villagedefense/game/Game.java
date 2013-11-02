package de.dakror.villagedefense.game;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import de.dakror.villagedefense.game.entity.Entity;
import de.dakror.villagedefense.game.entity.creature.Villager;
import de.dakror.villagedefense.game.entity.struct.Barricade;
import de.dakror.villagedefense.game.entity.struct.House;
import de.dakror.villagedefense.game.entity.struct.Mine;
import de.dakror.villagedefense.game.entity.struct.Struct;
import de.dakror.villagedefense.game.entity.struct.tower.ArrowTower;
import de.dakror.villagedefense.game.world.Tile;
import de.dakror.villagedefense.game.world.World;
import de.dakror.villagedefense.settings.Attributes.Attribute;
import de.dakror.villagedefense.settings.Resources;
import de.dakror.villagedefense.settings.Resources.Resource;
import de.dakror.villagedefense.settings.WaveManager;
import de.dakror.villagedefense.settings.WaveManager.Monster;
import de.dakror.villagedefense.ui.BuildButton;
import de.dakror.villagedefense.ui.Component;
import de.dakror.villagedefense.util.Assistant;
import de.dakror.villagedefense.util.EventListener;
import de.dakror.villagedefense.util.Vector;

/**
 * @author Dakror
 */
public class Game extends EventListener
{
	public static Game currentGame;
	public static JFrame w;
	public static World world;
	public static Struct[] buildableStructs = { new House(0, 0), new Mine(0, 0), new ArrowTower(0, 0), new Barricade(0, 0) };
	
	static HashMap<String, BufferedImage> imageCache = new HashMap<>();
	
	int frames = 0;
	long start = 0;
	
	boolean debug = false;
	
	/**
	 * 0 = playing<br>
	 * 1 = won<br>
	 * 2 = lost<br>
	 * 3 = pause<br>
	 */
	public int state;
	
	public Resources resources;
	public UpdateThread updateThread;
	
	// -- building menu -- //
	public Struct activeStruct;
	public boolean canPlace;
	
	ArrayList<Component> components = new ArrayList<>();
	
	Point mouse;
	Point mouseDown, mouseDownWorld;
	
	public Game()
	{
		currentGame = this;
		
		init();
	}
	
	public void init()
	{
		w = new JFrame("Village Defense");
		w.addKeyListener(this);
		w.addMouseListener(this);
		w.addMouseMotionListener(this);
		w.addMouseWheelListener(this);
		w.setBackground(Color.black);
		w.setForeground(Color.white);
		w.getContentPane().setBackground(Color.black);
		w.setCursor(Toolkit.getDefaultToolkit().createCustomCursor(getImage("cursor.png"), new Point(0, 0), "cursor"));
		
		w.setSize(Toolkit.getDefaultToolkit().getScreenSize());
		// w.setExtendedState(JFrame.MAXIMIZED_BOTH); makes game superlaggy somehow
		w.setMinimumSize(new Dimension(1280, 720));
		w.setUndecorated(true);
		w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		resources = new Resources();
		resources.set(Resource.GOLD, 1000);
		world = new World();
		world.init();
		state = 0;
		initGUI();
		WaveManager.init();
		w.setVisible(true);
		w.getContentPane().setIgnoreRepaint(true);
		try
		{
			w.setFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/MorrisRomanBlack.ttf")));
			w.createBufferStrategy(2);
		}
		catch (Exception e)
		{
			w.dispose();
			init();
			return;
		}
		
		updateThread = new UpdateThread();
	}
	
	public void initGUI()
	{
		for (int i = 0; i < buildableStructs.length; i++)
		{
			int x = getWidth() / 2 + BuildButton.SIZE / 4 - (buildableStructs.length * (BuildButton.SIZE + 32)) / 2 + i * (BuildButton.SIZE + 32);
			BuildButton bb = new BuildButton(x, getHeight() - 84, buildableStructs[i]);
			components.add(bb);
		}
	}
	
	public void draw()
	{
		if (start == 0) start = System.currentTimeMillis();
		if (frames == Integer.MAX_VALUE) frames = 0;
		
		if (!w.isVisible()) return;
		
		BufferStrategy s = w.getBufferStrategy();
		Graphics2D g = null;
		try
		{
			g = (Graphics2D) s.getDrawGraphics();
		}
		catch (Exception e)
		{
			return;
		}
		
		g.translate(w.getInsets().left, w.getInsets().top);
		
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		g.clearRect(0, 0, getWidth(), getHeight());
		
		world.draw(g);
		
		drawGUI(g);
		
		// TODO: DEBUG
		Color oldColor = g.getColor();
		Font oldFont = g.getFont();
		g.setColor(Color.green);
		g.setFont(new Font("Arial", Font.PLAIN, 18));
		g.drawString(Math.round(frames / ((System.currentTimeMillis() - start) / 1000f)) + " FPS", 0, 14);
		g.drawString(Math.round(updateThread.tick / ((System.currentTimeMillis() - start) / 1000f)) + " UPS", 0, 28);
		g.setColor(oldColor);
		g.setFont(oldFont);
		
		drawBuildStruct(g);
		
		drawState(g);
		
		g.dispose();
		
		if (!s.contentsLost()) s.show();
		
		frames++;
	}
	
	public void drawBuildStruct(Graphics2D g)
	{
		if (activeStruct != null)
		{
			activeStruct.setX(Assistant.round(mouse.x - activeStruct.getBump(false).x, Tile.SIZE)/* - activeStruct.getBump(false).x - (activeStruct.getBump(true).width % Tile.SIZE) */);
			activeStruct.setY(Assistant.round(mouse.y - activeStruct.getBump(false).y, Tile.SIZE)/* - activeStruct.getBump(false).y - (activeStruct.getBump(true).height % Tile.SIZE) */);
			activeStruct.setClicked(true);
			
			Rectangle bump = activeStruct.getBump(true);
			int malus = 5;
			
			canPlace = true;
			
			for (int i = bump.x + world.x; i < bump.x + bump.width + world.x; i += Tile.SIZE)
			{
				for (int j = bump.y + world.y; j < bump.y + bump.height + world.y; j += Tile.SIZE)
				{
					boolean blocked = false;
					
					if (activeStruct.canPlaceOnWay())
					{
						blocked = true;
						canPlace = false;
					}
					
					if ((Assistant.round(j, Tile.SIZE) == Assistant.round(getHeight() / 2, Tile.SIZE) || Assistant.round(j, Tile.SIZE) == Assistant.round(getHeight() / 2, Tile.SIZE) - Tile.SIZE))
					{
						blocked = !activeStruct.canPlaceOnWay();
						canPlace = activeStruct.canPlaceOnWay();
					}
					
					for (Entity e : world.entities)
					{
						if (e.getBump(true).intersects(i + 5, j + 5, Tile.SIZE - 10, Tile.SIZE - 10))
						{
							blocked = true;
							canPlace = false;
							break;
						}
					}
					
					
					g.drawImage(getImage(blocked ? "tile/blockedtile.png" : "tile/freetile.png"), Assistant.round(i, Tile.SIZE) - malus, Assistant.round(j, Tile.SIZE) - malus, Tile.SIZE + malus * 2, Tile.SIZE + malus * 2, w);
				}
			}
			
			Composite oldComposite = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
			activeStruct.draw(g);
			g.setComposite(oldComposite);
		}
	}
	
	public void drawGUI(Graphics2D g)
	{
		try
		{
			Assistant.drawContainer(getWidth() / 2 - 175, 70, 350, 60, false, false, g);
			
			// -- wave monster faces -- //
			if (WaveManager.monsters.size() > 0)
			{
				int cSize = 70;
				Monster[] keys = WaveManager.monsters.keySet().toArray(new Monster[] {});
				for (int i = 0; i < WaveManager.monsters.size(); i++)
				{
					Monster m = keys[i];
					
					int x = getWidth() / 2 + 200 + i * cSize;
					Assistant.drawShadow(x, 72, cSize, cSize, g);
					Assistant.drawOutline(x, 72, cSize, cSize, false, g);
					g.drawImage(getImage("creature/" + m.getImage() + "_face.png"), getWidth() / 2 + 200 + i * cSize + (cSize - 48) / 2, 72 + (cSize - 48) / 2, 48, 48, w);
					
					Assistant.drawString(WaveManager.monsters.get(m) + "", x + 6, 72 + cSize - 6, g, 22);
				}
			}
			
			if (world.selectedEntity != null)
			{
				Assistant.drawHorizontallyCenteredString(world.selectedEntity.getName(), getWidth(), 111, g, 40);
				
				if (world.selectedEntity.getAttributes().get(Attribute.HEALTH_MAX) > Attribute.HEALTH_MAX.getDefaultValue())
				{
					Assistant.drawProgressBar(getWidth() / 2 - 179, 111, 358, world.selectedEntity.getAttributes().get(Attribute.HEALTH) / world.selectedEntity.getAttributes().get(Attribute.HEALTH_MAX), "ff3232", g);
					Color oldColor = g.getColor();
					g.setColor(Color.white);
					Assistant.drawHorizontallyCenteredString((int) world.selectedEntity.getAttributes().get(Attribute.HEALTH) + " / " + (int) world.selectedEntity.getAttributes().get(Attribute.HEALTH_MAX), getWidth(), 126, g, 15);
					g.setColor(oldColor);
				}
				
				if (world.selectedEntity.getResources().size() > 0)
				{
					ArrayList<Resource> resources = world.selectedEntity.getResources().getFilled();
					Assistant.drawShadow(0, 80, 160, resources.size() * 24 + 40, g);
					for (int i = 0; i < resources.size(); i++)
					{
						Assistant.drawResource(world.selectedEntity.getResources(), resources.get(i), 16, 100 + i * 24, 26, 30, g);
					}
				}
			}
			
			// -- top bar -- //
			Assistant.drawContainer(0, 0, getWidth(), 80, false, false, g);
			for (int i = 0; i < Resource.values().length; i++)
			{
				int w = (getWidth() / 2 - 100) / 4;
				
				Assistant.drawResource(resources, Resource.values()[i], 25 + i * w, 30, 30, 25, g);
			}
			
			// -- wave info -- //
			Assistant.drawString("Welle: " + WaveManager.wave, getWidth() / 2 + 170, 55, g, 45);
			
			Assistant.drawHorizontallyCenteredString("Punktestand: " + getPlayerScore(), getWidth() / 2, getWidth() / 2, 50, g, 25);
			
			// -- time panel -- //
			Assistant.drawContainer(getWidth() / 2 - 150, 0, 300, 80, true, true, g);
			String minutes = ((int) Math.floor(WaveManager.nextWave / 60f)) + "";
			String seconds = (WaveManager.nextWave % 60) + "";
			
			while (minutes.length() < 2)
				minutes = "0" + minutes;
			while (seconds.length() < 2)
				seconds = "0" + seconds;
			
			Assistant.drawHorizontallyCenteredString(minutes + ":" + seconds, getWidth(), 60, g, 70);
			
			// -- pause -- //
			if (!new Rectangle(getWidth() - 75, 5, 70, 70).contains(mouse)) Assistant.drawContainer(getWidth() - 75, 5, 70, 70, false, false, g);
			else Assistant.drawContainer(getWidth() - 80, 0, 80, 80, false, true, g);
			
			g.drawImage(getImage("gui/pause.png"), getWidth() - 75, 5, 70, 70, w);
			
			// -- build/bottom bar -- //
			Assistant.drawContainer(0, getHeight() - 100, getWidth(), 100, false, false, g);
			
			// -- UI components -- //
			BuildButton hovered = null;
			for (Component c : components)
			{
				c.draw(g);
				if (c instanceof BuildButton && ((BuildButton) c).isHovered()) hovered = (BuildButton) c;
			}
			
			// -- tooltip -- //
			if (hovered != null) hovered.drawTooltip(mouse.x, mouse.y, g);
		}
		catch (Exception e)
		{}
	}
	
	public void drawState(Graphics2D g)
	{
		if (state != 0)
		{
			Composite composite = g.getComposite();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
			g.setColor(Color.darkGray);
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(Color.white);
			g.setComposite(composite);
			
			Assistant.drawHorizontallyCenteredString(state == 1 ? "Gewonnen!" : (state == 2) ? "Niederlage!" : "Spiel pausiert", getWidth(), getHeight() / 2, g, 100);
			Assistant.drawHorizontallyCenteredString(state != 3 ? "Punktestand: " + getPlayerScore() : "Klicken, um fortzusetzen", getWidth(), getHeight() / 2 + 100, g, 60);
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_ESCAPE:
			{
				if (activeStruct != null) activeStruct = null;
				break;
			}
			case KeyEvent.VK_F11:
			{
				w.dispose();
				w.setUndecorated(!w.isUndecorated());
				w.setVisible(true);
				w.createBufferStrategy(2);
				break;
			}
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e)
	{
		if (e.getModifiers() == MouseEvent.BUTTON3_MASK) return;
		
		e.translatePoint(-w.getInsets().left, -w.getInsets().top);
		Vector p = new Vector(e.getPoint()).sub(new Vector(mouseDown));
		
		int x = (int) (mouseDownWorld.x + p.x);
		int y = (int) (mouseDownWorld.y + p.y);
		
		if (world.width > getWidth() && x <= 0 && x + world.width >= getWidth()) world.x = x;
		if (world.height > getHeight() && y <= 0 && y + world.height >= getHeight()) world.y = y;
	}
	
	@Override
	public void mouseMoved(MouseEvent e)
	{
		e.translatePoint(-w.getInsets().left, -w.getInsets().top);
		if (state == 0)
		{
			mouse = e.getPoint();
			world.mouseMoved(e);
			for (Component c : components)
				c.mouseMoved(e);
		}
	}
	
	@Override
	public void mouseReleased(MouseEvent e)
	{
		mouseDown = null;
		mouseDownWorld = null;
	}
	
	@Override
	public void mousePressed(MouseEvent e)
	{
		e.translatePoint(-w.getInsets().left, -w.getInsets().top);
		
		mouseDown = e.getPoint();
		mouseDownWorld = new Point(world.x, world.y);
		
		if (state == 0)
		{
			if (new Rectangle(getWidth() - 75, 5, 70, 70).contains(e.getPoint())) // pause
			{
				state = 3;
				return;
			}
			
			if (activeStruct != null && e.getButton() == 1)
			{
				if (canPlace && e.getY() < getHeight() - 100)
				{
					activeStruct.setClicked(false);
					ArrayList<Resource> filled = activeStruct.getBuildingCosts().getFilled();
					for (Resource r : filled)
					{
						if (!r.isUsable()) continue;
						resources.add(r, -activeStruct.getBuildingCosts().get(r));
					}
					world.addEntity(activeStruct);
					
					for (Component c : components)
					{
						if (c instanceof BuildButton)
						{
							BuildButton b = (BuildButton) c;
							if (b.getStruct().getName().equals(activeStruct.getName()))
							{
								c.update(0);
								if (b.isEnabled())
								{
									activeStruct.setClicked(false);
									activeStruct = (Struct) b.getStruct().clone();
									
									return;
								}
							}
						}
					}
				}
				else return;
			}
			
			activeStruct = null;
			
			world.mousePressed(e);
			for (Component c : components)
				c.mousePressed(e);
		}
		else if (state == 3) state = 0;
	}
	
	public void setState(int state)
	{
		this.state = state;
	}
	
	public int getPeople()
	{
		int people = 0;
		
		for (Entity e : world.entities)
		{
			if (e instanceof Villager && e.alpha > 0)
			{
				Villager v = (Villager) e;
				if (v.getTargetEntity() != null && v.getTargetEntity() instanceof Struct)
				{
					Struct s = (Struct) v.getTargetEntity();
					if (s.getBuildingCosts().get(Resource.PEOPLE) > 0) continue;
				}
				
				people++;
			}
		}
		
		return people;
	}
	
	public int getPlayerScore()
	{
		int score = 0;
		
		for (Resource r : resources.getFilled())
			score += resources.get(r);
		
		for (Entity e : world.entities)
		{
			if (e instanceof Struct && e.getResources().size() == 0) score += 50;
		}
		
		score += WaveManager.wave * 250;
		
		score -= (world.core.getAttributes().get(Attribute.HEALTH_MAX) - world.core.getAttributes().get(Attribute.HEALTH)) * 10;
		
		return score - 1551;
	}
	
	public static int getWidth()
	{
		return w.getWidth() - (w.getInsets().left + w.getInsets().right);
	}
	
	public static int getHeight()
	{
		return w.getHeight() - (w.getInsets().top + w.getInsets().bottom);
	}
	
	public static BufferedImage getImage(String p)
	{
		if (imageCache.containsKey(p)) return imageCache.get(p);
		else
		{
			try
			{
				BufferedImage i = ImageIO.read(Game.class.getResource("/img/" + p));
				imageCache.put(p, i);
				
				return i;
			}
			catch (Exception e)
			{
				System.err.println("Missing image: " + p);
				return null;
			}
		}
	}
}
