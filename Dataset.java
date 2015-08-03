import java.io.*;
import java.util.*;

public class Dataset implements Debuggable{

    public static String KEY_SEPARATION_TYPES [] = {"@TABULATION", "@COMMA"};
    public static String KEY_SEPARATION_STRING[] = {"\t", ","};
    public static int SEPARATION_INDEX = 1;

    public static String DEFAULT_DIR = "Datasets", SEP = "/", KEY_COMMENT = "//";
    
    public static String KEY_DIRECTORY = "@DIRECTORY";
    public static String KEY_PREFIX = "@PREFIX";
    public static String KEY_ALGORITHM = "@ALGORITHM";

    public static String MAX_RADO = "@MAX_RADO_SIZE";
    public static String DP_EPSILON = "@DP_EPSILON";

    public static String START_KEYWORD = "@";
    public static String FIT_CLASS = "@FIT_CLASS";

    public static String [] FIT_CLASS_MODALITIES = {"NONE", "MEAN", "MEDIAN", "MINMAX", "BINARY"};
    // NONE means the problem has two class modalities -> {-1, 1}, default value
    // Otherwise, classes are fit in {-1,1} the keyword gives the meaning of the 0 value for the class : 
    // * MEAN -> classes are translated so that mean = 0 and then shrunk to fit in [-1,1]
    // * MEDIAN -> classes are translated so that median = 0 and then shrunk to fit in [-1,1]
    // * MINMAX -> classes are translated so that (min+max)/2 = 0 and then shrunk to fit in [-1,1]
    // * BINARY -> classes are translated so that mean = 0 and then BINARIZED in {-1, +1} with signs
    public static int DEFAULT_INDEX_FIT_CLASS = 0;

    public static int INDEX_FIT_CLASS(String s){
	int i;
	for (i=0;i<FIT_CLASS_MODALITIES.length;i++)
	    if (s.equals(FIT_CLASS_MODALITIES[i]))
		return i;
	Dataset.perror("Value " + s + " for keyword " + FIT_CLASS + " not recognized");
	return -1;
    }

    public static double TRANSLATE_SHRINK(double v, double translate_v, double min_v, double max_v, double max_m){
	if ( (v < min_v) || (v > max_v) )
	    Dataset.perror("Value " + v + " is not in the interval [" + min_v + ", " + max_v + "]");

	if (max_v < min_v)
	    Dataset.perror("Max " + max_v + " < Min " + min_v);

	if (max_v == min_v)
	    return v;

	double delta = (v - translate_v);
	double mm;
	if (Math.abs(max_v) > Math.abs(min_v))
	    mm = Math.abs(max_v - translate_v);
	else
	    mm = Math.abs(min_v - translate_v);

	return ( (delta/mm) * max_m );
    }

    public static double CENTER(double v, double avg, double stddev){
	if (stddev == 0.0){
	    Dataset.warning("Standard deviation is zero");
	    return 0.0;
	}
	return ( (v - avg) / stddev );
    }

    public static String SUFFIX_FEATURES = "features";
    public static String SUFFIX_EXAMPLES = "data";

    int index_class, number_initial_features, number_real_features, number_examples_total;

    boolean [] outlier_features_3s;
    boolean [] outlier_features_10s;
    boolean [] outlier_features_100s;

    String nameFeatures, nameExamples, pathSave, domainName;

    Vector features, examples;
    // WARNING : features also includes class

    boolean [] isbinaryfeature;

    Vector minmax_real_features;

    Vector stratified_sample, training_sample, test_sample;

    Domain myDS;

    public static void perror(String error_text){
        System.out.println(error_text);
        System.out.println("\nExiting to system\n");
        System.exit(1);
    }

    public static void warning(String warning_text){
        System.out.println(" * WARNING * " + warning_text);
    }

    Dataset(String dir, String pref, Domain d){
	outlier_features_3s = outlier_features_10s = outlier_features_100s = null;
	myDS = d;
	domainName = pref;
	nameFeatures = dir + SEP + pref + SEP + pref + "." + SUFFIX_FEATURES;
	nameExamples = dir + SEP + pref + SEP + pref + "." + SUFFIX_EXAMPLES;
	pathSave = dir + SEP + pref + SEP;
	if (Feature.NORMALIZE_REAL_FEATURES)
	    Dataset.warning("Centering domain features");
    }

    public void printFeatures(){
	int i;
	System.out.println(features.size() + " features : ");
	for (i=0;i<features.size();i++)
	    System.out.println((Feature) features.elementAt(i));
	System.out.println("Class index : " + index_class);
    }

