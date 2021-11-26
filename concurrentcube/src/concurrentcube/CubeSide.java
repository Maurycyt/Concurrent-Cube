package concurrentcube;

/**
 * Class representing the side of a Cube.
 * It consists of size * size fields of type Colour,
 * and contains references to its neighbouring sides.
 */
public class CubeSide
{
	/**
	 * The size of the cube.
	 */
	private final int size;
	/**
	 * Describes the number of times the side was rotated clockwise,
	 * relative to its beginning position, modulo 4.
	 */
	private int rotation;
	/**
	 * The fields of the side.
	 */
	private final Colour [][] colours;
	/**
	 * References to the neighbouring sides in clockwise order.
	 */
	private final CubeSide [] neighbours;
	/**
	 * The rotations of the beginning positions of the neighbouring
	 * sides, relative to the rotation in which their uppermost
	 * rows runs along the border between it and this side.
	 */
	private final int [] relativeRotations;
	/**
	 * The cube side opposite to this one.
	 */
	private CubeSide opposite;
	
	/**
	 * The constructor of the CubeSide.
	 * Does not fill out <code>neighbours</code> and <code>relativeRotations</code> fields,
	 * because these may not yet be known or exist. 
	 * @param size The size of the cube side.
	 * @param colour The beginning colour of the cube side.
	 */
	public CubeSide(int size, int colour)
	{
		this.size = size;
		this.rotation = 0;
		this.colours = new Colour[size][size];
		for (int row = 0; row < size; row++)
			for (int column = 0; column < size; column++)
				colours[row][column] = new Colour(colour);
		this.neighbours = new CubeSide[4];
		this.relativeRotations = new int [4];
	}
	
	/**
	 * Sets references to and important information about the neighbouring and opposite sides.
	 * @param neighbours The references to the neighbouring sides in clockwise order.
	 * @param relativeRotations The rotations of the beginning positions of the
	 * neighbouring sides relative to the rotation in which their uppermost row
	 * runs along the border between it and this side.
	 * @param opposite The reference to the opposite side.
	 */
	public void setCubemates(CubeSide [] neighbours, int [] relativeRotations, CubeSide opposite)
	{
		for (int i = 0; i < 4; i++)
		{
			this.neighbours[i] = neighbours[i];
			this.relativeRotations[i] = relativeRotations[i];
		}
		this.opposite = opposite;
	}
	
	/**
	 * Gets the reference to the Colour at the given coordinates on the side
	 * if the side were to be rotated by another <code>rotationModifier</code> rotations.
	 * @param row The row coordinate.
	 * @param column The column coordinate.
	 * @param rotationModifier The extra rotation to be taken into account.
	 * @return The requested reference to the Colour object.
	 */
	private Colour getColour(int row, int column, int rotationModifier)
	{
		Colour result = null;
		int simulatedRotation = (rotation + rotationModifier + 4) % 4;
		switch(simulatedRotation)
		{
		case 0:
			result = colours[row][column];
			break;
		case 1:
			result = colours[size - 1 - column][row];
			break;
		case 2:
			result = colours[size - 1 - row][size - 1 - column];
			break;
		default: // 3
			result = colours[column][size - 1 - row];
			break;
		}
		return result;
	}
	
	/**
	 * Gets and array of references to the Colours in a given row
	 * if the side were to be rotated by another <code>rotationModifier</code> rotations.
	 * This is (should be) more optimal than using getColour multiple times.
	 * @param row The row 
	 * @param rotationModifier The extra rotation to be taken into account.
	 * @return The requested references to the Colour objects.
	 */
	private Colour [] getColourRow(int row, int rotationModifier)
	{
		Colour [] result = new Colour[size];
		int simulatedRotation = (rotation + rotationModifier + 4) % 4;
		switch(simulatedRotation)
		{
		case 0:
			for (int column = 0; column < size; column++)
				result[column] = colours[row][column];
			break;
		case 1:
			for (int column = 0; column < size; column++)
				result[column] = colours[size - 1 - column][row];
			break;
		case 2:
			for (int column = 0; column < size; column++)
				result[column] = colours[size - 1 - row][size - 1 - column];
			break;
		default: // 3
			for (int column = 0; column < size; column++)
				result[column] = colours[column][size - 1 - row];
			break;
		}
		return result;
	}
	
	/**
	 * Appends the text representation of the cube side to the provided StringBuilder.
	 * @param stringBuilder The StringBuilder object to which to append.
	 */
	public void appendToStringBuilder(StringBuilder stringBuilder)
	{
		for (int row = 0; row < size; row++)
			for (int column = 0; column < size; column++)
				stringBuilder.append(getColour(row, column, 0));
	}
	
	/**
	 * Gets the array of Colour objects on the <code>neighbour</code> side
	 * considered in the rotation of the <code>layer</code>-th layer looking from this side.   
	 * @param neighbour The number of the neighbouring side (relative to this side).
	 * @param layer The layer of the rotation.
	 * @param index The index of the colours which are being swapped.
	 * @return The appropriate Colour object.
	 */
	private Colour [] getNeighbourRotationColours(int neighbour, int layer)
	{
		return neighbours[neighbour].getColourRow(layer, relativeRotations[neighbour]);
	}
	
	/**
	 * Performs a clockwise rotation of the given layer when looking at the cube from this side.
	 * @param layer The layer which must be rotated.
	 */
	public void rotate(int layer)
	{
		if (layer == 0)
			rotation = (rotation + 1) % 4;
		if (layer == size - 1)
			opposite.rotation = (opposite.rotation + 3) % 4;
		
		Colour [] c0, c1, c2, c3;
		c0 = getNeighbourRotationColours(0, layer);
		c1 = getNeighbourRotationColours(1, layer);
		c2 = getNeighbourRotationColours(2, layer);
		c3 = getNeighbourRotationColours(3, layer);
		int temp;
		
		for (int index = 0; index < size; index++)
		{
			temp = c3[index].getColour();
			c3[index].setColour(c2[index].getColour());
			c2[index].setColour(c1[index].getColour());
			c1[index].setColour(c0[index].getColour());
			c0[index].setColour(temp);
		}
	}
}
