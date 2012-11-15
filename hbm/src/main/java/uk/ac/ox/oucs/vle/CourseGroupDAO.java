package uk.ac.ox.oucs.vle;
// Generated Aug 17, 2010 10:15:40 AM by Hibernate Tools 3.2.2.GA


import java.util.HashSet;
import java.util.Set;

/**
 * CourseGroupDAO generated by hbm2java
 */
public class CourseGroupDAO implements java.io.Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
    private String title;
    private String dept;
    private Set<CourseComponentDAO> components = new HashSet<CourseComponentDAO>(0);
    private Set<CourseSignupDAO> signups = new HashSet<CourseSignupDAO>(0);
    private Set<String> administrators = new HashSet<String>(0);
    private Set<String> superusers = new HashSet<String>(0);
    private Set<String> otherDepartments = new HashSet<String>(0);
    private Set<CourseCategoryDAO> categories = new HashSet<CourseCategoryDAO>(0);
	private String description;
	private String departmentName;
	private String subunit;
	private String subunitName;
	private String contactEmail;
	private String visibility;
	private String source;
	private boolean publicView;
	private boolean supervisorApproval;
	private boolean administratorApproval;
	private boolean hideGroup;

    public CourseGroupDAO() {
    }
	
    public CourseGroupDAO(String id) {
        this.id = id;
    }
    
    public String getId() {
        return this.id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDept() {
        return this.dept;
    }
    
    public void setDept(String dept) {
        this.dept = dept;
    }
    
    public String getSubunit() {
        return this.subunit;
    }
    
    public void setSubunit(String subunit) {
        this.subunit = subunit;
    }
    
    public Set<CourseComponentDAO> getComponents() {
        return this.components;
    }
    
    public void setComponents(Set<CourseComponentDAO> components) {
        this.components = components;
    }
    
    public Set<CourseSignupDAO> getSignups() {
        return this.signups;
    }
    
    public void setSignups(Set<CourseSignupDAO> signups) {
        this.signups = signups;
    }

    public Set<String> getAdministrators() {
        return this.administrators;
    }
    
    public void setAdministrators(Set<String> administrators) {
        this.administrators = administrators;
    }
    
    public Set<String> getSuperusers() {
        return this.superusers;
    }
    
    public void setSuperusers(Set<String> superusers) {
        this.superusers = superusers;
    }
    
    public Set<String> getOtherDepartments() {
        return this.otherDepartments;
    }
    
    public void setOtherDepartments(Set<String> otherDepartments) {
        this.otherDepartments = otherDepartments;
    }
    
    public Set<CourseCategoryDAO> getCategories() {
        return categories;
    }
    
    public void setCategories(Set<CourseCategoryDAO> categories) {
        this.categories = categories;
    }

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	public String getDepartmentName() {
		return departmentName;
	}

	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}
	
	
	public String getSubunitName() {
		return subunitName;
	}

	public void setSubunitName(String subunitName) {
		this.subunitName = subunitName;
	}
	/*
	public boolean getPublicView() {
		return publicView;
	}

	public void setPublicView(boolean publicView) {
		this.publicView = publicView;
	}
	*/
	public boolean getSupervisorApproval() {
		return supervisorApproval;
	}

	public void setSupervisorApproval(boolean supervisorApproval) {
		this.supervisorApproval = supervisorApproval;
	}
	
	public boolean getAdministratorApproval() {
		return administratorApproval;
	}

	public void setAdministratorApproval(boolean administratorApproval) {
		this.administratorApproval = administratorApproval;
	}
	
	public boolean getHideGroup() {
		return hideGroup;
	}

	public void setHideGroup(boolean hideGroup) {
		this.hideGroup = hideGroup;
	}
	
	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}
	
	public String getVisibility() {
		return visibility;
	}

	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}
	
	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
}


