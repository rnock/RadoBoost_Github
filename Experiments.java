import java.io.*;
import java.util.*;


class Experiments implements Debuggable{
    
    public static String KEY_HELP = "--help", KEY_RESOURCE = "-R";
    
    Algorithm myAlgos;
    Domain myDomain;

    public static String help(){
	String ret = "";
	ret += KEY_HELP + " : example command line\n\n";
	ret += KEY_RESOURCE + " :: name of resource file to parse algorithms\n";

	return ret;
    }

    public static void main(String [] arg){
	int i;
	String kR = "";
	for (i=0;i<arg.length;i++){
	    if (arg[i].equals(KEY_HELP)){
		Dataset.perror(help());
	    }

	    if (arg[i].equals(KEY_RESOURCE))
		kR = arg[i+1];
	}
	
	if (kR.equals(new String("")))
	    Dataset.perror("No resource file name found in command line");

	if (SAVE_MEMORY)
	    System.out.print("** Saving memory for processing\n");
	System.out.print("\n\n** Parsing resource file " + kR + " ...");

	Experiments ee = new Experiments();
	ee.go(kR);
    }

    public void go(String rs){
	Vector v;
	parse(rs);
	v = myAlgos.go();
	myAlgos.save(v);

	myDomain.myMemoryMonitor.stop();
    }
    
    public void parse(String rs){
	FileReader e;
	BufferedReader br;
	StringTokenizer t;
	String dum, n, nameD = "", nameP = "";
	Vector v;
	int maxr = -1;
	double epsilon = -1.0;

	myDomain = null;

	// Domain
	try{
	    e = new FileReader(rs);
	    br = new BufferedReader(e);
	    
	    while ( (dum=br.readLine()) != null){
		if ( (dum.length() == 1) || ( (dum.length()>1) && (!dum.substring(0,Dataset.KEY_COMMENT.length()).equals(Dataset.KEY_COMMENT)) ) ){
		    t = new StringTokenizer(dum,Dataset.KEY_SEPARATION_STRING[Dataset.SEPARATION_INDEX]); 
		    n = t.nextToken();
		    if (n.equals(Dataset.KEY_DIRECTORY))
			nameD = t.nextToken();
		    else if (n.equals(Dataset.KEY_PREFIX))
			nameP = t.nextToken();
		    else if (n.equals(Dataset.MAX_RADO))
			maxr = Integer.parseInt(t.nextToken());
		    else if (n.equals(Dataset.DP_EPSILON))
			epsilon = Double.parseDouble(t.nextToken());
		}
	    }
	    e.close();
	}catch(IOException eee){
	    System.out.println("Problem loading ." + rs + " resource file --- Check the access to file");
	    System.exit(0);
	}

	if (nameD.equals(new String("")))
	    Dataset.perror("No domain in resource file");
	if (nameP.equals(new String("")))
	    Dataset.perror("No prefix in resource file");
	if (maxr == -1)
	    Dataset.perror("No value specified for max rado size in resource file");

	System.out.println("Domain " + nameP + " in directory " + nameD + "");

	myDomain = new Domain(nameD, nameP, maxr);
	myAlgos = new Algorithm(myDomain);

	if (epsilon > 0.0)
	    Domain.DP_EPSILON = epsilon;

	// Algos
	try{
	    e = new FileReader(rs);
	    br = new BufferedReader(e);
	    
	    while ( (dum=br.readLine()) != null){
		if ( (dum.length() == 1) || ( (dum.length()>1) && (!dum.substring(0,Dataset.KEY_COMMENT.length()).equals(Dataset.KEY_COMMENT)) ) ){
		    t = new StringTokenizer(dum,Dataset.KEY_SEPARATION_STRING[Dataset.SEPARATION_INDEX]); 
		    n = t.nextToken();
		    if (n.equals(Dataset.KEY_ALGORITHM)){
			v = new Vector();
			while(t.hasMoreTokens())
			    v.addElement(new String(t.nextToken()));
			myAlgos.addAlgorithm(v);
		    }
		}
	    }
	    e.close();
	}catch(IOException eee){
	    System.out.println("Problem loading ." + rs + " resource file --- Check the access to file");
	    System.exit(0);
	}
    }

}
