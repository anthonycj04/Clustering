package clustering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jsonobject.DataSet;
import jsonobject.User;
import weka.clusterers.SimpleKMeans;
import weka.core.EuclideanDistance;
import weka.core.Instances;
import weka.experiment.InstanceQuery;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Clustering {
	public static void main(String[] args){
		(new Clustering()).start();
	}

	public void start(){
		long startTime;
		DataSet dataSet;
		HashMap<Integer, UserInfo> users = new HashMap<Integer, UserInfo>();  // used to store the checkins and friendlist of a user
		HashMap<String, LocationInfo> locations = new HashMap<String, LocationInfo>();

		startTime = System.currentTimeMillis();
		dataSet = readData(Config.dataFilename);
		System.out.println("reading time: " + (double)(System.currentTimeMillis() - startTime) / 1000);

		startTime = System.currentTimeMillis();
		convertDataSet(dataSet, locations, users);
		System.out.println("converting time: " + (double)(System.currentTimeMillis() - startTime) / 1000);

		startTime = System.currentTimeMillis();
		cluster(Config.inputFilename, locations, users, Config.numOfClusters, Config.seed, Config.maxIterations, Config.outputFilename);
		System.out.println("clustering time: " + (double)(System.currentTimeMillis() - startTime) / 1000);

		checkAccuracy(Config.outputFilename, Config.answerFilename);
		System.out.println("Done");

		// for (int i = 2; i < 200; i += 2){
		// 	System.out.print(i + ": ");
		// 	cluster(Config.inputFilename, locations, users, i, Config.seed, Config.maxIterations, Config.outputFilename);
		// 	checkAccuracy(Config.outputFilename, Config.answerFilename);
		// }
	}

	// reads a json string from a given file and convert it into a java class
	private DataSet readData(String filename){
		if ((new File(filename).exists())){
			// file exists, start reading data
			try {
				String line;
				BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
				line = bufferedReader.readLine();
				line = line.replace("\"check-ins\":", "\"checkins\":");
				bufferedReader.close();
				ObjectMapper mapper = new ObjectMapper();
				DataSet dataSet = mapper.readValue(line, DataSet.class);
				return dataSet;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else{
			// file doesn't exist
			System.err.println("file doesn't exist");
			System.exit(1);
		}
		return null;
	}

	// read thourgh the dataset and convert it to more accessable data structure
	private void convertDataSet(DataSet dataSet, 
								HashMap<String, LocationInfo>  locations, 
								HashMap<Integer, UserInfo> users){
		String date;
		double xCoordinate, yCoordinate;
		String[] splittedCoordinates;
		// initialize the locations
		for (Entry<String, String> entry: dataSet.getLocations().getLocationMap().entrySet()){
			splittedCoordinates = entry.getValue().split(",");
			xCoordinate = Double.valueOf(splittedCoordinates[0].substring(1));
			yCoordinate = Double.valueOf(splittedCoordinates[1].substring(1, splittedCoordinates[1].length() - 1));
			locations.put(entry.getKey(), new LocationInfo(xCoordinate, yCoordinate));
		}
		// run through the data and add the user's checkin into the hashmap, and also count the number of checkins of a location
		for (User user: dataSet.getUsers()){
			// add the user if it doesn't exist
			if (!users.containsKey(user.getUid()))
				users.put(user.getUid(), new UserInfo());
			UserInfo tempUserInfo = users.get(user.getUid());
			for (List<String> checkin: user.getCheckins()){
				date = checkin.get(1).substring(0, 7);

				tempUserInfo.incNumOfCheckin(checkin.get(0));
				locations.get(checkin.get(0)).incNumOfVisits();
				tempUserInfo.addCheckin(checkin.get(0), date);
				
				locations.get(checkin.get(0)).incNumOfVisitsPerMonth(date);
			}
		}
		// run through the friendships and add the relationships
		for (List<Integer> friends: dataSet.getFriendships()){
			if (!users.containsKey(friends.get(0)))
				users.put(friends.get(0), new UserInfo());
			if (!users.containsKey(friends.get(1)))
				users.put(friends.get(1), new UserInfo());
			users.get(friends.get(0)).addFriend(friends.get(1));
			users.get(friends.get(1)).addFriend(friends.get(0));
		}
	}

	// cluster the locations according to its latitude and longitude
	private void cluster(String inputFilename,
							HashMap<String, LocationInfo>  locations, 
							HashMap<Integer, UserInfo> users, 
							int numOfClusters, 
							int seed, 
							int maxIterations, 
							String outputFilename){
		HashMap<String, Integer> clusterInfo = new HashMap<String, Integer>();
		try {
			// read from mysql database
			InstanceQuery query = new InstanceQuery();
			query.setUsername("sunnyboy");
			query.setPassword("777");
			query.setQuery("select * from locations");
			Instances data = query.retrieveInstances();;
			// discard the location's name
			Remove filter = new Remove();
			filter.setAttributeIndicesArray(new int[]{0});
			filter.setInputFormat(data);
			Instances newData = Filter.useFilter(data, filter);
			
			// setup the simplekmeans configuration
			SimpleKMeans KM = new SimpleKMeans();
			KM.setNumClusters(numOfClusters);
			KM.setSeed(seed);
			KM.setMaxIterations(maxIterations);
			KM.setDistanceFunction(new EuclideanDistance());
			KM.setPreserveInstancesOrder(true);
			KM.buildClusterer(newData);
			
			// save the cluster information
			int[] assignments = KM.getAssignments();
			int i=0;
			for(int clusterNum : assignments)
				clusterInfo.put(data.instance(i++).stringValue(0), clusterNum);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if ((new File(inputFilename).exists())){
			// file exists, start reading data
			try {
				String line, location, result;
				String[] splittedLine;
				Integer uid;
				UserInfo tempUserInfo;
				BufferedReader bufferedReader = new BufferedReader(new FileReader(inputFilename));
				PrintWriter printWriter = new PrintWriter(new FileOutputStream(outputFilename, false), true);
				
				while ((line = bufferedReader.readLine()) != null){
					splittedLine = line.split(";");
					uid = Integer.valueOf(splittedLine[0]);
					location = splittedLine[1];
					tempUserInfo = users.get(uid);
					result = ";No";
					// if the user has visited a location in the same cluster
					for (Entry<String, ArrayList<String>> entry: tempUserInfo.getCheckins().entrySet()){
						if (clusterInfo.get(location) == clusterInfo.get(entry.getKey())){
							result = ";Yes";
							break;
						}
					}

					// if the user's friend has visited a location in the same cluster
					if (result.equals(";No")){
						for (Integer friend: tempUserInfo.getFriends()){
							tempUserInfo = users.get(friend);
							for (Entry<String, ArrayList<String>> entry: tempUserInfo.getCheckins().entrySet()){
								if (clusterInfo.get(location) == clusterInfo.get(entry.getKey())){
									result = ";Yes";
									break;
								}
							}
						}
					}
					printWriter.println(line + result);
				}
				bufferedReader.close();
				printWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// check the accuracy by reading the output file and the answer file
	private void checkAccuracy(String outputFilename, String answerFilename){
		if ((new File(outputFilename).exists()) && (new File(answerFilename).exists())){
			// file exists, start reading data
			int totalTries = 0, correctTries = 0;
			try {
				String inLine, outLine;
				BufferedReader inBufferedReader = new BufferedReader(new FileReader(outputFilename));
				BufferedReader outBufferedReader = new BufferedReader(new FileReader(answerFilename));
				while ((inLine = inBufferedReader.readLine()) != null){
					outLine = outBufferedReader.readLine();
					totalTries++;
					if (outLine.equals(inLine))
						correctTries++;
				}
				inBufferedReader.close();
				outBufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Accuracy: " + (double) correctTries / totalTries);
		}
		else{
			// file doesn't exist
			System.err.println("file doesn't exist");
			System.exit(1);
		}
	}
}
