package edu.psu.elion.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.faces.model.SelectItem;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.international.StatusMessages;
import org.jboss.seam.log.Log;

import edu.psu.elion.common.Course;
import edu.psu.elion.common.FunctionUtil;
import edu.psu.elion.domain.data.semester.SemesterData;
import edu.psu.elion.domain.data.semester.SemesterService;
import edu.psu.elion.domain.util.TimeIgnoringComparator;
import edu.psu.elion.eventlog.DailyEventLogService;
import edu.psu.elion.eventlog.LogSubTask;
import edu.psu.elion.member.ELionConfig;
import edu.psu.elion.person.ElionPerson;
import edu.psu.elion.person.PSUIdentity;
import edu.psu.nexus.cache.CacheValueLoadException;
import edu.psu.nexus.codeset.CodesetException;
import edu.psu.nexus.function.NexusFunction;
import edu.psu.nexus.gi.GiException;
import edu.psu.nexus.services.identity.PsuIdNotFoundException;
import edu.psu.nexus.services.identity.PsuIdService;

/**
 *
 * This file contains the functions to interact with the manage schedule page.
 *
 * @author MJBoldin <mjb160@psu.edu>
 * @since  2010-02-13
 */

@Name("elion.manageSchedule")
@Scope(ScopeType.CONVERSATION)
public class ManageScheduleAction implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String GI_CODE_SEMESTER_NOT_ENROLLED = "0002";

	private static final String TAB_WEEKLY_VIEW = "tabWeeklyView";
	private static final String TAB_DETAIL_VIEW = "tabDetailView";
	private static final String TAB_BUY_BOOKS = "tabBuyBooks";

	private static final String SEMESTER_CODE_SPRING = "SP";

	// NOTE: this is temporary until we can figure out the calendar onLoad issue
	// should really default to detail view
	private static final String DEFAULT_TAB = TAB_DETAIL_VIEW;

	private static final String TABLE_NOTES_HONORS = "* Denotes that the course is being taken as an Honors Option.";

	@In("elion.studentScheduleService")
	StudentScheduleService studentScheduleService;

	@In("elion.semesterService")
	SemesterService semesterService;

	@In("elion.weeklyViewAction")
	WeeklyViewAction weeklyViewAction;

	@In("elion.person")
	ElionPerson currentPerson;

	@In("nexus.psuIdService")
	PsuIdService psuIdService;

	@In("nexus.memberConfig")
	ELionConfig memberConfig;

	@In("elion.scheduleUtil")
	ScheduleUtil convert;

	@In
	StatusMessages statusMessages;

	@In("elion.dailyEventLogService")
	DailyEventLogService eventLogService;

	@In(required = false)
	@Out(required = false)
	String selectedSemester;

	@Out(required = false)
	List<SelectItem> semesterList;

	@Out(required = false, scope = ScopeType.EVENT)
	StudentSchedule studentSchedule;

	@Out
	String selectedTab = DEFAULT_TAB;

	@Out
	List<String> tableEndNotes = new ArrayList<String>();

	@Logger
	Log log;

	private String weeklyViewData;
	private String previousSelectedSemester;
	private String passedInPsuID;
	private String memberId;
	private String holdPsuID;
	private String passedInDigID;

    /**
     * Manage schedule page create tasks.
     */
	@Create
	public void create() {
		eventLogService.writeDailyEventLog(LogSubTask.COUNT, "same",
				new Date(), new Date(), null);

		// initialize variables
		passedInPsuID = new String();
		passedInDigID = new String();
		memberId = new String();
		holdPsuID = new String();
	}

    /**
     * Retrieves semester data records fofr the current and next semester.
     */
	public void lookupSemesters() throws Exception {
		log.info("Looking up semesters");

		// if function id exists in nexus
		if (FunctionUtil.isElionEnabled(((NexusFunction) Component
				.getInstance("nexus.currentFunction")).getFunctionId())) {

			// get data for current semester and its corresponding code
			String aCurrentSemester = semesterService.getCurrentSemester();
			String semesterCode = aCurrentSemester.substring(6);

			// call semester service to get current and future semesters
			String sem1 = semesterService.getDisplayFormat(aCurrentSemester);
			String sem2 = semesterService
					.getDisplayFutureSemester1(aCurrentSemester);
			String sem3 = null;

			// if current semester is spring get the next fall semester
			if (semesterCode.equals(SEMESTER_CODE_SPRING)) {
				sem3 = semesterService
						.getDisplayFutureSemester2(aCurrentSemester);
			}

			// build list of semester data
			semesterList = new ArrayList<SelectItem>();

			if (sem1 != null && sem1.length() > 0) {
				semesterList.add(new SelectItem(sem1.toUpperCase(), sem1
						.toUpperCase()));
			}

			if (ems2 != null && sem2.length() > 0) {
				semesterList.add(new SelectItem(sem2.toUpperCase(), sem2
						.toUpperCase()));
			}

			if (sem3 != null && sem3.length() > 0) {
				semesterList.add(new SelectItem(sem3.toUpperCase(), sem3
						.toUpperCase()));
			}

			// select first item in semester list drop down
			SelectItem sem1ListItem = semesterList.get(0);
			selectedSemester = (String) sem1ListItem.getValue();

			// display data for selected semester
			displaySchedule();
		}
	}

    /**
     * Looks up schedule data on a semester change.
     */
	public String displaySchedule() throws Exception {
		return lookUpSchedule(true);
	}

    /**
     * Looks up schedule data.
	 *
	 * @param semesterChanged semester changed flag
	 * @return next page to display
     */
	private String lookUpSchedule(boolean semesterChanged) throws Exception {
		log.info("Looking up " + selectedSemester);

		String nextPage = null;

		try {
			// determmine the detail view
			lookupDetailViews();

			// if semester changed build a new course list
			if (semesterChanged) {
				weeklyViewData = weeklyViewAction.buildCourseLists(
						selectedSemester, studentSchedule);
			}

		} catch (GiException ex) {
			// if not enrolled for selected semester display message and reset
			// back to previous semester
			if (ex.getMessage().indexOf(GI_CODE_SEMESTER_NOT_ENROLLED) > -1) {
				log.error(ex.getMessage());
				statusMessages
						.add(Severity.ERROR,
						"Schedule not found for the semester you selected. Select another semester.");
				resetSemesterValues();
			} else {
				// log error for unknown code
				log.error(ex.getMessage());
			}

		} catch (Exception ex) {
			// disoplay error message and reset back to previous semester
			log.error(ex.getMessage());
			resetSemesterValues();
		}

		// if semester changed, redisplay screen and reset previous semester to
		// selected semester
		if (semesterChanged) {
			nextPage = "/secure/schedule/schedule.xhtml";
			previousSelectedSemester = selectedSemester;
		}
		return nextPage;
	}

    /**
     * User has selected a different tab.
     */
	public void onTabChange() {
		// if previous semester is null or semester changed
		if (previousSelectedSemester == null
				|| !selectedSemester.equals(previousSelectedSemester)) {
			// load schedule if necessary
			if (studentSchedule == null) {
				try {
					previousSelectedSemester = selectedSemester;
					String temp = lookUpSchedule(false);
				} catch (Exception ex) {
					log.error(ex.getMessage());
					return;
				}
			}

			// determine if which tab selected & if data needs to be loaded
			if (selectedTab == null) {
				selectedTab = DEFAULT_TAB;
			}

			// rebuild course schedule for new data if weekly view selected
			if (selectedTab.equals(TAB_WEEKLY_VIEW) && studentSchedule != null) {
				if (weeklyViewData == null) {
					weeklyViewData = weeklyViewAction.buildCourseLists(
							selectedSemester, studentSchedule);
				}
			}
		}
	}

    /**
     * Retrieve weekly view data.
     */
	private void lookupDetailViews() throws GiException,
			PsuIdNotFoundException, CodesetException, CacheValueLoadException {
		StringBuffer buf = new StringBuffer("Looking up detail view for "
				+ selectedSemester + " for ");

		Date eventStart = new Date();
		String paramType = new String();
		String paramId = new String();
		String eventId = new String();

		// determine service parameters depending on member type
		if (memberId.equalsIgnoreCase("eLionAdviser")
				|| memberId.equalsIgnoreCase("eLionParent")) {
			buf.append(passedInPsuID + " (" + passedInDigID
					+ "); Current user ID - ");
			paramType = "A";
			paramId = passedInPsuID;
			eventId = passedInDigID;
		} else {
			paramType = "S";
			paramId = "";
			eventId = "same";
		}
		buf.append(currentPerson.getDigitalId());
		log.info(buf.toString());

		// retrieve schedule data from service
		studentSchedule = studentScheduleService.getSchedule(
				currentPerson.getPsuId(), currentPerson.getDigitalId(), 
				paramType, paramId, selectedSemester);

		Date eventEnd = new Date();
		eventLogService.writeDailyEventLog(LogSubTask.DATA, eventId, eventStart,
				eventEnd, "getMFSchedule");
	}

    /**
     * Reset schedule and weekly data.
     */
	private void resetSemesterValues() {
		studentSchedule = null;
		weeklyViewData = null;
	}

    /**
     * Set needed ids.
	 *
	 * @param psu identity record
     */
	public void determineIdData(PSUIdentity psuIdentity) throws Exception {
		// set needed id data
		passedInDigID = psuIdentity.getDigitalId().toLowerCase();
		passedInPsuID = psuIdentity.getPsuId();
		memberId = memberConfig.getId();

		// id has no holds so display semester data
		if (!passedInPsuID.equalsIgnoreCase(holdPsuID)) {
			holdPsuID = psuIdentity.getPsuId();
			lookupSemesters();
		}
	}

    /**
     * Retrieve passed in psu id.
	 *
	 * @return psu id
     */
	public String getPassedInPsuID() {
		return passedInPsuID;
	}

    /**
     * Retrieve member id.
	 *
	 * @return member id
     */
	public String getMemberId() {
		return memberId;
	}

    /**
     * Retrieve passed in digital id.
	 *
	 * @return digital id
     */
	public String getPassedInDigID() {
		return passedInDigID;
	}
}