    public void load_features(){
	FileReader e;
	BufferedReader br;
	StringTokenizer t;
	String dum, n, ty;
	Vector v = null;
	int index = 0;

	features = new Vector();
	index_class = -1;

	try{
	    e = new FileReader(nameFeatures);
	    br = new BufferedReader(e);
	    
	    while ( (dum=br.readLine()) != null){
		if ( (dum.length() == 1) || ( (dum.length()>1) && (!dum.substring(0,KEY_COMMENT.length()).equals(KEY_COMMENT)) ) ){
		    if (dum.substring(0,1).equals(Dataset.START_KEYWORD)){
			t = new StringTokenizer(dum,KEY_SEPARATION_STRING[SEPARATION_INDEX]); 
			if (t.countTokens()<2)
			    Dataset.perror("No value for keyword " + t.nextToken());
			n = t.nextToken();
			if (n.equals(Dataset.FIT_CLASS))
			    Dataset.DEFAULT_INDEX_FIT_CLASS = Dataset.INDEX_FIT_CLASS(t.nextToken());
		    }else{
			t = new StringTokenizer(dum,KEY_SEPARATION_STRING[SEPARATION_INDEX]);
			if (t.countTokens()>0){
			    n = t.nextToken();
			    ty = t.nextToken();
			    
			    if (Feature.INDEX(ty) == Feature.CLASS_INDEX){
				if (index_class != -1)
				    Dataset.perror("At least two classes named such in feature file");
			    else
				index_class = index;
			    }

			    if (Feature.HAS_MODALITIES(ty)){
				v = new Vector();
				while(t.hasMoreTokens())
				    v.addElement(t.nextToken());
			    }
			    features.addElement(new Feature(n, ty, v));
			    index++;
			}
		    }
		}
	    }
	    e.close();
	}catch(IOException eee){
	    System.out.println("Problem loading ." + SUFFIX_FEATURES + " file --- Check the access to file " + nameFeatures + "...");
	    System.exit(0);
	}

	Dataset.warning("Class renormalization using method : " + Dataset.FIT_CLASS_MODALITIES[DEFAULT_INDEX_FIT_CLASS]);

	number_initial_features = features.size() - 1;
	if (Debug) System.out.println("Found " + features.size() + " features, including class");
    }


