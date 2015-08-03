//Linear Radoboost and AdaBoost(R, SS)

import java.util.*;
import java.io.*;

public class LinearBoost implements Debuggable{
    public static int MAX_BOOSTING_ITERS = 100;

    public static String [] METHOD_WEIGHTS_STRING = {"AdaBoost.R", "Real Adaboost (S.S.)"};
    public static  double SCALE_MU_THRESHOLD = 0.1; //RAD-C2
    public static String [] METHOD_MU_STRING = {"Edge used as is", "Edge scaled so its |.| is >= " + SCALE_MU_THRESHOLD + " "};
    public static double MAX_ABS_MU = 0.8;

    Domain myDomain;
    
    int split_number;

    double [] theta;
    double [] weights;
    double [] star;

    int methodWeights, nIterMax, methodMu, nRows, nExact; //nExact is the "ideal" value if specified of the number of examples
    String methodRows, methodRados, name;

    boolean isRadoType;
    // true iff of RadoBoost type

    double epsilon;
    // differential privacy epsilon

    LinearBoost(Domain dom, String type, int nIter, int nMu, int nWeight, String sRows, int nEx){
	myDomain = dom;
	if (type.equals(Algorithm.RADOBOOST))
	    isRadoType = true;
	else if (type.equals(Algorithm.ADABOOST))
	    isRadoType = false;
	else
	    Dataset.perror("Keyword " + type + " not authorized for algorithm in resource file");

	name = type;

	nIterMax = nIter;
	methodWeights = nWeight;
	methodMu = nMu;
	methodRows = sRows;
	
	methodRados = null;
	nRows = -1;
	nExact = nEx;

	weights =null;

	epsilon = -1.0;
    }

    public String fullName(){
	String ret = name;
	if (methodRados != null)
	    ret += "(" + methodRados + ", " + epsilon + ")";
	ret += " - Iter = " + nIterMax + ", weights = " + METHOD_WEIGHTS_STRING[methodWeights] + ", mu = " + METHOD_MU_STRING[methodMu] + ", methodRows = " + methodRows + ", nRowsLearning = " + nRows + ", nExact = " + nExact;
	return ret;
    }

    LinearBoost(Domain dom, String type, int nIter, int nMu, int nWeight, String sRows, int nExact, String sRados){
	this(dom, type, nIter, nMu, nWeight, sRows, nExact);
	methodRados = sRados;

	if (Domain.INDEX_GENERATE(methodRados) == 1){
	    dom.compute_dp_feature();
	}
    }


    LinearBoost(Domain dom, String type, int nIter, int nMu, int nWeight, String sRows, int nExact, String sRados, double dd){
	this(dom, type, nIter, nMu, nWeight, sRows, nExact, sRados);
	
	if (Domain.INDEX_GENERATE(methodRados) == 1){
	    epsilon = dd;
	    if (dd < 0.0)
		Dataset.perror("Negative value for epsilon = " + epsilon);
	}
    }

    // display methods

    public String theta(){
	String val = "";
	int i;
	val += ("Theta = ");
	for (i=0;i<theta.length;i++)
	    val += (DF.format(theta[i]) + " ");
	return val;
    }

    // Boosting algorithms

