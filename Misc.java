//Contains all miscellaneous classes for RadoBoost

import java.text.DecimalFormat;

interface Debuggable {
    static DecimalFormat DF = new DecimalFormat("#0.0000");
    static DecimalFormat DF0 = new DecimalFormat("#0.00");
    static DecimalFormat DF2 = new DecimalFormat("#0.000000");

    static boolean Debug = false;

    static boolean SAVE_MEMORY = true;

    static double EPS = 1E-4;

    static double EPS2 = 1E-6;

    public static int NUMBER_STRATIFIED_CV = 10;
}
