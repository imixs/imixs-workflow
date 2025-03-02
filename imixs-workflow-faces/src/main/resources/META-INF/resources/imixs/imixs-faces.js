// default jQuery date format
var dateDisplayFormat = "yy-mm-dd";
let currentFileUploads = [];				// // Array to store selected files
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

/* This method styles input elements and imixs page elements */
$.fn.imixsLayout = function (options) {
	return this.each(function () {

		$('input:submit,input:reset,input:button,button,.button', this).button();

		// disable default layout for input
		// $('input,select,textarea', this).addClass('ui-state-default');

		$('.imixsdatatable', this).layoutImixsTable();

		$('.imixs-toggle-panel', this).layoutImixsToggelPanel();

		$(".imixs-tabs", this).tabs();

		// regional : 'de'
		$(".imixs-date", this).datepicker({
			showOtherMonths: true,
			selectOtherMonths: true,
			dateFormat: dateDisplayFormat
		});

		// initialize datetime picker widget
		$(".imixs-datetime-picker").each(
			function (index) {
				// add minute options
				addDatetimePickerMinuteOptions($(this).children(
					'.imixs-time-minute'));
				// set current date value
				setDateTimeInput($(this));
				// on change events
				$(this).children('.imixs-date').change(function () {
					getDateTimeInput($(this));
				});
				$(this).children('.imixs-time-hour').change(function () {
					getDateTimeInput($(this));
				});
				$(this).children('.imixs-time-minute').change(function () {
					getDateTimeInput($(this));
				});

			});

		//$('.imixsFileUpload').imixsLayoutFileUpload();


	});
};


$.fn.layoutImixsTable = function (options) {
	var defaults = {
		css: 'styleTable'
	};
	options = $.extend(defaults, options);

	return this.each(function () {

		input = $(this);
		input.addClass(options.css);

		input.find('tr').on('mouseover mouseout', function (event) {
			if (event.type == 'mouseover') {
				$(this).children('td').addClass('ui-state-hover');

			} else {
				$(this).children('td').removeClass('ui-state-hover');
			}
		});

		input.find('th').addClass('ui-state-default');
		input.find('td').addClass('ui-widget-content');

		input.find('td').css('font-weight', 'normal');

		input.find('tr').each(function () {
			$(this).children('td:not(:first)').addClass('first');
			$(this).children('th:not(:first)').addClass('first');
		});
	});
};