    public void initRound(int split){
	theta = new double [myDomain.myDS.number_real_features];
	star = new double [myDomain.myDS.number_real_features];

	System.out.print(" * " + name + " > Starting fold " + (split+1) + "/" + NUMBER_STRATIFIED_CV + " -- ");
	split_number = split;	

	if (methodRows.equals(Algorithm.EXAMPLES_ALL))
	    nRows = myDomain.myDS.train_size(split_number);
	else if (methodRows.equals(Algorithm.EXAMPLES_MAX)){
	    // [RAD-C1] Here, controls that the number of rados generated does not exceed max{train/2, 1000}
	    if (myDomain.maxRados > myDomain.myDS.train_size(split_number) / 2)
		nRows = myDomain.myDS.train_size(split_number) / 2;
	    else
		nRows = myDomain.maxRados;
	}else if (methodRows.equals(Algorithm.EXAMPLES_EXACTLY)){
	    if (nExact > myDomain.myDS.train_size(split_number))
		nRows = myDomain.myDS.train_size(split_number);
	    else
		nRows = nExact;
	}else
	    Dataset.perror("initRound :: unauthorized value " + methodRows);

	int i, ne = myDomain.myDS.train_size(split_number), nr;
	for (i=0;i<theta.length;i++){
	    theta[i] = 0.0;
	}

	if (isRadoType){
	    myDomain.rado_generate(split_number, methodRados, epsilon);
	    nr = myDomain.rados.size();
	    weights = new double [nr];
	    for (i=0;i<nr;i++)
		weights[i] = 1.0 / ((double) nr);
	    for (i=0;i<theta.length;i++){
		star[i] = myDomain.radostar(i);
	    }
	}else{
	    weights = new double [ne];
	    for (i=0;i<ne;i++){
		weights[i] = 1.0 / ((double) ne);
	    }
	    for (i=0;i<theta.length;i++)
		star[i] = myDomain.myDS.xstar(split_number, i, -1);
	}
    }

    public Vector boost(){
	int i;
	Vector cur, res = new Vector();

	for (i=0;i<NUMBER_STRATIFIED_CV;i++){
	    initRound(i);
	    cur = iterate();
	    res.addElement(cur);
	}
	System.out.println("");

	if (SAVE_MEMORY)
	    theta = star = weights = null;

	return res;
    }

    public Vector iterate(){

	Vector ret = new Vector();
	// 0. Tmax
	// 1. besti
	// 2. err_emp_init
	// 3. min_err_emp
	// 4. get_err_test
	// 5. l2
	// 6. supporteps

	int i, besti = -1, Tmax = 0;
	double err_emp, min_err_emp = -1.0, err_test, get_err_test = -1.0, l2, supporteps, err_emp_init;
	double [] bestTheta = null;
	boolean improvement;

	err_emp_init = error(true);

	for (i=0;i<nIterMax;i++){
	    improvement = one_iteration();
	    err_emp = error(true);

	    if ( (i==0) || (err_emp <= min_err_emp) ){
		err_test = error(false);
		bestTheta = Rado.copyOf(theta);
		min_err_emp = err_emp;
		get_err_test = err_test;
		besti = i;
	    }

	    if (i%(nIterMax/10) == 0){
		System.out.print( ((i/(nIterMax/10)) * 10) + "% ");
		if (Domain.SAVE_MEMORY)
		    System.out.print(myDomain.memString() + " ");
	    }

	    Tmax = (i+1);
	    if (!improvement)
		i = nIterMax;
	}
	l2 = Rado.L2(bestTheta);
	supporteps = Rado.supportEPS(bestTheta);

	System.out.println("ok. \t(perr err l2 sup) = (" + DF.format(min_err_emp) + " " + DF.format(get_err_test) + " " + DF.format(l2) + " " + DF.format(supporteps) + ")");

	ret.addElement(new Double(Tmax));
	ret.addElement(new Double((double) besti));
	ret.addElement(new Double(err_emp_init * 100.0));
	ret.addElement(new Double(min_err_emp * 100.0));
	ret.addElement(new Double(get_err_test * 100.0));
	ret.addElement(new Double(l2));
	ret.addElement(new Double(supporteps * 100.0));

	return ret;
    }

    public int wfi(){
	//implements Weak Feature Index oracle
	int i, iret = -1;
	double cur, mx = 0.0, curmu;
	for (i=0;i<myDomain.dimOperator;i++){
	    curmu = muBoost(i);
	    cur = Math.abs(curmu);

	    if ( (i==0) || (cur > mx) ){
		iret = i;
		mx = cur;
	    }
	}

	return iret;
    }

    public double muBoost(int nf){
	// computes edge mu using operators
	int index;
	double sum = 0.0, vv, sig;
	for (index=0;index<nRows;index++){
	    vv = (weights[index] * getOperator(index)[nf]);
	    sum += vv;
	}
	sum /= star[nf];

	if (Math.abs(sum) > 1.0)
	    Dataset.perror("Mu = " + sum + " > 1.0");
	if (Math.abs(sum) > LinearBoost.MAX_ABS_MU){
	    sum = Math.signum(sum) * LinearBoost.MAX_ABS_MU;
	}
	    
	return sum;
    }

