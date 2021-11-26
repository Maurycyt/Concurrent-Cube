# Concurrent-Cube
A project made for the Concurrent Programming course during the winter semester of the 2021/22 academic year.
Implemented in Java.

## The project statement
### The functionality
The task was to create a Cube class, representing a concurrent Rubik's Cube of given size, which supplies three methods:
  1) Its constructor <code>Cube(int size, BiConsumer<Integer,Integer> beforeRotation, BiConsumer<Integer,Integer> afterRotation, Runnable beforeShowing, Runnable afterShowing)</code>, which constructs a Rubik's Cube of dimensions <code>size</code>x<code>size</code>x<code>size</code>, the operations on which are to be performed concurrently if possible.
  2) The rotate method <code>void rotate(int side, int layer)</code> which rotates the <code>layer</code>-th layer as looking from the <code>side</code>-th side, indexing from 0, that is <code>side</code> belongs to the range [0,5] and <code>layer</code> belongs to the range [0,<code>size</code>-1].
  3) The show method <code>String show()</code> which creates a String object with a text representation of the Cube.

The rotate method should have also called <code>beforeRotation(side, layer)</code> directly before performing the actual rotation and <code>afterRotation(side, layer)</code> directly after performing the rotation. Similarly, the show method had to call <code>beforeShowing()</code> and <code>afterShowing</code> directly before and after reading the state of the Cube. There were no further assumptions which could be made about these four functions, apart from that they would halt.
  
### The concurrency
The Cube had to support concurrent operations, which meant that if two threads could perform an operation at the same time, then they should have performed it at the same time if there were no contraindications, like the existence of other waiting processes. No two threads should have been performing a rotate and a show operation at the same time and no two threads should have been performing a rotation, which shared a block of the Rubik's Cube. There were no other safety requirements. Interrupts also had to be taken into account and an interrupted thread had to give up performing the planned operation if it was interrupted before clearing the entry protocols. An interrupted thread could not compromise the integrity of the protocols or the Cube.
  
The only requirement regarding the order of operations performed on the cube is that the result of the show method had to be correct for at least one possible order of performing the rotate and show methods.

### The tests
The concurrentcube package was also required to contain JUnit 5 tests testing the Cube class thoroughly, including its correctness, concurrent safety and concurrent liveness and how it reacted to interrupts.
  
### The documentation
No documentation was required. But I provided one anyway. Except for the tests.
