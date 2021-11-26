package concurrentcube;

import java.util.concurrent.Semaphore;

/**
 * The class which controls concurrent processes on the Cube.
 * Not actually a monitor, but it is probably best to think of it that way.
 */
public class CubeMonitor
{
	/**
	 * The number of different thread groups (0-2 for Rotate, 3 for Show).
	 */
	private static final int NUMBER_OF_GROUPS = 4;
	/**
	 * Keeps track of the number of processes currently doing anything with the Cube.
	 */
	private int workingOnCube;
	/**
	 * Keeps track of the group which is currently doing something or which has done something most recently on the cube.
	 * The exact value is not important if workingOnCube is equal to 0.
	 */
	private int groupOnCube;
	/**
	 * Keeps track of which thread group has priority.
	 */
	private int prioritizedGroup;
	/**
	 * Keeps track of the number of processes waiting to rotate in one of the three directions or show.
	 * Rotate groups are indexed with their direction numbers (0,1,2) and the Show group is number 3.
	 */
	private int [] waitingForGroup;
	/**
	 * Keeps track of the number of threads which were woken but did not yet react to being woken.
	 */
	private int wokenThreads;
	/**
	 * Mutual exclusion semaphore for monitor access, particularly to prevent threads
	 * from "barging in" while other threads are being woken. 
	 */
	private final Semaphore bigMutex;
	/**
	 * Mutual exclusion for access to variables and operations influencing
	 * interrupted threads and threads waiting on or releasing groupSemaphore.
	 * Often works in parallel with bigMutex.
	 */
	private final Semaphore smallMutex;
	/**
	 * Makes the processes wait until rotation or showing becomes possible.
	 * Rotate groups are indexed with their direction numbers (0,1,2) and the Show group is number 3.
	 */
	private final Semaphore [] groupSemaphore;
	/**
	 * Makes the processes wait until the plane they want to rotate will be free.
	 */
	private final Semaphore [] planeSemaphore;
	
	/**
	 * The constructor of the cube monitor.
	 * @param size The size of the cube
	 */
	public CubeMonitor(int size)
	{
		this.workingOnCube = 0;
		this.groupOnCube = -1;
		this.prioritizedGroup = 0;
		this.waitingForGroup = new int[NUMBER_OF_GROUPS];
		for (int i = 0; i < NUMBER_OF_GROUPS; i++)
			this.waitingForGroup[i] = 0;
		this.wokenThreads = 0;
		this.bigMutex = new Semaphore(1, true); // This semaphore needs to be fair in order to guarantee that every thread gets to execute the entry protocol.
		this.smallMutex = new Semaphore(1, false); // This one doesn't need to be fair, since bigMutex will block infinitely many other threads from acquiring it.
		this.groupSemaphore = new Semaphore[NUMBER_OF_GROUPS];
		for (int i = 0; i < NUMBER_OF_GROUPS; i++)
			this.groupSemaphore[i] = new Semaphore(0, false); // This one doesn't need to be fair, since all waiting threads will be released at once.
		this.planeSemaphore = new Semaphore[size];
		for (int i = 0; i < size; i++)
			this.planeSemaphore[i] = new Semaphore(1, false); // This one doesn't need to be fair, since threads will acquire it in finite groups at a time.
	}
	
	/**
	 * Checks if the thread of a given group can skip waiting at a semaphore.
	 * To be executed under smallMutex protection.
	 * @param group The group of the thread
	 * @return true if the thread can skip waiting, false otherwise
	 */
	private boolean canSkipWaiting(int group)
	{
		boolean result = true;
		for (int i = 0; i < NUMBER_OF_GROUPS; i++)
			result &= (waitingForGroup[i] == 0);
		result &= (workingOnCube == 0 || groupOnCube == group);
		return result;
	}
	
	/**
	 * Fairly wakes up waiting processes if they exist and sets groupOnCube.
	 * Checks if there is a process waiting. If so, the whole group of the waiting process is
	 * woken and the return value is true. Otherwise, nothing happens and false is returned.
	 * To be executed under smallMutex protection.
	 * @return True if a process was woken
	 */
	private boolean tryWakeNextGroup()
	{
		int group = prioritizedGroup;
		int groupToWake = -1;
		
		do
		{
			if (waitingForGroup[group] > 0)
				groupToWake = group;
			group = (group + 1) % NUMBER_OF_GROUPS;
		} while (group != prioritizedGroup && groupToWake == -1);
		
		prioritizedGroup = group;
		
		// Now, if groupToWake != -1, then prioritizedGroup is set to
		// the group after the one which was woken. Otherwise, it's left
		// equal to what it was before.
		
		if (groupToWake == -1)
		{
			return false;
		}
		else
		{
			groupOnCube = groupToWake;
			wokenThreads = waitingForGroup[groupToWake];
			groupSemaphore[groupToWake].release(wokenThreads);
			return true;
		}
	}
	
