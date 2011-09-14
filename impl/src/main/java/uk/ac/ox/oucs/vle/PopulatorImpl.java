package uk.ac.ox.oucs.vle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.content.api.ContentHostingService;
import org.sakaiproject.content.api.ContentResourceEdit;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionManager;


/**
 * The idea is this class populates the course group rows.
 * 
 * @author buckett
 * 
 */
public class PopulatorImpl implements Populator{

	private static final Log log = LogFactory.getLog(PopulatorImpl.class);
	
	private ContentHostingService contentHostingService;
	private ServerConfigurationService serverConfigurationService;
	private SessionManager sessionManager;
	/**
	 * The source to pull the data from.
	 */
	private DataSource ds;
	
	/**
	 * The DAO to update our entries through.
	 */
	private CourseDAO dao;
	
	/**
	 * The proxy for getting users.
	 */
	private SakaiProxy proxy;
	
	private Writer writer;
	
	private String preHTMLa = "<html><head></head><body>" +
		"<h3>Errors and Warnings from SES Import ";
	private String preHTMLb = "</h3><pre>";
	private String postHTML = "</pre></body></html>";
	
	public void setDataSource(DataSource ds) {
		this.ds = ds;
	}

	public void setCourseDao(CourseDAO dao) {
		this.dao = dao;
	}

	public void setProxy(SakaiProxy proxy) {
		this.proxy = proxy;
	}
	
