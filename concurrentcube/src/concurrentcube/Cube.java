package concurrentcube;

import java.util.function.BiConsumer;

/**
 * Main class of the concurrent cube package.
 */
public class Cube
{	
	/**
	 * The size of the cube.
	 */
	private final int size;
	/**
	 * The sides of the cube.
	 */
	private final CubeSide [] sides;
	/**
	 * The two-parameter function to be called directly before a rotation.
	 */
	private final BiConsumer<Integer, Integer> beforeRotation;
	/**
	 * The two-parameter function to be called directly after a rotation.
	 */
	private final BiConsumer<Integer, Integer> afterRotation;
	/**
	 * The function to be called directly before showing.
	 */
	private final Runnable beforeShowing;
	/**
	 * The function to be called directly after showing.
	 */
	private final Runnable afterShowing;
	/**
	 * The monitor which makes sure no processes concurrently perform conflicting actions on the cube.
	 */
	private final CubeMonitor monitor;
	
	/**
	 * The constructor of the cube.
	 * @param size The size of the cube.
	 * @param beforeRotation The BiConsumer to be called directly before a rotation. 
	 * @param afterRotation The BiConsumer to be called directly after a rotation.
	 * @param beforeShowing The Runnable to be called directly before showing.
	 * @param afterShowing The Runnable to be called directly after showing.
	 */
	public Cube(int size,
	            BiConsumer<Integer, Integer> beforeRotation,
	            BiConsumer<Integer, Integer> afterRotation,
	            Runnable beforeShowing,
	            Runnable afterShowing)
	{
		this.size = size;
		this.beforeRotation = beforeRotation;
		this.afterRotation = afterRotation;
		this.beforeShowing = beforeShowing;
		this.afterShowing = afterShowing;
		sides = new CubeSide [6];
		for (int side = 0; side < 6; side++)
			sides[side] = new CubeSide(size, side);
		sides[0].setCubemates(new CubeSide [] {sides[4],  sides[3], sides[2], sides[1]}, new int [] {0, 0, 0, 0}, sides[5]);
		sides[1].setCubemates(new CubeSide [] {sides[0],  sides[2], sides[5], sides[4]}, new int [] {1, 1, 1, 3}, sides[3]);
		sides[2].setCubemates(new CubeSide [] {sides[0],  sides[3], sides[5], sides[1]}, new int [] {2, 1, 0, 3}, sides[4]);
		sides[3].setCubemates(new CubeSide [] {sides[5],  sides[2], sides[0], sides[4]}, new int [] {3, 3, 3, 1}, sides[1]);
		sides[4].setCubemates(new CubeSide [] {sides[0],  sides[1], sides[5], sides[3]}, new int [] {0, 1, 2, 3}, sides[2]);
		sides[5].setCubemates(new CubeSide [] {sides[1],  sides[2], sides[3], sides[4]}, new int [] {2, 2, 2, 2}, sides[0]);
		this.monitor = new CubeMonitor(size);
	}
	
	/**
	 * Gets the direction of a rotation from the given side.
	 * @param side The side
	 * @return The direction
	 */
	private int getDirection(int side)
	{
		int result = -1;
		switch(side)
		{
		case 0:
		case 5:
			result = 0;
			break;
		case 1:
		case 3:
			result = 1;
			break;
		default: // 2, 4
			result = 2;
			break;
		}
		return result;
	}
	
	/**
	 * Gets the plane number when rotating a given layer from the given side, knowing the direction.
	 * @param side The side
	 * @param direction The direction
	 * @param layer The layer
	 * @return The plane
	 */
	private int getPlane(int side, int direction, int layer)
	{
		if (direction == side)
			return layer;
		else
			return size - 1 - layer;
	}
	
	/**
	 * Rotates the <code>layer</code>-th layer from the <code>side</side>'s perspective clockwise.
	 * @param side The side.
	 * @param layer The layer.
	 */
	public void rotate(int side, int layer) throws InterruptedException
	{
		int direction = getDirection(side);
		int plane = getPlane(side, direction, layer);
		
		monitor.enterRotate(direction, plane);
		
		if (beforeRotation != null)
			beforeRotation.accept(side, layer);
		
		sides[side].rotate(layer);
		
		if (afterRotation != null)
			afterRotation.accept(side, layer);
		
		monitor.exitRotate(direction, plane);
	}
	
	/**
	 * Gets a string with the text representation of the cube.
	 * @return The string with the text representation of the cube.
	 */
	public String show() throws InterruptedException
	{
		monitor.enterShow();
		
		if (beforeShowing != null)
			beforeShowing.run();
		
		StringBuilder stringBuilder = new StringBuilder();
		for (int side = 0; side < 6; side++)
			sides[side].appendToStringBuilder(stringBuilder);
		
		if (afterShowing != null)
			afterShowing.run();
		
		monitor.exitShow();
		
		return stringBuilder.toString();
	}
}
