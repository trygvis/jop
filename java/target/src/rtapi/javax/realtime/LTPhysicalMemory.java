package javax.realtime;

/**
 * TODO: should be removed, but SPM experiments depend on it.
 * How can we find a nicer SCJ abstraction on core local memory?
 * 
 * @author martin
 *
 */
public class LTPhysicalMemory extends ScopedMemory {
	
	public LTPhysicalMemory(java.lang.Object type, long size) {
//		super(size);
	}

	@Override
	public void enter(Runnable logic)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public long memoryConsumed()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long memoryRemaining()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long size()
	{
		// TODO Auto-generated method stub
		return 0;
	}
}
