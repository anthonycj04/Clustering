package clustering;

public class Config {
	public static String dataFilename = "dm2013_dataset_3.dat";
	public static String inputFilename = "dm2013_hw3_demo_query.dat";
	public static String outputFilename = "dm2013_hw3_demo_query_output.dat";
	public static String answerFilename = "dm2013_dataset_3_100result.dat";
	public static int checkinThreshold = 1;
	public static int numOfFriendsVisitedThreshold = 1;
	public static int numOfVisitsOfFriendsThreshold = 1;
	public static int locationThreshold = 80;
	public static double nearestDistanceOfVisitedThreshold = 0.0045;
	public static double nearestDistanceOfVisitedLastMonthThreshold = 0.009;
	public static int numOfClusters = 106;
	public static int seed = 38;
	public static int maxIterations = 5000;
}
