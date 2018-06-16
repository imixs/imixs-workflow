// default jQuery date format
var dateDisplayFormat = "yy-mm-dd";

/*
 * This method converts a Java Date String into the jQuery format. See:
 * http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
 * http://docs.jquery.com/UI/Datepicker/formatDate
 * 
 * As jquery and javaDate format differs from each other we need to translate
 * the JavaFormat in a jquery understandable format. Also the locale need to be
 * set!
 */
function setDateFormat(javaDate) {

	// convert yyyy to yy,
	// yy to y,
	if (javaDate.indexOf('yyyy') > -1)
		javaDate = javaDate.replace('yyyy', 'yy');
	else if (javaDate.indexOf('yy') > -1)
		javaDate = javaDate.replace('yy', 'y');

	// MMM to M,
	// MM to m,
	// M to m
	if (javaDate.indexOf('MMMM') > -1)
		javaDate = javaDate.replace('MMMM', 'MM');
	else if (javaDate.indexOf('MMM') > -1)
		javaDate = javaDate.replace('MMM', 'M');
	else if (javaDate.indexOf('MM') > -1)
		javaDate = javaDate.replace('MM', 'mm');
	else if (javaDate.indexOf('M') > -1)
		javaDate = javaDate.replace('M', 'm');

	// set jquery date format
	dateDisplayFormat = javaDate;
}

/*
 * Update the date time string for the imixs-datetime-picker....
 */
function getDateTimeInput(ainput) {
	// find parent
	datetimepicker = $(ainput).parent();
	dateTimeInput = $(datetimepicker).children('[id$=imixsdatetimeinput]');
	dateInput = $(datetimepicker).children('.imixs-date');
	hourInput = $(datetimepicker).children('.imixs-time-hour');
	minuteInput = $(datetimepicker).children('.imixs-time-minute');
	timeString = $(dateInput).val();
	if (hourInput.length && minuteInput.length) {
		timeString = timeString + " " + $(hourInput).val() + ":"
				+ $(minuteInput).val();
	} else {
		timeString = timeString + " 00:00";

	}
	$(dateTimeInput).val(timeString);
}

/*
 * Update the calender inputs for the imixs-datetime-picker
 */
function setDateTimeInput(datetimepicker) {
	dateTimeInput = $(datetimepicker).children('[id$=imixsdatetimeinput]');
	dateInput = $(datetimepicker).children('.imixs-date');
	hourInput = $(datetimepicker).children('.imixs-time-hour');
	minuteInput = $(datetimepicker).children('.imixs-time-minute');
	var timeString = $(dateTimeInput).val();
	var timeValues = timeString.split(" ");
	if (timeValues[0])
		$(dateInput).val(timeValues[0]);
	if (timeValues[1]) {
		timeValues = timeValues[1].split(":");
		if (timeValues[0] && hourInput.length)
			$(hourInput).val(timeValues[0]);
		if (timeValues[1] && minuteInput.length)
			$(minuteInput).val(timeValues[1]);
	}

}

/*
 * adds the minute options to the time-picker minute combobox depending on the
 * first two options. The second option indicates the interval
 */
function addDatetimePickerMinuteOptions(input) {
	options = $(input).children('option');
	if (options.length > 1) {
		var interval = parseInt(options[1].value);
		var minute = 0;
		// clear options
		$(input).find('option').remove();
		// rebuild option list
		while (minute < 60) {
			if (minute < 10)
				sminute = '0' + minute;
			else
				sminute = minute;
			$(input).append(
					'<option value=' + sminute + '>' + sminute + '</option>');
			minute = minute + interval;
		}

	}
}