    public double muClamp(double mu){
	if ( (methodMu == 1) && (Math.abs(mu) < LinearBoost.SCALE_MU_THRESHOLD) )
	    return (Math.signum(mu) * LinearBoost.SCALE_MU_THRESHOLD);
	else
	    return mu;
    }

    public boolean one_iteration(){
	// return true iff weights change, i.e. significant difference with update
	boolean sig;

	int nf = wfi();
	double mu = muClamp(muBoost(nf));
	double alpha = alphaBoost(mu, nf);

	theta[nf] += alpha;
	sig = reweightAll(mu, nf, alpha);
	return sig;
    }

    public double alphaBoost(double mu, int nf){
	double val = 0.0;
	double num = 1.0 + mu, den = 1.0 - mu;
	val = Math.log(num/den);
	val /= (2.0 * star[nf]);

	return val;
    }

    public boolean reweightAll(double mu, int nf, double alp){
	if (methodWeights == 1)
	    return reweightAll_AdaBoostSS(mu, nf, alp);
	else if (methodWeights == 0)
	    return reweightAll_AdaBoostR(mu, nf, alp);
	else
	    Dataset.perror("No weighting method for " + methodWeights);
	return false;
    }

    public void normalizeWeights(){
	int index;
	double w, sum = 0.0, diff;

	for (index=0;index<nRows;index++){
	    w = weights[index];
	    sum += w;
	}

	if (sum < EPS)
	    Dataset.perror("sum of weights is approximately zero (" + sum + ")");

	for (index=0;index<nRows;index++){
	    w = weights[index];
	    w /= sum;
	    weights[index] = w;
	}

	// double check
	sum = 0.0;
	for (index=0;index<nRows;index++)
	    sum += weights[index];
	
	diff = Math.abs(1.0 - sum);
	if (diff > EPS)
	    Dataset.perror("LinearBoost :: sum of weights " + sum + " != 1.0");
    }

    public boolean reweightAll_AdaBoostSS(double mu, int nf, double alp){
	int index, i;
	double op, w, sum = 0.0, diff;
	boolean improvement;

	for (index=0;index<nRows;index++){
	    w = weights[index];
	    op = getOperator(index)[nf];
	    w = (w * Math.exp(- alp * op));
	    weights[index] = w;
	}
	normalizeWeights();
	improvement = (Math.abs(alp) > EPS2);

	return improvement;
    }

    public boolean reweightAll_AdaBoostR(double mu, int nf, double alp){
	double lastw, neww, den, num, op, diffc, maxd = 0.0, w;
	int index, i;
	boolean improvement;

	den = 1.0 - (mu * mu);

	for (index=0;index<nRows;index++){
	    lastw = weights[index];
	    w = lastw;
	    if (lastw <= 0.0)
		Dataset.perror("LinearBoost :: weights " + w + " < 0.0");
	    op = getOperator(index)[nf];
	    num = 1.0 - ( (mu * op) / star[nf]);
	    w = w * (num / den);
	    neww = w;

	    diffc = Math.abs(lastw - neww);
	    if ( (index == 0) || (diffc > maxd) )
		maxd = diffc;
	    
	    weights[index] = w;
	}

	normalizeWeights();
	// kept for numerical imprecisions that otherwise may bring r_t out of bounds

	improvement = (maxd > 0.0);

	if (Math.abs(alp) < EPS2)
	    improvement = false;

	if (oneWeight())
	  improvement = false;
	
	return improvement;
    }

    public boolean oneWeight(){
	// returns true iff all weight concentrated on one example, indicating a possibly trivial feature
	double w, maxw = 0.0;
	int index;

	for (index=0;index<nRows;index++){
	    w = weights[index];
	    if ( (index == 0) || (maxw < w) )
		maxw = w;
	}
	if (maxw > 1.0 - EPS){
	    return true;
	}
	return false;
    }

    // BOOSTING MISC FUNCTIONS

