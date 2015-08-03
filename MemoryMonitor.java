import java.awt.*;
import javax.swing.*;
 
public class MemoryMonitor
{
    public long freeMemory, totalMemory, maxMemory, memoryUsed;
    public String memString;
    public boolean stop;
    public int iter;

    public MemoryMonitor()
    {	
	memString = "";
	stop = false;
	iter = 0;
	Thread thread =
	    new Thread()
	    {
		public void run()
		{
		    while(!stop){
			compute();
			try { Thread.sleep(2000); } catch (Exception e) {}
		    }
		}
	    };
	thread.start();
    }
 
    public void stop(){
	stop = true;
    }

    public void compute()
    {
	int rr, gg, pmemu, pmemm;
	    
	freeMemory = Runtime.getRuntime().freeMemory();
	totalMemory = Runtime.getRuntime().totalMemory();
	maxMemory = Runtime.getRuntime().maxMemory();
	
	memoryUsed = totalMemory-freeMemory;
	
	pmemu = (int) (memoryUsed/(1024*1024));
	pmemm = (int) (maxMemory/(1024*1024));

	memString = "[" + pmemu + "/" + pmemm + " Mb]";
	
    }
}
