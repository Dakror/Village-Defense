package de.dakror.villagedefense.ui;

import java.awt.Rectangle;

import de.dakror.villagedefense.util.Drawable;
import de.dakror.villagedefense.util.EventListener;

/**
 * @author Dakror
 */
public abstract class Component extends EventListener implements Drawable
{
	protected int x, y, width, height;
	
	public Component(int x, int y, int width, int height)
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	public boolean contains(int x, int y)
	{
		return new Rectangle(this.x, this.y, width, height).contains(x, y);
	}
	
	public int getX()
	{
		return x;
	}
	
	public void setX(int x)
	{
		this.x = x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public void setY(int y)
	{
		this.y = y;
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public void setWidth(int width)
	{
		this.width = width;
	}
	
	public int getHeight()
	{
		return height;
	}
	
	public void setHeight(int height)
	{
		this.height = height;
	}
}