    public void load_examples(){
	FileReader e;
	BufferedReader br;
	StringTokenizer t;
	String dum;
	Vector v = null;

	examples = new Vector();
	Example ee = null;
	int idd = 0, nex = 0;

	try{
	    e = new FileReader(nameExamples);
	    br = new BufferedReader(e);
	    
	    while ( (dum=br.readLine()) != null){
		if ( (dum.length() == 1) || ( (dum.length()>1) && (!dum.substring(0,KEY_COMMENT.length()).equals(KEY_COMMENT)) ) ){
		    t = new StringTokenizer(dum,KEY_SEPARATION_STRING[SEPARATION_INDEX]);
		    if (t.countTokens()>0){
			nex++;
		    }
		}
	    }
	    e.close();
	}catch(IOException eee){
	    System.out.println("Problem loading ." + SUFFIX_FEATURES + " file --- Check the access to file " + nameFeatures + "...");
	    System.exit(0);
	}

	if (SAVE_MEMORY)
	    System.out.print(nex + " examples to load... ");

	try{
	    e = new FileReader(nameExamples);
	    br = new BufferedReader(e);
	    
	    while ( (dum=br.readLine()) != null){
		if ( (dum.length() == 1) || ( (dum.length()>1) && (!dum.substring(0,KEY_COMMENT.length()).equals(KEY_COMMENT)) ) ){
		    t = new StringTokenizer(dum,KEY_SEPARATION_STRING[SEPARATION_INDEX]);
		    if (t.countTokens()>0){
			v = new Vector();
			while(t.hasMoreTokens())
			    v.addElement(t.nextToken());
			ee = new Example(idd, v, index_class);
			ee.complete_unnormalized(features, index_class);

			examples.addElement(ee);

			number_real_features = ee.unnormalized_real_features.length;
			idd++;

			if (SAVE_MEMORY)
			    if (idd%(nex/20) == 0)
				System.out.print( ((idd/(nex/20)) * 5) + "% " + myDS.memString() + " ");
		    }
		}
	    }
	    e.close();
	}catch(IOException eee){
	    System.out.println("Problem loading ." + SUFFIX_EXAMPLES + " file --- Check the access to file " + nameExamples + "...");
	    System.exit(0);
	}

	if (SAVE_MEMORY)
	    System.out.print("ok. \n");

	// checking for binary features
	int i, j;
	number_examples_total = examples.size();

	isbinaryfeature = new boolean [number_real_features];
	for (i=0;i<number_real_features;i++){
	    isbinaryfeature[i] = true;
	    double v1 = 0.0, v2 = 0.0, vc;
	    boolean fv = false;
	    j=0;
	    do{
		vc = ((Example) examples.elementAt(j)).unnormalized_real_features[i];
		if (j==0)
		    v1 = vc;
		else if ( (!fv) && (vc != v1) ){
		    v2 = vc;
		    fv = true;
		}else if ( (vc != v1) && (vc != v2) )
		    isbinaryfeature[i] = false;
		j++;
	    }while ( (isbinaryfeature[i]) && (j<number_examples_total) );
	}

	// normalizing features

	double [] minf = new double [number_real_features];
	double [] maxf = new double [number_real_features];
	double [] avg = new double [number_real_features];
	double [] med = new double [number_real_features];
	double [] stddev = new double [number_real_features];

	double minc = 0.0, maxc = 0.0;

	double dumd, vc;
	double [] dumvd = new double [number_examples_total];

	outlier_features_3s = new boolean [number_real_features];
	outlier_features_10s = new boolean [number_real_features];
	outlier_features_100s = new boolean [number_real_features];

	for (i=0;i<number_real_features;i++){
	    for (j=0;j<number_examples_total;j++)
		dumvd[j] = ((Example) examples.elementAt(j)).unnormalized_real_features[i];
	    minf[i] = Rado.min(dumvd);
	    maxf[i] = Rado.max(dumvd);
	    avg[i] = Rado.mean(dumvd);
	    med[i] = Rado.median(dumvd);
	    stddev[i] = Rado.stddev(dumvd, avg[i]);

	    outlier_features_3s[i] = Rado.outliers(dumvd, stddev[i], 3.0);
	    outlier_features_10s[i] = Rado.outliers(dumvd, stddev[i], 10.0);
	    outlier_features_100s[i] = Rado.outliers(dumvd, stddev[i], 100.0);
	}

	Vector min_max = new Vector();
	boolean [] normalize = new boolean [number_real_features];
	Vector vv;
	for (i=0;i<number_real_features;i++){
	    vv = new Vector();
	    vv.addElement(new Double(minf[i]));
	    vv.addElement(new Double(maxf[i]));

	    min_max.addElement(vv);
	    normalize[i] = Feature.NORMALIZE_REAL_FEATURES;
	}

	if (SAVE_MEMORY)
	    System.out.print("Computing normalized features... ");

	for (i=0;i<number_examples_total;i++){
	    ee = (Example) examples.elementAt(i);
	    ee.complete_normalized_features(min_max, avg, stddev, normalize);

	    if (SAVE_MEMORY)
		if (i%(number_examples_total/20) == 0)
		    System.out.print( ((i/(number_examples_total/20)) * 5) + "% " + myDS.memString() + " ");

	    if (SAVE_MEMORY)
		ee.saveMemory();
	}

	if (SAVE_MEMORY)
	    System.out.println(" ok.");

	//normalizing classes

	double [] all_classes = new double [number_examples_total];
	for (i=0;i<number_examples_total;i++)
	    all_classes[i] = ((Example) examples.elementAt(i)).unnormalized_class;

	double min_c, max_c, tv;

	max_c = min_c = tv = 0.0;
	for (i=0;i<number_examples_total;i++){
	    if ( (i==0) || (max_c < all_classes[i]) )
		max_c = all_classes[i];
	    if ( (i==0) || (min_c > all_classes[i]) )
		min_c = all_classes[i];
	}
	if (min_c == max_c)
	    Dataset.perror("Only one class modality, " + min_c);

	if (DEFAULT_INDEX_FIT_CLASS == 0){
	    //checks that there is only two modalities
	    for (i=0;i<number_examples_total;i++)
		if ( (all_classes[i] != min_c) && (all_classes[i] != max_c) )
		    Dataset.perror("class value " + all_classes[i] + " should be either " + min_c + " or " + max_c);
	    tv = (min_c + max_c) / 2.0;
	}else if ( (DEFAULT_INDEX_FIT_CLASS == 1) || (DEFAULT_INDEX_FIT_CLASS == 4) ){
	    tv = 0.0;
	    for (i=0;i<number_examples_total;i++)
		tv += all_classes[i];
	    tv /= (double) number_examples_total;
	}else if (DEFAULT_INDEX_FIT_CLASS == 2){
	    tv = 0.0;
	    QuickSort.quicksort(all_classes);
	    tv = all_classes[number_examples_total/2];
	}else if (DEFAULT_INDEX_FIT_CLASS == 3){
	    tv = (min_c + max_c) / 2.0;
	}

	for (i=0;i<number_examples_total;i++){
	    ee = (Example) examples.elementAt(i);
	    ee.complete_normalized_class(tv, min_c, max_c);
	}

	// completing operator

	for (i=0;i<number_examples_total;i++){
	    ee = (Example) examples.elementAt(i);
	    ee.complete_operator();
	}
	
	System.out.print("\nFeatures + Examples loaded --- First example : \n");
	if (SAVE_MEMORY)
	    ((Example) examples.elementAt(0)).affiche_operator();
	else
	    ((Example) examples.elementAt(0)).affiche_initial();
	System.out.println("");
	
	if (SAVE_MEMORY){
	    for (j=0;j<number_examples_total;j++)
		((Example) examples.elementAt(j)).saveMemory();
	}

	//EXAMPLES TESTED

	safeCheck();

	if (Debug) System.out.println("Found " + examples.size() + " examples");
    }

