package edu.psu.elion.schedule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

import edu.psu.ais.codeset.PSUCodeset;
import edu.psu.ais.codeset.PSUCodesetRecord;
import edu.psu.elion.common.Course;
import edu.psu.elion.domain.data.college.College;
import edu.psu.elion.domain.data.majorNames.MajorNamesService;
import edu.psu.nexus.cache.CacheValueLoadException;
import edu.psu.nexus.codeset.CodesetException;
import edu.psu.nexus.codeset.CodesetService;
import edu.psu.nexus.gi.GiClient;
import edu.psu.nexus.gi.GiException;
import edu.psu.nexus.util.ConvertUtil;

/**
 *
 * This file contains the service calls to retrieve the schedule data.
 *
 * @author MJBoldin <mjb160@psu.edu>
 * @since  2010-02-13
 */

@Name("elion.studentScheduleService")
@AutoCreate
public class StudentScheduleService implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final String SERVICE_NAME_GET_SCHEDULE = "getStudentSchedule";

	@In("nexus.gi.client")
	GiClient giClient;

	@In("nexus.codesetService")
	CodesetService codesetService;

	@In("elion.codeset.campusLocations")
	PSUCodeset campusLocations;

	@In("student.collegeCodeMap")
	Map<String, College> collegeCodeMap;

	@In("student.majorNamesService")
	MajorNamesService majorNamesService;

	@Logger
	private Log log;

    /**
     * Looks up schedule data for a particular semester.
	 *
	 * @param psuId current psu id
	 * @param digitalId current digital id
	 * @param paramType current member type code
	 * @param paramId student psu id
	 * @param selectedSemester selected semester
	 * @return semester schedule data
     */
	public StudentSchedule getSchedule(String psuId, String digitalId,
			String paramType, String paramId, String selectedSemester)
			throws GiException, CodesetException, CacheValueLoadException {
		// build service parameter list
		List<Object> paramList = new Vector<Object>();
		paramList.add(psuId);
		paramList.add(digitalId);
		paramList.add(paramType);
		paramList.add(paramId);
		paramList.add(selectedSemester);

		// call gi service to get semester data
		Vector<Object> response = (Vector<Object>) giClient.callGi(
				SERVICE_NAME_GET_SCHEDULE, paramList);

		// get the return code and message
		String returnCode = (String) response.get(0);
		String returnMessage = (String) response.get(1);

		// in valid return
		if (!returnCode.equals("0000")) {
			throw new GiException(returnCode + ": " + returnMessage);
		}

		ConvertUtil convert = ConvertUtil.getInstance();
		StudentSchedule studentSchedule = new StudentSchedule();

		// add retrieved data into student schedule class
		// -- semester data
		studentSchedule.setSemester8(convert.trimString((String) response
				.get(7)));
		studentSchedule.setSemesterStanding(convert
				.trimString((String) response.get(8)));

		// -- campus data
		String campus = convert.trimString((String) response.get(9));
		PSUCodesetRecord campusRecord = campusLocations
				.getCodesetRecord(campus);
		studentSchedule.setCampus(campusRecord);

		// -- college major data
		String college = convert.trimString((String) response.get(10));
		studentSchedule.setCollege(collegeCodeMap.get(college));
		String major = convert.trimString((String) response.get(11));
		studentSchedule.setMajor(majorNamesService.getMajorNames(major));
		studentSchedule
				.setExamNum(convert.trimString((String) response.get(12)));

		// -- account messages data
		Integer msgCount = new Integer((String) response.get(13));
		List<String> messages = (List<String>) response.get(14);
		for (int i = 0; i < msgCount; i++) {
			if (studentSchedule.getMessages() == null) {
				studentSchedule.setMessages(new ArrayList<String>());
			}
			String message = convert.trimString(messages.get(i));
			studentSchedule.getMessages().add(message);
		}

		// -- schedule hour data
		studentSchedule.setTotalHours(convert.trimString((String) response
				.get(15)));

		studentSchedule.setLateDropCreditsRemaining(convert
				.trimString((String) response.get(34)));

		// -- parse service scheduled course data
		Integer numberOfRecords = new Integer((String) response.get(16));

		List<String> ids = (List<String>) response.get(17);
		List<String> departments = (List<String>) response.get(18);
		List<String> numbers = (List<String>) response.get(19);
		List<String> sectionNumbers = (List<String>) response.get(20);
		List<String> creditHours = (List<String>) response.get(21);
		List<String> honors = (List<String>) response.get(22);
		List<String> days = (List<String>) response.get(23);
		List<String> times = (List<String>) response.get(24);
		List<String> locations = (List<String>) response.get(25);
		List<String> beginDates = (List<String>) response.get(26);
		List<String> endDates = (List<String>) response.get(27);
		List<String> professors = (List<String>) response.get(28);
		List<String> titles = (List<String>) response.get(29);
		List<String> dropDates = (List<String>) response.get(30);
		List<String> lateDrops = (List<String>) response.get(31);
		List<String> partials = (List<String>) response.get(32);
		List<String> campuses = (List<String>) response.get(33);
		List<String> deliveries = (List<String>) response.get(35);

		Course currentCourse = null;
		for (int i = 0; i < numberOfRecords; i++) {
			// data per course
			String courseId = convert.trimString(ids.get(i));

			// course data is not null and id exists
			if (courseId != null && courseId.length() > 0) {
				// build a new course
				Course course = new Course();
				currentCourse = course;
				if (studentSchedule.getCourses() == null) {
					studentSchedule.setCourses(new ArrayList<Course>());
				}
				studentSchedule.getCourses().add(course);

				// course id data
				course.setId(courseId);
				course.setDepartment(convert.trimString(departments.get(i)));
				course.setNumber(convert.trimString(numbers.get(i)));
				course.setSectionNumber(convert.trimString(sectionNumbers
						.get(i)));
				course.setCreditHours(convert.trimString(creditHours.get(i)));

				// honors course?
				String tempHonors = honors.get(i);
				course.setHonors(Boolean.FALSE);
				if (tempHonors != null && tempHonors.equalsIgnoreCase("Y")) {
					course.setHonors(Boolean.TRUE);
				}

				// course duration/location/times
				course.getDay().add((String) convert.trimString(days.get(i)));
				course.getTime().add((String) convert.trimString(times.get(i)));
				course.getLocation().add(
						(String) convert.trimString(locations.get(i)));

				course.setBeginDate((String) convert.trimString(beginDates
						.get(i)));
				course.setEndDate((String) convert.trimString(endDates.get(i)));
				course.setProfessor((String) convert.trimString(professors
						.get(i)));
				course.setTitle((String) convert.trimString(titles.get(i)));
				course.setDropDate((String) convert.trimString(dropDates.get(i)));
				course.setLateDate((String) convert.trimString(lateDrops.get(i)));

				// partial course?
				String tempPartial = partials.get(i);
				course.setPartial(Boolean.FALSE);
				if (tempPartial != null && tempPartial.equalsIgnoreCase("Y")) {
					course.setPartial(Boolean.TRUE);
				}

				// course campus
				course.setCampus((String) convert.trimString(campuses.get(i)));
				course.setDelivery((String) convert.trimString(deliveries
						.get(i)));

			} else {
				// course data is null orand id doesn't exist
				currentCourse.getDay().add(
						(String) convert.trimString(days.get(i)));
				currentCourse.getTime().add(
						(String) convert.trimString(times.get(i)));
				currentCourse.getLocation().add(
						(String) convert.trimString(locations.get(i)));
			}
		}

		return studentSchedule;
	}
}
