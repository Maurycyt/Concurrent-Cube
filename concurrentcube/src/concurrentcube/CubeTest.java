package concurrentcube;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.Random;
import java.util.function.BiConsumer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class CubeTest
{
	private static final int MAX_SIZE = 2000;
	private Cube cube;
	
	// For concurrent tests
	// Concurrent safety
	private int rotating;
	private int showing;
	private int rotatingDirection [];
	private int rotatingPlane [];
	private boolean concurrencyFailed;
	
	// Concurrent speed
	private int maxOnCube;
	
	// Concurrent liveness
	private int operationNumber;
	private int lastOperationFromGroup [];
	
	// For interrupt tests
	private CyclicBarrier barrier;
	
	// For protected access to the above variables
	private Semaphore concurrencyTestSemaphore = new Semaphore(1);
	
	private final Random random = new Random();
	
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
	
	private int getOppositeSide(int side)
	{
		int result = -1;
		switch(side)
		{
		case 0:
		case 5:
			result = 5 - side;
			break;
		case 1:
		case 3:
			result = 4 - side;
			break;
		default: // 2, 4
			result = 6 - side;
			break;
		}
		return result;
	}
	
	private int getPlane(int size, int side, int direction, int layer)
	{
		if (direction == side)
			return layer;
		else
			return size - 1 - layer;
	}
	
	private BiConsumer<Integer, Integer> getBeforeRotationForSizeWithDelay(int size, int delay)
	{
		return (side, layer) ->
		{
			concurrencyTestSemaphore.acquireUninterruptibly();
			// auxiliary values
			int direction = getDirection(side);
			int plane = getPlane(size, side, direction, layer);
			// primitive logging
			operationNumber++;
			lastOperationFromGroup[direction] = operationNumber;
			// safety checks
			rotating++;
			rotatingDirection[direction]++;
			rotatingPlane[plane]++;
			concurrencyFailed |= (showing > 0);
			concurrencyFailed |= (rotatingDirection[(direction + 1) % 3] > 0);
			concurrencyFailed |= (rotatingDirection[(direction + 2) % 3] > 0);
			concurrencyFailed |= (rotatingPlane[plane] > 1);
			// speed / safety log
			maxOnCube = Math.max(maxOnCube, rotating);
			concurrencyTestSemaphore.release();
			
			// delay
			if (delay != 0)
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				System.out.println("BeforeRotation interrupted!");
			}
		};
	}
	
	BiConsumer<Integer, Integer> getAfterRotationForSizeWithDelay(int size, int delay)
	{	
		return (side, layer) ->
		{
			concurrencyTestSemaphore.acquireUninterruptibly();
			int direction = getDirection(side);
			int plane = getPlane(size, side, direction, layer);
			rotating--;
			rotatingDirection[direction]--;
			rotatingPlane[plane]--;
			concurrencyTestSemaphore.release();
			
			if (delay != 0)
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				System.out.println("AfterRotation interrupted!");
			}
		};
	}
	
	Runnable getBeforeShowingWithDelay(int delay)
	{
		return () ->
		{
			concurrencyTestSemaphore.acquireUninterruptibly();
			// primitive logging
			operationNumber++;
			lastOperationFromGroup[3] = operationNumber;
			// safety checks
			showing++;
			concurrencyFailed |= (rotating > 0);
			maxOnCube = Math.max(maxOnCube, showing);
			concurrencyTestSemaphore.release();
			
			if (delay != 0)
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				System.out.println("BeforeShowing interrupted!");
			}
		};
	}
	
	Runnable getAfterShowingWithDelay(int delay)
	{
		return () ->
		{
			concurrencyTestSemaphore.acquireUninterruptibly();
			showing--;
			concurrencyTestSemaphore.release();
			
			if (delay != 0)
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				System.out.println("AfterShowing interrupted!");
			}
		};
	}
	
	@BeforeEach
	void setUp()
	{
		cube = new Cube(3, null, null, null, null);
		
		rotating = 0;
		showing = 0;
		rotatingDirection = new int [3];
		rotatingPlane = new int [MAX_SIZE];
		concurrencyFailed = false;
		maxOnCube = 0;
		operationNumber = 0;
		lastOperationFromGroup = new int [4];
		barrier = new CyclicBarrier(1);
	}
	
	// Rotation tests created with the help of
	// https://ruwix.com/online-puzzle-simulators/
	
	// Regular, clockwise rotation tests.
	
	void clockwiseRotationTest(int side, String expectedResult) throws InterruptedException
	{
		// Break symmetry. The side with number (side + 3) % 6 is always a neighbouring side.
		cube.rotate((side + 3) % 6, 0);
		// Actual test.
		cube.rotate(side,  1);
		assertEquals(cube.show(), expectedResult);
	}
	
	@Test
	void AclockwiseRotationTest0() throws InterruptedException
	{
		clockwiseRotationTest(0, ""
				+ "002002002"
				+ "111225111"
				+ "225333225"
				+ "333044333"
				+ "044111044"
				+ "554554554");
	}
	
	@Test
	void AclockwiseRotationTest1() throws InterruptedException
	{
		clockwiseRotationTest(1, ""
				+ "343040040"
				+ "011011011"
				+ "232202202"
				+ "335335335"
				+ "414454454"
				+ "525525121");
	}
	
	@Test
	void AclockwiseRotationTest2() throws InterruptedException
	{
		clockwiseRotationTest(2, ""
				+ "000411000"
				+ "151151454"
				+ "222222111"
				+ "303303202"
				+ "444444333"
				+ "555233555");
	}
	
	@Test
	void AclockwiseRotationTest3() throws InterruptedException
	{
		clockwiseRotationTest(3, ""
				+ "030020020"
				+ "222111111"
				+ "353252252"
				+ "444333333"
				+ "101404404"
				+ "545545515");
	}
	
	@Test
	void AclockwiseRotationTest4() throws InterruptedException
	{
		clockwiseRotationTest(4, ""
				+ "400333400"
				+ "101101141"
				+ "022022022"
				+ "353353323"
				+ "445445445"
				+ "255111255");
	}
	
	@Test
	void AclockwiseRotationTest5() throws InterruptedException
	{
		clockwiseRotationTest(5, ""
				+ "000000111"
				+ "115444115"
				+ "222115222"
				+ "033222033"
				+ "444033444"
				+ "333555555");
	}
	
	// Assuming all clockwise rotation tests passed,
	// proceed to counter-clockwise rotation tests.
	
	void counterclockwiseRotationTest(int side) throws InterruptedException
	{
		// Break symmetry. The side with number (side + 3) % 6 is always a neighbouring side.
		cube.rotate((side + 3) % 6, 0);
		// Save result.
		String result = cube.show();
		// Perform clockwise rotation.
		cube.rotate(side, 0);
		// Perform counter-clockwise rotation.
		cube.rotate(getOppositeSide(side), 2);
		// Check correctness.
		assertEquals(cube.show(), result);
	}
	
	@Test
	void BcounterclockwiseRotationTest0() throws InterruptedException
	{
		counterclockwiseRotationTest(0);
	}
	
	@Test
	void BcounterclockwiseRotationTest1() throws InterruptedException
	{
		counterclockwiseRotationTest(1);
	}
	
	@Test
	void BcounterclockwiseRotationTest2() throws InterruptedException
	{
		counterclockwiseRotationTest(2);
	}
	
	@Test
	void BcounterclockwiseRotationTest3() throws InterruptedException
	{
		counterclockwiseRotationTest(3);
	}
	
	@Test
	void BcounterclockwiseRotationTest4() throws InterruptedException
	{
		counterclockwiseRotationTest(4);
	}
	
	@Test
	void BcounterclockwiseRotationTest5() throws InterruptedException
	{
		counterclockwiseRotationTest(5);
	}
	
	// Two extra silly tests... but you never know!
	@Test
	void Csize0Test() throws InterruptedException
	{
		cube = new Cube(0, null, null, null, null);
		
		// No possible correct arguments to call rotate().
		assertEquals(cube.show(), "");
	}
	
	@Test
	void Csize1Test() throws InterruptedException
	{
		cube = new Cube(1, null, null, null, null);
		
		cube.rotate(0, 0);
		assertEquals(cube.show(), "023415");
	}
	
	private class ShowRunnable implements Runnable
	{
		private final Cube cube;
		private final int times;
		
		public ShowRunnable(Cube cube, int times)
		{
			this.cube = cube;
			this.times = times;
		}
		
		public void run()
		{
			try {
				barrier.await();
				for (int time = 0; time < times; time++)
					cube.show();
			} catch (InterruptedException e) {
				System.out.println("Show runnable interrupted!");
			} catch (BrokenBarrierException e) {
				System.out.println("Broke barrier?");
			}
		}
	}
	
	private class RotateRunnable implements Runnable
	{
		private final Cube cube;
		private final int side;
		private final int layer;
		private final int times;
		
		public RotateRunnable(Cube cube, int side, int layer, int times)
		{
			this.cube = cube;
			this.side = side;
			this.layer = layer;
			this.times = times;
		}
		
		public void run()
		{
			try {
				barrier.await();
				for (int rotation = 0; rotation < times; rotation++)
					cube.rotate(side, layer);
			} catch (InterruptedException e) {
				System.out.println("Rotate runnable interrupted!");
			} catch (BrokenBarrierException e) {
				System.out.println("Barrier broken?");
			}
		}
	}
	
	// Fills an array of threads with random threads performing cube operations,
	private Thread [] makeThreads(int operations, Cube bigCube, int size)
	{
		Thread [] threads = new Thread[operations];
		
		for (int thread = 0; thread < operations; thread++)
		{
			if (random.nextDouble() < 0.25)
			{
				threads[thread] = new Thread(new ShowRunnable(bigCube, 1));
			}
			else
			{
				threads[thread] = new Thread(new RotateRunnable(bigCube, random.nextInt(6), random.nextInt(size), 1));
			}
		}
		
		return threads;
	}
	
	// Does not check if the cube is in a correct configuration,
	// but performs a simple, partial check if all colours appear
	// exactly as many times as necessary.
	private boolean checkColourCounts(Cube cube, int size) throws InterruptedException
	{
		boolean result = true;
		String str = cube.show();
		int [] colourCounts = new int [6];
		for (int i = 0; i < str.length(); i++)
			colourCounts[str.charAt(i) - '0']++;
		for (int i = 0; i < 6; i++)
			result &= (colourCounts[i] == size * size);
		return result;
	}
	
	// Performs a concurrent safety test for given size and number of operations.
	// Since all operations are done in separate threads and there's a lot of them,
	// this is also something of a deadlock test.
	// The executions of this test all should take about or under 10 seconds.
	private void concurrentSafetyTest(int size, int operations) throws InterruptedException
	{	
		Cube bigCube = new Cube (size, getBeforeRotationForSizeWithDelay(size, 0), getAfterRotationForSizeWithDelay(size, 0), 
		                         getBeforeShowingWithDelay(0), getAfterShowingWithDelay(0));
		
		Thread [] threads = new Thread[operations];
		
		threads = makeThreads(operations, bigCube, size);
		
		for (int thread = 0; thread < operations; thread++)
			threads[thread].start();
		for (int thread = 0; thread < operations; thread++)
			threads[thread].join();
		
		assertTrue(checkColourCounts(bigCube, size));
		assertFalse(concurrencyFailed);
	}
	
	@Test
	void DconcurrentSafetyTest1() throws InterruptedException
	{
		concurrentSafetyTest(10, 100000);
	}
	
	@Test
	void DconcurrentSafetyTest2() throws InterruptedException
	{
		concurrentSafetyTest(100, 16000);
	}
	
	@Test
	void DconcurrentSafetyTest3() throws InterruptedException
	{
		concurrentSafetyTest(1000, 200);
	}
	
	private void concurrentSpeedTest(int size, int times, int groups, int delay) throws InterruptedException
	{
		Cube bigCube = new Cube (size, getBeforeRotationForSizeWithDelay(size, delay), getAfterRotationForSizeWithDelay(size, delay), 
		                         getBeforeShowingWithDelay(delay), getAfterShowingWithDelay(delay));
		Thread [] threads;
		
		// Threads which will have to wait for one another.
		threads = new Thread [groups];
		for (int group = 0; group < Math.min(3, groups); group++)
			threads[group] = new Thread(new RotateRunnable(bigCube, group, 0, times));
		if (groups == 4)
			threads[3] = new Thread(new ShowRunnable(bigCube, Math.max(2, times/size)));
		
		long sequentialStart = System.currentTimeMillis();
		for (int thread = 0; thread < groups; thread++)
			threads[thread].start();
		for (int thread = 0; thread < groups; thread++)
			threads[thread].join();
		long sequentialTime = System.currentTimeMillis() - sequentialStart;
		
		// Extra safety check.
		assertFalse(concurrencyFailed);
		assertTrue(maxOnCube == 1);
		maxOnCube = 0;
		
		// Threads which will be able to execute concurrently. 
		threads = new Thread [groups];
		for (int group = 0; group < Math.min(3, groups); group++)
			threads[group] = new Thread(new RotateRunnable(bigCube, 0, group, times));
		if (groups == 4)
			threads[3] = new Thread(new ShowRunnable(bigCube, Math.max(2, times/size)));
		
		long concurrentStart = System.currentTimeMillis();
		for (int thread = 0; thread < groups; thread++)
			threads[thread].start();
		for (int thread = 0; thread < groups; thread++)
			threads[thread].join();
		long concurrentTime = System.currentTimeMillis() - concurrentStart;
		
		// Extra safety check.
		assertFalse(concurrencyFailed);
		// Check for existing concurrency.
		assertTrue(maxOnCube == Math.min(3, groups));
		assertTrue(concurrentTime < sequentialTime);
	}
	
	@Test
	void EconcurrentSpeedTest1() throws InterruptedException
	{
		concurrentSpeedTest(100, 50, 2, 20);
	}
	
	@Test
	void EconcurrentSpeedTest2() throws InterruptedException
	{
		concurrentSpeedTest(500, 40, 3, 20);
	}
	
	@Test
	void EconcurrentSpeedTest3() throws InterruptedException
	{
		concurrentSpeedTest(2000, 20, 4, 20);
	}
	
	// Groups are 0-2 for Rotate, 3 for Show.
	// We attempt to starve group number starvedGroup in thread t0 by creating threads
	// doing two other, pairwise exclusive operations in a loop, say t1 and t2.
	// We start t1 and then start t0 with a small delay, which should ensure that t0
	// will wait for access to the cube. Then we start t2 and we check if the last logged
	// operation performed by t0 was the last operation overall.
	// The starved group performs only one operation and the other groups perform times operations.
	// In fact, the starved group should perform its operation as the second or third overall operation,
	// (depending on its number relative to prioritizedGroup) but such bounded bypass is not checked.
	private void starvationTest(int starvedGroup, int times) throws InterruptedException
	{
		// time unit, here 10 milliseconds
		int tu = 10;
		cube = new Cube (1, getBeforeRotationForSizeWithDelay(1, 2 * tu), getAfterRotationForSizeWithDelay(1, 2 * tu), 
                getBeforeShowingWithDelay(2 * tu), getAfterShowingWithDelay(2 * tu));
		
		Thread [] t = new Thread [3];
		for (int i = 0; i < 3; i++)
		{
			int group = (starvedGroup + i) % 4;
			if (group == 3)
				t[i] = new Thread(new ShowRunnable(cube, i == 0 ? 1 : times));
			else
				t[i] = new Thread(new RotateRunnable(cube, group, 0, i == 0 ? 1 : times));
		}
		
		t[1].start();
		Thread.sleep(tu);
		t[0].start();
		Thread.sleep(tu);
		t[2].start();
		
		for (int i = 0; i < 3; i++)
			t[i].join();
		
		assertTrue(lastOperationFromGroup[starvedGroup] != operationNumber);
		assertFalse(concurrencyFailed);
	}

	@Test
	void FstarvationTest0() throws InterruptedException
	{
		starvationTest(0, 10);
	}
	
	@Test
	void FstarvationTest1() throws InterruptedException
	{
		starvationTest(1, 10);
	}
	
	@Test
	void FstarvationTest2() throws InterruptedException
	{
		starvationTest(2, 10);
	}
	
	@Test
	void FstarvationTest3() throws InterruptedException
	{
		starvationTest(3, 10);
	}
	
	// In this interrupt test four threads are created with delays on the cube
	// such that it is easy to control the order in which they are executed.
	// The second thread is interrupted while it waits on the group semaphore.
	// The other threads are expected to finish execution at some point and then
	// the state of the cube is checked for correctness.
	// The rotations are the same which are performed in AclockwiseRotationTest0.
	// This test should cause "Rotate runnable interrupted!" to appear on standard output.
	@Test
	void GinterruptTestOnGroupSemaphore() throws InterruptedException
	{
		// time unit, here 10 milliseconds
		int tu = 10;
		cube = new Cube(3, getBeforeRotationForSizeWithDelay(3, 5 * tu), getAfterRotationForSizeWithDelay(3, 5 * tu), 
                getBeforeShowingWithDelay(5 * tu), getAfterShowingWithDelay(5 * tu));
		
		Thread [] t = new Thread [4];
		t[0] = new Thread(new RotateRunnable(cube, 3, 0, 1));
		t[1] = new Thread(new ShowRunnable(cube, 1));
		t[2] = new Thread(new RotateRunnable(cube, 0, 1, 1));
		t[3] = new Thread(new ShowRunnable(cube, 1));
		
		for (int i = 0; i < 4; i++)
		{
			t[i].start();
			Thread.sleep(tu);
		}
		t[1].interrupt();
		for (int i = 0; i < 4; i++)
			t[i].join();
		
		assertEquals(cube.show(), ""
				+ "002002002"
				+ "111225111"
				+ "225333225"
				+ "333044333"
				+ "044111044"
				+ "554554554");
		assertFalse(concurrencyFailed);
	}
	
	// This interrupt test is the same as above but the second thread is supposed to
	// be interrupted while waiting on the plane semaphore.
	@Test
	void GinterruptTestOnPlaneSemaphore() throws InterruptedException
	{
		// time unit, here 10 milliseconds
		int tu = 10;
		cube = new Cube(3, getBeforeRotationForSizeWithDelay(3, 5 * tu), getAfterRotationForSizeWithDelay(3, 5 * tu), 
                getBeforeShowingWithDelay(5 * tu), getAfterShowingWithDelay(5 * tu));
		
		Thread [] t = new Thread [4];
		t[0] = new Thread(new RotateRunnable(cube, 3, 0, 1));
		t[1] = new Thread(new RotateRunnable(cube, 1, 2, 1));
		t[2] = new Thread(new RotateRunnable(cube, 0, 1, 1));
		t[3] = new Thread(new ShowRunnable(cube, 1));
		
		for (int i = 0; i < 4; i++)
		{
			t[i].start();
			Thread.sleep(tu);
		}
		t[1].interrupt();
		for (int i = 0; i < 4; i++)
			t[i].join();
		
		assertEquals(cube.show(), ""
				+ "002002002"
				+ "111225111"
				+ "225333225"
				+ "333044333"
				+ "044111044"
				+ "554554554");
		assertFalse(concurrencyFailed);
	}
	
	// This interrupt test attempts to interrupt a thread which
	// was meant to be woken. It is expected that all threads finish
	// execution at some point, but the resulting cube is undetermined
	// and thus it isn't checked.
	// This test works using the fact that there is
	// 1) low probability to lose control between starting a thread and entering cube protocols
	// 2) low probability to NOT lose control during a rotation, which allows the other thread to enter protocol
	// 3) low probability to lose control between exiting cube protocols and interrupting the woken thread
	// The attempts parameter means how many times the test will be attempted.
	// The more attempts, the higher chance that the desired scenario will happen.
	// The threadsWaiting parameter tells us how many threads should be waiting at the groupSemaphore each
	// time an attempt at the test is made.
	// The threads wait on a barrier to delay their entry into the protocols. This test is meant to
	// invoke certain race conditions and increase code coverage. I should make cause interruption messages
	// to appear on standard output and may cause barrier exception messages to appear as well.
	// The latter are not to be worried about, this is why there are many attempts.
	//
	// !!! If these tests don't result in all branches in CubeMonitor being covered, run again. !!!
	@Test
	private void interruptTestWokenThread(int attempts, int threadsWaiting) throws InterruptedException
	{
		barrier = new CyclicBarrier(threadsWaiting, () -> {Thread.yield();});
		cube = new Cube(1000, getBeforeRotationForSizeWithDelay(1000, 10), getAfterRotationForSizeWithDelay(1000, 0), 
                getBeforeShowingWithDelay(0), getAfterShowingWithDelay(0));
		
		int threads = attempts * threadsWaiting;
		Thread [] t = new Thread [threads + 1];
		t[0] = new Thread(() -> {
			for (int i = 0; i < attempts; i++)
			{
				System.out.println("attempt " + i);
				for (int j = 1; j <= threadsWaiting; j++)
					t[i * threadsWaiting + j].start();
				
				try {
					cube.rotate(0, 0);
				} catch (InterruptedException e) {
					System.out.println("interruptTestWokenThread main thread interrupted! (intended)");
					assertEquals(i, attempts - 1);
				}
				
				t[i * threadsWaiting + 1].interrupt();
				if (i == attempts - 2)
					Thread.currentThread().interrupt();
			}
		});
		
		Random random = new Random();
		for (int i = 1; i <= threads; i++)
			t[i] = new Thread(new RotateRunnable(cube, random.nextInt(2) + 1, 0, 1));
		
		t[0].start();
		
		for (int i = 0; i <= threads; i++)
			t[i].join();
		
		assertFalse(concurrencyFailed);
	}
	
	@Test
	void GinterruptTestWokenThread1() throws InterruptedException
	{
		interruptTestWokenThread(10, 1);
	}
	
	// This version proved to be the most effective at invoking desired race conditions.
	// Hence so many attempts.
	@Test
	void GinterruptTestWokenThread2() throws InterruptedException
	{
		interruptTestWokenThread(100, 2);
	}
	
	@Test
	void GinterruptTestWokenThread3() throws InterruptedException
	{
		interruptTestWokenThread(30, 10);
	}
}