    public static double MIN_PROP = 0.01;
    public void safeCheck(){
	if (getProportionExamplesSign(true) < MIN_PROP)
	    Dataset.perror("The proportion of positive examples is " + getProportionExamplesSign(true) + " < " + MIN_PROP);
	if (getProportionExamplesSign(false) < MIN_PROP)
	    Dataset.perror("The proportion of negative examples is " + getProportionExamplesSign(false) + " < " + MIN_PROP);
    }


    public double getProportionExamplesSign(boolean positive){
	int i;
	double cc, tot = 0.0;
	for (i=0;i<number_examples_total;i++){
	    cc = ((Example) examples.elementAt(i)).normalized_class;
	    if ( (positive) && (cc >= 0.0) )
		tot++;
	    else if ( (!positive) && (cc < 0.0) )
		tot++;
	}
	tot /= (double) number_examples_total;
	return tot;
    }

    public double getProportionExamplesSign(Vector v, boolean positive){
	int i;
	double cc, tot = 0.0;
	for (i=0;i<v.size();i++){
	    cc = ((Example) examples.elementAt(((Integer) v.elementAt(i)).intValue())).normalized_class;
	    if ( (positive) && (cc >= 0.0) )
		tot++;
	    else if ( (!positive) && (cc < 0.0) )
		tot++;
	}
	tot /= (double) number_examples_total;
	return tot;
    }

    public boolean isNonTrivial(Vector v, int nf){
	// Checks that for any feature f, there exists examples with f * y > 0 and f * y < 0
	// otherwise the classification problem is trivial and no reason for boosting

	int i, f;
	double vc, fc, cc;
	int np = 0, nn = 0; 

	boolean okcheck;
	f = 0;
	do{
	    np = nn = 0;
	    okcheck = false;
	    i = 0;
	    do{
		fc = ((Example) examples.elementAt(((Integer) v.elementAt(i)).intValue())).operator[f];
		cc = ((Example) examples.elementAt(((Integer) v.elementAt(i)).intValue())).normalized_class;
		if (fc * cc > 0.0)
		    np++;
		if (fc * cc < 0.0)
		    nn++;
		if ( (np > 0) && (nn > 0) )
		    okcheck = true;
		i++;
	    }while ( (i < v.size()) && (!okcheck) );

	    //CHECK
	    if (!okcheck)
		System.out.println("Trivial feature " + f);

	    if (!okcheck)
		return false;
	    f++;
	}while( (f < nf) && (okcheck = true) );
	return true;
    }

    public void generate_stratified_sample_with_check(int dim){
	// stratifies sample and checks that each training sample has at least one example of each class sign & samples are non trivial
	boolean check_ok = true;
	int i;
	Vector cts;
	double v;

	do{
	    if (Debug) System.out.print("Checking that each fold has at least one example of each class & is non trivial (no variable with edges of the same sign) ");
	    check_ok = true;
	    generate_stratified_sample();
	    i = 0;
	    do{
		cts = (Vector) training_sample.elementAt(i);
		v = getProportionExamplesSign(cts, true);
		if ( (v == 0.0) || (v == 1.0) )
		    check_ok = false;

		if (check_ok)
		    check_ok = isNonTrivial(cts, dim);

		i++;
		if (Debug) System.out.print(".");
	    }while ( (i < training_sample.size()) && (check_ok) );
	    if (Debug && check_ok) System.out.println("ok.");
	    if (Debug && !check_ok) System.out.println("\nBad fold# " + (i-1) + " Retrying");
	}while(!check_ok);
    }

