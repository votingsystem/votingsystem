To run Vicket request:
$gradle testRequestVickets

----------------------------------------------------------------------------------------------------
To only run one test we must invoke the test task from the command-line with the Java system property
-Dtest.single=Sample (http://java.dzone.com/articles/gradle-goodness-running-single):
$gradle -Dtest.single=UserVSTransaction test