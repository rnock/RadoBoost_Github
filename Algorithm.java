//Implementation of RadoBoost vs AdaBoost(SS, R)

import java.util.*;
import java.io.*;

class Algorithm implements Debuggable{
    
    Vector all_algorithms;
    Domain myDomain;

    Algorithm(Domain dom){
	all_algorithms = new Vector();
	myDomain = dom;
    }

    public static String ADABOOST = "@AdaBoost";
    public static String RADOBOOST = "@RadoBoost";
    public static String EXAMPLES_ALL = "@ALL", EXAMPLES_MAX = "@MAX", EXAMPLES_EXACTLY = "@EXACTLY";

    public void addAlgorithm(Vector all_params){
	String d = (String) all_params.elementAt(0), sRows;
	int nIter, nMu, nWeight, nMethod, nExact = -1, delta;

	nIter = Integer.parseInt((String) all_params.elementAt(1));
	nMu = Integer.parseInt((String) all_params.elementAt(2));
	nWeight = Integer.parseInt((String) all_params.elementAt(3));

	sRows = (String) all_params.elementAt(4);
	if ( !(sRows.equals(Algorithm.EXAMPLES_ALL)) && !(sRows.equals(Algorithm.EXAMPLES_MAX)) && !(sRows.equals(Algorithm.EXAMPLES_EXACTLY)) )
	    Dataset.perror("Algorithm :: Keyword " + sRows + " not authorized for number of examples/rados in resource file");
	
	if (sRows.equals(Algorithm.EXAMPLES_EXACTLY)){
	    nExact = Integer.parseInt((String) all_params.elementAt(5));
	    delta = 1;
	}else
	    delta = 0;

	if (d.equals(Algorithm.ADABOOST)){
	    all_algorithms.addElement(new LinearBoost(myDomain, d, nIter, nMu, nWeight, sRows, nExact));
	}else if (d.equals(Algorithm.RADOBOOST)){
	    if ( ( (delta == 0) && (all_params.size() == 6) ) || ( (delta == 1) && (all_params.size() == 7) ) )
		all_algorithms.addElement(new LinearBoost(myDomain, d, nIter, nMu, nWeight, sRows, nExact, (String) all_params.elementAt(5+delta)));
	    else
		all_algorithms.addElement(new LinearBoost(myDomain, d, nIter, nMu, nWeight, sRows, nExact, (String) all_params.elementAt(5+delta), Double.parseDouble((String) all_params.elementAt(6+delta))));
	}else
	    Dataset.perror("Algorithm :: Keyword " + d + " not authorized for algorithm in resource file");
    }

    public Vector go(){
	int i;
	Vector vcur, vret = new Vector();
	for (i=0;i<all_algorithms.size();i++){
	    vcur = ((LinearBoost) all_algorithms.elementAt(i)).boost();
	    vret.addElement(vcur);
	}
	return vret;
    }