    public void generate_stratified_sample(){
	Vector all = new Vector();
	Vector all2 = new Vector();
	Vector dumv, dumvtr, dumvte, refv;
	Random r = new Random();
	int indexex = 0, indexse = 0;
	stratified_sample = new Vector();
	int i, ir, j, k;

	for (i=0;i<number_examples_total;i++)
	    all.addElement(new Integer(i));

	do{
	    if (all.size() > 1)
		ir = r.nextInt(all.size());
	    else
		ir = 0;

	    all2.addElement((Integer) all.elementAt(ir));
	    all.removeElementAt(ir);
	}while(all.size()>0);

	for (i=0;i<Dataset.NUMBER_STRATIFIED_CV;i++)
	    stratified_sample.addElement(new Vector());

	do{
	    dumv = (Vector) stratified_sample.elementAt(indexse);
	    dumv.addElement((Integer) all2.elementAt(indexex));
	    indexex++;
	    indexse++;
	    if (indexse >= Dataset.NUMBER_STRATIFIED_CV)
		indexse = 0;
	}while (indexex < number_examples_total);

	training_sample = new Vector();
	test_sample = new Vector();

	for (i=0;i<Dataset.NUMBER_STRATIFIED_CV;i++){
	    dumvtr = new Vector();
	    dumvte = new Vector();
	    for (j=0;j<Dataset.NUMBER_STRATIFIED_CV;j++){
		dumv = (Vector) stratified_sample.elementAt(j);
		if (j==i)
		    refv = dumvte;
		else
		    refv = dumvtr;
		for (k=0;k<dumv.size();k++)
		    refv.addElement((Integer) dumv.elementAt(k));
	    }
	    training_sample.addElement(dumvtr); 
	    test_sample.addElement(dumvte);
	}
    }

    public int train_size(int fold){
	return ((Vector) training_sample.elementAt(fold)).size();
    }

    public int test_size(int fold){
	return ((Vector) test_sample.elementAt(fold)).size();
    }

    public Example train_example(int fold, int nex){
	return (Example) examples.elementAt(((Integer) ((Vector) training_sample.elementAt(fold)).elementAt(nex)).intValue());
    }

    public Example test_example(int fold, int nex){
	return  (Example) examples.elementAt(((Integer) ((Vector) test_sample.elementAt(fold)).elementAt(nex)).intValue());
    }

    public double averageTrain_size(){
	int i;
	double val = 0.0;
	for (i=0;i<NUMBER_STRATIFIED_CV;i++)
	    val += (double) train_size(i);
	val /= (double) NUMBER_STRATIFIED_CV;
	return val;
    }

    public double xstar(int fold, int nv, int maxIndex){

	double val = 0.0, cv, ucv;
	Example ce;
	int i, maxi;
	if (maxIndex == -1)
	    maxi = train_size(fold);
	else
	    maxi = maxIndex;

	for (i=0;i<train_size(fold);i++){
	    ce = train_example(fold, i);

	    ucv = ce.operator[nv];
	    cv = Math.abs(ucv);

	    if ( (i==0) || (cv > val) ){
		val = cv;
	    }
	}
	return val;
    }
}

/**************************************************************************************************************************************
 * Class Rado
 *****/

class Rado implements Debuggable{
    int domain_id;
    double [] operator;
    // features, name matches the examples

    double [] rademacher;
    // contains the assignation of Rademacher variables that lead to this Rado
    // NOT BUILT IF SAVE_MEMORY = true

    public static double min(double [] vect){
	double tv = 0.0;
	int i;
	for (i=0;i<vect.length;i++)
	    if ( (i==0) || (vect[i] < tv) )
		tv = vect[i];
	return tv;
    }

    public static double max(double [] vect){
	double tv = 0.0;
	int i;
	for (i=0;i<vect.length;i++)
	    if ( (i==0) || (tv < vect[i]) )
		tv = vect[i];
	return tv;
    }

    public static double mean(double [] vect){
	double tv = 0.0;
	int i;
	for (i=0;i<vect.length;i++)
	    tv += vect[i];
	tv /= (double) vect.length;
	return tv;
    }

