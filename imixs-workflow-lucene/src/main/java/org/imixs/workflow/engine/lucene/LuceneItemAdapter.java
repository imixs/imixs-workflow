package org.imixs.workflow.engine.lucene;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import javax.inject.Named;

import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.BytesRef;
import org.imixs.workflow.engine.index.UpdateService;

/**
 * The LuceneItemAdapter is a CDI bean, providing methods to convert the value
 * of an Imixs Item into a IndexableField. This kind of lucene fields can be
 * stored in a Lucene Document.
 * <p>
 * You can overwrite the bean to provide an alternative method of value
 * conversion.
 * 
 * @see LuceneUdpateService#addItemValues
 * @author rsoika
 * @version 1.1
 */
@Named
public class LuceneItemAdapter {

	private static Logger logger = Logger.getLogger(UpdateService.class.getName());

	/**
	 * Creates a Indexable Lucene Field to be added into a Lucene document. The
	 * content of the itemValue will be converted. Depending on the parameter
	 * doAnalye a TextField or a StringField is created. The later is used for exact
	 * match.
	 * <p>
	 * The item value will not be stored into the lucene document
	 * 
	 * @param itemName
	 *            - name of the item will be used as the doc field name
	 * @param itemValue
	 *            - the item value which will be converted by the method
	 *            convertItemValue
	 * @param doAnalyze
	 *            - if true the content will by analyzed by the LuceneAnalyzer
	 *            configured in the IndexWriter
	 * @return
	 */
	public IndexableField adaptItemValue(String itemName, Object itemValue, boolean doAnalyze) {
		return adaptItemValue(itemName, itemValue, doAnalyze, Store.NO);
	}

	/**
	 * Creates a Indexable Lucene Field to be added into a Lucene document. The
	 * content of the itemValue will be converted. Depending on the parameter
	 * doAnalye a TextField or a StringField is created. The later is used for exact
	 * match.
	 * <p>
	 * The parameter 'store' indicates if Lucene should store the item value in the
	 * document
	 * <p>
	 * <code>Store.NO - Store.YES</code>
	 * 
	 * @param itemName
	 *            - name of the item will be used as the doc field name
	 * @param itemValue
	 *            - the item value which will be converted by the method
	 *            convertItemValue
	 * @param doAnalyze
	 *            - if true the content will by analyzed by the LuceneAnalyzer
	 *            configured in the IndexWriter
	 * @return
	 */
	public IndexableField adaptItemValue(String itemName, Object itemValue, boolean doAnalyze, Store stored) {
		String stringValue = convertItemValue(itemValue);
		logger.finest("......lucene add IndexField (analyzed=" + doAnalyze + "): " + itemName + "=" + stringValue);
		if (doAnalyze) {
			// just create a text field to be indexed
			return new TextField(itemName, stringValue, stored);
		} else {
			// do not analyze content. Content can be used for exact match and sortable
			// fields!
			return new StringField(itemName, stringValue, stored);
		}
	}

	/**
	 * This method converts an ItemValue into a SortedDocValuesField.
	 * 
	 * @param itemName
	 *            - name of the item will be used as the doc field name
	 * @param itemValue
	 *            - the item value which will be converted by the method
	 *            convertItemValue
	 * @return SortedDocValuesField
	 */
	public SortedDocValuesField adaptSortableItemValue(String itemName, Object itemValue) {
		String stringValue = convertItemValue(itemValue);
		logger.finest("......lucene add sortable IndexValue: " + itemName + "=" + stringValue);
		return new SortedDocValuesField(itemName, new BytesRef(stringValue));
	}

	/**
	 * This method converts a ItemValue into a indexable text format. This method
	 * currently converts only Calendar and Date objects into a Luncene string
	 * representation of a date time value
	 * 
	 * @param itemValue
	 *            - object to be converted into a string
	 * @return string value
	 */
	public String convertItemValue(Object itemValue) {
		String convertedValue = "";

		if (itemValue instanceof Calendar || itemValue instanceof Date) {
			SimpleDateFormat dateformat = new SimpleDateFormat("yyyyMMddHHmmss");

			// convert calendar to lucene string representation
			String sDateValue;
			if (itemValue instanceof Calendar) {
				sDateValue = dateformat.format(((Calendar) itemValue).getTime());
			} else {
				sDateValue = dateformat.format((Date) itemValue);
			}
			convertedValue = sDateValue;
		} else {
			// default
			convertedValue = itemValue.toString();
		}
		return convertedValue;
	}
}
