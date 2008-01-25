/**
 * $Id: EvalImportJob.java 1000 May 28, 2007 12:07:31 AM azeckoski $
 * $URL: https://source.sakaiproject.org/contrib $
 * EvalImportJob.java - evaluation - May 28, 2007 12:07:31 AM - rwellis
 **************************************************************************
 * Copyright (c) 2008 Centre for Academic Research in Educational Technologies
 * Licensed under the Educational Community License version 1.0
 * 
 * A copy of the Educational Community License has been included in this 
 * distribution and is available at: http://www.opensource.org/licenses/ecl1.php
 *
 * Aaron Zeckoski (azeckoski@gmail.com) (aaronz@vt.edu) (aaron@caret.cam.ac.uk)
 */

package org.sakaiproject.evaluation.logic.externals;

import org.quartz.Job;

/**
 * A Quartz job to process an XML ContentResource and persist the contained evaluation data
 * 
 * @author rwellis
 */
public interface EvalImportJob extends Job {

}