    public static double stddev(double [] vect, double mean){
	double tv = 0.0;
	int i;
	for (i=0;i<vect.length;i++)
	    tv += ( (vect[i] - mean) * (vect[i] - mean) );
	tv /= (double) (vect.length - 1);
	tv = Math.sqrt(tv);
	return tv;
    }

    public static boolean outliers(double [] vect, double stddev, double coeff){
	double tv = 0.0;
	int i = 0;
	do{
	    if (Math.abs(vect[i]) > coeff * stddev)
		return true;
	    i++;
	}while (i<vect.length);
	return false;
    }

    public static double median(double [] vect){
	double [] cp = new double[vect.length];
	double tv = 0.0;
	int i;
	for (i=0;i<cp.length;i++)
	    cp[i] = vect[i];
	QuickSort.quicksort(cp);
	tv = cp[cp.length/2];
	return tv;
    }

    public String toString(){
	String val = "";
	int i;

	if (!SAVE_MEMORY){
	    val += "Rademacher assignments : ";
	    for (i=0;i<rademacher.length;i++)
		val += rademacher[i] + " ";
	}

	val += "\nFeatures : ";
	for (i=0;i<operator.length;i++)
	    val += operator[i] + " ";

	return val;
    }

    public static double L2(double [] vect){
	double val = 0.0;
	int i;
	for (i=0;i<vect.length;i++){
	    val += (vect[i] * vect[i]);
	}
	return Math.sqrt(val);
    }

    public static double supportEPS(double [] vect){
	double val = 0.0;
	int i;
	for (i=0;i<vect.length;i++){
	    if (Math.abs(vect[i]) > EPS)
		val += 1.0;
	}
	val /= (double) vect.length;

	return val;
    }

    public static double [] copyOf(double [] vect){
	double [] val = new double [vect.length];
	int i;
	for (i=0;i<vect.length;i++){
	    val[i] = vect[i];
	}
	return val;
    }

    public static double [] add(double [] feat1, double [] feat2){
	if (feat1.length != feat2.length)
	    Dataset.perror("Rado.class :: Dimension mismatch : " + feat1.length + " != " + feat2.length);
	double [] val = new double [feat1.length];
	int i;
	for (i=0;i<feat1.length;i++){
	    val[i] = feat1[i] + feat2[i];
	}
	return val;
    }

    public static double [] times(double [] feat, double v){
	double [] val = new double [feat.length];
	int i;
	for (i=0;i<feat.length;i++){
	    val[i] = feat[i] * v;
	}
	return val;
    }

    public static double [] fromLabels(Dataset ds, double [] rade, double [] magnitude, int fold){
	double [] dumf = new double [ds.number_real_features];
	double [] dumfs = new double [ds.number_real_features];
	int i, j;

	for (i=0;i<ds.number_real_features;i++)
	    dumfs[i] = 0.0;

	double [] dumex;
	double radclass;
	for (i=0;i<ds.train_size(fold);i++){
	    dumex = ds.train_example(fold,i).normalized_real_features;
	    radclass = rade[i] * magnitude[i];
	    dumf = Rado.times(dumex, radclass);
	    dumfs = Rado.add(dumfs, dumf);
	}
	return dumfs;
    }

    Rado(int id, Dataset ds, double [] rade, int fold, Rado radmeano){
	domain_id = id;
	double [] mag = new double[ds.train_size(fold)];
	double [] meano = radmeano.operator;
	double [] ope2;
	int i;
	for (i=0;i<ds.train_size(fold);i++){
	    mag[i] = Math.abs(ds.train_example(fold,i).normalized_class);
	}
	ope2 = fromLabels(ds, rade, mag, fold);

	operator = Rado.add(ope2, meano);

	if (!SAVE_MEMORY)
	    rademacher = rade;
    }
    
    Rado(int id, Dataset ds, int fold){
	domain_id = id;
	
	double [] rade = new double[ds.train_size(fold)];
	double [] mag = new double[ds.train_size(fold)];
	int i;
	for (i=0;i<ds.train_size(fold);i++){
	    rade[i] = Math.signum(ds.train_example(fold,i).normalized_class);
	    mag[i] = Math.abs(ds.train_example(fold,i).normalized_class);
	}
	operator = fromLabels(ds, rade, mag, fold);

	if (!SAVE_MEMORY)
	    rademacher = rade;
    }
}


/**************************************************************************************************************************************
 * Class Example
 *****/



class Example implements Debuggable{
    int domain_id;

    Vector initial_features;
    double initial_class;

    double [] unnormalized_real_features;
    double unnormalized_class;

