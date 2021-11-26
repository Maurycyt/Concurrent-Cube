package concurrentcube;

/**
 * Class containing the representing the colour of a single
 * field (square) on a cube side.
 */
public class Colour
{
	/**
	 * The colour value.
	 */
	private int c;
	
	/**
	 * The constructor of the Colour object.
	 * @param c The colour value.
	 */
	public Colour(int c)
	{
		this.c = c;
	}
	
	/**
	 * Gets the colour value.
	 */
	public int getColour()
	{
		return c;
	}
	
	/**
	 * Sets the colour value.
	 * @param c The new colour.
	 */
	public void setColour(int c)
	{
		this.c = c;
	}
	
	/**
	 * Gets the text representation of the colour value.
	 */
	@Override
	public String toString()
	{
		return Integer.toString(c);
	}
}