$.fn.layoutImixsToggelPanel = function (options) {

	return this.each(function () {
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
			function () {
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

$.fn.layoutImixsTooltip = function (options) {

	return this.each(function () {
		/* Imixs Tooltip support */
		// tested: flipfit none flipfit is not usefull for long content. better
		// use flip
		$(this).prev().tooltip({
			position: {
				my: "left top",
				at: "left+10 bottom",
				collision: "flip"
			},
			show: {
				duration: 800
			},
			tooltipClass: "imixs-tooltip-content",
			content: function () {
				var tooltip = $(this).next();
				tooltip.hide();
				return tooltip.html();
			}

		});
		// hide all imixs tooltips
		$(this).hide();
	});
};


$.fn.layoutImixsEditor = function (rootContext, _with, _height) {

	return this.each(function () {
		$(this).tinymce(
			{
				// Location of TinyMCE script
				script_url: rootContext + '/imixs/tinymce/jscripts/tiny_mce/tiny_mce.js',
				width: _with,
				height: _height,
				// General options
				theme: "advanced",
				plugins: "inlinepopups,fullscreen",
				// Theme options
				theme_advanced_buttons1: "cut,copy,paste,removeformat,cleanup,|,undo,redo,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,hr,bullist,numlist,",
				theme_advanced_buttons2: "formatselect,fontsizeselect,outdent,indent,blockquote,|,link,unlink,image,|,forecolor,backcolor,|,fullscreen,code",
				theme_advanced_toolbar_location: "top",
				theme_advanced_toolbar_align: "left",
				theme_advanced_statusbar_location: "bottom",
				theme_advanced_resizing: true,
				content_css: rootContext + "/imixs/tinymce/content.css"
			})
	});
};




/** Imixs file upload methods **/

/* This method initializes the imixs fileupload component */
imixsFileUploadInit = function (options) {
	const dropArea = document.querySelector('.imixsfileupload .drop-area');
	const fileInput = document.querySelector('.imixsfileupload .imixsfileuploadinput');
	
	if (!dropArea || !fileInput) {
		// no op
		return;
	}
	// Prevent default drag behaviors
	['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
		dropArea.addEventListener(eventName, preventDefaults, false);
		document.body.addEventListener(eventName, preventDefaults, false);
	});
	
	// Highlight drop area when item is dragged over it
	['dragenter', 'dragover'].forEach(eventName => {
		dropArea.addEventListener(eventName, highlight, false);
	});
	
	// Remove highlight when item leaves drop area
	['dragleave', 'drop'].forEach(eventName => {
		dropArea.addEventListener(eventName, unhighlight, false);
	});
	
	// Handle dropped files
	dropArea.addEventListener('drop', handleDrop, false);
	
	function preventDefaults(e) {
		e.preventDefault();
		e.stopPropagation();
	}
	
	function highlight() {
		dropArea.classList.add('highlight');
	}
	
	function unhighlight() {
		dropArea.classList.remove('highlight');
	}
	
	function handleDrop(e) {
		const dt = e.dataTransfer;
		const files = dt.files;
		fileInput.files = files;
		
		// Hier kannst du einen Event auslösen, falls nötig
		const event = new Event('change');
		fileInput.dispatchEvent(event);
		
	}
};

imixsFileUploadRefresh = function () {
	const fileInput = document.querySelector('.imixsfileupload .imixsfileuploadinput');
	const fileTableContainer = document.querySelector('.imixsfileupload-table');
	
	// Clear previous table content
	fileTableContainer.innerHTML = '';
	if (fileInput.files.length > 0) {
		currentFileUploads=Array.from(fileInput.files);
		var table = document.createElement('table');
		// Create table header
		var thead = document.createElement('thead');
		var headerRow = document.createElement('tr');
		var headers = ['Name', 'Size', 'Type', ' '];
		headers.forEach(function(headerText) {
			var th = document.createElement('th');
			th.textContent = headerText;
			headerRow.appendChild(th);
		});		
		thead.appendChild(headerRow);
		table.appendChild(thead);
		// Create table body
		var tbody = document.createElement('tbody');
		currentFileUploads.forEach(function(file, index) {
			var row = document.createElement('tr');
			
			// Add file name
			var nameCell = document.createElement('td');
			nameCell.textContent = file.name;
			row.appendChild(nameCell);
			
			// Add file size
			var sizeCell = document.createElement('td');
			sizeCell.textContent = fileSizeToString(file.size) + ' bytes';
			row.appendChild(sizeCell);
			
			// Add file type
			var typeCell = document.createElement('td');
			typeCell.textContent = file.type;
			row.appendChild(typeCell);
			
			// Add remove button
			// Add remove link
			var actionCell = document.createElement('td');
			var removeLink = document.createElement('a');
			removeLink.textContent = '🗙';
			removeLink.className = 'remove-link';
			removeLink.onclick = function() {
				imixsFileUploadRemoveFile(index);
			};
			actionCell.appendChild(removeLink);
			row.appendChild(actionCell);

			tbody.appendChild(row);
		});
		
		table.appendChild(tbody);
		fileTableContainer.appendChild(table);
	}
};

imixsFileUploadRemoveFile = function (index) {
	currentFileUploads.splice(index, 1);
	const fileInput = document.querySelector('.imixsfileupload .imixsfileuploadinput');
	var dataTransfer = new DataTransfer();
	currentFileUploads.forEach(file => dataTransfer.items.add(file));
	fileInput.files = dataTransfer.files;
	
	// Update the table
	imixsFileUploadRefresh();
};




function fileSizeToString(bytes) {
	if (bytes >= 1000000000) { bytes = (bytes / 1000000000).toFixed(2) + ' GB'; }
	else if (bytes >= 1000000) { bytes = (bytes / 1000000).toFixed(2) + ' MB'; }
	else if (bytes >= 1000) { bytes = (bytes / 1000).toFixed(2) + ' KB'; }
	else { bytes = bytes + ' bytes'; }
	return bytes;
}