	/**
	 * Execute protocol for the entry of a process from a given group.
	 * @param group The group which the process belongs to (0-2 for Rotate, 3 for Show)
	 * @throws InterruptedException
	 */
	private void enterGroup(int group) throws InterruptedException
	{
		bigMutex.acquireUninterruptibly();
		smallMutex.acquireUninterruptibly();
		// If can skip waiting, then skip waiting, else wait.
		if (canSkipWaiting(group))
		{
			workingOnCube++;
			groupOnCube = group;
			smallMutex.release();
			bigMutex.release();
		}
		else
		{
			waitingForGroup[group]++;
			smallMutex.release();
			bigMutex.release();
			
			try {
				groupSemaphore[group].acquire();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				smallMutex.acquireUninterruptibly();
				waitingForGroup[group]--;
				// If the interrupted thread is part of a group which was just woken.
				// Note: there is bigMutex inheritance here.
				if (wokenThreads > 0 && groupOnCube == group)
				{
					// React to the waking.
					wokenThreads--;
					// We know that the semaphore has been raised for this thread.
					groupSemaphore[group].acquireUninterruptibly();
					// If the interrupted thread is the last to react to the waking, it must take care of bigMutex.
					if (wokenThreads == 0)
					{
						// Due to bigMutex inheritance in this case, it must be released,
						// but only if there is no other process being woken.
						if (workingOnCube > 0 || tryWakeNextGroup() == false)
							bigMutex.release();
					}
				}
				// Let other threads enter the protocols or react to being woken or interrupted.
				// Other threads will enter the protocol only if bigMutex is raised, which
				// is not the case if threads are being woken.
				smallMutex.release();
				throw e;
			}
			// All went well, bigMutex is inherited.
			smallMutex.acquireUninterruptibly();
			waitingForGroup[group]--;
			wokenThreads--;
			workingOnCube++;
			// Release bigMutex only if all threads reacted to being woken.
			if (wokenThreads == 0)
				bigMutex.release();
			smallMutex.release();
		}
	}
	
	/**
	 * Execute protocol for the entry of a process from a given group.
	 * @param group The group which the process belongs to (0-2 for Rotate, 3 for Show)
	 */
	private void exitGroup(int group)
	{
		bigMutex.acquireUninterruptibly();
		workingOnCube--;
		// If there are still processes in the critical section or none were woken,
		// then bigMutex wasn't inherited, so we need to release it.
		smallMutex.acquireUninterruptibly();
		if (workingOnCube > 0 || tryWakeNextGroup() == false)
			bigMutex.release();
		smallMutex.release();
	}
	
	/**
	 * Synchronizes the Rotate threads' cube entry moments.
	 * @param direction The direction of rotation
	 * @param plane The plane of rotation
	 * @throws InterruptedException
	 */
	public void enterRotate(int direction, int plane) throws InterruptedException
	{
		// Enter with the group of processes performing the same action as this process.
		// If this throws an exception we assume the clean-up has been already performed.
		enterGroup(direction);
		
		// Wait for plane availability.
		// If this throws an exception we need to skip the planned
		// operation and simply perform the group exit protocol.
		try {
			planeSemaphore[plane].acquire();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			exitGroup(direction);
			throw e;
		}
	}
	
	/**
	 * Allows a Rotate process to wake up other processes if possible.
	 * @param direction The direction of rotation
	 * @param plane The plane of rotation
	 * @throws InterruptedException
	 */
	public void exitRotate(int direction, int plane) throws InterruptedException
	{
		// Signal that the plane is free again.
		planeSemaphore[plane].release();
		
		// Exit the cube critical section waking up other processes if necessary.
		exitGroup(direction);
	}
	
	/**
	 * Synchronizes the Show threads' cube entry moments.
	 * @throws InterruptedException
	 */
	public void enterShow() throws InterruptedException
	{
		// Enter with the group of processes performing the same action as this process.
		// If this throws an exception we assume the clean-up has been already performed.
		enterGroup(3);
	}
	
	/**
	 * Allows a Show process to wake up other processes if possible.
	 */
	public void exitShow()
	{
		// Exit the cube critical section waking up other processes if necessary.
		exitGroup(3);
	}
}
