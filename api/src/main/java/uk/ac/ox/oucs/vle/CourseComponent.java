/*
 * #%L
 * Course Signup API
 * %%
 * Copyright (C) 2010 - 2013 University of Oxford
 * %%
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *             http://opensource.org/licenses/ecl2
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package uk.ac.ox.oucs.vle;

import java.util.Date;
import java.util.List;

public interface CourseComponent {

	public String getPresentationId();
	
	public String getSubject();
	
	public String getTitle();
	
	public int getPlaces();
	
	public int getSize();
	
	public Person getPresenter();
	
	public String getLocation();
	
	public Date getOpens();
	
	public String getOpensText();
	
	public Date getCloses();
	
	public String getClosesText();
	
	public Date getStarts();
	
	public String getStartsText();
	
	public Date getEnds();
	
	public String getEndsText();
	
	public Date getCreated();
	
	/**
	 * Since Oxcap, we can not rely on there being data in any column,
	 * the import sets a baseDate (if possible) as to the best date to 
	 * use for signups availability.
	 * This is usually either when signup closes closes or when the course starts.
	 * @return The date when this course becomes old.
	 */
	public Date getBaseDate();
	
	public String getSlot();
	
	public String getWhen();
	
	public String getSessions();
	
	public boolean getBookable();

	/**
	 * The URL to apply to the course.
	 * This comes from standard XCRI.
	 * @return A String URL.
	 * @see #getMemberApplyTo()
	 */
	public String getApplyTo();
	
	public String getTeachingDetails();

	/**
	 * The URL to apply to a course for members of the institution.
	 * This is a non-standard extension.
	 * @return A String URL.
	 * @see #getApplyTo()
	 */
	public String getMemberApplyTo();
	
	public String getAttendanceMode();
	
	public String getAttendanceModeText();
	
	public String getAttendancePattern();
	
	public String getAttendancePatternText();
	
	public String getSource();
	
	public List<CourseComponentSession> getComponentSessions();

	/**
	 * The ID of the component set that this component belongs to.
	 * This is used when there are multiple copies of a component running at once to discover
	 * which ones the user can select from.
	 * 
	 * If a set has just one component (and is within the signup period, and has places) then it has a checkbox.
	 * If a set has multiple components (and is within ....), then it has a radio button within the set, so just one option is available for signup.
	 * @return
	 */ 
	public String getComponentSet();
	
}
