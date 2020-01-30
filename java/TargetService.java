package mil.smil.jiatfs.jems.model.data.nodeServices.target;

import mil.smil.jiatfs.jems.model.data.nodeServices.NodeServiceListResponse;
import mil.smil.jiatfs.jems.model.data.nodeServices.NodeServiceObjectResponse;
import mil.smil.jiatfs.jems.model.data.nodeServices.NodeServicesUtil;
import mil.smil.jiatfs.jems.model.data.nodeServices.picklist.PickList;
import mil.smil.jiatfs.jems.model.data.nodeServices.picklist.PicklistExternalService;
import mil.smil.jiatfs.jems.view.util.PropertiesUtil;
import org.apache.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.*;

/**
 *
 * This file calls the target service to retrieve target data.
 *
 * @author MJBoldin <raidersmj@yahoo.com>
 * @since  2018-03-31
 */

public class TargetService implements Serializable {

    private static final long serialVersionUID = 4573252461L;
    private static final Logger LOG = Logger.getLogger(TargetService.class);

    private String targetUrl;
    private HttpHeaders headers;

    /**
     * Target service class.
     */
    public TargetService() {
        initialize();
    }

    /**
     * Initializes needed node service values.
     */
    private void initialize() {
        // initializes node service util 
        HashMap<String, Object> serviceData = NodeServicesUtil.initialize(
            PropertiesUtil.SERVICES);

        // retrieve node service util headers
        headers = (HttpHeaders) serviceData.get(NodeServicesUtil.HEADERS);

        // get the target service property
        targetUrl = ((Properties) serviceData.get(NodeServicesUtil.PROPERTIES))
            .getProperty("target.service.url");
    }

    /**
     * Retrieves current target service from the faces context.
     *
     * @return current target service reference.
     */
    public static TargetService getService() {
        FacesContext context = FacesContext.getCurrentInstance();
        return (TargetService) context.getApplication().evaluateExpressionGet(
            context, "#{targetServiceNH}", TargetService.class);
    }

    /**
     * ****************************************
     * Organization Targets
     * ****************************************
     */

    /**
     * Retrieves all organizations from the database.
     *
     * @return list of organizations
     */
    public List<Organization> getAllOrganizations() {
        // service endpoint url
        String url = targetUrl + "/organizationManagement?filter={filter}";

        // generate filter to only return organizations of type dto/tco,
        // unknown, and criminal
        String filter = "{\"where\": {\"or\": [{\"type\":\"DTO/TCO\"}," +
            "{\"type\":\"Unknown\"},{\"type\":\"Criminal\"}]}}";
        return getOrgs(url, filter);
    }

    /**
     * Retrieves a list of organizations from the database based on ids.
     *
     * @param orgIds url of the target service
     * @return list of organizations
     */
    public List<Organization> getOrganizationsById(List<Long> orgIds) {
        // generate filter to only return organizations with speicif ids
        StringBuffer sb = new StringBuffer("{\"where\": {\"id\":{\"inq\": [");
        int i = 0;
        for (Long item: orgIds) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(item.toString());
            i++;
        }
        sb.append("]}}}");

        // service endpoint url
        String url = targetUrl + "/organizationManagement?filter={filter}";

        // get specific organizations
        return getOrgs(url, sb.toString());
    }

    /**
     * Retrieves organizations from the database based on the url and the 
     * filter.
     *
     * @param url url of the target service
     * @param filter filter parameters for the target service
     * @return list of organizations
     */
    private List<Organization> getOrgs(String url, String filter) {
        // create response object
        NodeServiceListResponse<Organization> response = 
            new NodeServiceListResponse<Organization>(Organization.class);
        List<Organization> orgList;

        try {
            // create http entity class with common headers
            HttpEntity entity = new HttpEntity(headers);

            // retrieve the response from the node service call
            response = NodeServicesUtil.getNodeListResponse(url, entity, 
                HttpMethod.GET, response, filter);
            
            // retrieve data from the response object
            orgList = response.getConvertedData();

        // catch and log all errors from the service
        } catch (Exception e) {
            // error getting data
            orgList = new ArrayList<Organization>();
            LOG.error("There was an error getting organizations " + e);
        }

        return orgList;
    }

    /**
     * ****************************************
     * Vessel Targets
     * ****************************************
     */

    /**
     * Retrieves a list of alpha name suggestions from the target service based
     * upon text entered by the user.
     *
     * @param name user text entered in the alpha field
     * @return list of alpha name suggestions
     */
    public List<String> getAlphaVesselNameSuggest(String name) {
        List<String> alphaNameList = new ArrayList<String>();
        
        try {
            // create http entity class with common headers
            HttpEntity entity = new HttpEntity(headers);
            NodeServiceListResponse<AlphaVessel> response = new NodeServiceListResponse<AlphaVessel>(
                    AlphaVessel.class);

            // retrieve the response from the node service call
            response = NodeServicesUtil.getNodeListResponse(targetUrl + "vessel/filterAlphas?q=" + name,
                    entity, HttpMethod.GET, response, null);

            // retrieve data from the response object
            List<AlphaVessel> vslList = response.getConvertedData();

            // remove duplicates from result set
            for (AlphaVessel vsl : vslList) {
                if (!alphaNameList.contains(vsl.getName())) {
                    alphaNameList.add(vsl.getName());
                }
            }

            // sort the results set
            Collections.sort(alphaNameList, String.CASE_INSENSITIVE_ORDER);

        // catch and log all errors from the service
        } catch (Exception e) {
            // error getting data
            LOG.error("There was an error getting alpha vessel names: " + e);
        }

        return alphaNameList;
    }
}