    public double[] getOperator(int index){
	if (isRadoType)
	    return ((Rado) myDomain.rados.elementAt(index)).operator;
	return myDomain.myDS.train_example(split_number, index).operator;
    }

    //Computes the error

    public double error(boolean onTraining){
	Example ee;
	double sumerr = 0.0;
	boolean rc;
	int i, ne;
	if (onTraining)
	    ne = myDomain.myDS.train_size(split_number);
	else
	    ne = myDomain.myDS.test_size(split_number);

	for (i=0;i<ne;i++){
	    if (onTraining)
		ee = myDomain.myDS.train_example(split_number, i);
	    else
		ee = myDomain.myDS.test_example(split_number, i);
	    rc = ee.rightClass(theta);
	    if (!rc)
		sumerr += 1.0;
	}
	sumerr /= (double) ne;

	return sumerr;
    }

    // Saving results stuff
}


/**************************************************************************************************************************************
 * Class Domain
 *****/

class Domain implements Debuggable{
    public static int MAX_RADO_SIZE = 1000;
    public static double DP_EPSILON = 0.01;

    public MemoryMonitor myMemoryMonitor;

    Dataset myDS;
    Vector rados;

    int dimOperator, maxRados;

    int dp_feature;
    // feature for differential privacy on features

    public static String [] RADO_GENERATE = {"@RANDOM", "@DIFFPRIVK"};

    public static int INDEX_GENERATE(String s){
	int i=0;
	do{
	    if (RADO_GENERATE[i].equals(s))
		return i;
	    i++;
	}while (i<RADO_GENERATE.length);
	Dataset.perror("Rado generation method " + s + " not found");
	return -1;
    }

    Domain(String nameD, String nameP, int nrado){
	dp_feature = -1;

	myMemoryMonitor = new MemoryMonitor();

	myDS = new Dataset(nameD, nameP, this);
	myDS.load_features();
	myDS.load_examples();
	dimOperator = ((Example) myDS.examples.elementAt(0)).operator.length;
	myDS.generate_stratified_sample_with_check(dimOperator);

	if (nrado > 0)
	    maxRados = nrado;
	else
	    maxRados = MAX_RADO_SIZE;
    }

    public String memString(){
	return myMemoryMonitor.memString;
    }

    public void rado_generate(int fold, String method, double eps){
	int i, nt = 0, index = Domain.INDEX_GENERATE(method);
	boolean [] trivial;

	System.out.print("Generating " + maxRados + " c.rados --- method = " + Domain.RADO_GENERATE[index] + " ");

	do{
	    if (index == 0){
		rado_generate_dumb(fold);
		nt = -1;
	    }else if (index == 1){
		if (eps == -1.0)
		    eps = Domain.DP_EPSILON;

		rado_generate_differential_privacy_feature(fold, eps);
		nt = -1;
	    }
	}while(nt>0);
    }

    public void rado_generate_dumb(int fold){

	Random r = new Random();
	rados = new Vector();

	Rado meano = new Rado(-1, myDS, fold);	
	int i, j;
	double [] rade = new double [myDS.train_size(fold)];
	Rado dumr;

	for (i=0;i<maxRados;i++){
	    for (j=0;j<myDS.train_size(fold);j++){
		if (r.nextBoolean())
		    rade[j] = 1.0;
		else
		    rade[j] = -1.0;
	    }

	    dumr = new Rado(i, myDS, rade, fold, meano);

	    rados.addElement(dumr);

	    if (i%(maxRados/10) == 0)
		System.out.print( ((i/(maxRados/10)) * 10) + "% ");
	}
	System.out.print("ok...  ");
    }