/* This method styles input elements an imixs page elements */
$.fn.imixsLayout = function(options) {
	return this.each(function() {

		$('input:submit,input:reset,input:button,button,.button', this).button();
		
		// disable default layout for input
		// $('input,select,textarea', this).addClass('ui-state-default');

		$('.imixsdatatable', this).layoutImixsTable();

		$('.imixs-toggle-panel', this).layoutImixsToggelPanel();

		$(".imixs-tabs", this).tabs();

		// regional : 'de'
		$(".imixs-date", this).datepicker({
			showOtherMonths : true,
			selectOtherMonths : true,
			dateFormat : dateDisplayFormat
		});

		// initialize datetime picker widget
		$(".imixs-datetime-picker").each(
				function(index) {
					// add minute options
					addDatetimePickerMinuteOptions($(this).children(
							'.imixs-time-minute'));
					// set current date value
					setDateTimeInput($(this));
					// on change events
					$(this).children('.imixs-date').change(function() {
						getDateTimeInput($(this));
					});
					$(this).children('.imixs-time-hour').change(function() {
						getDateTimeInput($(this));
					});
					$(this).children('.imixs-time-minute').change(function() {
						getDateTimeInput($(this));
					});

				});
		
		$('.imixsFileUpload').imixsLayoutFileUpload();


	});
};


$.fn.layoutImixsTable = function(options) {
	var defaults = {
		css : 'styleTable'
	};
	options = $.extend(defaults, options);

	return this.each(function() {

		input = $(this);
		input.addClass(options.css);

		input.find('tr').on('mouseover mouseout', function(event) {
			if (event.type == 'mouseover') {
				$(this).children('td').addClass('ui-state-hover');

			} else {
				$(this).children('td').removeClass('ui-state-hover');
			}
		});

		input.find('th').addClass('ui-state-default');
		input.find('td').addClass('ui-widget-content');

		input.find('td').css('font-weight', 'normal');

		input.find('tr').each(function() {
			$(this).children('td:not(:first)').addClass('first');
			$(this).children('th:not(:first)').addClass('first');
		});
	});
};

$.fn.layoutImixsToggelPanel = function(options) {

	return this.each(function() {
		var togglepanels = $('h1', this);
		togglepanels.css('cursor', 'pointer');

		var anker = $('<a class="imixs-toggle-panel-link" />');
		togglepanels.wrapInner(anker);

		togglepanels.removeClass().addClass(
				'ui-state-default ui-widget-header ui-corner-all');
		var toggleIcon = $('<span></span>').addClass(
				'ui-icon ui-icon-triangle-1-e').css('float', 'left');
		toggleIcon.insertBefore('.imixs-toggle-panel-link', this);

		togglepanels.click(
				function() {
					$(this).next().toggle();

					var t1 = $('.ui-icon-triangle-1-s', this);
					var t2 = $('.ui-icon-triangle-1-e', this);
					t1.removeClass('ui-icon-triangle-1-s').addClass(
							'ui-icon-triangle-1-e');
					t2.removeClass('ui-icon-triangle-1-e').addClass(
							'ui-icon-triangle-1-s');

					return false;
				}).next().hide();

	});
};

$.fn.layoutImixsTooltip = function(options) {

	return this.each(function() {
		/* Imixs Tooltip support */
		// tested: flipfit none flipfit is not usefull for long content. better
		// use flip
		$(this).prev().tooltip({
			position : {
				my : "left top",
				at : "left+10 bottom",
				collision : "flip"
			},
			show : {
				duration : 800
			},
			tooltipClass : "imixs-tooltip-content",
			content : function() {
				var tooltip = $(this).next();
				tooltip.hide();
				return tooltip.html();
			}

		});
		// hide all imixs tooltips
		$(this).hide();
	});
};


$.fn.layoutImixsEditor = function(rootContext,_with,_height) {

	return this.each(function() {
		$(this).tinymce(
		{
			// Location of TinyMCE script
			script_url : rootContext+'/imixs/tinymce/jscripts/tiny_mce/tiny_mce.js',
			width : _with,
			height : _height,
			// General options
			theme : "advanced",
			plugins : "inlinepopups,fullscreen",
			// Theme options
			theme_advanced_buttons1 : "cut,copy,paste,removeformat,cleanup,|,undo,redo,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,hr,bullist,numlist,",
			theme_advanced_buttons2 : "formatselect,fontsizeselect,outdent,indent,blockquote,|,link,unlink,image,|,forecolor,backcolor,|,fullscreen,code",
			theme_advanced_toolbar_location : "top",
			theme_advanced_toolbar_align : "left",
			theme_advanced_statusbar_location : "bottom",
			theme_advanced_resizing : true,
			content_css : rootContext+"/imixs/tinymce/content.css"
		})
	});
};




