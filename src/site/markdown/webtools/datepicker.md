# Imixs DateTimePicker

Imixs JFS provides a custom component for a date-time picker widget. The widget uses the  jquery date picker component and provides an easy way to add a date/time widget into a jsf page.
 
    <i:imixsDateInput value="#{workflowController.workitem.item['datDate']}"/>

This example adds a date input widget which stores the date value into the item value 'datDate'. The time component is not editable and will be set to '00:00'. With the attribute 'showtime=true' you can add a selectbox the hour and minute. 

    <i:imixsDateInput value="#{workflowController.workitem.item['datDate']}"  showtime="true"/>

The default intervall for the minute selection is '15'. You can override this with the attribute 'minuteinterval'.
 
    <i:imixsDateInput value="#{workflowController.workitem.item['datDate']}"  showtime="true" minuteinterval="30"/>


## jQuery DatePicker
When using the imixsHeader it is also possible to use the jQuery-UI Datepicker widget native.  You just need to add the styleClass 'imixs-date' to a h:inputText. By adding a f:convertDateTime you can store the value as a java Date object. See the following example:
 
	 <h:inputText value="#{workflowController.workitem.item['_enddate']}"
					styleClass="imixs-date">
					<f:convertDateTime pattern="dd.MM.yyyy" />
				</h:inputText>


The implementation to convert a simple jsf input field into a jquery-UI Datepicker widget looks like this:
 
	$(".imixs-date", this).datepicker({
				showOtherMonths : true,
				selectOtherMonths : true,
				dateFormat : "yy-mm-dd"
			}); 

Note: The class name of the input field must set to 'imixs-date'!
 
### Date Format
There is a different in formating a date value into a string between jQuery and Java. For this reason the Java Date Format have to be converted into a jQuery date format. There for the javaScript method  'setDateFormat' can be used.  You can change the display format with the following script:
 
	 ...
	 setDateFormat("dd.MM.yyyy"); 
	 $(this).imixsLayout();
	 ...

 
As the imixsHeader component automatically converts input fields with the class 'imixs-date' into a  jQuery datePicker component you can set the dateformat as a attribute of the imixsHeader component:
 
    <i:imixsHeader disablecss="false" dateformat="dd.MM.yyyy"  />
 
Using the imixsHeader custom component will automatically convert input fields with class name 'imixs-date'
into a jQuery datepicker component.
 
Using the jQuery datepicker component typically requires that the value of  a jsf input component is also converted into a Date value using the standard convertDateTime.  Take care about the format pattern. The convertDateTime must use the same format as provided with dateformat attribute in the imixsHeader component!  Also be careful setting a locale. Translations of Month did also differ from jQuery to Java.  So it is strongly recommended to to use a short date format like 'dd.MM.yyyy'
 
	 <h:inputText value="#{workflowController.workitem.item['_enddate']}"
					styleClass="imixs-date">
					<f:convertDateTime pattern="dd.MM.yyyy" />
				</h:inputText>
 
See details about how to format a date value in jQuery and java here: 
 
 * http://docs.jquery.com/UI/Datepicker/formatDate
 * http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
 
 
 			