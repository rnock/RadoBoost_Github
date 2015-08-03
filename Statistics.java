import java.util.*;

// Borrowed from NR

public class Statistics implements Debuggable{
    public static int ngau = 18;

    static final int SWITCH=3000;

    static final double EPS   = 6E-8;
    static final double FPMIN = 1E-30;
    static int ASWITCH=100;
    static double gln;

    static final double [] yyy = {0.0021695375159141994,
				 0.011413521097787704,0.027972308950302116,0.051727015600492421,
				 0.082502225484340941, 0.12007019910960293,0.16415283300752470,
				 0.21442376986779355, 0.27051082840644336, 0.33199876341447887,
				 0.39843234186401943, 0.46931971407375483, 0.54413605556657973,
				 0.62232745288031077, 0.70331500465597174, 0.78649910768313447,
				 0.87126389619061517, 0.95698180152629142};


    static final double [] www = {0.0055657196642445571,
				  0.012915947284065419,0.020181515297735382,0.027298621498568734,
				  0.034213810770299537,0.040875750923643261,0.047235083490265582,
				  0.053244713977759692,0.058860144245324798,0.064039797355015485,
				  0.068745323835736408,0.072941885005653087,0.076598410645870640,
				  0.079687828912071670,0.082187266704339706,0.084078218979661945,
				  0.085346685739338721,0.085983275670394821};

    public static double gammln(double xx) {
        int j;
        double x,tmp,y,ser;
        double [] cof={57.1562356658629235,-59.5979603554754912,
			14.1360979747417471,-0.491913816097620199,.339946499848118887e-4,
			.465236289270485756e-4,-.983744753048795646e-4,.158088703224912494e-3,
			-.210264441724104883e-3,.217439618115212643e-3,-.164318106536763890e-3,
			.844182239838527433e-4,-.261908384015814087e-4,.368991826595316234e-5};
        if (xx <= 0) Dataset.perror("bad arg in gammln");
        y=x=xx;
        tmp = x+5.24218750000000000;
        tmp = (x+0.5)*Math.log(tmp)-tmp;
        ser = 0.999999999999997092;
        for (j=0;j<14;j++) ser += cof[j]/++y;
        return tmp+Math.log(2.5066282746310005*ser/x);
    }

    public static double betaiapprox(double a, double b, double x) {
	int j;
	double xu,t,sum,ans;
	double a1 = a-1.0, b1 = b-1.0, mu = a/(a+b);
	double lnmu=Math.log(mu),lnmuc=Math.log(1.-mu);
	t = Math.sqrt(a*b/((a+b)*(a+b)*(a+b+1.0)));
	if (x > a/(a+b)) {
	    if (x >= 1.0) return 1.0;
	    xu = Math.min(1.,Math.max(mu + 10.*t, x + 5.0*t));
	} else {
	    if (x <= 0.0) return 0.0;
	    xu = Math.max(0.,Math.min(mu - 10.*t, x - 5.0*t));
	}
	sum = 0;
	for (j=0;j<18;j++) {
	    t = x + (xu-x)*yyy[j];
	    sum += www[j]*Math.exp(a1*(Math.log(t)-lnmu)+b1*(Math.log(1-t)-lnmuc));
	}
	ans = sum*(xu-x)*Math.exp(a1*lnmu-gammln(a)+b1*lnmuc-gammln(b)+gammln(a+b));
	return ans>0.0? 1.0-ans : -ans;
    }

    public static double betacf(double a, double b, double x) {
	int m,m2;
	double aa,c,d,del,h,qab,qam,qap;
	qab=a+b;
	qap=a+1.0;
	qam=a-1.0;
	c=1.0;
	d=1.0-qab*x/qap;
	if (Math.abs(d) < FPMIN) d=FPMIN;
	d=1.0/d;
	h=d;
	for (m=1;m<10000;m++) {
	    m2=2*m;
	    aa=m*(b-m)*x/((qam+m2)*(a+m2));
	    d=1.0+aa*d;
	    if (Math.abs(d) < FPMIN) d=FPMIN;
	    c=1.0+aa/c;
	    if (Math.abs(c) < FPMIN) c=FPMIN;
	    d=1.0/d;
	    h *= d*c;
	    aa = -(a+m)*(qab+m)*x/((a+m2)*(qap+m2));
	    d=1.0+aa*d;
	    if (Math.abs(d) < FPMIN) d=FPMIN;
	    c=1.0+aa/c;
	    if (Math.abs(c) < FPMIN) c=FPMIN;
	    d=1.0/d;
	    del=d*c;
	    h *= del;
	    if (Math.abs(del-1.0) <= EPS) break;
	}
	return h;
    }

    public static double betai(double a, double b, double x) {
	double bt;
	if (a <= 0.0 || b <= 0.0) Dataset.perror("Bad a or b in routine betai");
	if (x < 0.0 || x > 1.0) Dataset.perror("Bad x in routine betai");
	if (x == 0.0 || x == 1.0) return x;
	if (a > SWITCH && b > SWITCH) return betaiapprox(a,b,x);
	bt=Math.exp(gammln(a+b)-gammln(a)-gammln(b)+a*Math.log(x)+b*Math.log(1.0-x));
	if (x < (a+1.0)/(a+b+2.0)) return bt*betacf(a,b,x)/a;
	else return 1.0-bt*betacf(b,a,1.0-x)/b;
    }

    public static void avevar(double [] data, double [] aveETvar) {
        double s,ep,ave,var;
        int j,n=data.length;
        ave=0.0;
        for (j=0;j<n;j++) ave += data[j];
        ave /= n;
        var=ep=0.0;
        for (j=0;j<n;j++) {
                s=data[j]-ave;
                ep += s;
                var += s*s;
        }
        var=(var-ep*ep/n)/(n-1);
	aveETvar[0] = ave;
	aveETvar[1] = var;
    }

    public static void avestd(double [] data, double [] aveETstd) {
        double s,ep,ave,std;
        int j,n=data.length;
        ave=0.0;
        for (j=0;j<n;j++) ave += data[j];
        ave /= n;
        std=ep=0.0;
        for (j=0;j<n;j++) {
                s=data[j]-ave;
                ep += s;
                std += s*s;
        }
        std=Math.sqrt((std-ep*ep/n)/(n-1));
	aveETstd[0] = ave;
	aveETstd[1] = std;
    }

    public static double tptest(double [] data1, double [] data2) {
	int j, n=data1.length;
	double var1,var2,ave1,ave2,sd,df,cov=0.0,prob, t;
	double avar1 [] = new double [2];
	double avar2 [] = new double [2];

	avevar(data1,avar1);
        avevar(data2,avar2);
	ave1 = avar1[0];
	var1 = avar1[1];
	ave2 = avar2[0];
	var2 = avar2[1];

	for (j=0;j<n;j++) cov += (data1[j]-ave1)*(data2[j]-ave2);
	cov /= (df=n-1);
	sd=Math.sqrt((var1+var2-2.0*cov)/n);
	t=(ave1-ave2)/sd;
	prob=Statistics.betai(0.5*df,0.5,df/(df+t*t));

	return prob;
    }

}
