package uk.ac.ox.oucs.vle;

import java.util.List;

/**
 * Wrapper around a normal Sakai user. 
 * @author buckett
 *
 */
public class UserProxy {

	private String id;
	private String eid;
	private String firstname;
	private String lastname;
	private String displayname;
	private String email;
	private List<String> units;
	private String yearOfStudy;
	private String type;
	private String department;
	
	public UserProxy(String id, String eid, String firstname, String lastname, String name, String email, 
			String yearOfStudy, String type, String department, List<String> units) {
		this.id = id;
		this.eid = eid;
		this.firstname = firstname;
		this.lastname = lastname;
		this.displayname = name;
		this.email = email;
		this.units = units;
		this.yearOfStudy = yearOfStudy;
		this.type = type;
		this.department = department;
	}

	public String getId() {
		return id;
	}

	public String getEid() {
		return eid;
	}

	public String getFirstName() {
		return firstname;
	}
	
	public String getLastName() {
		return lastname;
	}
	
	public String getDisplayName() {
		return displayname;
	}

	public String getEmail() {
		return email;
	}
	
	public List<String> getUnits() {
		return units;
	}
	
	public String getYearOfStudy() {
		return yearOfStudy;
	}
	
	public String getType() {
		return type;
	}
	
	public String getDepartment() {
		return department;
	}
}