    double [] normalized_real_features;
    double [] operator;
    // normalized_real_features times the normalized_class

    double normalized_class;

    public void affiche_initial(){
	int i;
	for (i=0;i<initial_features.size();i++)
	    System.out.print((String) initial_features.elementAt(i) + " ");
	System.out.println(" -> " + initial_class);
    }

    public void affiche_operator(){
	int i;
	for (i=0;i<operator.length;i++)
	    System.out.print(operator[i] + " ");
	System.out.println(" -> " + initial_class);
    }

    public void affiche(){
	int i;
	System.out.print(domain_id + " Normalized features : ");
	for (i=0;i<normalized_real_features.length;i++)
	    System.out.print(normalized_real_features[i] + " ");
	System.out.println(" -> " + normalized_class);

	System.out.print(domain_id + " Operator : ");
	for (i=0;i<operator.length;i++)
	    System.out.print(operator[i] + " ");
	System.out.println(" -> " + normalized_class);
    }

    public void saveMemory(){
	initial_features = null;
	unnormalized_real_features = null;
    }

    static Vector TO_FEATURES(Vector ev, int index_class){
	Vector v = new Vector();
	int i;
	for (i=0;i<ev.size();i++)
	    if (i!=index_class)
		v.addElement(ev.elementAt(i));
	return v;
    }

    static double VAL_CLASS(Vector ev, int index_class){
	return Double.parseDouble(((String) ev.elementAt(index_class)));
    }

    static double[] TO_REAL_FEATURES(Example e, Vector fv, int index_class){
	Vector v = e.initial_features, v_map = new Vector();
	Feature f;
	String ve, vf;
	double [] ret;
	int i, ss = 0, j, reffeat = 0;
	
	for (i=0;i<fv.size();i++){
	    if (i!=index_class){
		f = (Feature) fv.elementAt(i);
		if (f.type.equals(Feature.CONTINUOUS)){
		    if (reffeat >= e.initial_features.size())
			System.out.println(e.domain_id + " -- " + i + " >= " + e.initial_features.size());

		    v_map.addElement(new Double(Double.parseDouble((String) e.initial_features.elementAt(reffeat))));
		    ss += 1;
		}if (f.type.equals(Feature.NOMINAL)){
		    ve = (String) e.initial_features.elementAt(reffeat);
		    for (j=0;j<f.modalities.size();j++)
			if (ve.equals((String) f.modalities.elementAt(j)))
			    v_map.addElement(new Double(Feature.MODALITY_PRESENT));
			else
			    v_map.addElement(new Double(Feature.MODALITY_ABSENT));
		    ss += f.modalities.size();
		}
		reffeat++;
	    }
	}

	ret = new double [ss];
	for (i=0;i<v_map.size();i++)
	    ret[i] = ((Double) v_map.elementAt(i)).doubleValue();
	return ret;
    }

    Example(int id, Vector v, int index_class){
	domain_id = id;
	initial_features = Example.TO_FEATURES(v, index_class);
	initial_class = Example.VAL_CLASS(v, index_class);
	unnormalized_class = initial_class;
    }

    public double [] add(double [] feat){
	return Rado.add(feat, normalized_real_features);
    }

    public double [] times(double v){
	return Rado.times(normalized_real_features, v);
    }

    public void printExample(){
	int i;

	System.out.println("Ex. " + domain_id);
	if ( (initial_features != null) && (initial_features.size()>0) ){
	    for (i=0;i<initial_features.size();i++)
		System.out.print(initial_features.elementAt(i) + " ");
	    System.out.println(" -- " + initial_class);
	}
	if ( (unnormalized_real_features != null) && (unnormalized_real_features.length>0) ){
	    for (i=0;i<unnormalized_real_features.length;i++)
		System.out.print(unnormalized_real_features[i] + " ");
	    System.out.println(" -- " + unnormalized_class);
	}
	if ( (normalized_real_features != null) && (normalized_real_features.length>0) ){
	    for (i=0;i<normalized_real_features.length;i++)
		System.out.print(normalized_real_features[i] + " ");
	    System.out.println(" -- " + normalized_class);
	}
	System.out.println("\n");
    }

    public void complete_unnormalized(Vector fv, int index_class){
	unnormalized_real_features = Example.TO_REAL_FEATURES(this, fv, index_class);
    }

