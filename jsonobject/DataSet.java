package jsonobject;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAnySetter;

/*
 * This class is used as a container only for reading JSON with "Jackson Java JSON-processor".
 * Maybe there's a better way to use the library.
 * Didn't have much time to read through the document...
 */

/*
 * Example of the JSON string:
{
	"users": [
		{
			"uid": "195947",
			"check-ins": [["1427231", "2010-07-12 02:33:31"]]
		},
		{
			"uid": "195986",
			"check-ins": [
				["207398", "2009-12-14 03:47:55"], ["211301", "2009-12-15 03:44:01"],
				["211301", "2009-12-15 03:50:01"]
			]
		}
	],
	"locations": {
		"211301": ["-117.91844791", "33.99449664"],
		"1427231": ["-122.040785567", "38.242269367"],
		"207398": ["-117.5478601167", "34.0726246333"]
	},
	"friendships": [
		["195947", "195986"]
	]
}
* 
*/

public class DataSet {
	private List<User> users;
	private Location locations;
	private List<List<Integer>> friendships;

	public List<List<Integer>> getFriendships(){
		return friendships;
	}

	public void setFriendships(List<List<Integer>> friendships){
		this.friendships = friendships;
	}

	public List<User> getUsers(){
		return users;
	}

	public void setUsers(List<User> users){
		this.users = users;
	}

	public int getStartTime(){
		return 0;
	}

	public int getEndTime(){
		return 1;
	}

	public int getNumOfFriendships(){
		return friendships.size();
	}

	public int getTotalNumOfRecords(){
		int totalNumOfRecords = 0;
		for (int i = 0; i < users.size(); i++)
			totalNumOfRecords += users.get(i).getNumOfRecords();
		return totalNumOfRecords;
	}

	public Location getLocations(){
		return locations;
	}

	public void setLocations(Location locations){
		this.locations = locations;
	}

	@JsonAnySetter
	public void handleUnknown(String key, Object value){
		System.out.println("handleUnknown in " + DataSet.class.toString());
	}

	@Override
	public String toString(){
		return "{users: " + users +", locations: " + locations + ", friendships: " + friendships + "}";
	}
}
