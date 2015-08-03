****************************************************************

This is the implementation of RadoBoost following the paper:

R. Nock, G. Patrini and A. Friedman
“Rademacher Observations, Private Data and Boosting”
International Conference on Machine Learning, 2015

This program is provided as is, without any warranty whatsoever. 
Use it at your own risks, and enjoy :).

Questions: to R. Nock at cord.krichna@gmail.com

****************************************************************

** Datasets format

RadoBoost is provided with four UCI datasets. It should be clear
how to format a new dataset, just make two files, .data and .features
where you indicate the nature of each observed feature, AND which
one is the class (and also how you represent class values from the observed data).

** Executing the program

How to compile:	as any Java program, it should compile with a bare 
javac compiler.

How to run (example):	java -d64 -Xmx5000m Experiments -R resource_example1.txt,

where resource_example1.txt contains the parameters to run the algorithms.
The program indicates at run time the memory load. Should help to properly parameterise
a 64bits JVM.

Key parameters for this file are:

@MAX_RADO_SIZE,1000  —> fixes the number of rados to generate in each fold.
(Warning: the as written in the ICML paper, the code (in LinearBoost.java, tag “RAD-C1”) contains a
test that controls that the number of rados does not exceed the training size / 2.
Remove this test if you want a larger number of rados)

@ALGORITHM,XXX,YYY,Z,T,UUU[,VVV]
like in @ALGORITHM,@AdaBoost,1000,0,0,@ALL

* XXX = type of algorithm to run (two choices: @RadoBoost or @AdaBoost)
* YYY = number of boosting rounds
* Z = If 0, then uses r_t as in the paper (eq. (9)). If 1, then clamps r_t
  (i.e. if |r_t| < SCALE_MU_THRESHOLD (in LinearBoost.java, tag “RAD-C2”), then fixes |r_t| = SCALE_MU_THRESHOLD;
  this accelerates the convergence of AdaBoost if SCALE_MU_THRESHOLD is not too large or too small; see 
  the supplementary information in the ArXiv version for more details)
* T = If 0, then uses AdaBoost.R weight update (for RadoBoost, this is the choice of the paper in eq. (11)).
  If 1, then uses AdaBoost weight update. Note that you can use Adaboost weight update with RadoBoost as well
  (we did not use this option for the paper)
* UUU = size of training sample (examples/rados) used to train the algorithm. If @MAX, then uses all the
  available data to train the algorithm. If @MAX, uses the @MAX_RADO_SIZE number. 
* For RadoBoost, VVV gives the method to generate rados. Two choices (in LinearBoost.java, tag “RAD-C3”): 
  @RANDOM = plain random rados;
  @DIFFPRIVK = picks a binary attribute at random and generate rados according to the Rademacher rejection
  sampling algorithm in Algorithm 2. In this case, epsilon is then given (delta corresponds to any value
  satisfying (77) in the long ArXiv version of the paper, http://arxiv.org/pdf/1502.02322v2.pdf)