    public void complete_normalized_features(Vector min_max, double [] avg, double [] stddev, boolean[] normalize){
	if (unnormalized_real_features.length != min_max.size())
	    Dataset.perror("Dimension mismatch between lengths of unnormalized_real_features and min_max " + unnormalized_real_features.length + " != " + min_max.size());
	if (normalize.length != min_max.size())
	    Dataset.perror("Dimension mismatch between lengths of normalize and min_max " + normalize.length + " != " + min_max.size());

	int i;
	double v, mi, ma, p;
	Vector mima;
	normalized_real_features = new double[unnormalized_real_features.length];

	for (i=0;i<unnormalized_real_features.length;i++){
	    v = unnormalized_real_features[i];
	    if (normalize[i] == false)
		normalized_real_features[i] = v;
	    else{
		mima = (Vector) min_max.elementAt(i);
		mi = ((Double) mima.elementAt(0)).doubleValue();
		ma = ((Double) mima.elementAt(1)).doubleValue();
		
		
		normalized_real_features[i] = Dataset.CENTER(v, avg[i], stddev[i]);
	    }
	}

	unnormalized_real_features = null;
    }

    public void complete_operator(){
	//computing the operator
	operator = new double [normalized_real_features.length];
	int i;

	for (i=0;i<normalized_real_features.length;i++){
	    operator[i] = normalized_real_features[i] * normalized_class;
	}
    }

    public void complete_normalized_class(double translate_v, double min_v, double max_v){
	// TODO Expand for more choices

	if ( (Dataset.DEFAULT_INDEX_FIT_CLASS != 0) && (Dataset.DEFAULT_INDEX_FIT_CLASS != 3) && (Dataset.DEFAULT_INDEX_FIT_CLASS != 4) )
	    Dataset.perror("Example.class :: Choice " + Dataset.DEFAULT_INDEX_FIT_CLASS + " not implemented to fit the class");

	normalized_class = Dataset.TRANSLATE_SHRINK(unnormalized_class, translate_v, min_v, max_v, Feature.MAX_CLASS_MAGNITUDE);
	if (Dataset.DEFAULT_INDEX_FIT_CLASS == 4)
	    normalized_class = Math.signum(normalized_class);
	unnormalized_class = 0.0;
    }

    // Classifier related methods

    public double output(double [] theta){
	int i;
	double val = 0.0;
	if (normalized_real_features.length != theta.length)
	    Dataset.perror("Example :: dimension mismatch " + normalized_real_features.length + " != " + theta.length + "\n");
	for (i=0;i<theta.length;i++)
	    val += (normalized_real_features[i] * theta[i]);
	return val;
    }

    public boolean rightClass(double [] theta){
	Random r = new Random();
	boolean br;
	double predicted;

	double res = output(theta);
	if (res != 0.0)
	    predicted = Math.signum(res);
	else{
	    br = r.nextBoolean();
	    if (br)
		predicted = 1.0;
	    else
		predicted = -1.0;
	}

	return ( (predicted * normalized_class) > 0.0);
    }
}



/**************************************************************************************************************************************
 * Class Feature
 *****/



class Feature implements Debuggable{
    
    public static String NOMINAL = "NOMINAL", CONTINUOUS = "CONTINUOUS", CLASS = "CLASS";

    public static String TYPE[] = {Feature.NOMINAL, Feature.CONTINUOUS, Feature.CLASS};
    public static boolean MODALITIES[] = {true, false, false};
    public static int CLASS_INDEX = 2;

    public static double MODALITY_PRESENT = 1.0, MODALITY_ABSENT = 0.0;

    public static boolean NORMALIZE_REAL_FEATURES = true;
    public static double MINV = -1.0, MAXV = 1.0; // values used for normalization in [MINV, MAXV]

    public static double MAX_CLASS_MAGNITUDE = 1.0;

    String name;
    String type;
    Vector modalities;

    static int INDEX(String t){
	int i = 0;
	do {
	    if (t.equals(TYPE[i]))
		return i;
	    i++;
	}while(i < TYPE.length);
	Dataset.perror("No type found for " + t);
	return -1;
    }

    static boolean HAS_MODALITIES(String t){
	return MODALITIES[Feature.INDEX(t)];
    }

    Feature(String n, String t, Vector m){
	name = n;
	type = t;
	modalities = null;

	if (Feature.HAS_MODALITIES(t))
	    modalities = m;
    }

    public String toString(){
	String v = "";
	int i;
	v += name + " -- " + type;
	if (Feature.HAS_MODALITIES(type)){
	    for (i=0;i<modalities.size();i++)
		v += " / " + modalities.elementAt(i);
	}
	return v;
    }
}