    public void compute_dp_feature(){
	Vector pick = new Vector();
	int i, j, numb = 0, diffmpc, diffmpmin = -1, ind = -1, pf = -1;
	double v;

	for (i=0;i<myDS.isbinaryfeature.length;i++)
	    if (myDS.isbinaryfeature[i]){
		numb++;
		pick.addElement(new Integer(i));
	    }
	if (numb == 0)
	    Dataset.perror("rado_generate_differential_privacy_feature :: Domain " + myDS.domainName + " does not contain binary features and is not eligible for diff_priv_binary");

	for (i=0;i<pick.size();i++){
	    ind = ((Integer) pick.elementAt(i)).intValue();
	    diffmpc = 0;
	    for (j=0;j<myDS.examples.size();j++){
		v = ((Example) myDS.examples.elementAt(j)).operator[ind];
		if (v > 0.0)
		    diffmpc++;
	    }
	    diffmpc = Math.abs((2 * diffmpc) - myDS.examples.size());

	    if ( (i==0) || (diffmpc < diffmpmin) ){
		pf = ind;
		diffmpmin = diffmpc;
	    }
	}

	dp_feature = pf;
    }

    public void rado_generate_differential_privacy_feature(int fold, double eps){

	Random r = new Random();

	// checking if applicable
	int i, j, numb = 0;

	double beta = 1.0/(1.0 + Math.exp(eps / 2.0));
	double mlim = ((1.0 + (2.0 * beta)) / (1.0 - (2.0 * beta)));
	if (mlim > (double) (myDS.number_examples_total * 9 / 10) )
	    Dataset.perror("rado_generate_differential_privacy_feature :: please choose a larger value for epsilon. Duplication of learning sample not implemented");

	int mplus = 0, m = myDS.train_size(fold);
	for (i=0;i<myDS.train_size(fold);i++)
	    if (myDS.train_example(fold, i).operator[dp_feature] > 0.0)
		mplus ++;
	
	double binf = -((double) m - (double) mplus) + (beta * ((double) m + 1.0));
	double bsup = mplus - (beta * ((double) m + 1.0));

	if (binf >= bsup)
	    Dataset.perror("rado_generate_differential_privacy_feature :: binf = " + binf + " >= bsup = " + bsup + "");

	rados = new Vector();

	Rado meano = new Rado(-1, myDS, fold);	
	double [] rade = new double [myDS.train_size(fold)];
	Rado dumr;

	boolean eligible = false;
	double valtest;

	for (i=0;i<maxRados;i++){
	    eligible = false;
	    do{
		for (j=0;j<myDS.train_size(fold);j++){
		    if (r.nextBoolean())
			rade[j] = 1.0;
		    else
			rade[j] = -1.0;
		}

		dumr = new Rado(i, myDS, rade, fold, meano);
		
		valtest = dumr.operator[dp_feature];
		if ( (valtest >= binf) && (valtest <= bsup) )
		    eligible = true;

	    }while(!eligible);

	    rados.addElement(dumr);

	    if (i%(maxRados/10) == 0)
		System.out.print( ((i/(maxRados/10)) * 10) + "% ");
	}
	System.out.print("ok...  ");
    }

    public double radostar(int nv){
	//returns the largest absolute magnitude in rado fold
	double val = 0.0, cv;
	Rado ce;
	int i;
	for (i=0;i<rados.size();i++){
	    ce = (Rado) rados.elementAt(i);
	    cv = Math.abs(ce.operator[nv]);
	    if ( (i==0) || (cv > val) )
		val = cv;
	}
	return val;
    }

    public void save_operator_and_class_examples(){
	String saveFileName = myDS.pathSave + myDS.domainName + "_domain.operator.txt";
	FileWriter f = null;
	int i, j;
	double [] curop;

	try{
	    f = new FileWriter(saveFileName);
	    f.write("// Domain + " + myDS.domainName + ", operators and classes in whole domain\n");
	    f.write("// Total #examples : " + myDS.number_examples_total + ", #features excl. class : " + myDS.number_initial_features + "\n");
	    for (i=0;i<myDS.number_examples_total;i++){
		curop = ((Example) myDS.examples.elementAt(i)).operator;
		for (j=0;j<curop.length;j++){
		    f.write(curop[j] + "");
		    f.write(Dataset.KEY_SEPARATION_STRING[Dataset.SEPARATION_INDEX]);
		}
		f.write(((Example) myDS.examples.elementAt(i)).normalized_class + "\n");
	    }
	    f.close();
	}catch(IOException e){
	    Dataset.perror("LinearAdoboost.class :: Saving examples/domain error in file " + saveFileName);
	}
    }

    public void safeCheck(){
	//does nothing
    }
}