/** jquery fileupload methods **/

/* This method initializes the imixs fileupload component */
$.fn.imixsInitFileUpload = function(options) {
	return this.each(function() {
		$('body').on('click', '#imixsFileUpload_button', function() { 
		    $('#imixsFileUpload_input').trigger('click');   
		    return false;
		});			
		
		// draganddrop fileupload
		 $('#imixsFileUpload_input').fileupload({
		        dataType: 'json',
		        done: function (e, data) {
		        	refreshFileList(data.result.files);
		        	$('#imixsFileUpload_button').blur();
		        },		
		        fail: function (e, data) {
		            alert("Unable to add file!");
		        },
		        progressall: function (e, data) {
		            var progress = parseInt(data.loaded / data.total * 100, 10);
		            if (progress==100)
		            	progress=0;
		            $('#imixsFileUpload_progress_bar').css(
		                'width',
		                progress + '%'
		            );
		        }
		    });
	}); 
};


/* This method layouts the imixs fileupload component */
$.fn.imixsLayoutFileUpload = function(options) {
	return this.each(function() {
		// hide fileupload and replace with imixsFile-Button
		$('#imixsFileUpload_input').hide();			
		$('#imixsFileUpload_button').button({
		      icons: {primary: "ui-icon-folder-open"}
		});
		$('.imixsFileUpload_delete','.imixsFileUpload_uploadlist').button({
		      icons: {primary: "ui-icon-close"}
		});
	}); 
};



function refreshFileList(files) {		
	// remove uploded file info form table
	$('.imixsFileUpload_uploaded_file').remove();
	$.each(files, function (index, file) {
		var fileLink='<a href="'+file.url+'" target="_blank" >'+file.name+'</a>';
        var cancelButton='<button class="imixsFileUpload_delete" onclick="cancelFileUpload(\''+file.name + '\');return false;">Delete</button>';
        var row='<tr class="imixsFileUpload_uploaded_file"><td class="imixsFileUpload_uploadlist_name">'+fileLink+'</td><td class="imixsFileUpload_uploadlist_size">'+fileSizeToString(file.size)+'</td><td class="imixsFileUpload_uploadlist_cancel">'+cancelButton+'</td></tr>';
        $('.imixsFileUpload_uploadlist').append(row);
    });
	$('button','.imixsFileUpload_uploadlist').button({
	      icons: {primary: "ui-icon-close"}
	});
}


/**
 * reloads the uploaded files and refresh the filelist
 */
function updateFileUpload() {	
	// upload url
	var base_url=$('#imixsFileUpload_input').attr( 'data-url' );	
	$.ajax({url:base_url,
		type: 'GET',
		dataType: "json",
		success:function(data){
			refreshFileList(data.files);
		}			
	});			
}

function cancelFileUpload(file) {	
	// upload url
	var base_url=$('#imixsFileUpload_input').attr( 'data-url' );	
	
	var cidPos=base_url.indexOf("?cid=");
	
	var target_url=base_url.substring(0,cidPos) + file + base_url.substring(cidPos);
	
	$.ajax({url:target_url,
		type: 'DELETE',
		dataType: "json",
		success:function(data){
			refreshFileList(data.files);
		}			
	});			
}

function fileSizeToString(bytes) {
	if (bytes>=1000000000) {bytes=(bytes/1000000000).toFixed(2)+' GB';}
        else if (bytes>=1000000)    {bytes=(bytes/1000000).toFixed(2)+' MB';}
        else if (bytes>=1000)      {bytes=(bytes/1000).toFixed(2)+' KB';}
        else {bytes=bytes+' bytes';}
    return bytes;
}
