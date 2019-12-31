package org.imixs.workflow.engine;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Priority;
import javax.ejb.Stateless;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.util.XMLParser;

/**
 * The TextForEachAdapter can be used to format text fragments with the 'for-each' tag. The adapter
 * will iterate over the value list of a specified item.
 * 
 * <pre>
 * {@code
 <for-each item="_partid">
  Order-No: <itemvalue>_orderid</itemvalue> - Part ID: <itemvalue>_partid</itemvalue><br />
 </for-each>  
 * }
 * </pre>
 * 
 * In this example, the for-each block will be executed for each single value of the item '_partid'.
 * Within the for-each block it is possible to access the current value of the iteration as also any
 * other values of the current document. The result may look like in the following example:
 * <p>
 * 
 * <pre>
 * {@code 
 * Order-No: 111222 - Part ID: A123
 * Order-No: 111222 - Part ID: B456
 * }
 * </pre>
 * <p>
 * In case the item contains an embedded list of child ItemCollections the content of the for-each
 * block will be processed in the context for each embedded ItemCollection:
 * 
 * <pre>
 * {@code
  <for-each item="_orderitems">
    <itemvalue>_orderid</itemvalue>: <itemvalue>_price</itemvalue>
  </for-each>  
 * }
 * </pre>
 * <p>
 * The result may look like in the following example:
 * <p>
 * 
 * <pre>
 * {@code 
 * Order ID: A123: 50.55
 * Order ID: B456: 150.10
 * }
 * </pre>
 * 
 * 
 * 
 * @author rsoika
 *
 */
@Stateless
public class TextForEachAdapter {

  private static Logger logger = Logger.getLogger(AbstractPlugin.class.getName());

  @Inject
  protected Event<TextEvent> textEvents;

  /**
   * This method reacts on CDI events of the type TextEvent and parses a string for xml tag
   * <for-each>. Those tags will be replaced with the corresponding system property value.
   * <p>
   * The priority of the CDI event is set to (APPLICATION-10) to ensure that the for-each adapter is
   * triggered before the TextItemValueAdapter
   * 
   */
  @SuppressWarnings("unchecked")
  public void onEvent(@Observes @Priority(Interceptor.Priority.APPLICATION - 10) TextEvent event) {

    String text = event.getText();
    String textResult = "";
    boolean debug = logger.isLoggable(Level.FINE);

    List<String> tagList = XMLParser.findNoEmptyTags(text, "for-each");
    if (debug) {
      logger.finest("......" + tagList.size() + " tags found");
    }
    // test if a <for-each> tag exists...
    for (String tag : tagList) {
      // find the item value list...
      String itemName = XMLParser.findAttribute(tag, "item");
      String innervalue = XMLParser.findTagValue(tag, "for-each");

      // next we iterate over all item values and test for each value if the value is
      // a basic value or an embedded ItemCollection.
      List<Object> values = event.getDocument().getItemValue(itemName);
      for (Object _value : values) {
        ItemCollection _tempDoc = null;
        // test if the value defines an embedded ItemCollection....
        if (_value instanceof Map<?, ?>) {
          try {
            _tempDoc = new ItemCollection((Map<String, List<Object>>) _value);
          } catch (ClassCastException e) {
            // embedded value can not be processed
            logger.warning("unable to cast embedded map to ItemCollection!");
            continue;
          }
        } else {
          // We treat the value as a normal object and delegate the processing by firing
          // a TextEvent.
          // Here we need to create a temporary document for processing....
          _tempDoc = new ItemCollection(event.getDocument());
          // replace the for-each item value with the current iteration!
          _tempDoc.setItemValue(itemName, _value);
        }

        // now we fire a recursive text event to process the content....
        TextEvent _event = new TextEvent(new String(innervalue), _tempDoc);
        if (textEvents != null) {
          textEvents.fire(_event);
          textResult = textResult + _event.getText();
        } else {
          logger.warning("CDI Support is missing - TextEvent wil not be fired");
          // here we apply a workaround for junit tests only....
          TextItemValueAdapter tiva = new TextItemValueAdapter();
          tiva.onEvent(_event);
          textResult = textResult + _event.getText();
        }
      }

      // now replace the tag with the result string
      int iStartPos = text.indexOf(tag);
      int iEndPos = text.indexOf(tag) + tag.length();
      // now replace the tag with the result string
      text = text.substring(0, iStartPos) + textResult + text.substring(iEndPos);
    }
    event.setText(text);
  }

}