	public void setContentHostingService(ContentHostingService contentHostingService) {
		this.contentHostingService = contentHostingService;
	}
	
	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}
	
	public void setSessionManager(SessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	/* (non-Javadoc)
	 * @see uk.ac.ox.oucs.vle.Populator#update()
	 */
	public void update() {
		switchUser();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writer = new OutputStreamWriter(out);
		ContentResourceEdit cre = null;
		
		try {
			writer.write(preHTMLa);
			writer.write(now());
			writer.write(preHTMLb);
			updater();
			writer.write(postHTML);
			writer.flush();
			writer.close();
		
			String jsonResourceId = contentHostingService.getSiteCollection(getSiteId())+ "import.html";
		
			try {
				// editResource() doesn't throw IdUnusedExcpetion but PermissionException
				// when the resource is missing so we first just tco to find it.
				contentHostingService.getResource(jsonResourceId);
				cre = contentHostingService.editResource(jsonResourceId);
			} catch (IdUnusedException e) {
				try {
					cre = contentHostingService.addResource(jsonResourceId);
					ResourceProperties props = cre.getPropertiesEdit();
					props.addProperty(ResourceProperties.PROP_DISPLAY_NAME, "importlog.html");
					cre.setContentType("text/html");
				} catch (Exception e1) {
					log.warn("Failed to create the import log file.", e1);
				}
			}
			
			cre.setContent(out.toByteArray());
			// Don't notify anyone about this resource.
			contentHostingService.commitResource(cre, NotificationService.NOTI_NONE);
		} catch (Exception e) {
			log.warn("Failed to write content to logfile.", e);
		} finally {
			if (null != cre && cre.isActiveEdit()) {
				contentHostingService.cancelResource(cre);
			}
		}
		
	}
	
	public void updater() {
		Connection con = null;
		
		int groupSeen = 0, groupUpdated = 0, groupCreated = 0;
		int componentSeen = 0, componentUpdated = 0, componentCreated = 0;
		try {
			con = ds.getConnection();
			Statement st = con.createStatement();
			
			departmentTable(st);
			subunitTable(st);
			Map<String, Set<String>> divisionalSuperusers = this.superUsersTable(st);
			
			Set<String> administrators = new HashSet<String>();
			Set<String> otherDepartments = new HashSet<String>();
			String code = null;
			String lastCode = null;
			String grouptitle = null;
			String departmentCode = null;
			String departmentName = null;
			String subunitCode = null;
			String subunitName = null;
			String divisionCode = null;
			String divisionEmail = null;
			boolean publicView = true;
			boolean supervisorApproval = true;
			boolean administratorApproval = true;
			String description = null;
			
			PreparedStatement pstmt = con.prepareStatement("SELECT DISTINCT " +
					" other_department_id FROM AssessmentUnitOtherDepartment " +
					" WHERE assessment_unit_id = ?");
			
			//con = ds.getConnection();
			//Statement st = con.createStatement();
			// Import all course groups (from assessment units)department
			String sql = ("SELECT DISTINCT au.id, au.assessment_unit_code, au.title, " +
					" Department.department_code, Department.department_name, " +
					" SubUnit.sub_unit_code, SubUnit.sub_unit_name, " +
					" Division.division_code, Division.division_wide_email, " +
					" au.description, au.public_view, " +
					" au.supervisor_approval, au.module_approval, " +
					" Employee.webauth_code " +
					" FROM AssessmentUnit au " + 
					" INNER JOIN AssessmentUnitEmployee ae ON au.id = ae.assessment_unit_id " +
					" INNER JOIN Employee ON ae.employee_id = Employee.id  " + 
					" INNER JOIN TeachingComponentAssessmentUnit ta ON ta.assessment_unit_id = au.id " +
					" INNER JOIN TeachingComponent tc ON tc.id = ta.teaching_component_id " +
					" INNER JOIN Department ON au.department_id = Department.id " + 
					" LEFT JOIN SubUnit ON au.sub_unit_id = SubUnit.id " +
					" INNER JOIN Division ON Department.division_id = Division.id;");
			ResultSet rs = st.executeQuery(sql);
			while(rs.next()) {
				
				code = rs.getString("assessment_unit_code");
				if (!code.equals(lastCode)) {
					
					groupSeen++;
					
					if (lastCode != null && !administrators.isEmpty()) {
						
						if (updateGroup(lastCode, grouptitle, departmentCode, subunitCode, description,
							departmentName, subunitName, publicView, 
							supervisorApproval, administratorApproval,
							divisionEmail, administrators, 
							divisionalSuperusers.get(divisionCode), otherDepartments)) {
							groupCreated++;
						} else {
							groupUpdated++;
						}
					}
					
					lastCode = code;
					grouptitle = rs.getString("title");
					departmentCode = rs.getString("department_code");
					departmentName = rs.getString("department_name");
					subunitCode = rs.getString("sub_unit_code");
					subunitName = rs.getString("sub_unit_name");
					divisionCode = rs.getString("division_code");
					divisionEmail = rs.getString("division_wide_email");
					publicView = rs.getBoolean("public_view");
					supervisorApproval = rs.getBoolean("supervisor_approval");
					administratorApproval = rs.getBoolean("module_approval");
					description = rs.getString("description");
					administrators = new HashSet<String>();
					otherDepartments = new HashSet<String>();
					
					//PreparedStatement pstmt = con.prepareStatement("SELECT DISTINCT " +
					//		" otherDepartment FROM course_group_otherDepartment " +
					//		" WHERE course_group = ?");
					pstmt.setString(1, lastCode);
					ResultSet innerRS = pstmt.executeQuery();
					while(innerRS.next()) {
						otherDepartments.add(rs.getString("otherDepartment"));
					}
				}
				
				String administrator = rs.getString("webauth_code");
				if (administrator == null || administrator.trim().length() == 0) {
					logFailure(code, null, "No administrator set");
					continue;
				}
				UserProxy user = proxy.findUserByEid(administrator); 
				if (user == null) {
					logFailure(code, null, "Failed to find administrator " + administrator);
					continue;
				}
				
				administrators.add(user.getId());
			}
			
			// End of ResultSet write the last coursegroup
			if (lastCode != null && !administrators.isEmpty()) {
				if (updateGroup(lastCode, grouptitle, departmentCode, subunitCode, description,
						departmentName, subunitName, publicView, 
						supervisorApproval, administratorApproval,
						divisionEmail, administrators,
						divisionalSuperusers.get(divisionCode), otherDepartments)) {
					groupCreated++;
				} else {
					groupUpdated++;
				}
			}
			
			// Now import all the course components ( from teaching (instances/components))
			sql = "SELECT" + 
					" ti.id, ti.open_date, ti.close_date, ti.expiry_date, ti.start_date, ti.end_date, " +
					" ti.sessions, ti.session_dates, ti.teaching_capacity, ti.bookable, " + 
					" au.assessment_unit_code, tc.id as teaching_component_id, tc.subject, " + 
					" Location.location_name, " + 
					" Term.term_code, Term.label, " + 
					" c.display_title, " + 
					" Employee.webauth_code, " +
					" concat_ws(' ', Employee.forename, Employee.surname) as teacher_name, " +
					" Employee.email as teacher_email " + 
					" FROM TeachingInstance ti " + 
					" LEFT JOIN TeachingComponent tc ON tc.id = ti.teaching_component_id " + 
					" LEFT JOIN TeachingComponentAssessmentUnit ta ON ta.teaching_component_id = tc.id " + 
					" LEFT JOIN AssessmentUnit au ON au.id = ta.assessment_unit_id " +
					" LEFT JOIN Term ON ti.term_id = Term.id " + 
					" LEFT JOIN Location ON ti.location_id = Location.id " + 
					" LEFT JOIN TeachingComponentType c ON tc.teaching_component_type_id = c.id " + 
					" LEFT JOIN Employee ON ti.employee_id = Employee.id;";
			rs = st.executeQuery(sql);
			String lastId = null;
			CourseComponentDAO componentDao = null;
			boolean created = false;
			
			while(rs.next()) {
				componentSeen++;
				
				String id = rs.getString("id");
				String assessmentUnitCode = rs.getString("assessment_unit_code");
				if (null == assessmentUnitCode) {
					logFailure(assessmentUnitCode, id, "No assessment unit code set.");
					continue;
				}
				
				if (!id.equals(lastId)) {
					
					if (null != lastId && null != componentDao) {
						if (created) {
							componentCreated++;
						} else {
							componentUpdated++;
						}
						dao.save(componentDao);
						componentDao = null;
					}
					
					lastId = id;

					// Since the refactoring (from string to int)
					// of the table we can just treat the int column like a boolean
					boolean bookable = rs.getBoolean("bookable");
					
					Date openDate = getDate(rs, "open_date");
					if (openDate == null) { 
						logFailure(assessmentUnitCode, id, "No open date set");
						continue;
					}
					Date closeDate = getDate(rs, "close_date");
					if (closeDate == null) {
						logFailure(assessmentUnitCode, id, "No close date set");
						continue;
					}
					Date expiryDate = getDate(rs, "expiry_date");
					if (expiryDate == null) {
						expiryDate = closeDate;
					}
					Date startDate = getDate(rs, "start_date");
					Date endDate = getDate(rs, "end_date");
					
					if (openDate.after(closeDate)){
						logFailure(assessmentUnitCode, id, "Open date is after close date");
						continue;
					}
					if(expiryDate.before(closeDate)){
						logFailure(assessmentUnitCode, id, "Expiry date is before close date");
						continue;
					}
					
					int capacity = rs.getInt("teaching_capacity");
					
					String subject = rs.getString("subject");
					if (subject == null || subject.trim().length() == 0) {
						logFailure(assessmentUnitCode, id, "Subject isn't set.");
						continue;
					}
					String title = rs.getString("display_title");
					if (title == null || title.trim().length() == 0) {
						logFailure(assessmentUnitCode, id, "Title isn't set.");
						continue;
					}
					String termCode = rs.getString("term_code");
					if (termCode == null || termCode.trim().length() == 0) {
						logFailure(assessmentUnitCode, id, "Term code can't be empty");
						continue;
					}
					String termName = rs.getString("label");
					if (termName == null || termName.trim().length() == 0) {
						logFailure(assessmentUnitCode, id, "Term name can't be empty");
						continue;
					}
					
					String teachingComponentId = rs.getString("teaching_component_id");
					if (teachingComponentId == null || teachingComponentId.trim().length()==0) {
						logFailure(assessmentUnitCode, id, "No teaching component ID found.");
						continue;
					}
					
					// Now find the component.
					componentDao = dao.findCourseComponent(id);
					if (componentDao == null) {
						componentDao = dao.newCourseComponent(id);
						created = true;
					}
					componentDao.setTitle(title);
					componentDao.setSubject(subject);
					componentDao.setOpens(openDate);
					componentDao.setCloses(closeDate);
					componentDao.setExpires(expiryDate);
					componentDao.setStarts(startDate);
					componentDao.setEnds(endDate);
					componentDao.setBookable(bookable);
					componentDao.setSize(capacity);
					componentDao.setTermcode(termCode);
					componentDao.setComponentId(teachingComponentId+":"+termCode);
					
					// Cleanout existing groups.
					componentDao.setGroups(new HashSet<CourseGroupDAO>());
					
					// Populate teacher details.
					// Look for details in WebLearn first then fallback to details in DAISY.
					String teacherName = null;
					String teacherEmail = null;
					String teacherId = rs.getString("webauth_code");
					if (teacherId != null && teacherId.length() > 0) {
						UserProxy teacher = proxy.findUserByEid(teacherId);
						if (teacher != null) {
							teacherName = teacher.getDisplayName();
							teacherEmail = teacher.getEmail();
						}
					}
					if (teacherName == null) {
						teacherName = rs.getString("teacher_name");
					}
					if (teacherName != null && teacherName.trim().length() > 0) {
						componentDao.setTeacherName(teacherName);
						if (teacherEmail == null) {
							teacherEmail = rs.getString("teacher_email");
						}
						if (teacherEmail != null && teacherName.trim().length() > 0) {
							componentDao.setTeacherEmail(teacherEmail);
						}
					}
					
					// Which term
					componentDao.setWhen(termName);
					
					// When they are happening.
					String sessionDates = rs.getString("session_dates");
					if (sessionDates != null && sessionDates.trim().length() > 0) {
						componentDao.setSlot(sessionDates);
					}
					
					// How many sessions
					String sessions = rs.getString("sessions");
					if (sessions != null && sessions.trim().length() > 0) {
						componentDao.setSessions(sessions);
					}
					
					// Where?
					String location = rs.getString("location_name");
					if (location != null && location.trim().length() > 0) {
						componentDao.setLocation(location);
					}
					
					assessmentUnitCode = assessmentUnitCode.trim();
					CourseGroupDAO courseDao = dao.findCourseGroupById(assessmentUnitCode);
					if (courseDao == null) {
						logWarning(id, "Couldn't find course group of: "+ assessmentUnitCode);
					} else {
						componentDao.getGroups().add(courseDao);
					}
				} 
			
				if (null != componentDao) { // may have failed validation
					assessmentUnitCode = assessmentUnitCode.trim();
					CourseGroupDAO courseDao = dao.findCourseGroupById(assessmentUnitCode);
					if (courseDao == null) {
						logWarning(id, "Couldn't find course group of: "+ assessmentUnitCode);
					} else {
						componentDao.getGroups().add(courseDao);
					}
				}
			}
			
			if (null != lastId && null != componentDao) {
				if (created) {
					componentCreated++;
				} else {
					componentUpdated++;
				}
				dao.save(componentDao);
			}
			
		} catch (IOException e) {
			log.warn("Problem importing.", e);
			
		} catch (SQLException e) {
			log.warn("Problem importing.", e);
			
		} finally {
			log.info("CourseGroups (seen: "+ groupSeen+ " created: "+ groupCreated+ ", updated: "+ groupUpdated+")");
			log.info("CourseComponents (seen: "+ componentSeen+ " created: "+ componentCreated+ ", updated: "+ componentUpdated+")");
			if (con != null) {
				try {
					con.close();
				} catch (SQLException sqle) {
				}
			}
		}

	}
	
	/**
	 * Departments
	 * @param con
	 * @throws SQLException
	 */
	private void departmentTable(Statement st) throws SQLException {
		
		String departmentCode = null;
		String lastDepartment = null;
		String departmentName = null;
		boolean approve = true;
		Set<String> approvers = new HashSet<String>();
		ResultSet rs = st.executeQuery(
				"SELECT Department.department_code, Department.department_name, " +
				"Employee.webauth_code " +
				"FROM Department " +
				"LEFT JOIN Employee ON Department.id = Employee.department_id " +
				"AND Employee.ses_approver = 1;");
		while(rs.next()) {
			departmentCode = rs.getString("department_code");
			if (!departmentCode.equals(lastDepartment)) {
				if (lastDepartment != null) {
					DepartmentDAO departmentDao = dao.findDepartmentByCode(lastDepartment);
					if (null == departmentDao) {
						departmentDao = new DepartmentDAO(lastDepartment);
					}
					departmentDao.setName(departmentName);
					departmentDao.setApprove(approve);
					departmentDao.setApprovers(approvers);
					dao.save(departmentDao);
				}
				
				lastDepartment = departmentCode;
				departmentName = rs.getString("department_name");
				approve = rs.getBoolean("approve");
			}
		}
		
		if (lastDepartment != null) {
			DepartmentDAO departmentDao = dao.findDepartmentByCode(lastDepartment);
			if (null == departmentDao) {
				departmentDao = new DepartmentDAO(lastDepartment);
			}
			departmentDao.setName(departmentName);
			departmentDao.setApprove(approve);
			departmentDao.setApprovers(approvers);
			dao.save(departmentDao);
		}
	}
	
	/**
	 * subunits
	 * @param st
	 * @throws SQLException
	 */
	private void subunitTable(Statement st) throws SQLException {
		
		String subunitCode = null;
		String subunitName = null;
		String departmentCode = null;
		
		Set<String> approvers = new HashSet<String>();
		ResultSet rs = st.executeQuery(
				"SELECT sub_unit_code, sub_unit_name, department_code " +
				"FROM SubUnit;");
		while(rs.next()) {
			subunitCode = rs.getString("sub_unit_code");
			subunitName = rs.getString("sub_unit_name");
			departmentCode = rs.getString("department_code");
			
			SubunitDAO subunitDao = dao.findSubunitByCode(subunitCode);
			if (null == subunitDao) {
				subunitDao = new SubunitDAO(subunitCode);
			}
			subunitDao.setSubunitName(subunitName);
			subunitDao.setDepartmentCode(departmentCode);
			dao.save(subunitDao);
		}
	}
	
	/**
	 * Divisional Superusers
	 * @param con
	 * @throws SQLException
	 */
	private Map<String, Set<String>> superUsersTable(Statement st) throws SQLException {
		
		Map<String, Set<String>> divisionalSuperusers = new HashMap<String, Set<String>>();
		Set<String> superusers = new HashSet<String>();
		
		String suDivisionCode = null;
		String lastSuDivisionCode = null;
		ResultSet rs = st.executeQuery(
				"SELECT DISTINCT Division.division_code, " +
				" Employee.webauth_code " +
				" FROM Employee " + 
				" INNER JOIN Department ON Employee.department_id = Department.id " + 
				" INNER JOIN Division ON Department.division_id = Division.id" +
				" WHERE Employee.ses_superuser = 1" +
				" ORDER BY Division.division_code;");
		while(rs.next()) {
			suDivisionCode = rs.getString("division_code");
			
			if (!suDivisionCode.equals(lastSuDivisionCode)) {
				
				if (lastSuDivisionCode != null && !superusers.isEmpty()) {
					divisionalSuperusers.put(lastSuDivisionCode, superusers);
					superusers = new HashSet<String>();
				}
				lastSuDivisionCode = suDivisionCode;
				
				String superuser = rs.getString("webauth_code");
				UserProxy user = proxy.findUserByEid(superuser); 
				if (user == null) {
					//logFailure(suDivisionCode, null, "Failed to find superuser " + superuser);
					continue;
				}
				
				superusers.add(user.getId());
			}
		}
		
		if (lastSuDivisionCode != null && !superusers.isEmpty()) {
			divisionalSuperusers.put(lastSuDivisionCode, superusers);
		}
		
		return divisionalSuperusers;
		
	}
	
	private boolean updateGroup(String code, String title, String departmentCode, String subunitCode, 
			String description, String departmentName, String subunitName, 
			boolean publicView, boolean supervisorApproval, boolean administratorApproval,
			String divisionEmail, 
			Set<String> administrators, Set<String> superusers, Set<String> otherDepartments) {
		
		log.info("Updategroup ["+code+":"+administrators.size()+":"+administrators.iterator().next()+"]");
		
		CourseGroupDAO groupDao = dao.findCourseGroupById(code);
		boolean created = false;
		if (groupDao == null) {
			groupDao = dao.newCourseGroup(code, title, departmentCode, subunitCode);
			created = true;
		} else {
			groupDao.setDept(departmentCode);
			groupDao.setSubunit(subunitCode);
			groupDao.setTitle(title);
		}
		groupDao.setDescription(description);
		groupDao.setDepartmentName(departmentName);
		groupDao.setSubunitName(subunitName);
		groupDao.setPublicView(publicView);
		groupDao.setSupervisorApproval(supervisorApproval);
		groupDao.setAdministratorApproval(administratorApproval);
		groupDao.setContactEmail(divisionEmail);
		groupDao.setAdministrators(administrators);
		if (null==superusers) {
			superusers = Collections.EMPTY_SET;
		}
		groupDao.setSuperusers(superusers);
		if (null==otherDepartments) {
			otherDepartments = Collections.EMPTY_SET;
		}
		groupDao.setOtherDepartments(otherDepartments);
		dao.save(groupDao);
		
		return created;
	}

	/**
	 * Gets a date out of a result set.
	 * This catches dates of the format 0000-00-00.
	 * @param rs The ResultSet.
	 * @param column The column to use.
	 * @return The date or <code>null</code> if the date can't be represented.
	 */
	private Date getDate(ResultSet rs, String column) {
		try {
			return rs.getDate(column);
		} catch (SQLException sqle) {
			return null;
		}
	}
	
	private void logFailure(String codes, String id, String reason) throws IOException {
		String message;
		if (null == id) {
			message = new String("Import failed for Assessment Unit(s) "+ codes +" because: "+ reason);
		} else if (null == codes) {
				message = new String("Import failed for Teaching Instance "+ id +" because: "+ reason);
		} else {
			message = new String("Import failed for Teaching Instance "+ id +" on Assessment Unit(s) "+codes +" because: "+ reason);
		}
		log.warn(message);
		writer.write(message+"\n");
	}
	
	private void logWarning(String id, String reason) throws IOException {
		String message = new String("Import issue for "+ id+ " because: "+ reason);
		log.warn(message);
		writer.write(message+"\n");
	}
	
	private String now() {
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    return sdf.format(cal.getTime());
	}
	
	/**
	 * This sets up the user for the current request.
	 */
	private void switchUser() {
		Session session = sessionManager.getCurrentSession();
		session.setUserEid("admin");
		session.setUserId("admin");
	}

	protected String getSiteId() {
		return serverConfigurationService.getString("course-signup.site-id", "course-signup");
	}

}