    public void save(Vector v){
	Calendar cal = Calendar.getInstance();

	String now = cal.get(Calendar.DAY_OF_MONTH) + "th__" + cal.get(Calendar.HOUR_OF_DAY) + "h_" + cal.get(Calendar.MINUTE) + "m_" + cal.get(Calendar.SECOND) + "s";
	String nameSave = myDomain.myDS.pathSave + "results_" + now + ".txt";
	int i, j, k;
	Vector vi, vj;

	FileWriter f = null;

	int smax = 7;
	// 0. Tmax
	// 1. besti
	// 2. err_emp_init
	// 3. min_err_emp
	// 4. get_err_test
	int index_err_test = 4;
	// 5. l2
	// 6. supporteps
	

	String [] names = {"Tmax            ", "BestIter          ", "Err_Emp_Init (*100)", "Min_Err_Emp (*100)", "Err_Test (*100)    ", "L2_Norm          ", "Support (*100) (%)   "};

	double [] entries;
	double [] entries2;
	double [] as;

	double tp;

	String latexNames = "", latexData = "";

	int out3 = 0, out10 = 0, out100 = 0;
	for (i=0;i<myDomain.myDS.outlier_features_3s.length;i++){
	    if (myDomain.myDS.outlier_features_3s[i])
		out3++;
	    if (myDomain.myDS.outlier_features_10s[i])
		out10++;
	    if (myDomain.myDS.outlier_features_100s[i])
		out100++;
	}

	try{
	    f = new FileWriter(nameSave);
	    f.write("%Domain " + myDomain.myDS.domainName + " with classes centered wrt " + Dataset.FIT_CLASS_MODALITIES[Dataset.DEFAULT_INDEX_FIT_CLASS] + "\n");
	    f.write("%Proportion of sign(+/-) in classes : \t" + DF.format(myDomain.myDS.getProportionExamplesSign(true)) + "/" + DF.format(myDomain.myDS.getProportionExamplesSign(false)) + "\n");
	    f.write("%Domain #examples : \t\t\t" + myDomain.myDS.number_examples_total + "\n");
	    f.write("%Domain #features : \t\t\t" + myDomain.myDS.number_initial_features + "\n");
	    f.write("%features (3*sigma) outliers   : \t" + out3 + "/" + myDomain.myDS.outlier_features_3s.length + "\n");
	    f.write("%features (10*sigma) outliers  : \t" + out10 + "/" + myDomain.myDS.outlier_features_3s.length + "\n");
	    f.write("%features (100*sigma) outliers : \t" + out100 + "/" + myDomain.myDS.outlier_features_3s.length + "\n\n");

	    f.write("% Single algorithms statistics ::\n");
	    for (i=0;i<all_algorithms.size();i++){
		latexNames += ((LinearBoost) all_algorithms.elementAt(i)).name + " & ";

		f.write("%Algo_" + i + " = " + ((LinearBoost) all_algorithms.elementAt(i)).fullName() + "\n");
		vi = (Vector) v.elementAt(i);
		if (vi.size() != NUMBER_STRATIFIED_CV)
			Dataset.perror("not the right statistics in vector " + i);
		for (j=0;j<smax;j++){
		    entries = new double[NUMBER_STRATIFIED_CV];
		    for (k=0;k<NUMBER_STRATIFIED_CV;k++)
			entries[k] = ((Double) ((Vector) vi.elementAt(k)).elementAt(j)).doubleValue();
		    as = new double[2];
		    Statistics.avestd(entries, as);
		    f.write(names[j] + "\t" + DF.format(as[0]) + "\t\\pm\t" + DF.format(as[1]) + "\n");

		    if (j==index_err_test)
			latexData += DF0.format(as[0]) + "$\\pm$" + DF0.format(as[1]) + " & ";
		}
		f.write("\n");
	    }

	    f.write("\n% Test errs paired t-tests ::\n");
	    for (i=0;i<all_algorithms.size()-1;i++)
		for (j=i+1;j<all_algorithms.size();j++){
		    f.write("%Algo_" + i + " = " + ((LinearBoost) all_algorithms.elementAt(i)).fullName() + "\n");
		    f.write("%Algo_" + j + " = " + ((LinearBoost) all_algorithms.elementAt(j)).fullName() + "\n");

		    vi = (Vector) v.elementAt(i);
		    vj = (Vector) v.elementAt(j);
		    
		    entries = new double[NUMBER_STRATIFIED_CV];
		    entries2 = new double[NUMBER_STRATIFIED_CV];

		    for (k=0;k<NUMBER_STRATIFIED_CV;k++){
			entries[k] = ((Double) ((Vector) vi.elementAt(k)).elementAt(index_err_test)).doubleValue(); //ERR TEST
			entries2[k] = ((Double) ((Vector) vj.elementAt(k)).elementAt(index_err_test)).doubleValue(); //ERR TEST
		    }

		    tp = Statistics.tptest(entries, entries2);
		    f.write("P-val = " + tp + "\n\n");
		}
	    f.write("\n\nStrings for Latex File : \n" + latexNames + "\n" + latexData + "\n");
	    f.close();
	}catch(IOException e){
	    Dataset.perror("LinearBoost.class :: Saving results error in file " + nameSave);
	}
    }
}
