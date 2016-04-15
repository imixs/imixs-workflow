package org.imixs.workflow.jee.ejb;

import java.util.List;
import java.util.Map;

import javax.ejb.Remote;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;

@Remote
public interface ReportServiceRemote {

	/**
	 * Returns a Report Entity identified by the attribute txtname
	 * 
	 * @param aReportName
	 *            - name of the report
	 * @return ItemCollection representing the Report
	 * @throws Exception
	 */
	public abstract ItemCollection getReport(String aReportName);

	/**
	 * This method returns a collection of reports (ItemCollection). The method
	 * should return a subset of a collection if the start and count parameters
	 * differ form the value -1.
	 * 
	 * The method returns only ItemCollections the call has sufficient read
	 * access for.
	 */
	public abstract List<ItemCollection> getReportList(int startpos, int count);

	/**
	 * updates a Entity Report Object. The Entity representing a report must
	 * have at least the attributes : txtQuery, numMaxCount, numStartPost,
	 * txtName.
	 * 
	 * txtName is the unique key to be use to get a query.
	 * 
	 * The method checks if a report with the same key allready exists. If so
	 * this report will be updated. If no report exists the new report will be
	 * created
	 * 
	 * @param report
	 * @throws InvalidItemValueException
	 * @throws AccessDeniedException
	 * 
	 */
	public abstract void updateReport(ItemCollection aReport) throws AccessDeniedException;

	/**
	 * This method executes the JQPL statement of a Report Entity.
	 * 
	 * 
	 * @param reportName
	 *            - name of the report to be executed
	 * @return result set
	 */
	public abstract List<ItemCollection> executeReport(String aReportName, int istartPos, int imaxcount,
			Map<String, String> params, List<String> itemList);

}